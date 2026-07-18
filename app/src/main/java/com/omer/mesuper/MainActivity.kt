package com.omer.mesuper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.omer.mesuper.core.ui.MeSuperTheme
import com.omer.mesuper.feature.dashboard.ui.ProofScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeSuperTheme {
                ProofScreen()
            }
        }
    }
}
