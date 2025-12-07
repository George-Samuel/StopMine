package com.george_samusevich.stopmine

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class AnalyticsManager(private val context: Context) {
    private val sharedPref: SharedPreferences = context.getSharedPreferences("stopmine_analytics", Context.MODE_PRIVATE)
    private val gson = Gson()

    // Save scan session
    fun saveScanSession(session: ScanSession) {
        val sessions = getScanSessions().toMutableList()
        sessions.add(0, session) // Add to beginning for reverse chronological order

        // Keep only last 50 sessions
        if (sessions.size > 50) {
            sessions.removeAt(sessions.size - 1)
        }

        sharedPref.edit().putString("scan_sessions", gson.toJson(sessions)).apply()
    }

    // Get all scan sessions
    fun getScanSessions(): List<ScanSession> {
        val sessionsJson = sharedPref.getString("scan_sessions", "[]")
        val type = object : TypeToken<List<ScanSession>>() {}.type
        return gson.fromJson(sessionsJson, type) ?: emptyList()
    }

    // Save CPU usage data
    fun saveCpuUsageData(data: CpuUsageData) {
        val allData = getCpuUsageData().toMutableList()
        allData.add(data)

        // Keep only last 1000 records per app
        val filteredData = allData
            .groupBy { it.packageName }
            .flatMap { (_, appData) ->
                appData.takeLast(1000)
            }

        sharedPref.edit().putString("cpu_usage_data", gson.toJson(filteredData)).apply()
    }

    // Get CPU usage data for specific app
    fun getCpuUsageData(packageName: String? = null): List<CpuUsageData> {
        val dataJson = sharedPref.getString("cpu_usage_data", "[]")
        val type = object : TypeToken<List<CpuUsageData>>() {}.type
        val allData = gson.fromJson<List<CpuUsageData>>(dataJson, type) ?: emptyList()

        return if (packageName != null) {
            allData.filter { it.packageName == packageName }
        } else {
            allData
        }
    }

    // Save network activity
    fun saveNetworkActivity(activity: NetworkActivity) {
        val allActivity = getNetworkActivity().toMutableList()
        allActivity.add(activity)

        // Keep only last 500 records
        if (allActivity.size > 500) {
            allActivity.removeAt(0)
        }

        sharedPref.edit().putString("network_activity", gson.toJson(allActivity)).apply()
    }

    // Get network activity
    fun getNetworkActivity(packageName: String? = null): List<NetworkActivity> {
        val activityJson = sharedPref.getString("network_activity", "[]")
        val type = object : TypeToken<List<NetworkActivity>>() {}.type
        val allActivity = gson.fromJson<List<NetworkActivity>>(activityJson, type) ?: emptyList()

        return if (packageName != null) {
            allActivity.filter { it.packageName == packageName }
        } else {
            allActivity
        }
    }

    // Generate risk heatmap
    fun getRiskHeatmap(): List<RiskHeatmapItem> {
        val sessions = getScanSessions()
        val appRiskData = mutableMapOf<String, MutableList<Int>>()
        val appNames = mutableMapOf<String, String>()
        val lastScanTimes = mutableMapOf<String, Long>()

        // Collect data from all sessions
        sessions.forEach { session ->
            session.scanResults.forEach { result ->
                val riskScore = when (result.riskLevel) {
                    RiskLevel.LOW -> 1
                    RiskLevel.MEDIUM -> 2
                    RiskLevel.HIGH -> 3
                }

                if (!appRiskData.containsKey(result.packageName)) {
                    appRiskData[result.packageName] = mutableListOf()
                }
                appRiskData[result.packageName]!!.add(riskScore)
                appNames[result.packageName] = result.appName
                lastScanTimes[result.packageName] = maxOf(
                    lastScanTimes[result.packageName] ?: 0,
                    session.timestamp
                )
            }
        }

        // Calculate average risk scores
        return appRiskData.map { (packageName, scores) ->
            val averageScore = scores.average().toInt()
            RiskHeatmapItem(
                packageName = packageName,
                appName = appNames[packageName] ?: "Unknown",
                riskScore = averageScore,
                scanCount = scores.size,
                lastScanTime = lastScanTimes[packageName] ?: 0
            )
        }.sortedByDescending { it.riskScore }
    }

    // Get statistics for dashboard
    fun getDashboardStats(): DashboardStats {
        val sessions = getScanSessions()
        val totalScans = sessions.size
        val totalAppsScanned = sessions.flatMap { it.scanResults }.distinctBy { it.packageName }.size

        val recentSession = sessions.firstOrNull()
        val highRiskTrend = calculateRiskTrend(sessions, RiskLevel.HIGH)
        val mediumRiskTrend = calculateRiskTrend(sessions, RiskLevel.MEDIUM)

        return DashboardStats(
            totalScans = totalScans,
            totalAppsScanned = totalAppsScanned,
            lastScanTime = recentSession?.timestamp ?: 0,
            highRiskTrend = highRiskTrend,
            mediumRiskTrend = mediumRiskTrend,
            averageScanDuration = sessions.map { it.duration }.average().toLong()
        )
    }

    private fun calculateRiskTrend(sessions: List<ScanSession>, riskLevel: RiskLevel): Double {
        if (sessions.size < 2) return 0.0

        val recentSessions = sessions.take(5) // Last 5 sessions
        val olderSessions = sessions.takeLast(5).take(5) // Previous 5 sessions

        val recentCount = recentSessions.sumOf { session ->
            when (riskLevel) {
                RiskLevel.HIGH -> session.highRiskApps
                RiskLevel.MEDIUM -> session.mediumRiskApps
                RiskLevel.LOW -> session.lowRiskApps
            }
        }

        val olderCount = olderSessions.sumOf { session ->
            when (riskLevel) {
                RiskLevel.HIGH -> session.highRiskApps
                RiskLevel.MEDIUM -> session.mediumRiskApps
                RiskLevel.LOW -> session.lowRiskApps
            }
        }

        return if (olderCount > 0) {
            ((recentCount - olderCount).toDouble() / olderCount) * 100
        } else {
            0.0
        }
    }
}