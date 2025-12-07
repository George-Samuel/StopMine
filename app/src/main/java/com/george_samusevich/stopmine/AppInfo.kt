package com.george_samusevich.stopmine

import android.content.Context
import android.content.pm.ApplicationInfo
import java.io.File

data class AppInfo(
    val packageName: String,
    val name: String,
    val versionName: String,
    val versionCode: Long,
    val installTime: Long,
    val updateTime: Long,
    val size: Long,
    val isSystemApp: Boolean = false
) {
    companion object {
        fun fromApplicationInfo(app: ApplicationInfo, context: Context): AppInfo? {
            return try {
                val packageManager = context.packageManager
                val packageInfo = packageManager.getPackageInfo(app.packageName, 0)

                AppInfo(
                    packageName = app.packageName,
                    name = app.loadLabel(packageManager).toString(),
                    versionName = packageInfo.versionName ?: "Unknown",
                    versionCode = packageInfo.versionCode.toLong(),
                    installTime = packageInfo.firstInstallTime,
                    updateTime = packageInfo.lastUpdateTime,
                    size = File(app.sourceDir).length(),
                    isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

