package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.navigation.MainAppScaffold
import com.example.ui.theme.GeoDarkBg
import com.example.ui.theme.GeoNewsStudioTheme
import com.example.ui.viewmodel.GeoNewsViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GeoNewsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            GeoNewsStudioTheme(darkTheme = settings.isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = GeoDarkBg
                ) {
                    MainAppScaffold(viewModel = viewModel)
                }
            }
        }
    }
}
