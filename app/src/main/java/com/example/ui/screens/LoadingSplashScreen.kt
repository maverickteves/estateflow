package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ForestGreenLight
import com.example.ui.theme.NaturalBg

/** Logo screen shown for a moment while the app starts up. */
@Composable
fun LoadingSplashScreen(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(NaturalBg)
      // Swallow taps so nothing underneath reacts while the logo is up.
      .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
      .testTag("loading_splash_screen"),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Image(
        painter = painterResource(id = R.drawable.estateflow_logo),
        contentDescription = "Estateflow",
        modifier = Modifier.width(220.dp)
      )
      Spacer(modifier = Modifier.height(36.dp))
      CircularProgressIndicator(
        color = ForestGreen,
        trackColor = ForestGreenLight,
        strokeWidth = 3.dp,
        modifier = Modifier.size(28.dp)
      )
    }
  }
}
