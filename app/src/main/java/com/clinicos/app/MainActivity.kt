package com.clinicos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.clinicos.app.navigation.ClinicOSAppEntry
import com.clinicos.app.ui.theme.ClinicOSTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClinicOSTheme {
                ClinicOSAppEntry()
            }
        }
    }
}
