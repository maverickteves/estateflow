package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.NaturalBg
import com.example.ui.theme.NaturalDark
import com.example.ui.theme.SandMuted
import com.example.ui.theme.SandWarm
import com.example.ui.theme.TaupeDark

@Composable
fun WelcomeScreen(
  onGetStarted: () -> Unit,
  onAgentPortalClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  var visible by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    visible = true
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            NaturalBg,
            SandMuted,
            Color.White
          )
        )
      )
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Spacer(modifier = Modifier.height(16.dp))

      // Hero Header & Branding
      AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -40 })
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // Brand Icon Badge
          Box(
            modifier = Modifier
              .size(80.dp)
              .clip(CircleShape)
              .background(
                Brush.linearGradient(
                  colors = listOf(ForestGreen, Color(0xFF384034))
                )
              )
              .border(2.dp, LimeAccent, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Home,
              contentDescription = "Estateflow Logo",
              tint = Color.White,
              modifier = Modifier.size(42.dp)
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Brand Name
          Text(
            text = "Estateflow",
            style = MaterialTheme.typography.displaySmall.copy(
              fontWeight = FontWeight.ExtraBold,
              color = ForestGreen,
              letterSpacing = (-0.5).sp
            )
          )

          Spacer(modifier = Modifier.height(6.dp))

          Surface(
            shape = RoundedCornerShape(20.dp),
            color = ForestGreen.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForestGreen.copy(alpha = 0.25f))
          ) {
            Text(
              text = "LUXURY REAL ESTATE SANCTUARY",
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = ForestGreen,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp
              )
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Text(
            text = "Discover premier properties, schedule private tours, and connect directly with verified real estate professionals.",
            style = MaterialTheme.typography.bodyMedium.copy(
              color = TaupeDark,
              fontSize = 14.5.sp,
              lineHeight = 21.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Feature Highlights
      AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { 40 })
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          WelcomeFeatureCard(
            icon = Icons.Default.Home,
            title = "Explore Exclusive Listings",
            description = "Browse verified houses, apartments, condos, and townhouses in Cebu City with rich media."
          )

          WelcomeFeatureCard(
            icon = Icons.Default.CalendarMonth,
            title = "Instant Viewing Reservation",
            description = "Book private tour appointments seamlessly with PostgreSQL real-time calendar synchronization."
          )

          WelcomeFeatureCard(
            icon = Icons.Default.ChatBubbleOutline,
            title = "Direct Agent Messaging",
            description = "Inquire and chat directly with licensed Realtors and listing agents with instant replies."
          )
        }
      }

      Spacer(modifier = Modifier.height(32.dp))

      // Action Buttons Footer
      AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { 60 })
      ) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Button(
            onClick = onGetStarted,
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("welcome_get_started_button"),
            colors = ButtonDefaults.buttonColors(
              containerColor = ForestGreen,
              contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp)
          ) {
            Text(
              text = "Get Started & Explore",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }

          OutlinedButton(
            onClick = onAgentPortalClick,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("welcome_agent_portal_button"),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForestGreen)
          ) {
            Icon(
              imageVector = Icons.Default.Badge,
              contentDescription = null,
              tint = ForestGreen,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Sign In as Licensed Agent / Broker",
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = ForestGreen
              )
            )
          }
        }
      }
    }
  }
}

@Composable
private fun WelcomeFeatureCard(
  icon: ImageVector,
  title: String,
  description: String
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, SandWarm)
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(ForestGreen.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = ForestGreen,
          modifier = Modifier.size(22.dp)
        )
      }

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            color = NaturalDark
          )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = description,
          style = MaterialTheme.typography.bodySmall.copy(
            color = TaupeDark,
            fontSize = 12.sp,
            lineHeight = 16.sp
          )
        )
      }
    }
  }
}
