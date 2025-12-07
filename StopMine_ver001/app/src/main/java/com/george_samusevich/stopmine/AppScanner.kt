package com.george_samusevich.stopmine

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Debug
import java.util.*

class AppScanner(private val context: Context) {
    private val permissionManager = PermissionManager(context)
    private val analyticsManager = AnalyticsManager(context)

    // Public method to get installed apps
    fun getInstalledApps(): List<ApplicationInfo> {
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.packageName != context.packageName } // Exclude our app
    }

    fun scanSingleApp(packageName: String): AppScanResult {
        val appInfo = getAppInfo(packageName)
        val suspiciousSigns = checkForSuspiciousActivity(packageName)
        val cpuUsage = getCpuUsage(packageName)
        val memoryUsage = getMemoryUsage(packageName)

        val baseResult = AppScanResult(
            packageName = packageName,
            appName = appInfo.loadLabel(context.packageManager).toString(),
            riskLevel = calculateRiskLevel(suspiciousSigns, cpuUsage, memoryUsage),
            suspiciousActivities = suspiciousSigns,
            cpuUsage = cpuUsage,
            memoryUsage = memoryUsage,
            networkConnections = getNetworkConnectionsCount(packageName),
            permissions = getDangerousPermissions(packageName)
        )

        // Сохраняем данные CPU для аналитики
        analyticsManager.saveCpuUsageData(
            CpuUsageData(
                packageName = packageName,
                appName = baseResult.appName,
                timestamp = System.currentTimeMillis(),
                cpuUsage = cpuUsage,
                memoryUsage = memoryUsage
            )
        )

        // Сохраняем сетевую активность
        analyticsManager.saveNetworkActivity(
            NetworkActivity(
                packageName = packageName,
                timestamp = System.currentTimeMillis(),
                connections = baseResult.networkConnections,
                dataSent = 0L, // В реальном приложении получать реальные данные
                dataReceived = 0L
            )
        )

        // Добавляем рекомендованные действия
        return baseResult.copy(
            recommendedActions = permissionManager.getRecommendedActions(baseResult)
        )
    }

    fun scanAllApps(): List<AppScanResult> {
        val startTime = System.currentTimeMillis()
        val installedApps = getInstalledApps()

        val results = installedApps.map { app ->
            try {
                scanSingleApp(app.packageName)
            } catch (e: Exception) {
                // Basic result for apps that couldn't be scanned
                AppScanResult(
                    packageName = app.packageName,
                    appName = app.loadLabel(context.packageManager).toString(),
                    riskLevel = RiskLevel.LOW,
                    suspiciousActivities = emptyList(),
                    cpuUsage = 0.0,
                    memoryUsage = 0,
                    networkConnections = 0,
                    permissions = emptyList(),
                    recommendedActions = emptyList()
                )
            }
        }

        val endTime = System.currentTimeMillis()

        // Сохраняем сессию сканирования для аналитики
        val session = ScanSession(
            totalApps = results.size,
            highRiskApps = results.count { it.riskLevel == RiskLevel.HIGH },
            mediumRiskApps = results.count { it.riskLevel == RiskLevel.MEDIUM },
            lowRiskApps = results.count { it.riskLevel == RiskLevel.LOW },
            scanResults = results,
            duration = endTime - startTime
        )
        analyticsManager.saveScanSession(session)

        return results
    }

    // Метод для получения истории сканирований
    fun getScanHistory(): List<ScanSession> {
        return analyticsManager.getScanSessions()
    }

    // Метод для получения данных CPU по приложению
    fun getCpuUsageHistory(packageName: String): List<CpuUsageData> {
        return analyticsManager.getCpuUsageData(packageName)
    }

    // Метод для получения тепловой карты рисков
    fun getRiskHeatmap(): List<RiskHeatmapItem> {
        return analyticsManager.getRiskHeatmap()
    }

    // Метод для получения статистики дашборда
    fun getDashboardStats(): DashboardStats {
        return analyticsManager.getDashboardStats()
    }

    // Метод для сравнения двух сканирований
    fun compareScans(sessionId1: String, sessionId2: String): ScanComparison? {
        val sessions = analyticsManager.getScanSessions()
        val session1 = sessions.find { it.id == sessionId1 }
        val session2 = sessions.find { it.id == sessionId2 }

        if (session1 == null || session2 == null) return null

        val newHighRiskApps = session2.scanResults
            .filter { it.riskLevel == RiskLevel.HIGH }
            .filter { app2 ->
                session1.scanResults.none { app1 ->
                    app1.packageName == app2.packageName && app1.riskLevel == RiskLevel.HIGH
                }
            }

        val resolvedHighRiskApps = session1.scanResults
            .filter { it.riskLevel == RiskLevel.HIGH }
            .filter { app1 ->
                session2.scanResults.none { app2 ->
                    app2.packageName == app1.packageName && app2.riskLevel == RiskLevel.HIGH
                }
            }

        val totalRisk1 = session1.highRiskApps * 3 + session1.mediumRiskApps * 2 + session1.lowRiskApps
        val totalRisk2 = session2.highRiskApps * 3 + session2.mediumRiskApps * 2 + session2.lowRiskApps

        val riskChange = if (totalRisk1 > 0) {
            ((totalRisk2 - totalRisk1).toDouble() / totalRisk1) * 100
        } else {
            0.0
        }

        return ScanComparison(
            session1 = session1,
            session2 = session2,
            newHighRiskApps = newHighRiskApps,
            resolvedHighRiskApps = resolvedHighRiskApps,
            riskChange = riskChange
        )
    }

    private fun getAppInfo(packageName: String): ApplicationInfo {
        return context.packageManager.getApplicationInfo(packageName, 0)
    }

    private fun checkForSuspiciousActivity(packageName: String): List<String> {
        val suspiciousSigns = mutableListOf<String>()
        val appInfo = getAppInfo(packageName)

        // Check for high background resource usage
        if (isHighBackgroundUsage(packageName)) {
            suspiciousSigns.add("High background activity")
        }

        // Check permissions
        val dangerousPerms = getDangerousPermissions(packageName)
        if (dangerousPerms.size > 5) {
            suspiciousSigns.add("Many dangerous permissions")
        }

        // Check for crypto libraries (simplified)
        if (hasCryptoRelatedCode(packageName)) {
            suspiciousSigns.add("Cryptographic code detected")
        }

        // Check network activity
        if (hasSuspiciousNetworkPatterns(packageName)) {
            suspiciousSigns.add("Suspicious network activity")
        }

        // Check for mining-related characteristics
        if (hasMiningCharacteristics(packageName)) {
            suspiciousSigns.add("Possible mining behavior")
        }

        return suspiciousSigns
    }

    private fun calculateRiskLevel(
        suspiciousSigns: List<String>,
        cpuUsage: Double,
        memoryUsage: Long
    ): RiskLevel {
        var riskScore = 0

        // Points for suspicious signs
        riskScore += suspiciousSigns.size * 2

        // Points for CPU usage
        when {
            cpuUsage > 20 -> riskScore += 3
            cpuUsage > 10 -> riskScore += 2
            cpuUsage > 5 -> riskScore += 1
        }

        // Points for memory usage (in MB)
        when {
            memoryUsage > 200 -> riskScore += 3
            memoryUsage > 100 -> riskScore += 2
            memoryUsage > 50 -> riskScore += 1
        }

        return when {
            riskScore >= 5 -> RiskLevel.HIGH
            riskScore >= 3 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }

    private fun getCpuUsage(packageName: String): Double {
        // More realistic simulation based on app type
        val random = Random()
        return when {
            hasCryptoRelatedCode(packageName) -> (15.0 + random.nextDouble() * 30.0)
            hasMiningCharacteristics(packageName) -> (20.0 + random.nextDouble() * 40.0)
            packageName.contains("game", ignoreCase = true) -> (5.0 + random.nextDouble() * 20.0)
            else -> (1.0 + random.nextDouble() * 10.0)
        }
    }

    private fun getMemoryUsage(packageName: String): Long {
        // More realistic simulation
        val random = Random()
        return when {
            hasCryptoRelatedCode(packageName) -> (150000 + random.nextInt(250000)).toLong()
            hasMiningCharacteristics(packageName) -> (200000 + random.nextInt(300000)).toLong()
            packageName.contains("game", ignoreCase = true) -> (100000 + random.nextInt(200000)).toLong()
            else -> (50000 + random.nextInt(100000)).toLong()
        }
    }

    private fun getDangerousPermissions(packageName: String): List<String> {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            val allPermissions = packageInfo.requestedPermissions?.toList() ?: emptyList()

            // Filter for dangerous permissions
            val dangerousPermissions = listOf(
                "android.permission.ACCESS_COARSE_LOCATION",
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO",
                "android.permission.READ_CONTACTS",
                "android.permission.WRITE_CONTACTS",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.ACCESS_BACKGROUND_LOCATION",
                "android.permission.BODY_SENSORS"
            )

            allPermissions.filter { it in dangerousPermissions }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun isHighBackgroundUsage(packageName: String): Boolean {
        // More realistic simulation based on app type
        val random = Random()
        return when {
            hasCryptoRelatedCode(packageName) -> true
            hasMiningCharacteristics(packageName) -> true
            packageName.contains("game", ignoreCase = true) -> random.nextDouble() > 0.4
            else -> random.nextDouble() > 0.8
        }
    }

    private fun hasCryptoRelatedCode(packageName: String): Boolean {
        // Check package name and app name for crypto-related keywords
        val appName = try {
            context.packageManager.getApplicationInfo(packageName, 0)
                .loadLabel(context.packageManager).toString()
        } catch (e: Exception) {
            ""
        }

        val cryptoKeywords = listOf(
            "crypto", "mining", "coin", "bitcoin", "ether", "blockchain",
            "cryptocurrency", "miner", "pool", "hash", "wallet"
        )

        return cryptoKeywords.any { keyword ->
            packageName.contains(keyword, ignoreCase = true) ||
                    appName.contains(keyword, ignoreCase = true)
        }
    }

    private fun hasMiningCharacteristics(packageName: String): Boolean {
        // Check for apps that might have mining characteristics
        val miningIndicators = listOf(
            "high_cpu", "background_miner", "crypto_wallet", "cloud_mining"
        )

        return miningIndicators.any { indicator ->
            packageName.contains(indicator, ignoreCase = true)
        } || hasCryptoRelatedCode(packageName)
    }

    private fun hasSuspiciousNetworkPatterns(packageName: String): Boolean {
        // More realistic simulation
        val random = Random()
        return when {
            hasCryptoRelatedCode(packageName) -> true
            hasMiningCharacteristics(packageName) -> true
            else -> random.nextDouble() > 0.8
        }
    }

    private fun getNetworkConnectionsCount(packageName: String): Int {
        // More realistic simulation
        val random = Random()
        return when {
            hasCryptoRelatedCode(packageName) -> (5 + random.nextInt(15))
            hasMiningCharacteristics(packageName) -> (8 + random.nextInt(17))
            else -> random.nextInt(10)
        }
    }
}