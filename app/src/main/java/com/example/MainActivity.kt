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
import com.example.ui.AttendanceViewModel
import com.example.ui.MainLayout
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val viewModel: AttendanceViewModel by viewModels {
    AttendanceViewModel.provideFactory(application)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val userConfig by viewModel.userConfig.collectAsStateWithLifecycle()
      MyApplicationTheme(
        darkTheme = userConfig.isDarkMode,
        dynamicColor = false
      ) {
        Surface(modifier = Modifier.fillMaxSize()) {
          MainLayout(viewModel = viewModel)
        }
      }
    }
  }
}
