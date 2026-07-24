package app.maqsadah.count_and_play

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests for the [LangPack] string packs (pure Kotlin, no Android). They
 * lock down the numeral conversion (Bengali shows ০-৯), number-word coverage, and
 * the noun phrasing each language uses — and guard the English pack against drift.
 */
class LangTest {

    /* ---------- digits ---------- */

    @Test
    fun enDigits_areWestern() {
        assertEquals("0", EnPack.digits(0))
        assertEquals("1234567890", EnPack.digits(1234567890))
        assertEquals("20", EnPack.digits(G.MAX_N))
    }

    @Test
    fun bnDigits_areBengali() {
        assertEquals("০", BnPack.digits(0))
        assertEquals("১২৩৪৫৬৭৮৯০", BnPack.digits(1234567890))
        assertEquals("২০", BnPack.digits(G.MAX_N))
    }

    /* ---------- number words cover the whole reachable range ---------- */

    @Test
    fun words_coverZeroThroughMax() {
        for (n in 0..G.MAX_N) {
            assertTrue("EN word $n", EnPack.word(n).isNotBlank())
            assertTrue("BN word $n", BnPack.word(n).isNotBlank())
        }
        assertEquals("zero", EnPack.word(0))
        assertEquals("twenty", EnPack.word(G.MAX_N))
        assertEquals("শূন্য", BnPack.word(0))
        assertEquals("বিশ", BnPack.word(G.MAX_N))
    }

    /* ---------- noun phrasing ---------- */

    @Test
    fun enCountPhrase_pluralizes() {
        assertEquals("three apples", EnPack.countPhrase(3, "apples"))
        assertEquals("one apple", EnPack.countPhrase(1, "apples"))
        assertEquals("one strawberry", EnPack.countPhrase(1, "strawberries"))
        assertEquals("two mangoes", EnPack.countPhrase(2, "mangoes"))
    }

    @Test
    fun bnCountPhrase_usesClassifier_notPlural() {
        // Bengali leaves the noun unchanged and adds the classifier "টি".
        assertEquals("তিনটি আপেল", BnPack.countPhrase(3, "আপেল"))
        assertEquals("একটি আপেল", BnPack.countPhrase(1, "আপেল"))
    }

    /* ---------- item names ---------- */

    @Test
    fun itemNames_coverEveryEmoji() {
        G.ITEMS.forEach { emoji ->
            assertTrue("EN name for $emoji", EnPack.itemName(emoji) != "things")
            assertTrue("BN name for $emoji", BnPack.itemName(emoji) != "things")
        }
        assertEquals("apples", EnPack.itemName("🍎"))
        assertEquals("আপেল", BnPack.itemName("🍎"))
    }

    /* ---------- enum wiring ---------- */

    @Test
    fun lang_packsAreWired() {
        assertTrue(Lang.EN.pack === EnPack)
        assertTrue(Lang.BN.pack === BnPack)
        assertEquals("en", Lang.EN.locale.language)
        assertEquals("bn", Lang.BN.locale.language)
    }
}
