package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.core.firebase.configureAppCheck
import com.example.ui.navigation.CanvaAoRootApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize Firebase App Check before any Firestore / Auth / AI Logic operations
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            CanvaAoRootApp()
        }
    }
}
