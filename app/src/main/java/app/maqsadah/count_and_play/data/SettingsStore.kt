package app.maqsadah.count_and_play.data

import android.content.Context
import android.content.SharedPreferences
import app.maqsadah.count_and_play.copy.Language
import app.maqsadah.count_and_play.core.Progress
import app.maqsadah.count_and_play.core.ShapeKind

data class Settings(
    val language: Language = Language.EN,
    val languageChosen: Boolean = false,
    val slowRate: Boolean = false,
    val soundOn: Boolean = true,
    val shape: ShapeKind = ShapeKind.APPLE,
) {
    fun voiceKey() = Stored.voiceKey(language.tag)
}

/**
 * Everything that outlives a session. Thin on purpose — the decisions live in
 * [Stored], which is pure and tested.
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(Stored.FILE, Context.MODE_PRIVATE)

    init {
        migrateOnce()
    }

    /** Runs at most once per install, on the first launch after the update. */
    private fun migrateOnce() {
        val existing = prefs.all
        if (existing[Stored.KEY_SCHEMA] == Stored.SCHEMA) return

        val migrated = Stored.migrate(existing)
        val obsolete = Stored.obsoleteKeys(existing)

        prefs.edit().apply {
            // Consume the legacy keys, so no v4 code ever needs to know their
            // names and the migration cannot silently run a second time.
            obsolete.forEach(::remove)
            remove("level")
            remove("stars")
            migrated.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                }
            }
        }.apply()
    }

    fun load(): Settings {
        val tag = prefs.getString(Stored.KEY_LANG, Language.EN.tag)
        return Settings(
            language = Language.entries.firstOrNull { it.tag == tag } ?: Language.EN,
            languageChosen = prefs.getBoolean(Stored.KEY_LANG_SET, false),
            slowRate = prefs.getBoolean(Stored.KEY_SLOW, false),
            soundOn = prefs.getBoolean(Stored.KEY_SOUND, true),
            shape = prefs.getString(Stored.KEY_SHAPE, null)
                ?.let { name -> ShapeKind.entries.firstOrNull { it.name == name } }
                ?: ShapeKind.APPLE,
        )
    }

    fun save(settings: Settings) {
        prefs.edit()
            .putString(Stored.KEY_LANG, settings.language.tag)
            .putBoolean(Stored.KEY_LANG_SET, settings.languageChosen)
            .putBoolean(Stored.KEY_SLOW, settings.slowRate)
            .putBoolean(Stored.KEY_SOUND, settings.soundOn)
            .putString(Stored.KEY_SHAPE, settings.shape.name)
            .apply()
    }

    fun voiceName(language: Language): String? =
        prefs.getString(Stored.voiceKey(language.tag), null)

    fun saveVoice(language: Language, name: String?) {
        prefs.edit().putString(Stored.voiceKey(language.tag), name).apply()
    }

    fun loadProgress(): Progress = Stored.decodeProgress(
        prefs.getString(Stored.KEY_PROGRESS, null),
        prefs.getInt(Stored.KEY_SESSION, 0),
    )

    fun saveProgress(progress: Progress) {
        prefs.edit()
            .putString(Stored.KEY_PROGRESS, Stored.encodeProgress(progress))
            .putInt(Stored.KEY_SESSION, progress.session)
            .apply()
    }

    fun resetProgress() {
        prefs.edit()
            .remove(Stored.KEY_PROGRESS)
            .remove(Stored.KEY_SESSION)
            .apply()
    }
}
