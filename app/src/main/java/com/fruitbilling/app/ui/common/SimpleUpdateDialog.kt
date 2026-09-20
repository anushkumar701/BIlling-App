package com.fruitbilling.app.ui.common

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fruitbilling.app.util.AppReleaseInfo

/**
 * Concise, non-intrusive OTA update dialog (§Over-The-Air Updates).
 * Avoids verbose release notes and technical definitions.
 */
@Composable
fun SimpleUpdateDialog(
    releaseInfo: AppReleaseInfo,
    onConfirmUpdate: () -> Unit,
    onDismissToday: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissToday,
        shape = RoundedCornerShape(16.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = "Update Available",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "New Update Available",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "A new version of Fruit Billing (${releaseInfo.latestVersion}) is ready to install.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Installed: v${releaseInfo.currentVersion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmUpdate,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Update Now", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismissToday,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Later")
            }
        }
    )
}
