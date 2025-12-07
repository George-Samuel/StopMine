package com.george_samusevich.stopmine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

class PermissionManager(private val context: Context) {

    fun getRecommendedActions(appScanResult: AppScanResult): List<SecurityAction> {
        val actions = mutableListOf<SecurityAction>()

        when (appScanResult.riskLevel) {
            RiskLevel.HIGH -> {
                actions.add(SecurityAction(
                    type = ActionType.REVOKE_PERMISSION,
                    description = "Revoke dangerous permissions"
                ))
                actions.add(SecurityAction(
                    type = ActionType.DISABLE_BACKGROUND,
                    description = "Disable background activity"
                ))
                actions.add(SecurityAction(
                    type = ActionType.UNINSTALL,
                    description = "Uninstall suspicious app"
                ))
            }
            RiskLevel.MEDIUM -> {
                actions.add(SecurityAction(
                    type = ActionType.REVOKE_PERMISSION,
                    description = "Review and revoke unnecessary permissions"
                ))
                if (appScanResult.cpuUsage > 10) {
                    actions.add(SecurityAction(
                        type = ActionType.DISABLE_BACKGROUND,
                        description = "Restrict background usage"
                    ))
                }
            }
            RiskLevel.LOW -> {
                // No actions for low risk apps
            }
        }

        return actions
    }

    fun openAppSettings(packageName: String): Boolean {
        return try {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun forceStopApp(packageName: String): Boolean {
        return try {
            // Open app settings where user can force stop manually
            openAppSettings(packageName)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun uninstallApp(packageName: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = Uri.parse("package:$packageName")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}