package com.example.chatplataformasmqtt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.chatplataformasmqtt.ui.screen.ChatScreen
import com.example.chatplataformasmqtt.ui.theme.ChatPlataformasMqttTheme
import com.example.chatplataformasmqtt.viewmodel.ChatViewModel
import com.example.chatplataformasmqtt.viewmodel.ChatViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val viewModel = ViewModelProvider(
            this,
            ChatViewModelFactory(applicationContext)
        )[ChatViewModel::class.java]

        setContent {
            ChatPlataformasMqttTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatScreen(viewModel = viewModel)
                }
            }
        }
    }
}