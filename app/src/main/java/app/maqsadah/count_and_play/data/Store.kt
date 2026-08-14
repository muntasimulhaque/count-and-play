package app.maqsadah.count_and_play.data

import android.content.Context
import app.maqsadah.count_and_play.copy.Language

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
        get() = Language.entries.firstOrNull { it.name == prefs.getString(KEY_LANGUAGE, null) }
        set(value) {
            prefs.edit().putString(KEY_LANGUAGE, value?.name).apply()
        }

    var languageChosen: Boolean
        get() = prefs.getBoolean(KEY_LANGUAGE_CHOSEN, false)
        set(value) {
            prefs.edit().putBoolean(KEY_LANGUAGE_CHOSEN, value).apply()
        }

    var muted: Boolean
        get() = prefs.getBoolean(KEY_MUTED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_MUTED, value).apply()
        }

    var levelCount: Int
        get() = prefs.getInt(KEY_LEVEL_COUNT, 0)
        set(value) {
            prefs.edit().putInt(KEY_LEVEL_COUNT, value).apply()
        }

    var levelAdd: Int
        get() = prefs.getInt(KEY_LEVEL_ADD, 0)
        set(value) {
            prefs.edit().putInt(KEY_LEVEL_ADD, value).apply()
        }

    var levelTake: Int
        get() = prefs.getInt(KEY_LEVEL_TAKE, 0)
        set(value) {
            prefs.edit().putInt(KEY_LEVEL_TAKE, value).apply()
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
