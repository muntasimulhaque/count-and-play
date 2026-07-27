package app.maqsadah.count_and_play.data

import app.maqsadah.count_and_play.core.Progress
import app.maqsadah.count_and_play.core.Skill
import app.maqsadah.count_and_play.core.SkillRecord

/**
 * The persistence format, as pure functions over a map.
 *
 * Kept free of Android so the update path — which runs exactly once on each
 * existing tester's device and can never be re-run — is covered by unit tests
 * rather than by hoping.
 */
object Stored {
    const val FILE = "count_and_play"
    const val SCHEMA = 4

    const val KEY_SCHEMA = "schema_version"
    const val KEY_LANG = "lang"
    const val KEY_LANG_SET = "lang_set"
    const val KEY_SLOW = "slow_rate"
    const val KEY_SOUND = "sound_on"
    const val KEY_SHAPE = "shape"
    const val KEY_PROGRESS = "progress"
    const val KEY_SESSION = "session"

    fun voiceKey(languageTag: String) = "voice_$languageTag"

    /** Keys written by v3.5 that carry a decision a grown-up made deliberately. */
    private val CARRIED = setOf(KEY_LANG, KEY_LANG_SET, KEY_SLOW, "voice_EN", "voice_BN")

    /** Keys written by v3.5 that describe the old four-rung ladder. */
    private val DISCARDED = setOf("level", "stars", "voice")

    /**
     * Migrates a v3.5 preferences map to v4.
     *
     * Settings survive: a Bengali family that chose বাংলা must not open the
     * update to an English language picker. Progress does not: the old `level`
     * was a number range (3/5/10/20) and the new ladder is six skills with
     * their own rungs, so `level = 4` maps to nothing honest. A child who can
     * count re-clears the early rungs in a couple of minutes and enjoys it;
     * a child dropped past Give-N has no way back.
     */
    fun migrate(old: Map<String, Any?>): Map<String, Any?> {
        if (old[KEY_SCHEMA] == SCHEMA) return old

        val carried = old.filterKeys { it in CARRIED }
            .mapKeys { (key, _) ->
                // v3.5 keyed voices by enum name; v4 keys them by language tag.
                when (key) {
                    "voice_EN" -> voiceKey("en")
                    "voice_BN" -> voiceKey("bn")
                    else -> key
                }
            }
            .filterValues { it != null }
            .mapValues { (key, value) ->
                if (key == KEY_LANG) normaliseLanguage(value) else value
            }

        return carried + mapOf(KEY_SCHEMA to SCHEMA)
    }

    /** Keys that must be gone after a migration, so it can never run twice. */
    fun obsoleteKeys(old: Map<String, Any?>): Set<String> =
        old.keys.filter { it in DISCARDED || it == "voice_EN" || it == "voice_BN" }.toSet()

    private fun normaliseLanguage(value: Any?) = when (value) {
        "BN", "bn" -> "bn"
        else -> "en"
    }

    // -- Progress ------------------------------------------------------------

    /** `COUNT:2|GIVE_N:1` — small, readable, and forgiving of unknown skills. */
    fun encodeProgress(progress: Progress): String =
        progress.skills.entries
            .sortedBy { it.key.name }
            .joinToString("|") { (skill, record) -> "${skill.name}:${record.level}" }

    fun decodeProgress(encoded: String?, session: Int): Progress {
        if (encoded.isNullOrBlank()) return Progress(session = session)
        val skills = encoded.split("|").mapNotNull { entry ->
            val (name, level) = entry.split(":").takeIf { it.size == 2 } ?: return@mapNotNull null
            // A skill this build no longer knows about is simply dropped, so a
            // downgrade or a rename can never crash a child's saved progress.
            val skill = Skill.entries.firstOrNull { it.name == name } ?: return@mapNotNull null
            val value = level.toIntOrNull()?.coerceAtLeast(1) ?: return@mapNotNull null
            skill to SkillRecord(level = value)
        }.toMap()
        return Progress(skills = skills, session = session)
    }
}
