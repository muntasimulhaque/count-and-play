package app.maqsadah.count_and_play.core

/** How many spare objects a child can build a prediction from. Enough to overshoot. */
const val RESERVE_SIZE = MAX_TOTAL + 1

/** Builds the opening board for a task. Pure: same task in, same tokens out. */
object Setup {

    fun tokens(task: Task): List<Token> {
        val ids = generateSequence(0) { it + 1 }.iterator()
        fun fill(n: Int, zone: Zone, origin: Zone = zone) =
            (0 until n).map { slot ->
                Token(id = ids.next(), shape = task.shape, zone = zone, slot = slot, origin = origin)
            }

        return when (task) {
            is Task.CountIt -> fill(task.n, Zone.BOWL)

            is Task.GiveMe -> fill(task.pool, Zone.SOURCE)

            is Task.WhichIsMore ->
                fill(task.left, Zone.DISH_A) + fill(task.right, Zone.DISH_B)

            is Task.UnderTheLeaf -> {
                val onMat = fill(task.start, Zone.BOWL)
                // A positive delta waits at the side and slides in under the leaf;
                // a negative one is already on the mat and will roll back out.
                val arriving = if (task.delta > 0) fill(task.delta, Zone.DISH_B) else emptyList()
                onMat + arriving + fill(RESERVE_SIZE, Zone.RESERVE)
            }

            is Task.Join ->
                fill(task.a, Zone.DISH_A) +
                    fill(task.b, Zone.DISH_B) +
                    fill(RESERVE_SIZE, Zone.RESERVE)

            is Task.Separate ->
                fill(task.whole, Zone.BOWL) + fill(RESERVE_SIZE, Zone.RESERVE)
        }
    }

    /**
     * Whether the voice supplies count words for a set of this size.
     *
     * The borrowing register: while a child cannot yet produce the sequence, the
     * voice lends it to him on every tap. Once he can count sets of that size
     * unaided the voice goes quiet for *that size* while still counting larger
     * ones. Contingent scaffolding, faded automatically — this is what makes the
     * voice a teacher rather than a soundtrack.
     */
    fun countsAloud(n: Int, progress: Progress): Boolean {
        val fluentTo = when (progress.level(Skill.COUNT)) {
            1 -> 0
            2 -> 3
            3 -> 5
            else -> 6
        }
        return n > fluentTo
    }
}
