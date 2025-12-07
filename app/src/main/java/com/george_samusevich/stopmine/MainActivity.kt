package com.george_samusevich.stopmine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appScanner = AppScanner(this)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(appScanner = appScanner)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(appScanner: AppScanner) {
    var currentScreen by remember { mutableStateOf("main") }

    // ДОБАВЛЕНО: Обработчик свайпа назад для экрана аналитики
    if (currentScreen == "analytics") {
        androidx.activity.compose.BackHandler {
            currentScreen = "main"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (currentScreen) {
                            "analytics" -> "Analytics Dashboard"
                            else -> "StopMine"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                navigationIcon = {
                    // ИСПРАВЛЕНО: меняем иконку Home на ArrowBack
                    if (currentScreen == "analytics") {
                        IconButton(onClick = { currentScreen = "main" }) {
                            Icon(Icons.Default.ArrowBack, "Back to main")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentScreen == "main") {
                FloatingActionButton(
                    onClick = { currentScreen = "analytics" },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Info, "Analytics Dashboard")
                }
            }
        }
    ) { innerPadding ->
        // Используем innerPadding чтобы убрать предупреждение
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                "analytics" -> AnalyticsDashboard(appScanner = appScanner)
                else -> AntiMiningApp(appScanner = appScanner)
            }
        }
    }
}

// ВСЕ ОСТАЛЬНЫЕ КОМПОНЕНТЫ БЕЗ ИЗМЕНЕНИЙ
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntiMiningApp(appScanner: AppScanner) {
    var scanResults by remember { mutableStateOf<List<AppScanResult>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var totalScans by remember { mutableStateOf(0) }
    var threatsFound by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val permissionManager = remember { PermissionManager(context) }

    LaunchedEffect(scanResults) {
        if (scanResults.isNotEmpty()) {
            totalScans++
            threatsFound += scanResults.count { it.riskLevel == RiskLevel.HIGH }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Hidden Mining Detector",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Check apps for hidden cryptocurrency mining",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Button(
                onClick = {
                    isScanning = true
                    scanResults = emptyList()
                },
                modifier = Modifier.weight(1f),
                enabled = !isScanning
            ) {
                Text("Check Random")
            }

            Button(
                onClick = {
                    isScanning = true
                    scanResults = emptyList()
                },
                modifier = Modifier.weight(1f),
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Check All")
            }
        }

        // Start scanning when isScanning becomes true
        LaunchedEffect(isScanning) {
            if (isScanning) {
                val results = withContext(Dispatchers.IO) {
                    appScanner.scanAllApps()
                }
                scanResults = results
                isScanning = false
            }
        }

        // Scanning indicator
        if (isScanning) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Scanning applications...")
            }
        }

        // Scan results
        if (scanResults.isNotEmpty()) {
            Text(
                text = "Scan Results (${scanResults.size} apps):",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val highRiskCount = scanResults.count { it.riskLevel == RiskLevel.HIGH }
            val mediumRiskCount = scanResults.count { it.riskLevel == RiskLevel.MEDIUM }
            val lowRiskCount = scanResults.count { it.riskLevel == RiskLevel.LOW }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                RiskStatChip("High risk: $highRiskCount", RiskLevel.HIGH)
                RiskStatChip("Medium risk: $mediumRiskCount", RiskLevel.MEDIUM)
                RiskStatChip("Low risk: $lowRiskCount", RiskLevel.LOW)
            }

            LazyColumn {
                items(scanResults) { result ->
                    AppScanResultItem(
                        result = result,
                        permissionManager = permissionManager
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Statistics when no results
        if (scanResults.isEmpty() && !isScanning) {
            ScanStatistics(totalScans = totalScans, threatsFound = threatsFound)
        }
    }
}

@Composable
fun AppScanResultItem(
    result: AppScanResult,
    permissionManager: PermissionManager
) {
    var showActions by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showActions = !showActions },
        colors = CardDefaults.cardColors(
            containerColor = when (result.riskLevel) {
                RiskLevel.LOW -> MaterialTheme.colorScheme.surfaceVariant
                RiskLevel.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer
                RiskLevel.HIGH -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.appName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Text(
                        text = result.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                RiskBadge(riskLevel = result.riskLevel)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier.fillMaxWidth()
            ) {
                InfoChip("CPU: ${String.format("%.1f", result.cpuUsage)}%")
                InfoChip("RAM: ${result.memoryUsage / 1024}MB")
                InfoChip("Net: ${result.networkConnections}")
            }

            if (result.suspiciousActivities.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ ${result.suspiciousActivities.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (result.permissions.isNotEmpty() && result.riskLevel != RiskLevel.LOW) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Permissions: ${result.permissions.take(3).joinToString(", ")}${if (result.permissions.size > 3) "..." else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (result.recommendedActions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showActions = !showActions },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                ) {
                    Text(
                        text = if (showActions) "Hide Security Actions" else "Show Security Actions (${result.recommendedActions.size})",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            if (showActions && result.recommendedActions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                SecurityActionsList(
                    actions = result.recommendedActions,
                    packageName = result.packageName,
                    permissionManager = permissionManager
                )
            }
        }
    }
}

@Composable
fun SecurityActionsList(
    actions: List<SecurityAction>,
    packageName: String,
    permissionManager: PermissionManager
) {
    Column {
        Text(
            text = "Recommended Security Actions:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        actions.forEach { action ->
            SecurityActionItem(
                action = action,
                packageName = packageName,
                permissionManager = permissionManager
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun SecurityActionItem(
    action: SecurityAction,
    packageName: String,
    permissionManager: PermissionManager
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                when (action.type) {
                    ActionType.REVOKE_PERMISSION -> permissionManager.openAppSettings(packageName)
                    ActionType.DISABLE_BACKGROUND -> permissionManager.openAppSettings(packageName)
                    ActionType.FORCE_STOP -> permissionManager.openAppSettings(packageName)
                    ActionType.UNINSTALL -> permissionManager.uninstallApp(packageName)
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (action.type) {
                        ActionType.REVOKE_PERMISSION -> "🔐 Manage Permissions"
                        ActionType.DISABLE_BACKGROUND -> "⏸️ Restrict Background"
                        ActionType.FORCE_STOP -> "🛑 App Info"
                        ActionType.UNINSTALL -> "🗑️ Uninstall App"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = action.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Execute action",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun RiskBadge(riskLevel: RiskLevel) {
    val (text, color) = when (riskLevel) {
        RiskLevel.LOW -> "Low" to MaterialTheme.colorScheme.primary
        RiskLevel.MEDIUM -> "Medium" to MaterialTheme.colorScheme.secondary
        RiskLevel.HIGH -> "High" to MaterialTheme.colorScheme.error
    }

    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun InfoChip(text: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun RiskStatChip(text: String, riskLevel: RiskLevel) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(
                color = riskLevel.getColor().copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = riskLevel.getColor(),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ScanStatistics(totalScans: Int, threatsFound: Int) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Security Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            StatItem("Total scans", totalScans.toString())
            StatItem("Threats found", threatsFound.toString())
            StatItem("Last scan",
                if (totalScans > 0) "Completed" else "Not performed"
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Preview(showBackground = true)
@Composable
fun AntiMiningAppPreview() {
    MaterialTheme {
        AntiMiningApp(appScanner = AppScanner(LocalContext.current))
    }
}