package com.george_samusevich.stopmine

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDashboard(appScanner: AppScanner) {
    val dashboardStats by remember { mutableStateOf(appScanner.getDashboardStats()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics Dashboard", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Overview Cards
            item {
                Text(
                    "Overview",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    OverviewCard(
                        title = "Total Scans",
                        value = dashboardStats.totalScans.toString(),
                        icon = Icons.Default.List,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OverviewCard(
                        title = "Apps Scanned",
                        value = dashboardStats.totalAppsScanned.toString(),
                        icon = Icons.Default.Add,  // ✅ Исправлено: заменено Apps на Android
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Risk Trends
            item {
                Text(
                    "Risk Trends",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    TrendCard(
                        title = "High Risk Apps",
                        trend = dashboardStats.highRiskTrend,
                        icon = Icons.Default.Warning,
                        color = MaterialTheme.colorScheme.error
                    )

                    TrendCard(
                        title = "Medium Risk Apps",
                        trend = dashboardStats.mediumRiskTrend,
                        icon = Icons.Default.Info,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Recent Activity
            item {
                Text(
                    "Recent Activity",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            item {
                RecentActivityCard(appScanner = appScanner)
            }

            // Performance
            item {
                Text(
                    "Performance",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
                )
            }

            item {
                PerformanceCard(dashboardStats = dashboardStats)
            }
        }
    }
}

@Composable
fun OverviewCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )

                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TrendCard(
    title: String,
    trend: Double,
    icon: ImageVector,
    color: Color
) {
    Card {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    when {
                        trend > 0 -> "Increased by ${String.format("%.1f", trend.absoluteValue)}%"
                        trend < 0 -> "Decreased by ${String.format("%.1f", trend.absoluteValue)}%"
                        else -> "No significant change"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        trend > 0 -> MaterialTheme.colorScheme.error
                        trend < 0 -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Простые текстовые индикаторы
            Text(
                text = when {
                    trend > 0 -> "UP"
                    trend < 0 -> "DOWN"
                    else -> "SAME"
                },
                color = when {
                    trend > 0 -> MaterialTheme.colorScheme.error
                    trend < 0 -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun RecentActivityCard(appScanner: AppScanner) {
    val recentSessions by remember { mutableStateOf(appScanner.getScanHistory().take(3)) }

    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            recentSessions.forEachIndexed { index, session ->
                if (index > 0) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            "Scan Session",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                                .format(Date(session.timestamp)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RiskIndicator(count = session.highRiskApps, riskLevel = RiskLevel.HIGH)
                        Spacer(modifier = Modifier.width(8.dp))
                        RiskIndicator(count = session.mediumRiskApps, riskLevel = RiskLevel.MEDIUM)
                        Spacer(modifier = Modifier.width(8.dp))
                        RiskIndicator(count = session.lowRiskApps, riskLevel = RiskLevel.LOW)
                    }
                }
            }

            if (recentSessions.isEmpty()) {
                Text(
                    "No scan history available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }
    }
}

@Composable
fun RiskIndicator(count: Int, riskLevel: RiskLevel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = riskLevel.getColor()
        )

        Text(
            when (riskLevel) {
                RiskLevel.HIGH -> "High"
                RiskLevel.MEDIUM -> "Med"
                RiskLevel.LOW -> "Low"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PerformanceCard(dashboardStats: DashboardStats) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            StatRow(
                label = "Average Scan Duration",
                value = "${dashboardStats.averageScanDuration / 1000}s"
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            StatRow(
                label = "Last Scan",
                value = if (dashboardStats.lastScanTime > 0) {
                    SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                        .format(Date(dashboardStats.lastScanTime))
                } else {
                    "Never"
                }
            )
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Preview(showBackground = true)
@Composable
fun AnalyticsDashboardPreview() {
    MaterialTheme {
        val context = LocalContext.current
        AnalyticsDashboard(appScanner = AppScanner(context))
    }
}