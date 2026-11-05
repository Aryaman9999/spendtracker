package com.aryaman.spendtracker.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorOsGuideScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    val isIgnoringBattery = remember(context) {
        powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ColorOS 16 Optimization Guide", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Why is this required on Oppo / ColorOS?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "ColorOS 16 has aggressive background management. Follow these 4 steps to ensure SMS & UPI notifications are captured reliably 24/7.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                    )
                }
            }

            // Step 1: Battery Optimization
            ChecklistStepCard(
                number = "1",
                icon = Icons.Default.BatteryAlert,
                title = "Battery: Allow Background Activity",
                description = "Set battery usage to 'Unrestricted' or 'Allow background activity' so ColorOS doesn't freeze the app.",
                statusText = if (isIgnoringBattery) "Already Whitelisted" else "Action Needed",
                isCompleted = isIgnoringBattery,
                buttonText = "Open Battery Settings",
                onAction = {
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    }
                }
            )

            // Step 2: SMS & Alert Permissions
            ChecklistStepCard(
                number = "2",
                icon = Icons.Default.NotificationsActive,
                title = "SMS & Alert Permissions",
                description = "Ensure SMS permissions and notifications are allowed so incoming bank transaction messages are captured reliably.",
                statusText = "Required",
                isCompleted = false,
                buttonText = "Check Permissions",
                onAction = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            )

            // Step 3: Auto-launch in ColorOS
            ChecklistStepCard(
                number = "3",
                icon = Icons.Default.Settings,
                title = "Enable Auto-Launch in ColorOS",
                description = "In Oppo Settings > Apps > Auto-launch (or App details), ensure SpendTracker is allowed to auto-start on boot.",
                statusText = "Action Needed",
                isCompleted = false,
                buttonText = "Open App Settings",
                onAction = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            )

            // Step 4: Lock in Recents
            ChecklistStepCard(
                number = "4",
                icon = Icons.Default.Lock,
                title = "Lock App in Recent Tasks",
                description = "Swipe up and hold to open Recent Apps overview > Tap the 3 dots (⋮) above SpendTracker > Tap 'Lock'. This keeps the listener active forever.",
                statusText = "Manual Step",
                isCompleted = false,
                buttonText = "Got It!",
                onAction = {}
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun ChecklistStepCard(
    number: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    statusText: String,
    isCompleted: Boolean,
    buttonText: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Step $number: $title",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCompleted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text(statusText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Text(statusText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }

                Button(onClick = onAction) {
                    Text(buttonText)
                }
            }
        }
    }
}
