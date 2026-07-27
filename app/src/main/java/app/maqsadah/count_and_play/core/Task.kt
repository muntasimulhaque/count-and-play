package app.maqsadah.count_and_play.core

/**
 * The six things this app teaches, in the order a child grows into them.
 *
 * There are no "modes". The ladder decides what comes next and the child just
 * plays; the old build's GUIDED / FREE / QUIZ were three parallel implementations
 * of one round flow.
 */
enum class Skill {
    /** Tap each object once; the last count word names the whole set. */
    COUNT,

    /** "Put three berries in the bowl." The child *produces* a quantity. */
    GIVE_N,

    /** Two bowls — which has more? Includes length-vs-number conflict trials. */
    COMPARE,

    /** Under the leaf: nonverbal addition and subtraction on a hidden set. */
    HIDDEN,

    /** Two dishes pour into one bowl. Addition, in the open, with a prediction. */
    JOIN,

    /** One bowl pours into a dish. Subtraction — the same picture, reversed. */
    SEPARATE,
}

/**
 * A single thing to do. [answer] is computed, never stored: the old build's
 * `Problem(op, a, b, answer)` made `Problem("+", 2, 3, 99)` representable.
 */
sealed interface Task {
    val skill: Skill
    val shape: ShapeKind
    val answer: Int

    /** Tap-count a five-framed set and say how many. */
    data class CountIt(
        val n: Int,
        override val shape: ShapeKind,
    ) : Task {
        override val skill = Skill.COUNT
        override val answer = n
    }

    /** Produce a set of [n] from a heap of [pool]. The keystone diagnostic. */
    data class GiveMe(
        val n: Int,
        val pool: Int,
        override val shape: ShapeKind,
    ) : Task {
        override val skill = Skill.GIVE_N
        override val answer = n
    }

    /** Which dish has more? [spread] marks a conflict trial: fewer objects, more space. */
    data class WhichIsMore(
        val left: Int,
        val right: Int,
        val spread: Boolean,
        override val shape: ShapeKind,
    ) : Task {
        override val skill = Skill.COMPARE
        override val answer = maxOf(left, right)
        val moreSide: Zone = if (left > right) Zone.DISH_A else Zone.DISH_B
    }

    /**
     * [start] objects go under the leaf, then [delta] visibly join or leave.
     * The child builds the total as a set — no numerals, no words needed.
     */
    data class UnderTheLeaf(
        val start: Int,
        val delta: Int,
        override val shape: ShapeKind,
    ) : Task {
        override val skill = Skill.HIDDEN
        override val answer = start + delta
    }

    /** [a] and [b] in two dishes, poured together. Addition made visible. */
    data class Join(
        val a: Int,
        val b: Int,
        override val shape: ShapeKind,
    ) : Task {
        override val skill = Skill.JOIN
        override val answer = a + b
    }

    /** [take] of [whole] travel out to the gone-dish. What is left? */
    data class Separate(
        val whole: Int,
        val take: Int,
        override val shape: ShapeKind,
    ) : Task {
        override val skill = Skill.SEPARATE
        override val answer = whole - take
    }
}

/** The largest set the app will ever show. Arithmetic stays at or below [MAX_TOTAL]. */
const val MAX_TOTAL = 5

/** Pure counting practice may go higher than arithmetic, but never past a ten-frame. */
const val MAX_COUNT = 10
