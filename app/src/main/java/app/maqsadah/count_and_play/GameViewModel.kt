package app.maqsadah.count_and_play

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Screen { START, MENU, GAME }

/** GUIDED = level-based Play; FREE = the classic pick-your-numbers flow; QUIZ = watch & answer. */
enum class Mode { GUIDED, FREE, QUIZ }

/**
 * What the child's taps currently do. Tap-to-count is the core of v3: the app
 * never counts FOR the child during learning — the child taps each object,
 * hears its number, and a persistent badge marks it as counted.
 */
enum class Phase {
    NONE,        // auto presentation / transitions (taps ignored)
    COUNT_A,     // tap-count the first group
    COUNT_B,     // tap-count the second group (addition)
    MERGE,       // waiting for the 🧺 put-together button
    COUNT_ALL,   // tap-count the merged set
    TAKE_AWAY,   // tap b items to turn them into ghost holes
    COUNT_LEFT,  // tap-count what remains
    ANSWER       // quiz: optional tap-counting while choosing an answer
}

/** One emoji on stage. */
class Item(val id: Int, val emoji: String, val group: Char) {
    /** Ghost hole: stays in its grid slot, rendered faded — never removed. */
    var ghost by mutableStateOf(false)
    var tappable by mutableStateOf(false)
    var pulseTick by mutableStateOf(0)
    var groupPulseTick by mutableStateOf(0)
    /** Transient bubble for auto-counting (quiz watch / recount together). */
    var bubble by mutableStateOf<Int?>(null)
    /** Persistent badge from the child's own tap-count (one-to-one correspondence). */
    var badge by mutableStateOf<Int?>(null)
    var bubbleTick = 0
    /** true once the drop-in animation has played (so moving zones doesn't replay it) */
    var dropped = false
}

/** A piece of the equation line. */
data class EqPart(val text: String, val kind: Int) {
    companion object {
        const val NORMAL = 0
        const val Q = 1      // orange "?"
        const val GHOST = 2  // pale preview number
    }
}

data class Problem(val op: String, val a: Int, val b: Int, val answer: Int)

/**
 * All game state + sequencing for v3.
 *
 * Round flow (guided + free addition):
 *   drop group A → COUNT_A (child taps 1..a) → drop group B → COUNT_B (1..b)
 *   → MERGE (🧺 button moves B into A, 90 ms/item) → COUNT_ALL (1..a+b)
 *   → praise + confetti + star.
 * Round flow (guided + free subtraction):
 *   drop a items → COUNT_A (1..a) → TAKE_AWAY (taps make b ghost holes in
 *   place, voice counts the take-aways) → COUNT_LEFT (1..a−b) → praise.
 * Quiz keeps the animated auto-counted watch phase, then ANSWER lets the
 * child tap-count the objects (with voice) before picking from 3 buttons.
 *
 * Progress (level + stars) persists in SharedPreferences; 5 stars in a level
 * raises the cap (L1 ≤3, L2 ≤5, L3 ≤10, L4 ≤20); at L4 stars just accumulate.
 *
 * Lives in a [ViewModel]: state and running sequences survive config changes,
 * coroutines run on [viewModelScope], and the [Speaker] is built from the
 * Application context — never an Activity — and released in [onCleared].
 */
