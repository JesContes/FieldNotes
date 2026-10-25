package com.fieldnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fieldnotes.ui.nav.AppNav
import com.fieldnotes.ui.theme.FieldNotesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FieldNotesTheme {
                AppNav()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as FieldNotesApp).container.onAppResumed()
    }
}
