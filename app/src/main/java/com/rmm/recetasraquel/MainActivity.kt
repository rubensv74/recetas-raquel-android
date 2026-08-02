package com.rmm.recetasraquel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rmm.recetasraquel.app.RecetasRaquelApp
import com.rmm.recetasraquel.ui.theme.RecetasRaquelTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecetasRaquelTheme {
                RecetasRaquelApp()
            }
        }
    }
}