class GameViewModel(
    private val app: Application,
    val speaker: Speaker,
    initialLang: Lang,
    initialLangSet: Boolean
) : ViewModel() {

    private val prefs = app.getSharedPreferences(G.PREFS, Context.MODE_PRIVATE)

    /* ---------- language ---------- */

    /** Active UI + narration language; persisted. Drives [L] and the TTS locale. */
    var lang by mutableStateOf(initialLang)
        private set

    /** The active language's string pack. Reading [lang] (via [L]) makes Compose
     *  recompose every c.L.…() label live when the language changes. */
    val L: LangPack
        get() = lang.pack

    /** false until a language has been chosen — gates the first-launch picker. */
    var langSet by mutableStateOf(initialLangSet)
        private set

    /** Switches the app language (grown-ups settings or first-launch picker). */
    fun setLanguage(newLang: Lang) {
        lang = newLang
        langSet = true
        prefs.edit()
            .putString(G.KEY_LANG, newLang.name)
            .putBoolean(G.KEY_LANG_SET, true)
            .apply()
        speaker.setLanguage(newLang)
        // A round in progress holds already-built (old-language) prompt/equation
        // text; drop back to the menu so everything rebuilds in the new language.
        if (screen == Screen.GAME) showMenu()
    }

    var screen by mutableStateOf(Screen.START)
    var mode by mutableStateOf<Mode?>(null)
    var phase by mutableStateOf(Phase.NONE)
    var busy by mutableStateOf(false)
        private set

    /* ---------- persisted progress ---------- */

    var level by mutableStateOf(prefs.getInt(G.KEY_LEVEL, 1).coerceIn(1, G.LEVEL_CAPS.size))
    var starsInLevel by mutableStateOf(prefs.getInt(G.KEY_STARS, 0).coerceAtLeast(0))
    var levelUpVisible by mutableStateOf(false)
    var settingsVisible by mutableStateOf(false)

    // header (quiz session stars, shown "as today")
    var correctCount by mutableStateOf(0)
        private set

    // equation + prompt
    var equation by mutableStateOf(listOf<EqPart>())
    var prompt by mutableStateOf("")

    // stage
    val plateA = mutableStateListOf<Item>()
    val plateB = mutableStateListOf<Item>()
    val goneItems = mutableStateListOf<Item>()   // quiz-mode "went away" ghosts
    var plateBVisible by mutableStateOf(false)
    var goneVisible by mutableStateOf(false)
    var goneLabel by mutableStateOf("went away")
    // How many items each zone will hold this round — drives the auto-fit sizing
    // in the UI (bigger objects, guaranteed to fit even at 20).
    var capA by mutableStateOf(1)
    var capB by mutableStateOf(1)
    var capGone by mutableStateOf(1)

    // picker (free play)
    var pickerVisible by mutableStateOf(false)
    var pickerTitle by mutableStateOf("")
    var gridVisible by mutableStateOf(false)
    var gridEnabledMax by mutableStateOf(G.MAX_N)
    var opRowVisible by mutableStateOf(false)
    var plusEnabled by mutableStateOf(true)
    var minusEnabled by mutableStateOf(true)
    var diceVisible by mutableStateOf(false)

    // action buttons
    var showMerge by mutableStateOf(false)
    var showAgain by mutableStateOf(false)
    var showNew by mutableStateOf(false)

    // quiz answers
    val answers = mutableStateListOf<Int>()
    var ansCorrectIdx by mutableStateOf(-1)
    var ansWrongIdx by mutableStateOf(-1)
    var ansWrongTick by mutableStateOf(0)
    var dimOthers by mutableStateOf(false)

    var confettiTick by mutableStateOf(0)
        private set

    private var nextId = 1
    private var job: Job? = null
    private var idleJob: Job? = null
    private var idleHint = ""

    // problem state
    private var la = 0
    private var lop = "+"
    private var lb = 0
    private var pickingSecond = false
    private var round = 0
    private var current: Problem? = null
    private var tapCount = 0
    private var tapTarget = 0
    private var itemName = "things"

    /* ---------- helpers ---------- */

    private fun part(s: String) = EqPart(s, EqPart.NORMAL)
    private fun qPart(s: String) = EqPart(s, EqPart.Q)
    private fun ghostPart(s: String) = EqPart(s, EqPart.GHOST)

    private fun eqFull(a: Int, op: String, b: Int, result: Int?) {
        equation = if (result == null) {
            listOf(part("${L.digits(a)} $op ${L.digits(b)} = "), qPart("?"))
        } else {
            listOf(part("${L.digits(a)} $op ${L.digits(b)} = ${L.digits(result)}"))
        }
    }

    private fun newItem(emoji: String, group: Char) = Item(nextId++, emoji, group)

    private fun showBubble(item: Item, num: Int) {
        item.bubble = num
        item.bubbleTick++
        val tick = item.bubbleTick
        viewModelScope.launch {
            delay(1400)
            if (item.bubbleTick == tick) item.bubble = null
        }
    }

    /** Auto-count used ONLY in quiz watch/recount — learning phases are child-tapped. */
    private suspend fun countItem(item: Item, num: Int) {
        item.pulseTick++
        showBubble(item, num)
        speaker.speak(L.word(num))
        delay(100)
    }

    private fun clearStage() {
        plateA.clear()
        plateB.clear()
        goneItems.clear()
        plateBVisible = false
        goneVisible = false
        prompt = ""
    }

    private fun hideActionBtns() {
        showMerge = false
        showAgain = false
        showNew = false
    }

    private fun hideAnswers() {
        answers.clear()
        ansCorrectIdx = -1
        ansWrongIdx = -1
        dimOthers = false
    }

    private fun confettiBurst() {
        confettiTick++
    }

    private fun sayAsync(text: String) {
        viewModelScope.launch { speaker.speak(text) }
    }

    private fun launchSeq(block: suspend () -> Unit) {
        job?.cancel()
        job = viewModelScope.launch { block() }
    }

    /* ---------- idle hint: nudge after ~8 s without a tap ---------- */

    private fun armIdleHint(hint: String) {
        idleHint = hint
        idleJob?.cancel()
        idleJob = viewModelScope.launch {
            while (true) {
                delay(8000)
                (plateA + plateB).filter { it.tappable && !it.ghost }
                    .forEach { it.groupPulseTick++ }
                speaker.speak(idleHint)
            }
        }
    }

    private fun resetIdleTimer() {
        if (idleJob != null) armIdleHint(idleHint)
    }

    private fun stopIdleHint() {
        idleJob?.cancel()
        idleJob = null
    }

    private fun enterPhase(p: Phase, hint: String) {
        phase = p
        if (p == Phase.NONE) stopIdleHint() else armIdleHint(hint)
    }

    /* ---------- progress (persisted) ---------- */

    fun levelCap(): Int = G.LEVEL_CAPS[(level - 1).coerceIn(0, G.LEVEL_CAPS.size - 1)]

    private fun saveProgress() {
        prefs.edit().putInt(G.KEY_LEVEL, level).putInt(G.KEY_STARS, starsInLevel).apply()
    }

    fun resetProgress() {
        level = 1
        starsInLevel = 0
        saveProgress()
    }

    private fun awardStar() {
        starsInLevel++
        if (level < G.LEVEL_CAPS.size && starsInLevel >= G.STARS_PER_LEVEL) {
            level++
            starsInLevel = 0
            saveProgress()
            levelUpVisible = true
            confettiBurst()
            sayAsync(L.levelUp(levelCap()))
            viewModelScope.launch {
                delay(3600)
                levelUpVisible = false
            }
        } else {
            saveProgress()
        }
    }

    /* ---------- grown-ups settings ---------- */

    fun openSettings() { settingsVisible = true }
    fun closeSettings() { settingsVisible = false }

    fun previewVoice(name: String?) {
        speaker.selectVoice(name)
        sayAsync(L.voicePreview())
    }

    fun setSlowRate(slow: Boolean) {
        speaker.applySlowRate(slow)
        sayAsync(if (slow) L.slowVoice() else L.normalVoice())
    }

    /* ============================================================
       TAP-TO-COUNT — the core interaction
       ============================================================ */

    fun onItemTap(itm: Item) {
        if (busy || !itm.tappable || itm.ghost) return
        when (phase) {
            Phase.COUNT_A, Phase.COUNT_B, Phase.COUNT_ALL, Phase.COUNT_LEFT, Phase.ANSWER ->
                countTap(itm)
            Phase.TAKE_AWAY -> takeAwayTap(itm)
            else -> {}
        }
    }

    /** Child taps an uncounted object: pulse, persistent badge, voice its number. */
    private fun countTap(itm: Item) {
        if (phase != Phase.ANSWER && tapCount >= tapTarget) return
        tapCount++
        itm.tappable = false
        itm.badge = tapCount
        itm.pulseTick++
        resetIdleTimer()
        val n = tapCount
        val done = phase != Phase.ANSWER && n >= tapTarget
        if (done) busy = true      // block further taps during the transition
        launchSeq {
            speaker.speak(L.word(n))
            if (done) onPhaseComplete()
        }
    }

    /** Subtraction: a tap turns the item into a ghost hole that keeps its grid slot. */
    private fun takeAwayTap(itm: Item) {
        if (tapCount >= tapTarget) return
        tapCount++
        itm.tappable = false
        itm.ghost = true
        itm.badge = null
        itm.pulseTick++
        resetIdleTimer()
        val n = tapCount
        val done = n >= tapTarget
        if (done) busy = true
        launchSeq {
            speaker.speak(L.word(n))
            if (done) {
                stopIdleHint()
                delay(350)
                val remaining = plateA.filter { !it.ghost }
                if (remaining.isEmpty()) {
                    // everything is gone — nothing left to count
                    finishRound()
                } else {
                    prompt = L.howManyLeftPrompt()
                    speaker.speak(L.howManyLeftCount())
                    remaining.forEach { it.badge = null; it.tappable = true }
                    tapCount = 0
                    tapTarget = remaining.size
                    enterPhase(Phase.COUNT_LEFT, L.countTheItems(itemName))
                    busy = false
                }
            }
        }
    }

    /** A count target was reached — move the round to its next phase. */
    private suspend fun onPhaseComplete() {
        busy = true
        stopIdleHint()
        when (phase) {
            Phase.COUNT_A -> if (lop == "+") {
                // group A counted — bring on group B, counted from 1 again
                speaker.speak(L.countExclaim(la, itemName))
                val em = plateA.firstOrNull()?.emoji ?: G.ITEMS[0]
                repeat(lb) {
                    plateB.add(newItem(em, 'B'))
                    delay(110)
                }
                prompt = L.tapAndCountPrompt()
                speaker.speak(L.andMore(lb))
                plateB.forEach { it.tappable = true }
                tapCount = 0
                tapTarget = lb
                enterPhase(Phase.COUNT_B, L.tapTheItems(itemName))
                busy = false
            } else {
                // counted them all — now take b away
                delay(300)
                prompt = L.takeAwayPrompt(lb)
                speaker.speak(L.takeAwaySpeak(lb, itemName))
                plateA.forEach { if (!it.ghost) it.tappable = true }
                tapCount = 0
                tapTarget = lb
                enterPhase(Phase.TAKE_AWAY, L.takeAwayHint(lb, itemName))
                busy = false
            }
            Phase.COUNT_B -> {
                delay(250)
                prompt = L.putTogetherPrompt()
                speaker.speak(L.putTogetherSpeak(la, lb))
                showMerge = true
                enterPhase(Phase.MERGE, L.tapTheBasket())
                busy = false
            }
            Phase.COUNT_ALL, Phase.COUNT_LEFT -> finishRound()
            else -> busy = false
        }
    }

    fun onMerge() {
        if (busy || phase != Phase.MERGE) return
        busy = true
        stopIdleHint()
        showMerge = false
        launchSeq { doMerge() }
    }

    private suspend fun doMerge() {
        val sum = la + lb

        // move group B items into plate A (90 ms/item, as before)
        capA = sum
        while (plateB.isNotEmpty()) {
            val itm = plateB.removeAt(0)
            itm.badge = null
            plateA.add(itm)
            delay(90)
        }
        plateBVisible = false

        // the merged set is counted fresh, from one
        plateA.forEach { it.badge = null; it.tappable = true }
        tapCount = 0
        tapTarget = sum
        prompt = L.countAllPrompt()
        speaker.speak(L.countAllSpeak())
        enterPhase(Phase.COUNT_ALL, L.countTheItems(itemName))
        busy = false
    }

    private suspend fun finishRound() {
        busy = true
        enterPhase(Phase.NONE, "")
        val result = if (lop == "+") la + lb else la - lb
        eqFull(la, lop, lb, result)
        confettiBurst()
        if (lop == "+") {
            prompt = L.plusResult(la, lb, result)
            speaker.speak(prompt)
        } else if (result == 0) {
            prompt = L.zeroPrompt()
            speaker.speak(L.minusZeroSpeak(la, lb))
        } else {
            prompt = L.minusResult(la, lb, result)
            speaker.speak(prompt)
        }
        awardStar()
        if (mode == Mode.GUIDED) {
            delay(1400)
            round++
            guidedRound()
        } else {
            showAgain = true
            showNew = true
            busy = false
        }
    }

    /* ============================================================
       GUIDED + FREE — acting out a problem with tap-to-count
       ============================================================ */

    private suspend fun guidedRound() {
        val p = G.guidedProblem(round, levelCap())
        current = p
        actProblem(p)
    }

    /** Shared guided/free setup: drop group A, then hand counting to the child. */
    private suspend fun actProblem(p: Problem) {
        busy = true
        enterPhase(Phase.NONE, "")
        clearStage()
        hideActionBtns()
        la = p.a
        lb = p.b
        lop = p.op
        val em = G.ITEMS[G.rand(G.ITEMS.size)]
        itemName = L.itemName(em)

        equation = listOf(part("${L.digits(p.a)} ${p.op} ${L.digits(p.b)} = "), qPart("?"))
        capA = p.a
        if (p.op == "+") {
            capB = p.b
            plateBVisible = true
        }

        // group A drops in — NO auto-counting; the child will tap-count
        repeat(p.a) {
            plateA.add(newItem(em, 'A'))
            delay(110)
        }

        prompt = L.tapAndCountPrompt()
        speaker.speak(L.tapAndCountItem(itemName))
        plateA.forEach { it.tappable = true }
        tapCount = 0
        tapTarget = p.a
        enterPhase(Phase.COUNT_A, L.tapTheItems(itemName))
        busy = false
    }

    /* ============================================================
       FREE PLAY — pick numbers, then the same tap-to-count acting
       ============================================================ */

    fun showPicker() {
        clearStage()
        hideActionBtns()
        pickingSecond = false
        pickerVisible = true
        diceVisible = true
        opRowVisible = false
        gridVisible = true
        gridEnabledMax = G.MAX_N
        pickerTitle = L.pickNumber()
        equation = listOf(ghostPart("?"))
        sayAsync(L.pickNumber())
    }

    fun onPickFirst(n: Int) {
        la = n
        sayAsync(L.word(n))
        equation = listOf(part(L.digits(n)))
        gridVisible = false
        diceVisible = false
        opRowVisible = true
        plusEnabled = n < G.MAX_N     // 20 + anything exceeds the cap
        minusEnabled = n >= 1
        pickerTitle = L.plusOrMinus()
    }

    fun onPickOp(op: String) {
        lop = op
        sayAsync(if (op == "+") L.plusWord() else L.minusWord())
        equation = listOf(part("${L.digits(la)} $op "), ghostPart("?"))
        opRowVisible = false
        gridVisible = true
        pickerTitle = L.pickAnother()
        gridEnabledMax = if (op == "+") G.MAX_N - la else la
        pickingSecond = true
    }

    /** Routes a number-grid tap to the right step of the picker. */
    fun onPickNumber(n: Int) {
        if (pickingSecond) onPickSecond(n) else onPickFirst(n)
    }

    fun onPickSecond(n: Int) {
        lb = n
        sayAsync(L.word(n))
        startActing()
    }

    fun diceRoll() {
        val p = G.diceProblem()
        la = p.a
        lb = p.b
        lop = p.op
        startActing()
    }

    fun startActing() {
        pickerVisible = false
        diceVisible = false
        hideActionBtns()
        val p = Problem(lop, la, lb, if (lop == "+") la + lb else la - lb)
        current = p
        launchSeq { actProblem(p) }
    }

    fun onAgain() {
        if (mode != Mode.FREE || busy) return
        startActing()
    }

    fun onNew() {
        if (mode != Mode.FREE || busy) return
        showPicker()
    }

    /* ============================================================
       QUIZ MODE — watch, (optionally tap-count), answer
       ============================================================ */

    private fun showAnswers(correct: Int, maxN: Int) {
        answers.clear()
        answers.addAll(G.answerOptions(correct, maxN))
        ansCorrectIdx = -1
        ansWrongIdx = -1
        dimOthers = false
    }

    private suspend fun playRound() {
        busy = true
        enterPhase(Phase.NONE, "")
        hideAnswers()
        clearStage()
        equation = emptyList()

        val p = G.guidedProblem(round, levelCap())
        current = p
        val emoji = G.ITEMS[G.rand(G.ITEMS.size)]
        itemName = L.itemName(emoji)
        capA = if (p.op == "+") p.a + p.b else p.a
        capGone = p.b

        delay(400)

        if (p.op == "+") {
            eqFull(p.a, "+", p.b, null)
            prompt = L.watchTheItems(itemName)
            speaker.speak(L.countPhrase(p.a, itemName))
            for (i in 1..p.a) {
                val itm = newItem(emoji, 'A')
                plateA.add(itm)
                countItem(itm, i)
            }
            delay(500)
            prompt = L.morePrompt(p.b)
            speaker.speak(L.moreComing(p.b, itemName))
            for (j in p.a + 1..p.a + p.b) {
                val itm = newItem(emoji, 'A')
                plateA.add(itm)
                countItem(itm, j)
            }
            delay(400)
            prompt = L.howManyAltogetherPrompt()
            speaker.speak(L.howManyAltogether(itemName))
        } else {
            eqFull(p.a, "−", p.b, null)
            prompt = L.watchTheItems(itemName)
            speaker.speak(L.countPhrase(p.a, itemName))
            for (m in 1..p.a) {
                val itm = newItem(emoji, 'A')
                plateA.add(itm)
                countItem(itm, m)
            }
            delay(600)
            goneLabel = L.wentAway()
            goneVisible = true
            prompt = L.goAwayPrompt(p.b)
            speaker.speak(L.goAway(p.b))
            repeat(p.b) { g ->
                if (plateA.isNotEmpty()) {
                    plateA.removeAt(plateA.size - 1)
                    val ghostItm = newItem(emoji, 'G')
                    ghostItm.ghost = true
                    ghostItm.dropped = true
                    goneItems.add(ghostItm)
                }
                speaker.speak(L.word(g + 1))
                delay(150)
            }
            delay(400)
            prompt = L.howManyLeftPrompt()
            speaker.speak(L.howManyLeft(itemName))
        }

        showAnswers(p.answer, levelCap())
        // the child may tap-count the objects (with voice) before answering
        plateA.forEach { it.tappable = true }
        tapCount = 0
        enterPhase(Phase.ANSWER, L.tapThenAnswer(itemName))
        busy = false
    }

    private suspend fun recountTogether() {
        val p = current ?: return
        prompt = L.countTogether()
        speaker.speak(L.countTogether())
        val items = plateA.toList()
        for ((i, itm) in items.withIndex()) {
            countItem(itm, i + 1)
        }
        if (p.op == "+") {
            prompt = L.howManyAltogetherPrompt()
            speaker.speak(L.recountAltogether(items.size))
        } else {
            prompt = L.howManyLeftPrompt()
            speaker.speak(L.recountLeft(items.size))
        }
    }

    fun onAnswer(idx: Int) {
        if (mode != Mode.QUIZ || busy || idx >= answers.size) return
        busy = true
        launchSeq { answerSeq(idx) }
    }

    private suspend fun answerSeq(idx: Int) {
        val p = current ?: run { busy = false; return }
        if (answers[idx] == p.answer) {
            stopIdleHint()
            dimOthers = true
            ansCorrectIdx = idx
            eqFull(p.a, p.op, p.b, p.answer)
            confettiBurst()
            correctCount++
            awardStar()
            speaker.speak(L.praiseAnswer(p.answer))
            delay(900)
            round++
            playRound()
        } else {
            ansWrongIdx = idx
            ansWrongTick++
            speaker.speak(L.tryCounting())
            ansWrongIdx = -1
            // clear the child's own badges; the recount re-counts everything,
            // then the child may tap-count and answer again
            plateA.forEach { it.badge = null; it.tappable = true }
            tapCount = 0
            recountTogether()
            enterPhase(Phase.ANSWER, L.tapThenAnswer(itemName))
            busy = false
        }
    }

    /* ============================================================
       MODE SWITCHING
       ============================================================ */

    private fun resetUI() {
        job?.cancel()
        job = null
        stopIdleHint()
        busy = false
        phase = Phase.NONE
        speaker.stop()
        hideAnswers()
        hideActionBtns()
        clearStage()
        equation = emptyList()
        pickerVisible = false
        diceVisible = false
    }

    fun onPlay() {
        screen = Screen.MENU
    }

    fun showMenu() {
        resetUI()
        mode = null
        screen = Screen.MENU
    }

    fun enterGuided() {
        resetUI()
        mode = Mode.GUIDED
        screen = Screen.GAME
        round = 0
        launchSeq { guidedRound() }
    }

    fun enterFree() {
        resetUI()
        mode = Mode.FREE
        screen = Screen.GAME
        showPicker()
    }

    fun enterQuiz() {
        resetUI()
        mode = Mode.QUIZ
        screen = Screen.GAME
        round = 0
        correctCount = 0
        launchSeq { playRound() }
    }

    /* ---------- lifecycle ---------- */

    override fun onCleared() {
        // viewModelScope (and with it `job`/`idleJob`) is cancelled by the
        // framework; here we only release the TTS engine.
        speaker.shutdown()
        super.onCleared()
    }

    companion object {
        /**
         * Builds the ViewModel with a [Speaker] tied to the Application context
         * (never an Activity), so no Activity reference can leak and the TTS
         * engine survives config changes. Released in [onCleared].
         */
        fun factory(app: Application): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val prefs = app.getSharedPreferences(G.PREFS, Context.MODE_PRIVATE)
                val langName = prefs.getString(G.KEY_LANG, Lang.EN.name) ?: Lang.EN.name
                val lang = Lang.values().firstOrNull { it.name == langName } ?: Lang.EN
                val langSet = prefs.getBoolean(G.KEY_LANG_SET, false)
                GameViewModel(app, Speaker(app, lang), lang, langSet)
            }
        }
    }
}
