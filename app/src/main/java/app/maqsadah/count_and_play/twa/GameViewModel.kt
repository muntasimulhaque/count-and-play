package app.maqsadah.count_and_play.twa

import android.app.Application
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
enum class Mode { LEARN, QUIZ }

/** One emoji on stage. Ported from the .item DOM element. */
class Item(val id: Int, val emoji: String, val group: Char) {
    var ghost by mutableStateOf(false)
    var tappable by mutableStateOf(false)
    var pulseTick by mutableStateOf(0)
    var groupPulseTick by mutableStateOf(0)
    var bubble by mutableStateOf<Int?>(null)
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
 * All game state + sequencing. A direct port of the original JS game logic;
 * JS async/await + session guards become coroutines + job cancellation.
 *
 * Lives in a [ViewModel]: state and running sequences survive config changes,
 * coroutines run on [viewModelScope] (cancelled automatically when the
 * ViewModel is cleared), and the [Speaker] is built from the Application
 * context — never an Activity — and released in [onCleared].
 */
class GameViewModel(val speaker: Speaker) : ViewModel() {

    var screen by mutableStateOf(Screen.START)
    var mode by mutableStateOf<Mode?>(null)
    var busy by mutableStateOf(false)
        private set

    // header
    var correctCount by mutableStateOf(0)
        private set

    // equation + prompt
    var equation by mutableStateOf(listOf<EqPart>())
    var prompt by mutableStateOf("")

    // stage
    val plateA = mutableStateListOf<Item>()
    val plateB = mutableStateListOf<Item>()
    val goneItems = mutableStateListOf<Item>()   // quiz-mode "went away" ghosts
    val slots = mutableStateListOf<Item?>()      // learn-mode take-away slots
    var plateBVisible by mutableStateOf(false)
    var goneVisible by mutableStateOf(false)
    var goneLabel by mutableStateOf("take away")
    // How many items each zone will hold this round — drives the auto-fit sizing
    // in the UI (bigger objects, guaranteed to fit even at 20).
    var capA by mutableStateOf(1)
    var capB by mutableStateOf(1)
    var capGone by mutableStateOf(1)

    // picker (learn mode)
    var pickerVisible by mutableStateOf(false)
    var pickerTitle by mutableStateOf("")
    var gridVisible by mutableStateOf(false)
    var gridEnabledMax by mutableStateOf(G.MAX_N)
    var opRowVisible by mutableStateOf(false)
    var plusEnabled by mutableStateOf(true)
    var minusEnabled by mutableStateOf(true)
    var diceVisible by mutableStateOf(false)

    // learn action buttons
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

    // learn state
    private var la = 0
    private var lop = "+"
    private var lb = 0
    private var slotsFilled = 0
    private var pickingSecond = false

    // quiz state
    private var round = 0
    private var current: Problem? = null

    /* ---------- helpers ---------- */

    private fun part(s: String) = EqPart(s, EqPart.NORMAL)
    private fun qPart(s: String) = EqPart(s, EqPart.Q)
    private fun ghostPart(s: String) = EqPart(s, EqPart.GHOST)

