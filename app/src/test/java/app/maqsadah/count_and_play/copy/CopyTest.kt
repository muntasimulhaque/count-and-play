package app.maqsadah.count_and_play.copy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CopyTest {

    private val packs = listOf(EnCopy, BnCopy)

    /** The ten shape names from the old Shapes enum. */
    private val shapeNames = listOf(
        "APPLE", "PEAR", "STAR", "LEAF", "BLOCK", "BEAD", "MELON", "CARROT", "TULIP", "BALL",
    )

    @Test
    fun numberWordsCoverZeroToTwenty() {
        for (copy in packs) {
            for (n in 0..20) {
                assertTrue("word($n) blank", copy.numberWord(n).isNotBlank())
            }
        }
    }

    @Test
    fun numberWordsAreDistinctThroughTwelve() {
        for (copy in packs) {
            val words = (0..12).map(copy::numberWord)
            assertEquals(words.size, words.distinct().size)
        }
    }

    @Test
    fun englishDigitsAreAscii() {
        for (n in 0..99) {
            assertEquals(n.toString(), EnCopy.digits(n))
            assertTrue(EnCopy.digits(n).all { it in '0'..'9' })
        }
    }

    @Test
    fun bengaliDigitsMapExactly() {
        val numerals = "০১২৩৪৫৬৭৮৯"
        for (d in 0..9) {
            assertEquals(numerals[d].toString(), BnCopy.digits(d))
        }
        assertEquals("১২", BnCopy.digits(12))
        assertEquals("২০", BnCopy.digits(20))
    }

    @Test
    fun itemNameCoversAllTenShapes() {
        for (copy in packs) {
            for (name in shapeNames) {
                assertTrue("itemName($name) blank", copy.itemName(name).isNotBlank())
            }
        }
    }

    @Test
    fun bengaliItemNamesAreTheOldPacksVerbatim() {
        assertEquals("আপেল", BnCopy.itemName("APPLE"))
        assertEquals("নাশপাতি", BnCopy.itemName("PEAR"))
        assertEquals("তারা", BnCopy.itemName("STAR"))
        assertEquals("পাতা", BnCopy.itemName("LEAF"))
        assertEquals("ব্লক", BnCopy.itemName("BLOCK"))
        assertEquals("পুঁতি", BnCopy.itemName("BEAD"))
        assertEquals("তরমুজ", BnCopy.itemName("MELON"))
        assertEquals("গাজর", BnCopy.itemName("CARROT"))
        assertEquals("টিউলিপ", BnCopy.itemName("TULIP"))
        assertEquals("বল", BnCopy.itemName("BALL"))
    }

    @Test
    fun englishFixedLinesAreTheContract() {
        assertEquals("What shall we play?", EnCopy.homeTitle())
        assertEquals("Count them", EnCopy.tileCount())
        assertEquals("Put together", EnCopy.tileAdd())
        assertEquals("Take away", EnCopy.tileTake())
        assertEquals("Tap each one and count!", EnCopy.promptCount())
        assertEquals("Put them together!", EnCopy.promptAdd())
        assertEquals("Take away two!", EnCopy.promptTake(2))
        assertEquals("Count them all!", EnCopy.promptAll())
        assertEquals("How many are left?", EnCopy.promptLeft())
        assertEquals("Four!", EnCopy.cardinal(4))
        assertEquals("Three and two is five!", EnCopy.factAdd(3, 2, 5))
        assertEquals("Five take away two is three!", EnCopy.factTake(5, 2, 3))
        assertEquals("Well done!", EnCopy.celebrate())
    }

    @Test
    fun englishFactsEmbedTheNumberWords() {
        val add = EnCopy.factAdd(3, 2, 5).lowercase()
        for (n in listOf(3, 2, 5)) {
            assertTrue("factAdd missing $n", add.contains(EnCopy.numberWord(n)))
        }

        val take = EnCopy.factTake(5, 2, 3).lowercase()
        for (n in listOf(5, 2, 3)) {
            assertTrue("factTake missing $n", take.contains(EnCopy.numberWord(n)))
        }

        assertTrue(EnCopy.promptTake(2).lowercase().contains(EnCopy.numberWord(2)))
    }

    @Test
    fun bengaliFactsEmbedTheNumberWords() {
        val add = BnCopy.factAdd(3, 2, 5)
        for (n in listOf(3, 2, 5)) {
            assertTrue("factAdd missing ${BnCopy.numberWord(n)}", add.contains(BnCopy.numberWord(n)))
        }

        val take = BnCopy.factTake(5, 2, 3)
        for (n in listOf(5, 2, 3)) {
            assertTrue("factTake missing ${BnCopy.numberWord(n)}", take.contains(BnCopy.numberWord(n)))
        }
        assertTrue("factTake names the operation বিয়োগ", take.contains("বিয়োগ"))

        assertTrue(BnCopy.promptTake(2).contains(BnCopy.numberWord(2)))
    }

    @Test
    fun bengaliTemplatedLinesKeepTheOldVoice() {
        assertEquals("ট্যাপ করে গুনো!", BnCopy.promptCount())
        assertEquals("একসাথে করো!", BnCopy.promptAdd())
        assertEquals("দুইটি বাদ দাও!", BnCopy.promptTake(2))
        assertEquals("সবগুলো গুনো!", BnCopy.promptAll())
        assertEquals("কতগুলো রইলো?", BnCopy.promptLeft())
        assertEquals("তিন যোগ দুই হয় পাঁচ!", BnCopy.factAdd(3, 2, 5))
        assertEquals("পাঁচ বিয়োগ দুই হয় তিন!", BnCopy.factTake(5, 2, 3))
        assertEquals("সাবাস!", BnCopy.celebrate())
    }

    @Test
    fun copyOfReturnsTheMatchingPack() {
        assertSame(EnCopy, copyOf(Language.EN))
        assertSame(BnCopy, copyOf(Language.BN))
    }

    @Test
    fun noTemplateIsBlankOrCarriesFormatMarkers() {
        for (copy in packs) {
            val lang = copy.javaClass.simpleName
            val lines = mutableListOf<String>()
            for (n in 0..20) {
                lines += copy.numberWord(n)
                lines += copy.digits(n)
                lines += copy.cardinal(n)
            }
            for (n in 21..99) lines += copy.digits(n)
            for (name in shapeNames) lines += copy.itemName(name)
            lines += copy.homeTitle()
            lines += copy.tileCount()
            lines += copy.tileAdd()
            lines += copy.tileTake()
            lines += copy.promptCount()
            lines += copy.promptAdd()
            lines += copy.promptAll()
            lines += copy.promptLeft()
            for (b in 1..10) lines += copy.promptTake(b)
            for (a in 0..5) for (b in 0..5) if (a + b <= 10) lines += copy.factAdd(a, b, a + b)
            for (n in 0..10) for (b in 0..n) lines += copy.factTake(n, b, n - b)
            lines += copy.celebrate()

            for (line in lines) {
                assertTrue("$lang: blank line", line.isNotBlank())
                assertFalse("$lang: unreplaced { in '$line'", line.contains('{'))
                assertFalse("$lang: unreplaced % in '$line'", line.contains('%'))
            }
        }
    }
}
