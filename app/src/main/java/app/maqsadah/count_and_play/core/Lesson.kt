package app.maqsadah.count_and_play.core

/** What to do once the current count reaches its target. */
enum class After { FINISH, CODA, COUNT_B, HIDE, PREDICT_JOIN, TAKE_OUT, CHECK }

sealed interface Step {
    data class Counting(
        val zone: Zone,
        val target: Int,
        val next: Int,
        val then: After,
    ) : Step

    /** Give-N: move objects from the heap into the bowl, then say done. */
    data object Giving : Step

    /** Two dishes; tap the one with more. */
    data object Choosing : Step

    /** Take-away: move objects out to the gone-dish. Overshooting is allowed. */
    data object Taking : Step

    /** Build a quantity in the answer frame. Never blocked, never corrected. */
    data object Predicting : Step

    data object Finished : Step
}

data class LessonState(
    val task: Task,
    val step: Step,
    val tokens: List<Token>,
    val progress: Progress,
    val predicted: Int? = null,
    val retaps: Int = 0,
    val nudges: Int = 0,
)

sealed interface Event {
    data class TapToken(val id: Int) : Event
    data class TapZone(val zone: Zone) : Event
    data object Done : Event
    data object Nudge : Event
}

data class TaskResult(
    val skill: Skill,
    val correct: Boolean,
    val given: Int,
    val expected: Int,
    val retaps: Int,
)

data class Outcome(
    val state: LessonState,
    val script: Script,
    val result: TaskResult? = null,
)

/**
 * The rules. Three pure functions, no clock, no coroutines, no Android.
 *
 * A tap always produces an outcome — impatience may fast-forward, but input is
 * never silently swallowed. To a 3-year-old an unresponsive screen is not
 * "wait", it is broken.
 */
object Lesson {

    fun begin(task: Task, progress: Progress): Outcome {
        val tokens = Setup.tokens(task)
        val state = LessonState(task, Step.Finished, tokens, progress)
        return when (task) {
            is Task.CountIt -> state
                .counting(Zone.BOWL, task.n, After.FINISH)
                .opening(tokens.inZone(Zone.BOWL), Line.CountThem)

            is Task.GiveMe -> state.copy(step = Step.Giving)
                .opening(tokens.inZone(Zone.SOURCE), Line.GiveN(task.n, task.shape))

            is Task.WhichIsMore -> state.copy(step = Step.Choosing)
                .opening(
                    tokens.inZone(Zone.DISH_A) + tokens.inZone(Zone.DISH_B),
                    Line.WhichHasMore,
                )

            is Task.UnderTheLeaf -> state
                .counting(Zone.BOWL, task.start, After.HIDE)
                .opening(tokens.inZone(Zone.BOWL), Line.CountThem)

            is Task.Join -> state
                .counting(Zone.DISH_A, task.a, After.COUNT_B)
                .opening(tokens.inZone(Zone.DISH_A), Line.CountThem)

            is Task.Separate -> state
                .counting(Zone.BOWL, task.whole, After.TAKE_OUT)
                .opening(tokens.inZone(Zone.BOWL), Line.CountThem)
        }
    }

    fun onEvent(state: LessonState, event: Event): Outcome = when (event) {
        is Event.Nudge -> nudge(state)
        is Event.TapZone -> onTapZone(state, event.zone)
        is Event.TapToken -> {
            val token = state.tokens.firstOrNull { it.id == event.id }
            if (token == null) state.inert() else onTapToken(state, token)
        }
        is Event.Done -> onDone(state)
    }

    /** Re-issues the current instruction — spoken on return from the background. */
    fun reprompt(state: LessonState): Script {
        val line = when (val step = state.step) {
            is Step.Counting -> Line.CountThem
            Step.Giving -> (state.task as Task.GiveMe).let { Line.GiveN(it.n, it.shape) }
            Step.Choosing -> Line.WhichHasMore
            Step.Taking -> Line.TakeOut((state.task as Task.Separate).take)
            Step.Predicting -> Line.MakeItHere
            Step.Finished -> return Script.none
        }
        return script { say(line) }
    }

    // -- Tapping ------------------------------------------------------------

    private fun onTapToken(state: LessonState, token: Token): Outcome =
        when (val step = state.step) {
            is Step.Counting -> count(state, step, token)
            Step.Giving -> shuttle(state, token, Zone.SOURCE, Zone.BOWL)
            Step.Taking -> shuttle(state, token, Zone.BOWL, Zone.DISH_B)
            Step.Predicting -> shuttle(state, token, Zone.RESERVE, Zone.ANSWER)
            Step.Choosing -> onTapZone(state, token.zone)
            Step.Finished -> state.inert()
        }

