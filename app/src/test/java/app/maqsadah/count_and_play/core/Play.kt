package app.maqsadah.count_and_play.core

/**
 * A test harness that plays the game the way a child would.
 *
 * The whole point of the core layer is that this is possible: the entire app is
 * playable, start to finish, with no Android, no emulator and no clock.
 */
object Play {

    data class Played(
        val state: LessonState,
        val results: List<TaskResult>,
        val scripts: List<Script>,
        val moves: Int,
    )

    /**
     * Plays one task to completion.
     *
     * [accuracy] lets a test play as a child who knows the answer (`PERFECT`) or
     * one who does not (`OVER` builds one too many).
     */
    fun task(task: Task, progress: Progress = Progress(), accuracy: Accuracy = Accuracy.PERFECT): Played {
        var outcome = Lesson.begin(task, progress)
        val results = mutableListOf<TaskResult>()
        val scripts = mutableListOf(outcome.script)
        var moves = 0

        outcome.result?.let(results::add)
        while (outcome.state.step != Step.Finished) {
            val event = move(outcome.state, accuracy)
                ?: error("no legal move from ${outcome.state.step} in $task")
            outcome = Lesson.onEvent(outcome.state, event)
            outcome.result?.let(results::add)
            scripts += outcome.script
            check(++moves < 400) { "task did not terminate: $task stuck at ${outcome.state.step}" }
        }
        return Played(outcome.state, results, scripts, moves)
    }

    enum class Accuracy { PERFECT, OVER }

    /** The next thing a child who is following along would do. */
    fun move(state: LessonState, accuracy: Accuracy = Accuracy.PERFECT): Event? {
        return when (val step = state.step) {
            is Step.Counting ->
                state.tokens.inZone(step.zone).firstOrNull { !it.isCounted }
                    ?.let { Event.TapToken(it.id) }
                    ?: Event.Done

            Step.Giving -> {
                val task = state.task as Task.GiveMe
                val want = target(task.n, task.pool, accuracy)
                fillFrom(state, Zone.SOURCE, Zone.BOWL, want)
            }

            Step.Choosing ->
                Event.TapZone((state.task as Task.WhichIsMore).moreSide)

            Step.Taking -> {
                val task = state.task as Task.Separate
                fillFrom(state, Zone.BOWL, Zone.DISH_B, target(task.take, task.whole, accuracy))
            }

            Step.Predicting ->
                fillFrom(state, Zone.RESERVE, Zone.ANSWER, prediction(state, accuracy))

            Step.Finished -> null
        }
    }

    private fun target(want: Int, cap: Int, accuracy: Accuracy) =
        if (accuracy == Accuracy.OVER) minOf(want + 1, cap) else want

    private fun prediction(state: LessonState, accuracy: Accuracy): Int {
        val truth = when (val task = state.task) {
            is Task.Separate -> state.tokens.countIn(Zone.BOWL)
            else -> task.answer
        }
        return target(truth, RESERVE_SIZE, accuracy)
    }

    private fun fillFrom(state: LessonState, from: Zone, to: Zone, want: Int): Event {
        val source = state.tokens.inZone(from)
        // A child who wants one more than exists simply cannot take it — the
        // player stops rather than reaching for something that isn't there.
        return if (state.tokens.countIn(to) < want && source.isNotEmpty()) {
            Event.TapToken(source.first().id)
        } else {
            Event.Done
        }
    }
}

/** Flattens a script to the lines it speaks, for assertions about narration. */
fun Script.lines(): List<Line> = beats.filterIsInstance<Beat.Say>().map { it.line }

fun List<Script>.allLines(): List<Line> = flatMap { it.lines() }

fun List<Script>.allSounds(): List<Sfx> =
    flatMap { it.beats }.filterIsInstance<Beat.Cue>().map { it.sound }
