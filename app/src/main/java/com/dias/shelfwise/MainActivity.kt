package com.dias.shelfwise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.dias.shelfwise.data.PreferencesLibraryStore
import com.dias.shelfwise.ui.ShelfwiseApp
import com.dias.shelfwise.ui.ShelfwiseTheme

class MainActivity : ComponentActivity() {
    private val libraryViewModel by viewModels<LibraryViewModel> {
        LibraryViewModel.Factory(PreferencesLibraryStore(applicationContext))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ShelfwiseTheme { ShelfwiseApp(libraryViewModel) } }
    }
}
