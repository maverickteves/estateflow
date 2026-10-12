package com.example.ui.screens

import com.example.ui.components.PersonAvatar
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AgentProfile
import com.example.data.model.UserAccount
import com.example.data.model.UserRole
import com.example.data.repository.DatabaseStats
import com.example.ui.components.ConnectionStatus
import com.example.ui.components.ConnectionStatusIndicator
import com.example.ui.dialogs.ChangePasswordDialog
import com.example.ui.theme.*
import androidx.compose.material.icons.filled.CloudSync

@Composable
fun ProfileScreen(
  userRole: UserRole,
  currentUser: UserAccount? = null,
  agentProfile: AgentProfile,
  savedPropertiesCount: Int,
  upcomingViewingsCount: Int,
  databaseStats: DatabaseStats? = null,
  connectionStatus: ConnectionStatus = ConnectionStatus.CHECKING,
  onSyncBackend: () -> Unit = {},
  onOpenClientAuth: () -> Unit = {},
  onOpenAgentLogin: () -> Unit = {},
  onLogout: () -> Unit = {},
  onResetDatabase: () -> Unit = {},
  onToggleRole: () -> Unit,
  onOpenNotifications: () -> Unit,
  onUpdatePassword: (currentPassword: String, newPassword: String) -> Unit = { _, _ -> },
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()
  var isChangePasswordOpen by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(scrollState)
      .padding(horizontal = 20.dp, vertical = 12.dp)
  ) {
    Text(
      text = "Account & Profile",
      style = MaterialTheme.typography.headlineSmall.copy(
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 22.sp
      )
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Profile Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      border = androidx.compose.foundation.BorderStroke(1.dp, SandWarm)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (userRole == UserRole.AGENT) {
            PersonAvatar(
              name = agentProfile.name,
              size = 56.dp,
              modifier = Modifier.border(1.5.dp, ForestGreen, CircleShape)
            )
          } else {
            Box(
              modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(SandWarm)
                .border(1.5.dp, if (currentUser != null) ForestGreen else SandWarm, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "User Profile Avatar",
                tint = ForestGreen,
                modifier = Modifier.size(30.dp)
              )
            }
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column(modifier = Modifier.weight(1f)) {
            val displayName = when {
              userRole == UserRole.AGENT -> agentProfile.name
              currentUser != null -> currentUser.name
              else -> "Guest Explorer"
            }
            val displayRole = when {
              userRole == UserRole.AGENT -> "${currentUser?.professionalRole ?: "Licensed Agent"} • ${agentProfile.licenseNo}"
              currentUser != null -> "Verified Client • ${currentUser.email}"
              else -> "Not signed in"
            }

            Text(
              text = displayName,
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = NaturalDark
              )
            )
            Text(
              text = displayRole,
              style = MaterialTheme.typography.bodySmall.copy(
                color = TaupeDark,
                fontSize = 12.sp
              )
            )

            if (userRole == UserRole.AGENT) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Star,
                  contentDescription = null,
                  tint = AmberPending,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = "${agentProfile.rating} (${agentProfile.reviewsCount} reviews)",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = NaturalDark,
                    fontSize = 11.sp
                  )
                )
              }
            }
          }
        }

        // Action Buttons inside user card
        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = SandWarm.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(10.dp))

        if (currentUser == null) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = onOpenClientAuth,
              modifier = Modifier
                .weight(1f)
                .testTag("profile_client_login_button"),
              colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Client Sign In", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
              onClick = onOpenAgentLogin,
              modifier = Modifier
                .weight(1f)
                .testTag("profile_agent_login_button"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Agent Login", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = ForestGreen,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (currentUser.role == UserRole.AGENT) "Agent Session Active" else "Signed in as Client",
                style = MaterialTheme.typography.bodySmall.copy(
                  color = ForestGreen,
                  fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
              onClick = onLogout,
              modifier = Modifier.testTag("profile_logout_button"),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB00020)),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Log Out", fontSize = 12.sp, maxLines = 1)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          HorizontalDivider(color = SandWarm.copy(alpha = 0.6f))
          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = ForestGreen,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Security & Password",
                style = MaterialTheme.typography.bodySmall.copy(color = NaturalDark, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
              onClick = { isChangePasswordOpen = true },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("profile_update_password_button"),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Update Password", fontSize = 12.sp, maxLines = 1)
            }
          }
        }
      }
    }

    if (userRole == UserRole.AGENT) {
      Spacer(modifier = Modifier.height(16.dp))

      // Agent Portal card: agents only. Clients never see it; agents sign in from the Welcome screen.
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("agent_portal_section_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (userRole == UserRole.AGENT) ForestGreen.copy(alpha = 0.08f) else Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
          1.dp,
          if (userRole == UserRole.AGENT) ForestGreen.copy(alpha = 0.4f) else SandWarm
        )
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .background(ForestGreen, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Badge,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                  text = "Licensed Agent Portal",
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = NaturalDark
                  ),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = if (userRole == UserRole.AGENT) "${currentUser?.professionalRole ?: "Licensed Agent"} • PRC Lic. ${agentProfile.licenseNo}" else "Broker, Agent & Admin Access",
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = TaupeDark,
                    fontSize = 12.sp
                  ),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }

            if (userRole == UserRole.AGENT) {
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "CONNECTED",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = ForestGreen,
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp
                ),
                maxLines = 1
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          if (userRole == UserRole.AGENT) {
            Text(
              text = "You are currently logged in with verified privileges as ${currentUser?.professionalRole ?: "Licensed Agent"} to publish listings, manage tour schedules, and review buyer leads.",
              style = MaterialTheme.typography.bodySmall.copy(color = NaturalDark, fontSize = 12.sp)
            )
          } else {
            Text(
              text = "Are you a licensed real estate agent, broker, or agency admin? Sign in or register your role to access professional property management.",
              style = MaterialTheme.typography.bodySmall.copy(color = TaupeDark, fontSize = 12.sp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = onOpenAgentLogin,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("select_login_agent_button"),
              colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Select Login Agent / Join Portal", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Stats Grid
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Card(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, SandWarm)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = "$savedPropertiesCount",
            style = MaterialTheme.typography.headlineMedium.copy(
              fontWeight = FontWeight.Bold,
              color = NaturalDark
            )
          )
          Text(
            text = "Saved Homes",
            style = MaterialTheme.typography.bodySmall.copy(color = TaupeDark)
          )
        }
      }

      Card(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, SandWarm)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = "$upcomingViewingsCount",
            style = MaterialTheme.typography.headlineMedium.copy(
              fontWeight = FontWeight.Bold,
              color = NaturalDark
            )
          )
          Text(
            text = "Active Viewings",
            style = MaterialTheme.typography.bodySmall.copy(color = TaupeDark)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(22.dp))

    // Settings & Quick Links
    Text(
      text = "PREFERENCES & SUPPORT",
      style = MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.Bold,
        color = TaupeMuted,
        fontSize = 11.sp,
        letterSpacing = 1.sp
      )
    )

    Spacer(modifier = Modifier.height(8.dp))

    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      border = androidx.compose.foundation.BorderStroke(1.dp, SandWarm)
    ) {
      Column {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenNotifications() }
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Notification Center & Alerts", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = NaturalDark))
          }
          Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = TaupeMuted, modifier = Modifier.size(13.dp))
        }

        HorizontalDivider(color = SandWarm, thickness = 1.dp)

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("App Version", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = NaturalDark))
          }
          Text("Estateflow v1.0", style = MaterialTheme.typography.labelSmall.copy(color = TaupeMuted))
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Connection status (no server details are shown)
    Text(
      text = "CONNECTION",
      style = MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.Bold,
        color = TaupeMuted,
        fontSize = 11.sp,
        letterSpacing = 1.sp
      )
    )

    Spacer(modifier = Modifier.height(8.dp))

    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("connection_card"),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      border = androidx.compose.foundation.BorderStroke(1.dp, SandWarm)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .background(ForestGreen.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.CloudSync,
            contentDescription = null,
            tint = ForestGreen,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Estateflow Cloud",
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.Bold,
              color = NaturalDark
            )
          )
          Spacer(modifier = Modifier.height(2.dp))
          ConnectionStatusIndicator(status = connectionStatus)
        }
        Button(
          onClick = onSyncBackend,
          modifier = Modifier.testTag("sync_backend_button"),
          colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Sync now", style = MaterialTheme.typography.labelLarge)
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }

  if (isChangePasswordOpen && currentUser != null) {
    ChangePasswordDialog(
      isOpen = isChangePasswordOpen,
      userEmail = currentUser.email,
      onDismiss = { isChangePasswordOpen = false },
      onPasswordChanged = { currentPass, newPass ->
        onUpdatePassword(currentPass, newPass)
        isChangePasswordOpen = false
      }
    )
  }
}
