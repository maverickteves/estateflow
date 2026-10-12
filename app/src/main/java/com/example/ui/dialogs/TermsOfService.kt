package com.example.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ForestGreen

private const val TERMS_LAST_UPDATED = "October 12, 2026"

private val termsSections = listOf(
  "1. About Estateflow" to
    "Estateflow is an app for browsing property listings in Cebu, booking viewings and contacting real estate agents. By creating an account you agree to these terms.",
  "2. Your account" to
    "Give accurate details when you sign up and keep your password private. You are responsible for activity on your account. We may suspend accounts that are used for fraud, spam or abuse.",
  "3. Listings and information" to
    "Listings, prices and availability are provided by agents and may change without notice. Estateflow does not own the properties shown and does not guarantee that a listing is accurate or still available. Always verify details with the agent before making any payment or commitment.",
  "4. Agents" to
    "Agents who register must hold a valid PRC real estate licence and follow Republic Act No. 9646 (Real Estate Service Act). Agents are responsible for the listings they publish and for their dealings with clients.",
  "5. Viewings and messages" to
    "Viewing requests and messages are sent to the listing agent. Estateflow is not a party to any sale, lease or agreement between clients and agents.",
  "6. Acceptable use" to
    "Do not post false listings, impersonate others, harass other users, or try to disrupt or misuse the app.",
  "7. Privacy" to
    "We collect the details you give us (such as your name, email and phone number) to run your account, arrange viewings and connect you with agents. We handle personal data in line with Republic Act No. 10173 (Data Privacy Act of 2012).",
  "8. Limitation of liability" to
    "The app is provided \"as is\". To the extent allowed by law, Estateflow is not liable for losses arising from listings, transactions between users, or service interruptions.",
  "9. Changes" to
    "We may update these terms. Continuing to use the app after an update means you accept the new terms.",
  "10. Contact" to
    "Questions about these terms can be sent to the Estateflow team through the app."
)

/** "☐ I agree to the Terms of Service" row used on the sign-up forms. */
@Composable
fun TermsAgreementRow(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  onOpenTerms: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Checkbox(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = CheckboxDefaults.colors(checkedColor = ForestGreen),
      modifier = Modifier.size(24.dp).testTag("terms_checkbox")
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = "I agree to the ",
      style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp),
      modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
    Text(
      text = buildAnnotatedString {
        withStyle(SpanStyle(color = ForestGreen, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)) {
          append("Terms of Service")
        }
      },
      style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
      modifier = Modifier
        .clickable(onClick = onOpenTerms)
        .testTag("terms_link")
    )
  }
}

@Composable
fun TermsOfServiceDialog(
  onDismiss: () -> Unit,
  onAgree: (() -> Unit)? = null
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(20.dp),
    modifier = Modifier.testTag("terms_dialog"),
    title = { Text("Terms of Service", fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier
          .heightIn(max = 420.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Text(
          text = "Last updated: $TERMS_LAST_UPDATED",
          style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        termsSections.forEach { (heading, body) ->
          Spacer(modifier = Modifier.padding(top = 12.dp))
          Text(heading, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
          Spacer(modifier = Modifier.padding(top = 4.dp))
          Text(body, style = MaterialTheme.typography.bodySmall)
        }
      }
    },
    confirmButton = {
      if (onAgree != null) {
        TextButton(onClick = { onAgree(); onDismiss() }) {
          Text("I Agree", color = ForestGreen, fontWeight = FontWeight.Bold)
        }
      } else {
        TextButton(onClick = onDismiss) { Text("Close", color = ForestGreen) }
      }
    },
    dismissButton = if (onAgree != null) {
      { TextButton(onClick = onDismiss) { Text("Close") } }
    } else {
      null
    }
  )
}
