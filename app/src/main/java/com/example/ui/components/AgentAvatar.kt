package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ForestGreenLight

/** The bundled headshot belongs to the demo agent only. */
fun hasDemoAgentPhoto(name: String): Boolean =
  name.trim().equals("Sarah Jenkins", ignoreCase = true)

/** Up to two initials, e.g. "Juan Dela Cruz" -> "JC". */
fun initialsOf(name: String): String {
  val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
  return when {
    words.isEmpty() -> "?"
    words.size == 1 -> words[0].take(1).uppercase()
    else -> (words.first().take(1) + words.last().take(1)).uppercase()
  }
}

/**
 * Circular avatar: the demo agent's photo, or initials for everyone else.
 * Callers add borders or click handling through [modifier].
 */
@Composable
fun PersonAvatar(
  name: String,
  size: Dp,
  modifier: Modifier = Modifier
) {
  if (hasDemoAgentPhoto(name)) {
    Image(
      painter = painterResource(id = R.drawable.agent_sarah_avatar_1787131767100),
      contentDescription = name,
      modifier = modifier
        .size(size)
        .clip(CircleShape),
      contentScale = ContentScale.Crop
    )
  } else {
    Box(
      modifier = modifier
        .size(size)
        .clip(CircleShape)
        .background(ForestGreenLight),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = initialsOf(name),
        style = TextStyle(
          fontWeight = FontWeight.Bold,
          color = ForestGreen,
          fontSize = (size.value * 0.38f).sp
        )
      )
    }
  }
}
