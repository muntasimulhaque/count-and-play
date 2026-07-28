package app.maqsadah.count_and_play.core

/**
 * The free tray: a heap, a bowl, and nothing at all to get wrong.
 *
 * Every other activity asks the child a question. This one never does — he puts
 * objects in, takes them out, and the app simply names what he has made. That
 * is the whole design.
 *
 * It earns its place for two reasons. It is the one part of the app a child can
 * be *wrong* in only by the app's refusal to say so, which makes it the natural
 * home for a 3-year-old who wants to play rather than answer; and naming a set
 * the child assembled himself — "five apples" — is the plainest form of the
 * count-to-cardinal lesson the rest of the app works towards.
 */
data class FreeState(val shape: ShapeKind, val tokens: List<Token>) {
    val made: Int get() = tokens.countIn(Zone.BOWL)
}

object FreePlay {

    fun begin(shape: ShapeKind): FreeState = FreeState(
        shape = shape,
        tokens = (0 until MAX_COUNT).map { slot ->
            Token(id = slot, shape = shape, zone = Zone.SOURCE, slot = slot)
        },
    )

    /** One tap moves one object, either direction. That is the entire rule set. */
    fun onTap(state: FreeState, id: Int): Pair<FreeState, Script> {
        val token = state.tokens.firstOrNull { it.id == id }
            ?: return state to script { cue(Sfx.HOLLOW) }

        val to = if (token.zone == Zone.SOURCE) Zone.BOWL else Zone.SOURCE
        val moved = state.tokens
            .moveTo(setOf(id), to)
            .compact(Zone.SOURCE)
            .compact(Zone.BOWL)
        val next = state.copy(tokens = moved)

        return next to script {
            show(StageChange.Travel(listOf(id), to))
            cue(if (to == Zone.BOWL) Sfx.THUD else Sfx.HOLLOW)
            // Named after every change, in the cardinal voice. No question is
            // asked, so there is no answer to be right or wrong about — the
            // number is just a fact about what is now in the bowl.
            pause(Pace.TINY)
            if (next.made == 0) settle(Line.NothingLeft) else settle(Line.Cardinal(next.made, state.shape))
        }
    }
}
