package app.maqsadah.count_and_play.core

/**
 * The curriculum, readable top to bottom.
 *
 * Every number in this file is bounded by [MAX_TOTAL] for arithmetic and
 * [MAX_COUNT] for pure counting practice. A 3-year-old's subitizing limit is 3
 * and object-tracking limit is 1-2; twenty objects is not "harder counting", it
 * is a different and much worse task.
 */
object Ladder {

    /** A skill becomes available only once the skills it rests on are solid. */
    fun isUnlocked(skill: Skill, progress: Progress): Boolean = when (skill) {
        Skill.COUNT -> true
        Skill.GIVE_N -> progress.level(Skill.COUNT) >= 2
        Skill.COMPARE -> progress.level(Skill.COUNT) >= 2
        Skill.HIDDEN -> progress.level(Skill.GIVE_N) >= 2
        Skill.JOIN -> progress.level(Skill.COUNT) >= 3 && progress.level(Skill.HIDDEN) >= 2
        Skill.SEPARATE -> progress.level(Skill.JOIN) >= 2
    }

    fun unlocked(progress: Progress): List<Skill> =
        Skill.entries.filter { isUnlocked(it, progress) }

    fun taskFor(skill: Skill, level: Int, shape: ShapeKind, rng: Rng): Task = when (skill) {
        Skill.COUNT -> countIt(level, shape, rng)
        Skill.GIVE_N -> giveMe(level, shape, rng)
        Skill.COMPARE -> whichIsMore(level, shape, rng)
        Skill.HIDDEN -> underTheLeaf(level, shape, rng)
        Skill.JOIN -> join(level, shape, rng)
        Skill.SEPARATE -> separate(level, shape, rng)
    }

    // -- Counting -----------------------------------------------------------
    // Starts inside the subitizing range so the answer can be *seen* before it
    // has to be counted, then grows to a ten-frame.

    private fun countIt(level: Int, shape: ShapeKind, rng: Rng): Task.CountIt {
        val n = when (level) {
            1 -> rng.range(1, 3)
            2 -> rng.range(2, 5)
            3 -> rng.range(3, 6)
            else -> rng.range(5, MAX_COUNT)
        }
        return Task.CountIt(n, shape)
    }

    // -- Give-N -------------------------------------------------------------
    // The keystone. Where the child stops *is* his knower-level, read directly.

    private fun giveMe(level: Int, shape: ShapeKind, rng: Rng): Task.GiveMe {
        val (n, pool) = when (level) {
            1 -> rng.range(1, 2) to 4
            2 -> rng.range(1, 3) to 6
            3 -> rng.range(2, 4) to 7
            else -> rng.range(2, MAX_TOTAL) to 8
        }
        return Task.GiveMe(n, pool, shape)
    }

    // -- Comparison ---------------------------------------------------------
    // Easy ratios succeed on perception alone; close ratios and conflict trials
    // (fewer objects spread over more space) are the ones that need number.

    private fun whichIsMore(level: Int, shape: ShapeKind, rng: Rng): Task.WhichIsMore {
        val more: Int
        val less: Int
        when (level) {
            1 -> { more = rng.range(4, 6); less = rng.range(1, more - 3) }
            2 -> { more = rng.range(3, 6); less = more - rng.range(1, 2) }
            3 -> { more = rng.range(3, 6); less = more - rng.range(1, 2) }
            else -> { more = rng.range(3, 6); less = more - 1 }
        }
        val spread = level >= 3 && rng.int(2) == 0
        val moreOnLeft = rng.int(2) == 0
        return Task.WhichIsMore(
            left = if (moreOnLeft) more else less,
            right = if (moreOnLeft) less else more,
            spread = spread,
            shape = shape,
        )
    }

    // -- Under the leaf -----------------------------------------------------
    // Nonverbal addition: 3-year-olds succeed here while failing the identical
    // problem posed in words. This is the honest entry point for operations.

    private fun underTheLeaf(level: Int, shape: ShapeKind, rng: Rng): Task.UnderTheLeaf {
        return when (level) {
            1 -> Task.UnderTheLeaf(rng.range(1, 2), 1, shape)
            2 -> {
                val start = rng.range(1, 3)
                Task.UnderTheLeaf(start, rng.range(1, minOf(2, 4 - start)), shape)
            }
            3 -> {
                val start = rng.range(2, 4)
                Task.UnderTheLeaf(start, -1, shape)
            }
            else -> {
                val start = rng.range(2, 4)
                val adding = rng.int(2) == 0
                val delta =
                    if (adding) rng.range(1, MAX_TOTAL - start)
                    else -rng.range(1, minOf(2, start))
                Task.UnderTheLeaf(start, delta, shape)
            }
        }
    }

    // -- Joining ------------------------------------------------------------
    // Level 4 makes the first part big and the second tiny: counting all nine
    // to add one is tedious, and that felt tedium is what provokes counting-on.
    // It cannot be instructed, only occasioned.

    private fun join(level: Int, shape: ShapeKind, rng: Rng): Task.Join {
        val total = when (level) {
            1 -> rng.range(2, 3)
            2 -> rng.range(3, 4)
            else -> MAX_TOTAL
        }
        return if (level >= 4) {
            Task.Join(a = total - 1, b = 1, shape = shape)
        } else {
            val a = rng.range(1, total - 1)
            Task.Join(a = a, b = total - a, shape = shape)
        }
    }

    // -- Separating ---------------------------------------------------------
    // Take-away only. "How many more do I have?" is a comparison, it is much
    // harder, and it does not belong in an app for 3-year-olds.

    private fun separate(level: Int, shape: ShapeKind, rng: Rng): Task.Separate {
        return when (level) {
            1 -> Task.Separate(rng.range(2, 3), 1, shape)
            2 -> {
                val whole = rng.range(3, 4)
                Task.Separate(whole, rng.range(1, 2), shape)
            }
            3 -> {
                val whole = rng.range(4, MAX_TOTAL)
                Task.Separate(whole, rng.range(1, 3), shape)
            }
            // Zero gets its own moment rather than being short-circuited away.
            else -> {
                val whole = rng.range(3, MAX_TOTAL)
                Task.Separate(whole, rng.range(1, whole), shape)
            }
        }
    }
}
