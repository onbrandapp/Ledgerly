package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BackupData
import com.example.data.BackupManager
import com.example.data.BackupMetadata
import com.example.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreSheet(
    viewModel: ExpenseViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val localBackups by viewModel.localBackups.collectAsState()
    val cloudBackups by viewModel.cloudBackups.collectAsState()
    val isLoading by viewModel.isBackupLoading.collectAsState()
    val currentUserEmail by viewModel.currentUserEmail.collectAsState()
    val isCloudAvailable = viewModel.isCloudAvailable

    // 0 = Local, 1 = Cloud
    var selectedTab by remember { mutableIntStateOf(0) }

    // Dialog state
    var backupToRestore by remember { mutableStateOf<BackupMetadata?>(null) }
    var fileToRestore by remember { mutableStateOf<File?>(null) }
    var uriToRestore by remember { mutableStateOf<Uri?>(null) }
    var backupToDelete by remember { mutableStateOf<BackupMetadata?>(null) }
    var justCreatedFile by remember { mutableStateOf<File?>(null) }
    var isRestoring by remember { mutableStateOf(false) }
    var restoringBackupId by remember { mutableStateOf<String?>(null) }

    // Load initial backups
    LaunchedEffect(Unit) {
        viewModel.loadLocalBackups()
        if (isCloudAvailable && !currentUserEmail.isNullOrBlank()) {
            viewModel.loadCloudBackups()
        }
    }

    // System File Picker for importing external JSON backup
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            uriToRestore = uri
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!isRestoring && !isLoading) {
                onDismiss()
            }
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Backup & Restore",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Preserve or restore your financial records",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    enabled = !isRestoring && !isLoading,
                    modifier = Modifier.testTag("close_backup_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = if (!isRestoring && !isLoading) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                }
            }

            // Storage Options Tab (Local vs Cloud)
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .padding(bottom = 16.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "Local & Files",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_local_backup")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        if (isCloudAvailable && !currentUserEmail.isNullOrBlank()) {
                            viewModel.loadCloudBackups()
                        }
                    },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "Cloud (Free)",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_cloud_backup")
                )
            }

            // Loading & Restoring Banner
            AnimatedVisibility(visible = isLoading || isRestoring) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isRestoring) "Restore actively running... Please wait and do not exit" else "Processing backup operation...",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Just Created Local Backup Success Banner
            AnimatedVisibility(visible = justCreatedFile != null) {
                justCreatedFile?.let { file ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Backup created successfully!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    BackupManager.shareBackupFile(context, file)
                                    justCreatedFile = null
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("share_created_backup_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // Content per Tab
            if (selectedTab == 0) {
                LocalBackupTabContent(
                    backups = localBackups,
                    isLoading = isLoading,
                    isRestoring = isRestoring,
                    restoringBackupId = restoringBackupId,
                    onCreateBackup = {
                        if (!isRestoring && !isLoading) {
                            viewModel.createLocalBackup { success, message, file ->
                                if (success && file != null) {
                                    justCreatedFile = file
                                }
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onImportFile = {
                        if (!isRestoring && !isLoading) {
                            filePickerLauncher.launch("*/*")
                        }
                    },
                    onRestore = { backup ->
                        if (!isRestoring && !isLoading) {
                            restoringBackupId = backup.id
                            val file = File(BackupManager.getBackupsDirectory(context), backup.fileName)
                            fileToRestore = file
                        }
                    },
                    onShare = { backup ->
                        if (!isRestoring && !isLoading) {
                            viewModel.shareLocalBackup(context, backup.fileName)
                        }
                    },
                    onDelete = { backup ->
                        if (!isRestoring && !isLoading) {
                            backupToDelete = backup
                        }
                    }
                )
            } else {
                CloudBackupTabContent(
                    backups = cloudBackups,
                    isLoading = isLoading,
                    isRestoring = isRestoring,
                    restoringBackupId = restoringBackupId,
                    isCloudAvailable = isCloudAvailable,
                    currentUserEmail = currentUserEmail,
                    onBackupToCloud = {
                        if (!isRestoring && !isLoading) {
                            viewModel.createCloudBackup { success, message ->
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onRefresh = {
                        if (!isRestoring && !isLoading) {
                            viewModel.loadCloudBackups()
                        }
                    },
                    onRestore = { backup ->
                        if (!isRestoring && !isLoading) {
                            restoringBackupId = backup.id
                            backupToRestore = backup
                        }
                    },
                    onDelete = { backup ->
                        if (!isRestoring && !isLoading) {
                            backupToDelete = backup
                        }
                    }
                )
            }
        }
    }

    // Confirmation Dialog: Restore from Local File
    fileToRestore?.let { file ->
        AlertDialog(
            onDismissRequest = {
                if (!isRestoring && !isLoading) {
                    fileToRestore = null
                    restoringBackupId = null
                }
            },
            icon = {
                if (isRestoring) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = if (isRestoring) "Restoring Local Backup..." else "Restore Local Backup?",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isRestoring) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "Restoring in progress...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Applying records and updating ledger. Please wait...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Restoring from file:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "This will restore all transactions, recurring items, categories, and settings from this backup. Your current ledger will be updated.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isRestoring || isLoading) return@Button
                        isRestoring = true
                        viewModel.restoreFromLocalFile(file, replaceExisting = true) { success, message ->
                            isRestoring = false
                            restoringBackupId = null
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            if (success) {
                                fileToRestore = null
                                onDismiss()
                            }
                        }
                    },
                    enabled = !isRestoring && !isLoading,
                    modifier = Modifier.testTag("confirm_restore_local_button")
                ) {
                    if (isRestoring) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restoring...")
                    } else {
                        Text("Restore Now")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        fileToRestore = null
                        restoringBackupId = null
                    },
                    enabled = !isRestoring && !isLoading
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation Dialog: Restore from Selected URI
    uriToRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = {
                if (!isRestoring && !isLoading) {
                    uriToRestore = null
                    restoringBackupId = null
                }
            },
            icon = {
                if (isRestoring) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = if (isRestoring) "Restoring from Backup File..." else "Restore from Backup File?",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isRestoring) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "Restoring in progress...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Verifying JSON and importing records. Please wait...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "The selected JSON file will be read and verified. Restoring will apply all saved transactions, recurring schedules, custom categories, and theme settings to this install.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isRestoring || isLoading) return@Button
                        isRestoring = true
                        viewModel.restoreFromUri(uri, replaceExisting = true) { success, message ->
                            isRestoring = false
                            restoringBackupId = null
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            if (success) {
                                uriToRestore = null
                                onDismiss()
                            }
                        }
                    },
                    enabled = !isRestoring && !isLoading,
                    modifier = Modifier.testTag("confirm_restore_uri_button")
                ) {
                    if (isRestoring) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restoring...")
                    } else {
                        Text("Restore")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        uriToRestore = null
                        restoringBackupId = null
                    },
                    enabled = !isRestoring && !isLoading
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation Dialog: Restore from Cloud
    backupToRestore?.let { backup ->
        AlertDialog(
            onDismissRequest = {
                if (!isRestoring && !isLoading) {
                    backupToRestore = null
                    restoringBackupId = null
                }
            },
            icon = {
                if (isRestoring) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.5.dp,
                        color = Color(0xFF10B981)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = if (isRestoring) "Restoring from Cloud..." else "Restore from Cloud?",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isRestoring) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF059669)
                                )
                                Column {
                                    Text(
                                        text = "Downloading Cloud Snapshot...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF047857)
                                    )
                                    Text(
                                        text = "Fetching and restoring records from Firestore. Please wait...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF047857).copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Cloud snapshot from ${backup.formattedDate}:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• ${backup.transactionsCount} Transactions\n• ${backup.recurringCount} Recurring Items\n• ${backup.categoriesCount} Categories\n• ${backup.forecastCount} Forecasts",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Restoring will update your local install with the cloud snapshot data.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isRestoring || isLoading) return@Button
                        isRestoring = true
                        viewModel.restoreFromCloud(backup.id, replaceExisting = true) { success, message ->
                            isRestoring = false
                            restoringBackupId = null
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            if (success) {
                                backupToRestore = null
                                onDismiss()
                            }
                        }
                    },
                    enabled = !isRestoring && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF059669)
                    ),
                    modifier = Modifier.testTag("confirm_restore_cloud_button")
                ) {
                    if (isRestoring) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restoring...", color = Color.White)
                    } else {
                        Text("Restore from Cloud", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        backupToRestore = null
                        restoringBackupId = null
                    },
                    enabled = !isRestoring && !isLoading
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Backup Confirmation Dialog
    backupToDelete?.let { backup ->
        AlertDialog(
            onDismissRequest = { backupToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = if (backup.isCloud) "Delete Cloud Backup?" else "Delete Local Backup?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete the backup from ${backup.formattedDate}? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (backup.isCloud) {
                            viewModel.deleteCloudBackup(backup.id)
                        } else {
                            viewModel.deleteLocalBackup(backup.fileName)
                        }
                        backupToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_backup_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { backupToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun LocalBackupTabContent(
    backups: List<BackupMetadata>,
    isLoading: Boolean,
    isRestoring: Boolean = false,
    restoringBackupId: String? = null,
    onCreateBackup: () -> Unit,
    onImportFile: () -> Unit,
    onRestore: (BackupMetadata) -> Unit,
    onShare: (BackupMetadata) -> Unit,
    onDelete: (BackupMetadata) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Actions Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Device & File Backups",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Create a self-contained backup file on your device. You can restore it anytime or export it to Google Drive, Files, or external storage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onCreateBackup,
                        enabled = !isLoading && !isRestoring,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("create_local_backup_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Backup", maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = onImportFile,
                        enabled = !isLoading && !isRestoring,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_backup_file_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore File", maxLines = 1)
                    }
                }
            }
        }

        // Saved Snapshots header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Saved Snapshots (${backups.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (backups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "No local snapshots saved yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap 'Create Backup' to generate your first snapshot.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(backups, key = { it.id }) { backup ->
                    BackupItemCard(
                        metadata = backup,
                        onRestore = { onRestore(backup) },
                        onShare = { onShare(backup) },
                        onDelete = { onDelete(backup) },
                        enabled = !isLoading && !isRestoring,
                        isRestoringThis = (isLoading || isRestoring) && restoringBackupId == backup.id
                    )
                }
            }
        }
    }
}

