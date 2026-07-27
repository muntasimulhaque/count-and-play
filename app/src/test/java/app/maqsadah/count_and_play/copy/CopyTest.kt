package app.maqsadah.count_and_play.copy

import app.maqsadah.count_and_play.core.Line
import app.maqsadah.count_and_play.core.ShapeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CopyTest {

    private val shape = ShapeKind.APPLE

    private val everyLine = listOf(
        Line.CountWord(3),
        Line.Cardinal(3, shape),
        Line.CountThem,
        Line.HowMany,
        Line.GiveN(3, shape),
        Line.GaveIt(3),
        Line.LetsCount,
        Line.TooMany(3, 2),
        Line.WhichHasMore,
        Line.ThisHasMore(5),
        Line.WhatsUnder,
        Line.MakeItHere,
        Line.PartsNamed(3, 2),
        Line.HowManyAltogether,
        Line.AllTogetherNow,
        Line.MakesTotal(3, 2, 5),
        Line.AndBackAgain(3, 2),
        Line.TakeOut(2),
        Line.HowManyLeft,
        Line.WeMade(5),
        Line.NothingLeft,
        Line.NudgeGentle,
        Line.NudgeModel(1),
        Line.SessionDone,
    )

    @Test
    fun `every line has words in every language`() {
        for (language in Language.entries) {
            val copy = copyFor(language)
            for (line in everyLine) {
                assertTrue("$language has nothing to say for $line", copy.speak(line).isNotBlank())
            }
        }
    }

    @Test
    fun `instructions stay inside a three-year-old's sentence length`() {
        // Seven words is the ceiling. The old build routinely stacked two
        // imperatives — "Now take away two! Tap two apples!" — and a 3-year-old
        // acts on roughly the last four words.
        for (line in everyLine) {
            val words = CopyEn.speak(line).split(" ").size
            assertTrue("too long to act on: \"${CopyEn.speak(line)}\"", words <= 8)
        }
    }

    @Test
    fun `no line anywhere tells a child he is wrong`() {
        val forbidden = listOf("wrong", "try again", "oops", "sorry", "no,", "not quite", "fail")
        for (line in everyLine) {
            val said = CopyEn.speak(line).lowercase()
            for (word in forbidden) {
                assertFalse("\"$said\" contains \"$word\"", said.contains(word))
            }
        }
    }

    @Test
    fun `bengali marks cardinality with a classifier, and only on the cardinal`() {
        assertEquals("তিন", CopyBn.speak(Line.CountWord(3)))
        assertTrue(
            "the cardinal must carry টা — that is the audible cue to cardinality",
            CopyBn.speak(Line.Cardinal(3, shape)).contains("তিনটা"),
        )
        assertFalse(
            "a bare count word must never carry the classifier",
            CopyBn.speak(Line.CountWord(3)).contains("টা"),
        )
    }

    @Test
    fun `bengali speaks like a parent, not like a textbook`() {
        // Only the *classifier* is under test — a number word followed by টি.
        // Ordinary words such as বাটি (bowl) contain the same letters and are
        // none of our business.
        val writtenClassifier = Regex("(এক|দুই|তিন|চার|পাঁচ|ছয়|সাত|আট|নয়|দশ)টি")
        for (line in everyLine) {
            val said = CopyBn.speak(line)
            assertFalse(
                "যোগ is a school word; a parent says আর — in \"$said\"",
                said.contains("যোগ"),
            )
            assertFalse(
                "টি is the written register; spoken narration uses টা — in \"$said\"",
                writtenClassifier.containsMatchIn(said),
            )
        }
    }

    @Test
    fun `bengali draws its own numerals`() {
        assertEquals("৩", CopyBn.digits(3))
        assertEquals("১০", CopyBn.digits(10))
        assertEquals("০", CopyBn.digits(0))
        assertEquals("10", CopyEn.digits(10))
    }

    @Test
    fun `zero is a number, not a missing one`() {
        assertTrue(CopyBn.speak(Line.Cardinal(0, shape)).contains("শূন্য"))
        assertFalse(
            "শূন্যটা is not a thing",
            CopyBn.speak(Line.Cardinal(0, shape)).contains("শূন্যটা"),
        )
        assertTrue(CopyEn.speak(Line.NothingLeft).contains("Zero"))
    }

    @Test
    fun `english pluralises the objects it names`() {
        assertEquals("one apple", "one " + CopyEn.noun(ShapeKind.APPLE, 1))
        assertEquals("three apples", "three " + CopyEn.noun(ShapeKind.APPLE, 3))
        assertEquals("three leaves", "three " + CopyEn.noun(ShapeKind.LEAF, 3))
    }

    @Test
    fun `bengali leaves the noun alone, because the classifier carries the count`() {
        assertEquals(CopyBn.noun(ShapeKind.APPLE, 1), CopyBn.noun(ShapeKind.APPLE, 5))
    }

    @Test
    fun `every shape can be named in every language`() {
        for (language in Language.entries) {
            val copy = copyFor(language)
            for (shape in ShapeKind.all) {
                assertTrue(copy.noun(shape, 1).isNotBlank())
                assertTrue(copy.noun(shape, 3).isNotBlank())
            }
        }
    }
}