    private fun eqFull(a: Int, op: String, b: Int, result: Int?) {
        equation = if (result == null) {
            listOf(part("$a $op $b = "), qPart("?"))
        } else {
            listOf(part("$a $op $b = $result"))
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

    private suspend fun countItem(item: Item, num: Int) {
        item.pulseTick++
        showBubble(item, num)
        speaker.speak(G.word(num))
        delay(100)
    }

    private fun clearStage() {
        plateA.clear()
        plateB.clear()
        goneItems.clear()
        slots.clear()
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

    /* ============================================================
       LEARN MODE — pick numbers, watch and do
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
        pickerTitle = "Pick a number!"
        equation = listOf(ghostPart("?"))
        sayAsync("Pick a number!")
    }

    fun onPickFirst(n: Int) {
        la = n
        sayAsync(G.word(n))
        equation = listOf(part("$n"))
        gridVisible = false
        diceVisible = false
        opRowVisible = true
        plusEnabled = n < G.MAX_N     // 20 + anything exceeds the cap
        minusEnabled = n >= 1
        pickerTitle = "Plus or take away?"
    }

    fun onPickOp(op: String) {
        lop = op
        sayAsync(if (op == "+") "plus" else "take away")
        equation = listOf(part("$la $op "), ghostPart("?"))
        opRowVisible = false
        gridVisible = true
        pickerTitle = "Pick another number!"
        gridEnabledMax = if (op == "+") G.MAX_N - la else la
        pickingSecond = true
    }

    /** Routes a number-grid tap to the right step of the picker. */
    fun onPickNumber(n: Int) {
        if (pickingSecond) onPickSecond(n) else onPickFirst(n)
    }

    fun onPickSecond(n: Int) {
        lb = n
        sayAsync(G.word(n))
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
        if (lop == "+") launchSeq { actAdd() } else launchSeq { actSub() }
    }

    /* ----- addition: two same-object groups join into one ----- */
    private suspend fun actAdd() {
        busy = true
        clearStage()
        val a = la
        val b = lb
        // Both groups are the SAME object (e.g. 13 stars + 7 stars) so kids add
        // like with like — clearer to grasp than mixing two different things.
        // The two groups stay visually distinct via their colored zones + merge.
        val em = G.ITEMS[G.rand(G.ITEMS.size)]
        val emA = em
        val emB = em
        val nameA = G.NAMES[em] ?: "things"
        val nameB = G.NAMES[em] ?: "things"

        equation = listOf(part("$a + "), ghostPart("$b"), part(" = "), qPart("?"))
        plateBVisible = true
        capA = a
        capB = b

        prompt = "${G.word(a)} ${G.plural(nameA, a)}!"
        speaker.speak("${G.word(a)} ${G.plural(nameA, a)}!")
        for (i in 1..a) {
            val itm = newItem(emA, 'A')
            plateA.add(itm)
            countItem(itm, i)
        }

        delay(400)
        equation = listOf(part("$a + $b = "), qPart("?"))
        prompt = "and ${G.word(b)} ${G.plural(nameB, b)}!"
        speaker.speak("and ${G.word(b)} ${G.plural(nameB, b)}!")
        for (j in 1..b) {
            val itm = newItem(emB, 'B')
            plateB.add(itm)
            countItem(itm, j)
        }

        delay(300)
        prompt = "Tap to put them together!"
        speaker.speak(
            "${G.word(a)} ${G.plural(nameA, a)} and ${G.word(b)} ${G.plural(nameB, b)}. Tap, put together!"
        )
        showMerge = true
        busy = false
    }

    fun onMerge() {
        if (mode != Mode.LEARN || busy) return
        busy = true
        launchSeq { doMerge() }
    }

    private suspend fun doMerge() {
        showMerge = false
        val a = la
        val b = lb
        val sum = a + b

        // move group B items into plate A
        capA = sum
        while (plateB.isNotEmpty()) {
            val itm = plateB.removeAt(0)
            plateA.add(itm)
            delay(90)
        }
        plateBVisible = false
        prompt = "All together now!"
        speaker.speak("All together now! Let's count on!")

        // counting on: pulse the whole first group and say its number...
        val aItems = plateA.filter { it.group == 'A' }
        aItems.forEach { it.groupPulseTick++ }
        aItems.lastOrNull()?.let { showBubble(it, a) }
        speaker.speak(G.word(a) + "!")

        // ...then count on over the second group: a+1, a+2, ...
        val bMoved = plateA.filter { it.group == 'B' }
        for ((k, itm) in bMoved.withIndex()) {
            countItem(itm, a + 1 + k)
        }

        eqFull(a, "+", b, sum)
        prompt = "${G.word(a)} plus ${G.word(b)} makes ${G.word(sum)}!"
        confettiBurst()
        speaker.speak("${G.word(a)} plus ${G.word(b)} makes ${G.word(sum)}!")
        showAgain = true
        showNew = true
        busy = false
    }

    /* ----- subtraction: fill the take-away slots ----- */
    private suspend fun actSub() {
        busy = true
        clearStage()
        val a = la
        val b = lb
        val em = G.ITEMS[G.rand(G.ITEMS.size)]
        val name = G.NAMES[em] ?: "things"

        equation = listOf(part("$a − "), ghostPart("$b"), part(" = "), qPart("?"))
        capA = a
        capGone = b

        prompt = "${G.word(a)} ${G.plural(name, a)}!"
        speaker.speak("${G.word(a)} ${G.plural(name, a)}!")
        for (i in 1..a) {
            val itm = newItem(em, 'A')
            plateA.add(itm)
            countItem(itm, i)
        }

        delay(400)
        equation = listOf(part("$a − $b = "), qPart("?"))
        goneLabel = "take away $b"
        goneVisible = true
        slots.clear()
        repeat(b) { slots.add(null) }
        slotsFilled = 0
        prompt = "Take away $b! Tap the $name!"
        speaker.speak("Now take away ${G.word(b)}! Tap ${G.word(b)} ${G.plural(name, b)}!")

        plateA.forEach { it.tappable = true }
        busy = false
    }

    fun onFruitTap(itm: Item) {
        if (mode != Mode.LEARN || busy) return
        if (slotsFilled >= lb || !itm.tappable) return
        busy = true
        launchSeq { fruitTapSeq(itm) }
    }

    private suspend fun fruitTapSeq(itm: Item) {
        itm.tappable = false
        plateA.remove(itm)
        slots[slotsFilled] = itm
        slotsFilled++
        showBubble(itm, slotsFilled)
        speaker.speak(G.word(slotsFilled))

        if (slotsFilled >= lb) {
            // all taken away
            plateA.forEach { it.tappable = false }
            prompt = "We took away $lb!"
            speaker.speak("We took away ${G.word(lb)}!")
            slots.forEach { it?.ghost = true }
            delay(400)

            // count what's left
            prompt = "How many are left?"
            speaker.speak("How many are left? Let's count!")
            val left = plateA.toList()
            for ((i, leftItem) in left.withIndex()) {
                countItem(leftItem, i + 1)
            }
            val result = la - lb
            eqFull(la, "−", lb, result)
            if (result == 0) {
                prompt = "Zero! All gone!"
                confettiBurst()
                speaker.speak("Zero! All gone! ${G.word(la)} take away ${G.word(lb)} leaves zero!")
            } else {
                prompt = "${G.word(la)} take away ${G.word(lb)} leaves ${G.word(result)}!"
                confettiBurst()
                speaker.speak("${G.word(la)} take away ${G.word(lb)} leaves ${G.word(result)}!")
            }
            showAgain = true
            showNew = true
        }
        busy = false
    }

    fun onAgain() {
        if (mode != Mode.LEARN || busy) return
        hideActionBtns()
        startActing()
    }

    fun onNew() {
        if (mode != Mode.LEARN || busy) return
        showPicker()
    }

    /* ============================================================
       QUIZ MODE — watch, count, answer
       ============================================================ */

    private fun makeProblem(): Problem = G.quizProblem(round, correctCount)

    private fun showAnswers(correct: Int, maxN: Int) {
        answers.clear()
        answers.addAll(G.answerOptions(correct, maxN))
        ansCorrectIdx = -1
        ansWrongIdx = -1
        dimOthers = false
    }

    private suspend fun playRound() {
        busy = true
        hideAnswers()
        clearStage()
        equation = emptyList()

        val p = makeProblem()
        current = p
        val emoji = G.ITEMS[G.rand(G.ITEMS.size)]
        val name = G.NAMES[emoji] ?: "things"
        capA = if (p.op == "+") p.a + p.b else p.a
        capGone = p.b

        delay(400)

        if (p.op == "+") {
            eqFull(p.a, "+", p.b, null)
            prompt = "Watch the $name!"
            speaker.speak("${G.word(p.a)} ${G.plural(name, p.a)}")
            for (i in 1..p.a) {
                val itm = newItem(emoji, 'A')
                plateA.add(itm)
                countItem(itm, i)
            }
            delay(500)
            prompt = "${p.b} more!"
            speaker.speak("${G.word(p.b)} more ${G.plural(name, p.b)} coming!")
            for (j in p.a + 1..p.a + p.b) {
                val itm = newItem(emoji, 'A')
                plateA.add(itm)
                countItem(itm, j)
            }
            delay(400)
            prompt = "How many altogether?"
            speaker.speak("How many $name altogether?")
        } else {
            eqFull(p.a, "−", p.b, null)
            prompt = "Watch the $name!"
            speaker.speak("${G.word(p.a)} ${G.plural(name, p.a)}")
            for (m in 1..p.a) {
                val itm = newItem(emoji, 'A')
                plateA.add(itm)
                countItem(itm, m)
            }
            delay(600)
            goneLabel = "went away"
            goneVisible = true
            prompt = "${p.b} go away!"
            speaker.speak("${G.word(p.b)} ${if (p.b == 1) "goes" else "go"} away!")
            repeat(p.b) { g ->
                if (plateA.isNotEmpty()) {
                    plateA.removeAt(plateA.size - 1)
                    val ghostItm = newItem(emoji, 'G')
                    ghostItm.ghost = true
                    ghostItm.dropped = true
                    goneItems.add(ghostItm)
                }
                speaker.speak(G.word(g + 1))
                delay(150)
            }
            delay(400)
            prompt = "How many are left?"
            speaker.speak("How many $name are left?")
        }

        showAnswers(p.answer, 10)
        busy = false
    }

    private suspend fun recountTogether() {
        val p = current ?: return
        prompt = "Let's count together!"
        speaker.speak("Let's count together!")
        val items = plateA.toList()
        for ((i, itm) in items.withIndex()) {
            countItem(itm, i + 1)
        }
        if (p.op == "+") {
            prompt = "How many altogether?"
            speaker.speak("${G.word(items.size)}! How many altogether?")
        } else {
            prompt = "How many are left?"
            speaker.speak("${G.word(items.size)}! How many are left?")
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
            dimOthers = true
            ansCorrectIdx = idx
            eqFull(p.a, p.op, p.b, p.answer)
            confettiBurst()
            correctCount++
            val praise = listOf("Yes! ", "Great job! ", "Well done! ", "Hooray! ")[G.rand(4)]
            speaker.speak(praise + G.word(p.answer) + "!")
            delay(900)
            round++
            playRound()
        } else {
            ansWrongIdx = idx
            ansWrongTick++
            speaker.speak("Hmm, let's try counting!")
            ansWrongIdx = -1
            recountTogether()
            busy = false
        }
    }

    /* ============================================================
       MODE SWITCHING
       ============================================================ */

    private fun resetUI() {
        job?.cancel()
        job = null
        busy = false
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

    fun enterLearn() {
        resetUI()
        mode = Mode.LEARN
        screen = Screen.GAME
        showPicker()
    }

    fun enterQuiz() {
        resetUI()
        mode = Mode.QUIZ
        screen = Screen.GAME
        round = 0
        launchSeq { playRound() }
    }

    /* ---------- lifecycle ---------- */

    override fun onCleared() {
        // viewModelScope (and with it `job`) is cancelled by the framework;
        // here we only release the TTS engine.
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
            initializer { GameViewModel(Speaker(app)) }
        }
    }
}
