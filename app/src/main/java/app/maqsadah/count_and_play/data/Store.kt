package app.maqsadah.count_and_play.data

import android.content.Context
import app.maqsadah.count_and_play.copy.Language
import app.maqsadah.count_and_play.core.Adapt

/**
 * Everything that survives a restart: the grown-up's language and mute
 * choices, and the three invisible difficulty levels. Six values, so plain
 * SharedPreferences is the whole format. Levels are persisted, streaks are
 * not (a streak is a run, and a run ends when the app does).
 */
class Store(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var language: Language?
        get() = runCatching {
            Language.entries.firstOrNull { it.name == prefs.getString(KEY_LANGUAGE, null) }
        }.getOrNull()
        set(value) {
            runCatching { prefs.edit().putString(KEY_LANGUAGE, value?.name).apply() }
        }

    var languageChosen: Boolean
        get() = runCatching { prefs.getBoolean(KEY_LANGUAGE_CHOSEN, false) }.getOrDefault(false)
        set(value) {
            runCatching { prefs.edit().putBoolean(KEY_LANGUAGE_CHOSEN, value).apply() }
        }

    var muted: Boolean
        get() = runCatching { prefs.getBoolean(KEY_MUTED, false) }.getOrDefault(false)
        set(value) {
            runCatching { prefs.edit().putBoolean(KEY_MUTED, value).apply() }
        }

    var levelCount: Int
        get() = readLevel(KEY_LEVEL_COUNT)
        set(value) {
            writeLevel(KEY_LEVEL_COUNT, value)
        }

    var levelAdd: Int
        get() = readLevel(KEY_LEVEL_ADD)
        set(value) {
            writeLevel(KEY_LEVEL_ADD, value)
        }

    var levelTake: Int
        get() = readLevel(KEY_LEVEL_TAKE)
        set(value) {
            writeLevel(KEY_LEVEL_TAKE, value)
        }

    private fun readLevel(key: String): Int = runCatching {
        prefs.getInt(key, 0)
    }.getOrDefault(0).coerceIn(0, Adapt.MAX_LEVEL)

    private fun writeLevel(key: String, value: Int) {
        val clamped = value.coerceIn(0, Adapt.MAX_LEVEL)
        runCatching { prefs.edit().putInt(key, clamped).apply() }
    }

    private companion object {
        const val FILE = "count_and_play"
        const val KEY_LANGUAGE = "language"
        const val KEY_LANGUAGE_CHOSEN = "language_chosen"
        const val KEY_MUTED = "muted"
        const val KEY_LEVEL_COUNT = "level_count"
        const val KEY_LEVEL_ADD = "level_add"
        const val KEY_LEVEL_TAKE = "level_take"
    }
}
