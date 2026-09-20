package com.fruitbilling.app.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * First-launch onboarding modal:
 * Welcomes the vendor, offers Google login for instant automatic backup restore,
 * provides an offline guest option, and ensures Google Play Store compliance
 * by displaying Terms of Service & Privacy Policy.
 */
@Composable
fun OnboardingDialog(
    onSignInWithGoogle: () -> Unit,
    onContinueOffline: () -> Unit
) {
    var showTerms by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { /* Modal: require choice */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // App Logo / Fruit icon
                Text(text = "🍎", fontSize = 48.sp)

                Text(
                    text = "Welcome to Fruit Billing",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Fast, reliable & 100% offline-first POS for fruit stalls & market vendors.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "⚡ Instant pricing for 500g, 1kg, and custom weights",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "💾 All sales & products stored 100% locally on device",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "☁️ Sign in with Google to auto-restore your data anytime",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action 1: Sign In with Google
                Button(
                    onClick = onSignInWithGoogle,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🔑 Sign in with Google Account",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                // Action 2: Continue Offline / Guest
                OutlinedButton(
                    onClick = onContinueOffline,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🏪 Start Billing Offline (Guest)",
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }

                // Terms & Privacy Notice
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "By continuing, you agree to our",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Terms & Conditions",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.clickable { showTerms = true }
                        )
                        Text(
                            text = "  •  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Privacy Policy",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.clickable { showPrivacy = true }
                        )
                    }
                }
            }
        }
    }

    if (showTerms) {
        TermsOfServiceDialog(onDismiss = { showTerms = false })
    }
    if (showPrivacy) {
        PrivacyPolicyDialog(onDismiss = { showPrivacy = false })
    }
}
