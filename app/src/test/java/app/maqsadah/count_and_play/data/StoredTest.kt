package app.maqsadah.count_and_play.data

import app.maqsadah.count_and_play.core.Progress
import app.maqsadah.count_and_play.core.Skill
import app.maqsadah.count_and_play.core.SkillRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Existing closed-testing users update in place, so their v3.5 preferences file
 * arrives intact. This runs once per device and cannot be re-run — so it is
 * tested rather than trusted.
 */
class StoredTest {

    /** Exactly what v3.5 leaves behind for a Bengali-speaking tester at level 4. */
    private val v35 = mapOf<String, Any?>(
        "level" to 4,
        "stars" to 37,
        "slow_rate" to true,
        "lang" to "BN",
        "lang_set" to true,
        "voice_EN" to "en-us-x-sfg#male_1",
        "voice_BN" to "bn-in-x-bik#female_1",
    )

    @Test
    fun `a parent's deliberate settings survive the update`() {
        val migrated = Stored.migrate(v35)

        assertEquals("bn", migrated[Stored.KEY_LANG])
        assertEquals(true, migrated[Stored.KEY_LANG_SET])
        assertEquals(true, migrated[Stored.KEY_SLOW])
        assertEquals("en-us-x-sfg#male_1", migrated[Stored.voiceKey("en")])
        assertEquals("bn-in-x-bik#female_1", migrated[Stored.voiceKey("bn")])
    }

    @Test
    fun `the old four-rung progress does not survive, because it maps to nothing`() {
        val migrated = Stored.migrate(v35)
        assertNull(migrated["level"])
        assertNull(migrated["stars"])
        assertFalse(migrated.containsKey(Stored.KEY_PROGRESS))
    }

    @Test
    fun `a returning tester never sees the language picker again`() {
        assertEquals(true, Stored.migrate(v35)[Stored.KEY_LANG_SET])
    }

    @Test
    fun `migration is one-shot and idempotent`() {
        val once = Stored.migrate(v35)
        val twice = Stored.migrate(once)
        assertEquals(once, twice)
        assertEquals(Stored.SCHEMA, once[Stored.KEY_SCHEMA])
    }

    @Test
    fun `a fresh install migrates to a clean slate without inventing settings`() {
        val migrated = Stored.migrate(emptyMap())
        assertEquals(mapOf<String, Any?>(Stored.KEY_SCHEMA to Stored.SCHEMA), migrated)
    }

    @Test
    fun `the legacy single-voice key is not resurrected`() {
        val migrated = Stored.migrate(v35 + ("voice" to "old-voice"))
        assertNull(migrated["voice"])
    }

    @Test
    fun `obsolete keys are named so they can be deleted from the file`() {
        val obsolete = Stored.obsoleteKeys(v35)
        assertTrue(obsolete.containsAll(setOf("level", "stars", "voice_EN", "voice_BN")))
    }

    @Test
    fun `migration does not invent a language nobody chose`() {
        val migrated = Stored.migrate(mapOf("lang" to null, "lang_set" to false))
        assertNull("an absent choice stays absent; the store defaults to English", migrated[Stored.KEY_LANG])
        assertEquals(false, migrated[Stored.KEY_LANG_SET])
    }

    @Test
    fun `the old enum-named language value becomes a language tag`() {
        assertEquals("bn", Stored.migrate(mapOf("lang" to "BN"))[Stored.KEY_LANG])
        assertEquals("en", Stored.migrate(mapOf("lang" to "EN"))[Stored.KEY_LANG])
        assertEquals("en", Stored.migrate(mapOf("lang" to "nonsense"))[Stored.KEY_LANG])
    }

    // -- Progress round-trip --------------------------------------------------

    @Test
    fun `progress survives a round trip`() {
        val progress = Progress(
            skills = mapOf(
                Skill.COUNT to SkillRecord(level = 3),
                Skill.GIVE_N to SkillRecord(level = 2),
            ),
            session = 9,
        )
        val decoded = Stored.decodeProgress(Stored.encodeProgress(progress), 9)

        assertEquals(3, decoded.level(Skill.COUNT))
        assertEquals(2, decoded.level(Skill.GIVE_N))
        assertEquals(9, decoded.session)
        assertEquals("an untouched skill stays at the start", 1, decoded.level(Skill.JOIN))
    }

    @Test
    fun `unreadable progress degrades to a fresh start instead of crashing`() {
        for (junk in listOf("", "   ", "garbage", "COUNT", "COUNT:", ":3", "COUNT:x", "OLD_SKILL:2")) {
            val decoded = Stored.decodeProgress(junk, session = 0)
            assertEquals("junk \"$junk\" should be survivable", 1, decoded.level(Skill.COUNT))
        }
        assertEquals(1, Stored.decodeProgress(null, 0).level(Skill.COUNT))
    }

    @Test
    fun `a level below the floor is clamped rather than trusted`() {
        assertEquals(1, Stored.decodeProgress("COUNT:0", 0).level(Skill.COUNT))
        assertEquals(1, Stored.decodeProgress("COUNT:-5", 0).level(Skill.COUNT))
    }

    @Test
    fun `a skill this build does not know is dropped, not fatal`() {
        val decoded = Stored.decodeProgress("COUNT:2|TIME_TRAVEL:9", 0)
        assertEquals(2, decoded.level(Skill.COUNT))
        assertEquals(1, decoded.skills.size)
    }
}
