package com.obratrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.obratrack.app.ui.ObraTrackNav
import com.obratrack.app.ui.theme.ObraTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ObraTrackTheme {
                ObraTrackNav()
            }
        }
    }
}
