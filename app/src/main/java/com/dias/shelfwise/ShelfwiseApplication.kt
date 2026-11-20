package com.dias.shelfwise

import android.app.Application
import androidx.room.Room
import com.dias.shelfwise.data.LibraryDatabase
import com.dias.shelfwise.data.LegacyPreferencesReader
import com.dias.shelfwise.data.RoomLibraryRepository

class ShelfwiseApplication : Application() {
    val database: LibraryDatabase by lazy {
        Room.databaseBuilder(this, LibraryDatabase::class.java, "shelfwise.db").build()
    }
    val repository by lazy {
        RoomLibraryRepository(database, readLegacy = { LegacyPreferencesReader(this).load() })
    }
}
