package com.panda.wishlist.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.panda.wishlist.BuildConfig
import com.panda.wishlist.core.database.entity.WishlistEntity
import com.panda.wishlist.core.logging.DebugLogEntry
import com.panda.wishlist.core.logging.LogLevel
import com.panda.wishlist.data.sync.AppSettings
import com.panda.wishlist.ui.components.CasioGlassCard
import com.panda.wishlist.ui.components.CasioGlassFilterChip
import com.panda.wishlist.ui.components.CasioGlassSwitch
import com.panda.wishlist.ui.components.glassEmeraldButtonColors
import com.panda.wishlist.ui.components.glassPrimaryButtonColors
import com.panda.wishlist.ui.components.glassSecondaryButtonColors
import com.panda.wishlist.ui.components.glassTextFieldColors
import com.panda.wishlist.ui.theme.CyberGlassTokens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    wishlists: List<WishlistEntity>,
    connectedGithubUser: String?,
    isCheckingUpdate: Boolean,
    debugLogs: List<DebugLogEntry>,
    onSaveOwnerName: (String) -> Unit,
    onSaveCustomToken: (String) -> Unit,
    onVerifyGitHub: () -> Unit,
    onSetAutoSync: (Boolean) -> Unit,
    onSetAutoUpdateCheck: (Boolean) -> Unit,
    onSetDefaultWishlist: (String) -> Unit,
    onSetAutoSaveShare: (Boolean) -> Unit,
    onCheckUpdatesNow: () -> Unit,
    onClearLogs: () -> Unit,
    onExportLogs: () -> String,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    var ownerNameInput by remember(settings.ownerDisplayName) { mutableStateOf(settings.ownerDisplayName) }
    var tokenInput by remember { mutableStateOf("") }
    var showTokenBox by remember { mutableStateOf(false) }
    var showDebugConsole by remember { mutableStateOf(false) }
    val logListState = rememberLazyListState()

    LaunchedEffect(debugLogs.size, showDebugConsole) {
        if (showDebugConsole && debugLogs.isNotEmpty()) {
            logListState.animateScrollToItem(debugLogs.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Settings",
            color = CyberGlassTokens.TextPrimary,
            fontSize = 21.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "GitHub Pages sync, preferences, updates & debug logs",
            color = CyberGlassTokens.TextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // GitHub Pages Connection Card
        CasioGlassCard(accentBorderColor = if (connectedGithubUser != null) CyberGlassTokens.MintEmerald else CyberGlassTokens.WarningAmber) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (connectedGithubUser != null) CyberGlassTokens.MintEmerald else CyberGlassTokens.WarningAmber,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GitHub Pages Backend (Free)",
                        color = CyberGlassTokens.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (connectedGithubUser != null) {
                            "Connected as @$connectedGithubUser • ${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}"
                        } else {
                            "Checking connection or offline — tap Verify"
                        },
                        color = if (connectedGithubUser != null) CyberGlassTokens.MintEmerald else CyberGlassTokens.WarningAmber,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = ownerNameInput,
                onValueChange = { ownerNameInput = it },
                label = { Text("Name Friends See on Web Page") },
                singleLine = true,
                colors = glassTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onSaveOwnerName(ownerNameInput) },
                    colors = glassPrimaryButtonColors(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Name", fontWeight = FontWeight.Bold, maxLines = 1)
                }
                OutlinedButton(
                    onClick = onVerifyGitHub,
                    colors = glassSecondaryButtonColors(),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Verify Sync", fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (showTokenBox) "Hide GitHub Token Override" else "Replace GitHub Token (Optional)",
                color = CyberGlassTokens.ElectricCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { showTokenBox = !showTokenBox }
            )

            if (showTokenBox) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { Text("Personal Access Token (leave blank for built-in)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = glassTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onSaveCustomToken(tokenInput)
                            tokenInput = ""
                            showTokenBox = false
                        },
                        colors = glassEmeraldButtonColors(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Apply Token", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            onSaveCustomToken("")
                            tokenInput = ""
                            showTokenBox = false
                        },
                        colors = glassSecondaryButtonColors(),
                        border = BorderStroke(1.dp, CyberGlassTokens.NeonRose.copy(alpha = 0.55f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Use Built-in", color = CyberGlassTokens.NeonRose, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sharing & Quick Add
        CasioGlassCard {
            Text(
                text = "Sharing & Quick Add",
                color = CyberGlassTokens.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            val hasExplicitDefault = settings.defaultWishlistId.isNotBlank() &&
                wishlists.any { it.id == settings.defaultWishlistId }
            Text(
                text = "Default Wishlist",
                color = CyberGlassTokens.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (!hasExplicitDefault) {
                    "Most Recent (Default) — uses the last folder you saved to"
                } else {
                    val pinned = wishlists.first { it.id == settings.defaultWishlistId }
                    "Pinned to ${pinned.emoji} ${pinned.name}"
                },
                color = CyberGlassTokens.TextSecondary,
                fontSize = 11.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CasioGlassFilterChip(
                    text = "Most Recent (Default)",
                    selected = !hasExplicitDefault,
                    onClick = { onSetDefaultWishlist("") }
                )
                wishlists.forEach { folder ->
                    CasioGlassFilterChip(
                        text = "${folder.emoji} ${folder.name}",
                        selected = settings.defaultWishlistId == folder.id,
                        onClick = { onSetDefaultWishlist(folder.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Auto-Save Shared Items (3s)",
                        color = CyberGlassTokens.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Wait 3 seconds after sharing to auto-save to your default wishlist (cancellable)",
                        color = CyberGlassTokens.TextSecondary,
                        fontSize = 11.5.sp
                    )
                }
                CasioGlassSwitch(
                    checked = settings.autoSaveShareEnabled,
                    onCheckedChange = onSetAutoSaveShare
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sync & App Preferences
        CasioGlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Auto-Sync Shared Wishlists",
                        color = CyberGlassTokens.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Immediately push updates to GitHub Pages when you add or remove a wish",
                        color = CyberGlassTokens.TextSecondary,
                        fontSize = 11.5.sp
                    )
                }
                CasioGlassSwitch(
                    checked = settings.autoSyncOnChange,
                    onCheckedChange = onSetAutoSync
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Startup Update Check",
                        color = CyberGlassTokens.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Check Panda App Store catalog on launch for newer builds",
                        color = CyberGlassTokens.TextSecondary,
                        fontSize = 11.5.sp
                    )
                }
                CasioGlassSwitch(
                    checked = settings.autoUpdateCheckEnabled,
                    onCheckedChange = onSetAutoUpdateCheck
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // App Version & Update Check
        CasioGlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Panda Wishlist v${BuildConfig.VERSION_NAME}",
                        color = CyberGlassTokens.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Build ${BuildConfig.VERSION_CODE} • ${BuildConfig.APPLICATION_ID}",
                        color = CyberGlassTokens.TextSecondary,
                        fontSize = 11.5.sp
                    )
                }
                OutlinedButton(
                    onClick = onCheckUpdatesNow,
                    enabled = !isCheckingUpdate,
                    colors = glassSecondaryButtonColors(),
                    border = BorderStroke(
                        1.dp,
                        if (!isCheckingUpdate) CyberGlassTokens.ElectricCyan.copy(alpha = 0.6f)
                        else Color.White.copy(alpha = 0.16f)
                    )
                ) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = CyberGlassTokens.ElectricCyan)
                    } else {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Check", fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Mandatory Debug Console
        CasioGlassCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDebugConsole = !showDebugConsole },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BugReport, contentDescription = null, tint = CyberGlassTokens.ElectricCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Debug Console",
                            color = CyberGlassTokens.TextPrimary,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${debugLogs.size} events logged",
                            color = CyberGlassTokens.TextSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                }
                Text(
                    text = if (showDebugConsole) "Hide ▲" else "Open ▼",
                    color = CyberGlassTokens.ElectricCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (showDebugConsole) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val txt = onExportLogs().ifBlank { "No logs yet." }
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Debug Logs", txt))
                            onShowToast("Logs copied to clipboard")
                        },
                        colors = glassSecondaryButtonColors(),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 11.5.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            val txt = onExportLogs().ifBlank { "No logs yet." }
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, txt)
                            }
                            context.startActivity(Intent.createChooser(send, "Share Debug Logs"))
                        },
                        colors = glassSecondaryButtonColors(),
                        border = BorderStroke(1.dp, CyberGlassTokens.ElectricCyan.copy(alpha = 0.55f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", fontSize = 11.5.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onClearLogs,
                        colors = glassSecondaryButtonColors(),
                        border = BorderStroke(1.dp, CyberGlassTokens.NeonRose.copy(alpha = 0.55f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = CyberGlassTokens.NeonRose, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear", color = CyberGlassTokens.NeonRose, fontSize = 11.5.sp, maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 230.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberGlassTokens.BgDeepSpace.copy(alpha = 0.85f))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    if (debugLogs.isEmpty()) {
                        Text("No logs captured yet.", color = CyberGlassTokens.TextTertiary, fontSize = 11.5.sp)
                    } else {
                        LazyColumn(state = logListState, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(debugLogs, key = { it.id }) { log ->
                                val col = when (log.level) {
                                    LogLevel.ALERT -> CyberGlassTokens.NeonRose
                                    LogLevel.WARN -> CyberGlassTokens.WarningAmber
                                    LogLevel.ACTION -> CyberGlassTokens.MintEmerald
                                    LogLevel.INFO -> CyberGlassTokens.TextSecondary
                                }
                                Text(
                                    text = "[${log.timestamp}] ${log.tag}: ${log.message}",
                                    color = col,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
