package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.remote.BackendConnectionInfo
import com.example.data.remote.BackendConnectionState
import com.example.ui.theme.AmberPending
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.RoseFavorite

/** What the user sees about the server: whether the app is connected, nothing more. */
enum class ConnectionStatus(val label: String) {
  ONLINE("Online"),
  CHECKING("Connecting"),
  OFFLINE("Offline")
}

fun BackendConnectionInfo?.toConnectionStatus(): ConnectionStatus = when (this?.state) {
  BackendConnectionState.CONNECTED -> ConnectionStatus.ONLINE
  BackendConnectionState.ERROR -> ConnectionStatus.OFFLINE
  // OFFLINE is the state before the first status check has finished.
  BackendConnectionState.CONNECTING, BackendConnectionState.OFFLINE, null -> ConnectionStatus.CHECKING
}

private fun ConnectionStatus.color(): Color = when (this) {
  ConnectionStatus.ONLINE -> OnlineGreen
  ConnectionStatus.CHECKING -> AmberPending
  ConnectionStatus.OFFLINE -> RoseFavorite
}

@Composable
fun ConnectionStatusDot(
  status: ConnectionStatus,
  modifier: Modifier = Modifier,
  size: Dp = 8.dp
) {
  val color by animateColorAsState(targetValue = status.color(), label = "connection_dot_color")
  // Only pulse while a check is in progress, so the steady states don't animate forever.
  val dotAlpha = if (status == ConnectionStatus.CHECKING) {
    val pulse = rememberInfiniteTransition(label = "connection_dot_pulse")
    val pulseAlpha by pulse.animateFloat(
      initialValue = 1f,
      targetValue = 0.35f,
      animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
      label = "connection_dot_alpha"
    )
    pulseAlpha
  } else {
    1f
  }
  Box(
    modifier = modifier
      .size(size)
      .alpha(dotAlpha)
      .background(color, CircleShape)
  )
}

/** Small "● Online" indicator. */
@Composable
fun ConnectionStatusIndicator(
  status: ConnectionStatus,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .semantics(mergeDescendants = true) { contentDescription = "Server status: ${status.label}" }
      .testTag("connection_status_indicator"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp)
  ) {
    ConnectionStatusDot(status = status)
    Text(
      text = status.label,
      style = MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    )
  }
}
