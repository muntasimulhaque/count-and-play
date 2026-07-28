package app.maqsadah.count_and_play.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonTest {

    private val apple = ShapeKind.APPLE

    // -- The safety property ------------------------------------------------

    @Test
    fun `no task, at any level, from any seed, can strand the child`() {
        for (skill in Skill.entries) {
            for (level in 1..Advancement.maxLevel(skill)) {
                for (seed in 0L until 120L) {
                    val task = Ladder.taskFor(skill, level, apple, SeededRng(seed))
                    val played = Play.task(task)
                    assertEquals(
                        "expected exactly one result from $task",
                        1,
                        played.results.size,
                    )
                }
            }
        }
    }

    @Test
    fun `a child who gets everything wrong still always reaches the end`() {
        for (skill in Skill.entries) {
            for (level in 1..Advancement.maxLevel(skill)) {
                val task = Ladder.taskFor(skill, level, apple, SeededRng(level.toLong()))
                val played = Play.task(task, accuracy = Play.Accuracy.OVER)
                assertEquals(Step.Finished, played.state.step)
                assertEquals(1, played.results.size)
            }
        }
    }

    @Test
    fun `every tap is acknowledged - a tap is never silently swallowed`() {
        var outcome = Lesson.begin(Task.CountIt(3, apple), Progress())
        val ids = outcome.state.tokens.map { it.id }

        // Including taps that do nothing at all: an unresponsive screen reads as
        // broken to a 3-year-old, not as "wait".
        for (id in ids + listOf(9999)) {
            val next = Lesson.onEvent(outcome.state, Event.TapToken(id))
            assertTrue("tap on $id produced silence", next.script.beats.isNotEmpty())
        }
        val stray = Lesson.onEvent(outcome.state, Event.Done)
        assertTrue("a stray done produced silence", stray.script.beats.isNotEmpty())
    }

    // -- Counting -----------------------------------------------------------

    @Test
    fun `the voice never counts ahead of the child`() {
        val opening = Lesson.begin(Task.CountIt(3, apple), Progress()).script
        assertTrue(
            "a number was spoken before the child touched anything",
            opening.lines().none { it is Line.CountWord || it is Line.Cardinal },
        )
    }

    @Test
    fun `the last tap pauses, collapses the tags, then names the whole`() {
        val played = Play.task(Task.CountIt(3, apple))
        val beats = played.scripts.flatMap { it.beats }

        val collapseAt = beats.indexOfFirst {
            it is Beat.Show && it.change is StageChange.Collapse
        }
        val cardinalAt = beats.indexOfFirst { it is Beat.Say && it.line is Line.Cardinal }
        assertTrue("no cardinal was ever spoken", cardinalAt >= 0)
        assertTrue("the tags must collapse before the whole is named", collapseAt in 0 until cardinalAt)

        val pauseBefore = beats.subList(0, collapseAt).last { it is Beat.Pause } as Beat.Pause
        assertEquals(
            "the count-to-cardinal pause is load-bearing",
            Pace.CARDINAL,
            pauseBefore.pace,
        )
        assertTrue(
            "the cardinal must be spoken settled - lower and slower",
            (beats[cardinalAt] as Beat.Say).settled,
        )
    }

    @Test
    fun `re-tapping a counted object is recorded, not blocked`() {
        var outcome = Lesson.begin(Task.CountIt(3, apple), Progress())
        val first = outcome.state.tokens.first()

        outcome = Lesson.onEvent(outcome.state, Event.TapToken(first.id))
        assertEquals(1, outcome.state.tokens.first { it.id == first.id }.ordinal)

        outcome = Lesson.onEvent(outcome.state, Event.TapToken(first.id))
        assertEquals("the re-tap must be counted as a signal", 1, outcome.state.retaps)
        assertEquals(
            "and it must not consume an ordinal",
            1,
            outcome.state.tokens.count { it.isCounted },
        )
        assertEquals(listOf(Sfx.HOLLOW), outcome.script.beats.filterIsInstance<Beat.Cue>().map { it.sound })
    }

    /**
     * A single re-tap used to fail the count outright, and the cost was steep:
     * the child was silently scored wrong, eased down a level, and the *next*
     * task was made easier too — so the more eagerly he played, the further the
     * app's picture of him drifted from the truth. Played over eight sittings, a
     * child who re-tapped every third tap never reached adding at all.
     *
     * Enthusiasm is now tolerated. Drumming still is not.
     */
    @Test
    fun `an eager re-tap still counts, drumming on the tray does not`() {
        val clean = Play.task(Task.CountIt(3, apple))
        assertTrue(clean.results.single().correct)

        assertTrue("one re-tap is a 3-year-old, not a broken count", countOf(3, retaps = 1).correct)
        assertTrue(countOf(5, retaps = 2).correct)

        assertFalse("a child hitting everything repeatedly is not counting", countOf(3, retaps = 4).correct)
        assertFalse(countOf(5, retaps = 5).correct)
    }

    /** Counts a set of [n], hammering the first object [retaps] extra times. */
    private fun countOf(n: Int, retaps: Int): TaskResult {
        var outcome = Lesson.begin(Task.CountIt(n, apple), Progress())
        val first = outcome.state.tokens[0].id
        var result = outcome.result
        outcome = Lesson.onEvent(outcome.state, Event.TapToken(first))
        result = result ?: outcome.result
        repeat(retaps) {
            outcome = Lesson.onEvent(outcome.state, Event.TapToken(first))
            result = result ?: outcome.result
        }
        while (outcome.state.step != Step.Finished) {
            outcome = Lesson.onEvent(outcome.state, Play.move(outcome.state)!!)
            result = result ?: outcome.result
        }
        return result!!
    }

    @Test
    fun `the voice lends count words only while the child needs them`() {
        val borrowing = Play.task(Task.CountIt(3, apple), Progress())
        assertTrue(
            "a child who cannot count yet must hear the sequence",
            borrowing.scripts.allLines().any { it is Line.CountWord },
        )

        val fluent = Progress(skills = mapOf(Skill.COUNT to SkillRecord(level = 3)))
        val silent = Play.task(Task.CountIt(3, apple), fluent)
        assertTrue(
            "once he owns sets of three the voice must go quiet for three",
            silent.scripts.allLines().none { it is Line.CountWord },
        )
        assertTrue(
            "but it still names the whole",
            silent.scripts.allLines().any { it is Line.Cardinal },
        )
        assertTrue(
            "and it still lends the sequence for a larger set",
            Play.task(Task.CountIt(8, apple), fluent).scripts.allLines()
                .any { it is Line.CountWord },
        )
    }

    // -- Give-N -------------------------------------------------------------

    @Test
    fun `give-N is scored on where the child stops`() {
        val right = Play.task(Task.GiveMe(3, 6, apple))
        assertTrue(right.results.single().correct)
        assertTrue(right.scripts.allSounds().contains(Sfx.CHIME))

        val over = Play.task(Task.GiveMe(3, 6, apple), accuracy = Play.Accuracy.OVER)
        val result = over.results.single()
        assertTrue("four given for three is not correct", !result.correct)
        assertEquals(4, result.given)
    }

    @Test
    fun `a missed give-N is answered by counting, never by correction`() {
        val over = Play.task(Task.GiveMe(2, 6, apple), accuracy = Play.Accuracy.OVER)
        val lines = over.scripts.allLines()
        assertTrue("he should be invited to count what he made", lines.contains(Line.LetsCount))
        assertTrue(
            "the request must not be repeated at him",
            lines.count { it is Line.GiveN } == 1,
        )
    }

    @Test
    fun `objects can always be taken back out of the bowl`() {
        var outcome = Lesson.begin(Task.GiveMe(3, 6, apple), Progress())
        val token = outcome.state.tokens.first()

        outcome = Lesson.onEvent(outcome.state, Event.TapToken(token.id))
        assertEquals(1, outcome.state.tokens.countIn(Zone.BOWL))

        outcome = Lesson.onEvent(outcome.state, Event.TapToken(token.id))
        assertEquals(
            "a child who cannot undo cannot decide when to stop",
            0,
            outcome.state.tokens.countIn(Zone.BOWL),
        )
    }

    // -- Joining and separating ---------------------------------------------

    @Test
    fun `the parts stay visibly inside the whole after the pour`() {
        val played = Play.task(Task.Join(3, 2, apple))
        val inBowl = played.state.tokens.inZone(Zone.BOWL)

        assertEquals(5, inBowl.size)
        assertEquals(
            "three objects must still show they came from the first dish",
            3,
            inBowl.count { it.origin == Zone.DISH_A },
        )
        assertEquals(2, inBowl.count { it.origin == Zone.DISH_B })
    }

    @Test
    fun `the join is reversible - the whole pours back into its parts`() {
        val played = Play.task(Task.Join(3, 2, apple))
        val lines = played.scripts.allLines()
        assertTrue(
            "without the pour-back, addition is an event rather than a relation",
            lines.any { it is Line.AndBackAgain },
        )
        assertTrue(lines.any { it == Line.AllTogetherNow })
    }

    @Test
    fun `a prediction is taken before the join, not after`() {
        val played = Play.task(Task.Join(2, 2, apple))
        val beats = played.scripts.flatMap { it.beats }
        val asked = beats.indexOfFirst { it is Beat.Say && it.line == Line.HowManyAltogether }
        val poured = beats.indexOfFirst {
            it is Beat.Show && (it.change as? StageChange.Travel)?.to == Zone.BOWL
        }
        assertTrue("the child must commit before the evidence arrives", asked in 0 until poured)
    }

    @Test
    fun `taking away moves objects to a visible dish rather than fading them`() {
        val played = Play.task(Task.Separate(5, 2, apple))
        assertEquals("what left must still be somewhere", 2, played.state.tokens.countIn(Zone.DISH_B))
        assertEquals(3, played.state.tokens.countIn(Zone.BOWL))
        assertTrue(
            "the remainder must compact so it can be seen at a glance",
            played.state.tokens.inZone(Zone.BOWL).map { it.slot }.sorted() == listOf(0, 1, 2),
        )
    }

    @Test
    fun `overshooting a take-away is allowed and simply named`() {
        val played = Play.task(Task.Separate(4, 1, apple), accuracy = Play.Accuracy.OVER)
        assertTrue(
            "he is told what he made, not stopped from making it",
            played.scripts.allLines().any { it is Line.TooMany },
        )
        assertEquals(Step.Finished, played.state.step)
    }

    @Test
    fun `taking everything gets its own moment for zero`() {
        val played = Play.task(Task.Separate(3, 3, apple))
        assertTrue(played.scripts.allLines().contains(Line.NothingLeft))
        assertEquals(0, played.results.single().expected)
    }

    // -- The nudge ladder ---------------------------------------------------

    @Test
    fun `the nudge ladder escalates then stops talking for good`() {
        var state = Lesson.begin(Task.CountIt(3, apple), Progress()).state

        val first = Lesson.onEvent(state, Event.Nudge)
        assertTrue(
            "the first nudge must be silent and spatial",
            first.script.lines().isEmpty(),
        )
        assertTrue(first.script.beats.any { it is Beat.Show && it.change is StageChange.Highlight })
        state = first.state

        val second = Lesson.onEvent(state, Event.Nudge)
        assertTrue(second.script.lines().isNotEmpty())
        state = second.state

        val third = Lesson.onEvent(state, Event.Nudge)
        assertTrue(third.script.lines().any { it is Line.NudgeModel })
        state = third.state

        repeat(5) {
            val quiet = Lesson.onEvent(state, Event.Nudge)
            assertTrue(
                "a prompt on a loop teaches a child to ignore the voice",
                quiet.script.isEmpty,
            )
            state = quiet.state
        }
    }

    @Test
    fun `a nudge points at one object, never at all of them`() {
        val state = Lesson.begin(Task.CountIt(5, apple), Progress()).state
        val highlight = Lesson.onEvent(state, Event.Nudge).script.beats
            .filterIsInstance<Beat.Show>()
            .map { it.change }
            .filterIsInstance<StageChange.Highlight>()
            .single()
        assertEquals(
            "pulsing everything says 'something', not 'here'",
            1,
            highlight.ids.size,
        )
    }

    // -- Returning to the app -----------------------------------------------

    @Test
    fun `coming back from the background re-states the instruction`() {
        val state = Lesson.begin(Task.GiveMe(3, 6, apple), Progress()).state
        assertEquals(listOf(Line.GiveN(3, apple)), Lesson.reprompt(state).lines())

        val finished = state.copy(step = Step.Finished)
        assertTrue(Lesson.reprompt(finished).isEmpty)
    }

    // -- Sound discipline ---------------------------------------------------

    @Test
    fun `the chime is reserved for success and never becomes a melody`() {
        val played = Play.task(Task.GiveMe(2, 6, apple))
        val sounds = played.scripts.allSounds()
        assertEquals("more than one chime in a task edges toward a tune", 1, sounds.count { it == Sfx.CHIME })

        val missed = Play.task(Task.GiveMe(2, 6, apple), accuracy = Play.Accuracy.OVER)
        assertEquals(
            "a missed answer gets no chime and no sad sound either",
            0,
            missed.scripts.allSounds().count { it == Sfx.CHIME },
        )
    }

    @Test
    fun `there is no failure line anywhere in a wrong answer`() {
        for (task in listOf(
            Task.Join(2, 2, apple),
            Task.Separate(4, 2, apple),
            Task.UnderTheLeaf(2, 1, apple),
            Task.GiveMe(3, 6, apple),
        )) {
            val missed = Play.task(task, accuracy = Play.Accuracy.OVER)
            assertNotNull(missed.results.single())
            assertNull(
                "nothing in the app may tell a child he is wrong",
                missed.scripts.allLines().firstOrNull {
                    it is Line.NudgeGentle && missed.state.step == Step.Finished
                },
            )
        }
    }
}
