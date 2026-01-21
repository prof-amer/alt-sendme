package com.altsendme.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.altsendme.app.ui.AltSendmeApp
import com.altsendme.app.ui.theme.AltSendmeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AltSendmeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AltSendmeTheme.colors.background
                ) {
                    AltSendmeApp()
                }
            }
        }
    }
}
