package com.george_samusevich.stopmine

import androidx.compose.ui.graphics.Color
import java.util.*

data class AppScanResult(
    val packageName: String,
    val appName: String,
    val icon: String = "",
    val riskLevel: RiskLevel,
    val suspiciousActivities: List<String>,
    val cpuUsage: Double,
    val memoryUsage: Long,
    val networkConnections: Int,
    val permissions: List<String>,
    val lastScanTime: Long = System.currentTimeMillis(),
    val recommendedActions: List<SecurityAction> = emptyList()
)

data class SecurityAction(
    val type: ActionType,
    val description: String,
    val isCompleted: Boolean = false
)

enum class ActionType {
    REVOKE_PERMISSION,
    DISABLE_BACKGROUND,
    FORCE_STOP,
    UNINSTALL
}

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH;

    fun getDescription(): String {
        return when (this) {
            LOW -> "Low risk"
            MEDIUM -> "Medium risk"
            HIGH -> "High risk - possible mining"
        }
    }

    fun getColor(): Color {
        return when (this) {
            LOW -> Color(0xFF4CAF50)  // Green
            MEDIUM -> Color(0xFFFF9800) // Orange
            HIGH -> Color(0xFFF44336) // Red
        }
    }
}

// Data class for real-time monitoring
data class AppMonitoringData(
    val packageName: String,
    val appName: String,
    val cpuUsageHistory: MutableList<Double> = mutableListOf(),
    val memoryUsageHistory: MutableList<Long> = mutableListOf(),
    var lastUpdate: Long = System.currentTimeMillis(),
    var alertCount: Int = 0
) {
    fun updateUsage(cpuUsage: Double, memoryUsage: Long) {
        cpuUsageHistory.add(cpuUsage)
        memoryUsageHistory.add(memoryUsage)

        // Keep only last 12 samples (1 minute at 5-second intervals)
        if (cpuUsageHistory.size > 12) cpuUsageHistory.removeAt(0)
        if (memoryUsageHistory.size > 12) memoryUsageHistory.removeAt(0)

        lastUpdate = System.currentTimeMillis()
    }

    fun isSuspicious(): Boolean {
        if (cpuUsageHistory.size < 3) return false

        val avgCpu = cpuUsageHistory.average()
        val maxCpu = cpuUsageHistory.maxOrNull() ?: 0.0

        // Suspicious if high CPU usage sustained
        return avgCpu > 15.0 && maxCpu > 25.0
    }

    fun getRiskLevel(): RiskLevel {
        return when {
            isSuspicious() -> RiskLevel.HIGH
            cpuUsageHistory.average() > 10 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }
}

// Analytics data models
data class ScanSession(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val totalApps: Int,
    val highRiskApps: Int,
    val mediumRiskApps: Int,
    val lowRiskApps: Int,
    val scanResults: List<AppScanResult>,
    val duration: Long // Duration in milliseconds
)

data class CpuUsageData(
    val packageName: String,
    val appName: String,
    val timestamp: Long,
    val cpuUsage: Double,
    val memoryUsage: Long
)

data class NetworkActivity(
    val packageName: String,
    val timestamp: Long,
    val connections: Int,
    val dataSent: Long,
    val dataReceived: Long
)

data class RiskHeatmapItem(
    val packageName: String,
    val appName: String,
    val riskScore: Int,
    val scanCount: Int,
    val lastScanTime: Long
)

data class DashboardStats(
    val totalScans: Int,
    val totalAppsScanned: Int,
    val lastScanTime: Long,
    val highRiskTrend: Double,
    val mediumRiskTrend: Double,
    val averageScanDuration: Long
)

// Chart data models
data class TimeSeriesData(
    val timestamp: Long,
    val value: Double,
    val label: String = ""
)

data class ChartDataset(
    val label: String,
    val data: List<TimeSeriesData>,
    val color: Color
)

data class RiskDistribution(
    val highRisk: Int,
    val mediumRisk: Int,
    val lowRisk: Int,
    val timestamp: Long = System.currentTimeMillis()
)

// Comparison models
data class ScanComparison(
    val session1: ScanSession,
    val session2: ScanSession,
    val newHighRiskApps: List<AppScanResult>,
    val resolvedHighRiskApps: List<AppScanResult>,
    val riskChange: Double // Percentage change
)