@Composable
private fun CloudBackupTabContent(
    backups: List<BackupMetadata>,
    isLoading: Boolean,
    isRestoring: Boolean = false,
    restoringBackupId: String? = null,
    isCloudAvailable: Boolean,
    currentUserEmail: String?,
    onBackupToCloud: () -> Unit,
    onRefresh: () -> Unit,
    onRestore: (BackupMetadata) -> Unit,
    onDelete: (BackupMetadata) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Free Cloud Tier Explanation Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Google Firebase Firestore",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "$0 Free Tier",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857),
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "Cloud backups are stored in your secure Firebase Firestore account with 100% zero subscription or maintenance cost.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!currentUserEmail.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Signed in as: $currentUserEmail",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onBackupToCloud,
                            enabled = !isLoading && !isRestoring,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("backup_to_cloud_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Backup to Cloud", maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = onRefresh,
                            enabled = !isLoading && !isRestoring,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("refresh_cloud_backups_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Please sign in to link Cloud Backups to your account. You can still create and restore Local Backups anytime!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        // Available Cloud Backups
        if (!currentUserEmail.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cloud Snapshots (${backups.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (backups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No cloud backups saved yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap 'Backup to Cloud' to create a remote snapshot.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(backups, key = { it.id }) { backup ->
                        BackupItemCard(
                            metadata = backup,
                            onRestore = { onRestore(backup) },
                            onShare = null,
                            onDelete = { onDelete(backup) },
                            enabled = !isLoading && !isRestoring,
                            isRestoringThis = (isLoading || isRestoring) && restoringBackupId == backup.id
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupItemCard(
    metadata: BackupMetadata,
    onRestore: () -> Unit,
    onShare: (() -> Unit)?,
    onDelete: () -> Unit,
    enabled: Boolean = true,
    isRestoringThis: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (metadata.isCloud) Color(0xFF10B981).copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (metadata.isCloud) Icons.Default.Cloud else Icons.Default.Description,
                            contentDescription = null,
                            tint = if (metadata.isCloud) Color(0xFF059669) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = metadata.formattedDate,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formatSize(metadata.fileSizeBytes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onShare != null) {
                        IconButton(
                            onClick = onShare,
                            enabled = enabled && !isRestoringThis,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = if (enabled && !isRestoringThis) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        enabled = enabled && !isRestoringThis,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = if (enabled && !isRestoringThis) MaterialTheme.colorScheme.error.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Summary of items included
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${metadata.transactionsCount} tx • ${metadata.recurringCount} rec • ${metadata.categoriesCount} cat • ${metadata.forecastCount} fc",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onRestore,
                    enabled = enabled && !isRestoringThis,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    if (isRestoringThis) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restoring...", style = MaterialTheme.typography.labelMedium)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "Snapshot"
    val kb = bytes / 1024.0
    return if (kb >= 1024.0) {
        String.format(java.util.Locale.US, "%.1f MB", kb / 1024.0)
    } else {
        String.format(java.util.Locale.US, "%.1f KB", kb)
    }
}