    private fun onTapZone(state: LessonState, zone: Zone): Outcome {
        val task = state.task
        if (state.step != Step.Choosing || task !is Task.WhichIsMore) return state.inert()
        if (zone != Zone.DISH_A && zone != Zone.DISH_B) return state.inert()

        val correct = zone == task.moreSide
        val more = maxOf(task.left, task.right)
        // Never "wrong": the app states the fact and moves on, with no sad sound
        // and nothing red. The next task will be an easier one.
        return Outcome(
            state = state.copy(step = Step.Finished),
            script = script {
                if (correct) cue(Sfx.CHIME) else pause(Pace.SETTLE)
                settle(Line.ThisHasMore(more))
            },
            result = TaskResult(Skill.COMPARE, correct, more, more, state.retaps),
        )
    }

    /** Counting is one routine for every activity that needs it. */
    private fun count(state: LessonState, step: Step.Counting, token: Token): Outcome {
        if (token.zone != step.zone) return state.inert()

        // A re-tap is permitted and *recorded*. Blocking it would hide the single
        // most diagnostic behaviour in early number: where the child stops.
        if (token.isCounted) {
            return Outcome(
                state = state.copy(retaps = state.retaps + 1),
                script = script { cue(Sfx.HOLLOW) },
            )
        }

        val ordinal = step.next
        val tokens = state.tokens.map {
            if (it.id == token.id) it.copy(state = TokenState.Counted(ordinal)) else it
        }
        val aloud = Setup.countsAloud(step.target, state.progress)

        if (ordinal < step.target) {
            return Outcome(
                state = state.copy(tokens = tokens, step = step.copy(next = ordinal + 1)),
                script = script {
                    cue(Sfx.TICK)
                    if (aloud) say(Line.CountWord(ordinal))
                },
            )
        }

        // The last tap. Pause, collapse the tags into one number, then name it —
        // lower and slower. That pause is the count-to-cardinal transition.
        val counted = state.copy(tokens = tokens, step = step.copy(next = ordinal))
        val lead = script {
            cue(Sfx.TICK)
            if (aloud) say(Line.CountWord(ordinal))
            pause(Pace.CARDINAL)
            show(StageChange.Collapse(step.zone, step.target))
            settle(Line.Cardinal(step.target, state.task.shape))
        }
        return advance(counted, step, lead)
    }

    /** Moves a token between a holding zone and a working zone, either direction. */
    private fun shuttle(state: LessonState, token: Token, from: Zone, to: Zone): Outcome =
        when (token.zone) {
            from -> Outcome(
                state = state.copy(tokens = state.tokens.moveTo(setOf(token.id), to)),
                script = script {
                    show(StageChange.Travel(listOf(token.id), to))
                    cue(Sfx.THUD)
                },
            )
            // Taking it back out again is always allowed — deciding when to stop
            // is the whole point, and a child who cannot undo cannot decide.
            to -> Outcome(
                state = state.copy(
                    tokens = state.tokens.moveTo(setOf(token.id), from).compact(to),
                ),
                script = script {
                    show(StageChange.Travel(listOf(token.id), from))
                    cue(Sfx.HOLLOW)
                },
            )
            else -> state.inert()
        }

    // -- Helpers ------------------------------------------------------------

    private fun nudge(state: LessonState): Outcome {
        val targets = nudgeTargets(state)
        val script = when (state.nudges) {
            // Silent and spatial first: one object breathing says "here",
            // where pulsing everything at once says only "something".
            0 -> script { show(StageChange.Highlight(targets)) }
            1 -> script { show(StageChange.Highlight(targets)); say(Line.NudgeGentle) }
            2 -> script {
                show(StageChange.Highlight(targets))
                say(Line.NudgeModel((state.step as? Step.Counting)?.next ?: 1))
            }
            // Then it stops for good. A prompt on a loop teaches a child to
            // ignore the voice.
            else -> Script.none
        }
        return Outcome(state.copy(nudges = state.nudges + 1), script)
    }

    private fun nudgeTargets(state: LessonState): List<Int> = when (val step = state.step) {
        is Step.Counting -> state.tokens.inZone(step.zone).filter { !it.isCounted }
        Step.Giving -> state.tokens.inZone(Zone.SOURCE)
        Step.Taking -> state.tokens.inZone(Zone.BOWL)
        Step.Predicting -> state.tokens.inZone(Zone.RESERVE)
        Step.Choosing -> state.tokens.inZone(Zone.DISH_A)
        Step.Finished -> emptyList()
    }.take(1).map { it.id }
}
