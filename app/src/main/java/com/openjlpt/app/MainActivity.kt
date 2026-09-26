package com.openjlpt.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.openjlpt.app.ui.AppNavHost
import com.openjlpt.app.ui.theme.OpenJlptTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as OpenJlptApp).container
        setContent {
            OpenJlptTheme {
                AppNavHost(container)
            }
        }
    }
}
