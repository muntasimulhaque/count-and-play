package app.maqsadah.count_and_play.core

/**
 * The countable objects.
 *
 * This enum *is* the no-animate-beings rule made structural: there is no code
 * path that renders a countable except through one of these ten, so a creature
 * cannot drift into the app later by accident. Four fruits, one vegetable, two
 * plant forms, two made objects, one abstract star. No face, no eyes, no limbs,
 * in any state or animation.
 */
enum class ShapeKind {
    APPLE,
    PEAR,
    STAR,
    LEAF,
    BLOCK,
    BEAD,
    MELON,
    CARROT,
    TULIP,
    BALL;

    companion object {
        val all: List<ShapeKind> = entries
    }
}
