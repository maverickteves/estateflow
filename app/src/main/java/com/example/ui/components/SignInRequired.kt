package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ForestGreenLight
import com.example.ui.theme.NaturalDark
import com.example.ui.theme.SandWarm
import com.example.ui.theme.TaupeMuted

/** Shown in place of a tab's content when the feature needs a client account. */
@Composable
fun SignInRequiredScreen(
  title: String,
  icon: ImageVector,
  heading: String,
  message: String,
  onSignIn: () -> Unit,
  onExplore: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("sign_in_required_screen")
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.headlineSmall.copy(
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 22.sp
      ),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp)
    )

    HorizontalDivider(color = SandWarm, thickness = 1.dp)

    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(32.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {
          Box(
            modifier = Modifier
              .size(72.dp)
              .background(ForestGreenLight, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = ForestGreen,
              modifier = Modifier.size(34.dp)
            )
          }
          Box(
            modifier = Modifier
              .size(26.dp)
              .background(ForestGreen, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(14.dp)
            )
          }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
          text = heading,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = NaturalDark
          ),
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = message,
          style = MaterialTheme.typography.bodyMedium.copy(color = TaupeMuted),
          textAlign = TextAlign.Center,
          modifier = Modifier.widthIn(max = 320.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
          onClick = onSignIn,
          colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .widthIn(min = 220.dp)
            .height(46.dp)
            .testTag("sign_in_required_button")
        ) {
          Text("Sign In or Create Account", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        TextButton(onClick = onExplore) {
          Text("Keep browsing listings", color = ForestGreen)
        }
      }
    }
  }
}
