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

    /**
     * Everything is available from the first minute.
     *
     * The chained version of this function (Give-N behind Count 2, Hidden behind
     * Give-N 2, Join behind Hidden 2, Separate behind Join 2) was measured by
     * playing the real rules: a child who answered *perfectly* first met adding
     * at task 41 and never met taking-away at all in eight sittings, and a child
     * who re-tapped — which is what 3-year-olds do — never left counting to
     * three. Worse, because availability was recomputed from current levels, a
     * demotion took away activities he had already been shown.
     *
     * Difficulty still adapts, but *inside* an activity. What is on the shelf
     * does not move, so nothing a child has met can ever disappear.
     */
    @Suppress("UNUSED_PARAMETER")
    fun isUnlocked(skill: Skill, progress: Progress): Boolean = true

    fun unlocked(progress: Progress): List<Skill> = Skill.entries.toList()

    // -- What the child may choose -------------------------------------------
    // He picks the number; the level decides how far the choice reaches. This is
    // choice inside a prepared environment: the ownership is real, and it cannot
    // land him outside the range where he can still see a quantity.

    /** The first (or only) number the child picks for [skill]. */
    fun pickRange(skill: Skill, level: Int): IntRange = when (skill) {
        Skill.COUNT -> 1..when (level) {
            1 -> 5
            2 -> 6
            3 -> 8
            else -> MAX_COUNT
        }
        Skill.GIVE_N -> 1..when (level) {
            1 -> 3
            2 -> 4
            else -> MAX_TOTAL
        }
        Skill.JOIN -> 1..when (level) {
            1 -> 2
            2 -> 3
            else -> MAX_TOTAL - 1
        }
        Skill.SEPARATE -> 2..when (level) {
            1 -> 3
            2 -> 4
            else -> MAX_TOTAL
        }
        // Comparison and the hidden set are about a relation the child does not
        // set, so there is nothing here for him to choose.
        Skill.COMPARE, Skill.HIDDEN -> IntRange.EMPTY
    }

    /** The second number, once [first] is settled. Never lets a total escape [MAX_TOTAL]. */
    fun secondPickRange(skill: Skill, first: Int): IntRange = when (skill) {
        Skill.JOIN -> 1..(MAX_TOTAL - first)
        Skill.SEPARATE -> 1..first
        else -> IntRange.EMPTY
    }

    fun needsTwoPicks(skill: Skill): Boolean = skill == Skill.JOIN || skill == Skill.SEPARATE

    /** Builds the task the child just described with his own taps. */
    fun taskFrom(skill: Skill, shape: ShapeKind, first: Int, second: Int): Task = when (skill) {
        Skill.COUNT -> Task.CountIt(first, shape)
        Skill.GIVE_N -> Task.GiveMe(first, (first + 3).coerceAtMost(MAX_COUNT), shape)
        Skill.JOIN -> Task.Join(first, second, shape)
        Skill.SEPARATE -> Task.Separate(first, second, shape)
        Skill.COMPARE, Skill.HIDDEN ->
            error("$skill is not built from the child's own numbers")
    }

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
