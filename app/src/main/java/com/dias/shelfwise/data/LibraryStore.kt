package com.dias.shelfwise.data

import android.content.Context
import com.dias.shelfwise.domain.ReadingRecord
import com.dias.shelfwise.domain.UserLibrary
import org.json.JSONObject

// Read-only bridge for the previous homework. All new writes go through Room.
class LegacyPreferencesReader(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun load(): UserLibrary = try {
        val raw = preferences.getString(KEY, null)
        if (raw == null) UserLibrary() else {
            val root = JSONObject(raw)
            val data = root.optJSONObject("records") ?: JSONObject()
            val records = data.keys().asSequence().mapNotNull { id ->
                val book = Catalog.book(id) ?: return@mapNotNull null
                val item = data.getJSONObject(id)
                id to ReadingRecord(item.optBoolean("saved"), if (item.isNull("borrowedAt")) null else item.getLong("borrowedAt"),
                    item.optInt("pagesRead").coerceIn(0, book.pages), item.optString("note").take(1000))
            }.toMap()
            UserLibrary(records, root.optInt("goalPages", 300).coerceIn(50, 5000))
        }
    } catch (_: Exception) { UserLibrary() }

    companion object { const val PREFS = "shelfwise_library"; const val KEY = "library_v1" }
}
