package com.dias.shelfwise.data

import android.content.Context
import com.dias.shelfwise.domain.ReadingRecord
import com.dias.shelfwise.domain.UserLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException

interface LibraryStore {
    fun load(): UserLibrary
    suspend fun save(library: UserLibrary)
}

class PreferencesLibraryStore(context: Context) : LibraryStore {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    override fun load(): UserLibrary = try {
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

    override suspend fun save(library: UserLibrary) = withContext(Dispatchers.IO) {
        val records = JSONObject()
        library.records.forEach { (id, record) ->
            records.put(id, JSONObject().put("saved", record.saved).put("borrowedAt", record.borrowedAt ?: JSONObject.NULL)
                .put("pagesRead", record.pagesRead).put("note", record.note))
        }
        val json = JSONObject().put("version", 1).put("goalPages", library.goalPages).put("records", records)
        if (!preferences.edit().putString(KEY, json.toString()).commit()) throw IOException("Could not persist library")
    }
    companion object { const val PREFS = "shelfwise_library"; const val KEY = "library_v1" }
}
