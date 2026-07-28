package app.maqsadah.count_and_play.core

import org.junit.Test
import java.io.File

/**
 * Diagnostic, not an assertion: prints the actual sequence of tasks a child meets
 * over his first several sittings, for three different children.
 *
 * The rules are pure, so "what does a real child actually see in week one?" is a
 * question that can be answered by running the game rather than by guessing.
 */
class ExperienceSimTest {

    private enum class Child { FLUENT, TAPPY, SUBSET }

    private val out = StringBuilder()

    private fun println(line: String = "") {
        out.append(line).append('\n')
    }

    @Test
    fun `what the child actually meets`() {
        for (child in Child.entries) {
            println("\n\n================ $child ================")
            simulate(child, sessions = 8, seed = 7)
        }
        File(System.getProperty("java.io.tmpdir"), "count-and-play-sim.txt")
            .writeText(out.toString())
        kotlin.io.println(out)
    }

    private fun simulate(child: Child, sessions: Int, seed: Long) {
        val rng = SeededRng(seed)
        var progress = Progress()
        var taskNo = 0
        var biggest = 0
        val firstSeen = mutableMapOf<Skill, Int>()

        for (s in 1..sessions) {
            var session = SessionState(progress = progress, shape = ShapeKind.APPLE)
            println("\n-- sitting $s --")
            while (session.index < Scheduler.TASKS_PER_SESSION) {
                val plan = Scheduler.plan(session, rng)
                val task = Ladder.taskFor(plan.skill, plan.level, session.shape, rng)
                val run = play(task, session.progress, child)
                taskNo++
                firstSeen.putIfAbsent(plan.skill, taskNo)
                biggest = maxOf(biggest, biggestNumberIn(task))
                println(
                    "  #%2d  %-8s L%d  %-20s  taps=%-3d %s".format(
                        taskNo, plan.skill, plan.level, describe(task), run.taps,
                        if (run.result?.correct == true) "ok" else "MISS",
                    ),
                )
                session = Scheduler.record(session, run.result!!)
            }
            progress = Scheduler.close(session)
            println("     levels " + Skill.entries.joinToString(" ") { "${it.name.take(3)}=${progress.level(it)}" })
            println("     unlocked " + Ladder.unlocked(progress).joinToString(", "))
        }

        println("\n  first appearance (task #): " + Skill.entries.joinToString(" ") { "${it.name.take(3)}=${firstSeen[it] ?: -1}" })
        println("  biggest number seen in ${sessions * Scheduler.TASKS_PER_SESSION} tasks: $biggest")
    }

    // -- a child playing --------------------------------------------------

    private data class Run(val taps: Int, val result: TaskResult?)

    private fun play(task: Task, progress: Progress, child: Child): Run {
        var outcome = Lesson.begin(task, progress)
        var taps = 0
        var result: TaskResult? = outcome.result
        var guard = 0
        while (outcome.state.step != Step.Finished) {
            val event = nextMove(outcome.state, child, taps) ?: break
            outcome = Lesson.onEvent(outcome.state, event)
            if (event is Event.TapToken) taps++
            if (result == null) result = outcome.result
            if (++guard > 400) break
        }
        return Run(taps, result)
    }

    private fun nextMove(state: LessonState, child: Child, taps: Int): Event? {
        val step = state.step
        // An excited 3-year-old taps an object he has already counted. The app
        // permits it and records it; this asks what that costs him.
        if (child == Child.TAPPY && step is Step.Counting && taps % 3 == 2) {
            state.tokens.inZone(step.zone).lastOrNull { it.isCounted }
                ?.let { return Event.TapToken(it.id) }
        }
        val accuracy = if (child == Child.SUBSET) Play.Accuracy.OVER else Play.Accuracy.PERFECT
        return Play.move(state, accuracy)
    }

    // -- reporting --------------------------------------------------------

    private fun describe(task: Task): String = when (task) {
        is Task.CountIt -> "count ${task.n}"
        is Task.GiveMe -> "give ${task.n} of ${task.pool}"
        is Task.WhichIsMore -> "${task.left} vs ${task.right}${if (task.spread) " spread" else ""}"
        is Task.UnderTheLeaf -> "${task.start} ${if (task.delta >= 0) "+ ${task.delta}" else "- ${-task.delta}"} hidden"
        is Task.Join -> "${task.a} + ${task.b}"
        is Task.Separate -> "${task.whole} - ${task.take}"
    }

    private fun biggestNumberIn(task: Task): Int = when (task) {
        is Task.CountIt -> task.n
        is Task.GiveMe -> task.pool
        is Task.WhichIsMore -> maxOf(task.left, task.right)
        is Task.UnderTheLeaf -> maxOf(task.start, task.answer)
        is Task.Join -> task.answer
        is Task.Separate -> task.whole
    }
}
