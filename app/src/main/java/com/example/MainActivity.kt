package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.screens.MainMapScreen
import com.example.ui.theme.HuntAlignTheme
import com.example.ui.viewmodel.HuntMapViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: HuntMapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HuntAlignTheme {
                MainMapScreen(viewModel = viewModel)
            }
        }
    }
}
