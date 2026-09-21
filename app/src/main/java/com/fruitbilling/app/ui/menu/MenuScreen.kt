package com.fruitbilling.app.ui.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import com.fruitbilling.app.data.backup.CloudBackupManager
import com.fruitbilling.app.util.rememberProductImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.fruitbilling.app.data.backup.GoogleAuthManager
import com.fruitbilling.app.ui.common.PrivacyPolicyDialog
import com.fruitbilling.app.ui.common.SimpleUpdateDialog
import com.fruitbilling.app.ui.common.TermsOfServiceDialog
import com.fruitbilling.app.util.DateUtils
import androidx.compose.ui.unit.sp
import com.fruitbilling.app.data.model.Product
import com.fruitbilling.app.util.MoneyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    viewModel: MenuViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.initContextData(context)
        viewModel.snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val userResult = GoogleAuthManager.handleSignInResult(context, result.data)
        userResult.onSuccess { user ->
            viewModel.onGoogleSignInSuccess(user, context)
        }.onFailure { error ->
            viewModel.onGoogleSignInFailure(error)
        }
    }


    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Menu & Products",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with Products title and Add Button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Product Catalog (${uiState.products.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (uiState.products.isNotEmpty()) {
                            OutlinedButton(
                                onClick = viewModel::onPromptClearAll,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Make Blank", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }

                        Button(
                            onClick = viewModel::onOpenAddDialog,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Add")
                        }
                    }
                }
            }

            // Empty state if catalog is blank
            if (uiState.products.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Catalog is currently blank",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "You can add your own custom fruits, or load standard professional fruit items with one tap.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = viewModel::onOpenAddDialog,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("+ Add Fruit", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = viewModel::onLoadProfessionalDefaults,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Load Standard Fruits", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Products list
            items(uiState.products, key = { it.id }) { product ->
                ProductRowItem(
                    product = product,
                    onEdit = { viewModel.onOpenEditDialog(product) },
                    onDelete = { viewModel.onPromptDelete(product) },
                    onToggleActive = { viewModel.onToggleActive(product) }
                )
            }

            // Google Account & Cloud Backup Section (§Google Login to Save Data)
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "☁️", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Google Account & Cloud Backup",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        // Google Sign-In Status
                        if (uiState.googleUser != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = uiState.googleUser?.displayName ?: "Google Account",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = uiState.googleUser?.email ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.onSignOut(context) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("Log Out", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    signInLauncher.launch(GoogleAuthManager.getSignInIntent(context))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "🔑 Sign in with Google Account",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "Sign in with Google to automatically back up your fruit inventory and bills to Google Cloud, and restore across reinstalls.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        // Backup Status & Action
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Cloud Status",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = CloudBackupManager.getLastSyncFormatted(context),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                )
                            }

                            Button(
                                onClick = { viewModel.onBackupToDrive(context) },
                                enabled = !uiState.isBackingUp,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (uiState.isBackingUp) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Syncing…")
                                } else {
                                    Text("☁️ Sync Now")
                                }
                            }
                        }

                        // Export / Share Backup and Cloud Restore Action
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.onShareBackupFile(context) },
                                enabled = !uiState.isBackingUp,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("📧 Share File", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.onPromptRestoreFromCloud()
                                },
                                enabled = !uiState.isRestoring,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (uiState.isRestoring) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text("🔄 Restore Cloud", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // OTA Updates Section (§Over-The-Air Updates)
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🚀", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Over-The-Air (OTA) Updates",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Installed: v${com.fruitbilling.app.BuildConfig.VERSION_NAME}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.onCheckForUpdates(userInitiated = true) },
                                enabled = !uiState.isCheckingUpdate,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (uiState.isCheckingUpdate) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Checking…")
                                } else {
                                    Text("Check Updates")
                                }
                            }
                        }
                    }
                }
            }

            // Settings & App Info Section
            item {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Fruit Billing POS • Production v${com.fruitbilling.app.BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Text(
                            text = "feedback-midnightcompiler01@gmail.com",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:feedback-midnightcompiler01@gmail.com")
                                        putExtra(Intent.EXTRA_SUBJECT, "Fruit Billing App Feedback")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Send Feedback"))
                                } catch (_: Exception) {}
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Terms & Conditions",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                modifier = Modifier.clickable { showTermsDialog = true }
                            )
                            Text(
                                text = "Privacy Policy",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                modifier = Modifier.clickable { showPrivacyDialog = true }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Add Dialog
        if (uiState.isAddDialogOpen) {
            AddEditProductDialog(
                product = null,
                onDismiss = viewModel::onCloseDialog,
                onSave = viewModel::onSaveProduct
            )
        }

        // Edit Dialog
        uiState.editingProduct?.let { productToEdit ->
            AddEditProductDialog(
                product = productToEdit,
                onDismiss = viewModel::onCloseDialog,
                onSave = viewModel::onSaveProduct
            )
        }

        // Delete Confirmation Dialog
        uiState.deleteConfirmationProduct?.let { productToDelete ->
            DeleteConfirmationDialog(
                product = productToDelete,
                onConfirm = viewModel::onConfirmDelete,
                onDismiss = viewModel::onCancelDelete
            )
        }

        // Cloud Restore Confirmation Dialog
        if (uiState.isRestoreConfirmationOpen) {
            AlertDialog(
                onDismissRequest = viewModel::onDismissRestorePrompt,
                title = {
                    Text(
                        text = "Restore from Cloud?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Text(
                        text = "This will download and restore your saved fruits and historical bills for ${uiState.googleUser?.email ?: "your account"} from Google Cloud.\n\nExisting records will be safely updated without losing any data.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.onConfirmRestoreFromCloud(context) },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Restore Data")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::onDismissRestorePrompt) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Clear All Confirmation Dialog
        if (uiState.isClearAllDialogOpen) {
            AlertDialog(
                onDismissRequest = viewModel::onDismissClearAll,
                title = {
                    Text(
                        text = "Make Catalog Blank?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Text(
                        text = "This will clear all ${uiState.products.size} fruit items from your catalog so you can start with a clean blank slate.\n\nYou can add custom fruits or load standard items anytime.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = viewModel::onConfirmClearAll,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Make Blank")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::onDismissClearAll) {
                        Text("Cancel")
                    }
                }
            )
        }

        // OTA Update Available Dialog — clean & minimal
        if (uiState.showUpdateDialog && uiState.updateReleaseInfo != null) {
            SimpleUpdateDialog(
                releaseInfo = uiState.updateReleaseInfo!!,
                onConfirmUpdate = { viewModel.onInstallUpdate(context) },
                onDismissToday = viewModel::onDismissUpdateDialog
            )
        }

        if (showTermsDialog) {
            TermsOfServiceDialog(onDismiss = { showTermsDialog = false })
        }

        if (showPrivacyDialog) {
            PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
        }
    }
}

@Composable
fun ProductRowItem(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (product.active) 1f else 0.5f),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imageBitmap = rememberProductImage(product.iconRef)
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (imageBitmap != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        if (!product.active) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Inactive",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${MoneyUtils.formatWholePrice(product.price)}${product.unit.unitLabel}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Deactivate/Activate (§8) -- reversible, keeps the product out of
                // billing without touching any historical bill that used it.
                OutlinedButton(
                    onClick = onToggleActive,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (product.active) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (product.active) "Deactivate" else "Activate",
                        modifier = Modifier.size(14.dp)
                    )
                }

                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(6.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
