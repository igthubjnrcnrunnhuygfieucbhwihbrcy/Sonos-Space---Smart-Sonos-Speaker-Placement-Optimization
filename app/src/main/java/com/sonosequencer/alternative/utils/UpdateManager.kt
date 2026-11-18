package com.sonosequencer.alternative.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.tasks.await

class UpdateManager(private val context: Context) {
    
    companion object {
        private const val TAG = "UpdateManager"
        private const val REQUEST_CODE_APP_UPDATE = 1001
    }
    
    private val appUpdateManager: AppUpdateManager by lazy {
        AppUpdateManagerFactory.create(context)
    }
    
    // Check for updates
    suspend fun checkForUpdates(): UpdateInfo? {
        return try {
            val appUpdateInfo = appUpdateManager.appUpdateInfo.await()
            
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                UpdateInfo(
                    isAvailable = true,
                    isFlexibleUpdate = appUpdateInfo.isFlexibleUpdateAvailable,
                    updateType = if (appUpdateInfo.isFlexibleUpdateAvailable) {
                        AppUpdateType.FLEXIBLE
                    } else {
                        AppUpdateType.IMMEDIATE
                    },
                    versionCode = appUpdateInfo.availableVersionCode()
                )
            } else {
                UpdateInfo(
                    isAvailable = false,
                    isFlexibleUpdate = false,
                    updateType = AppUpdateType.IMMEDIATE,
                    versionCode = appUpdateInfo.availableVersionCode()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
            UpdateInfo(
                isAvailable = false,
                isFlexibleUpdate = false,
                updateType = AppUpdateType.IMMEDIATE,
                versionCode = 0
            )
        }
    }
    
    // Start update flow
    fun startUpdateFlow(
        activity: Activity,
        updateType: Int,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                try {
                    if (updateType == AppUpdateType.FLEXIBLE) {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            updateType,
                            activity,
                            REQUEST_CODE_APP_UPDATE
                        )
                    } else {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            activity,
                            REQUEST_CODE_APP_UPDATE
                        )
                    }
                    onSuccess()
                } catch (e: Exception) {
                    onFailure(e)
                }
            }
        }.addOnFailureListener { e ->
            onFailure(e)
        }
    }
    
    // Check if update is in progress
    fun isUpdateInProgress(): Boolean {
        return appUpdateManager.isUpdateInProgress
    }
    
    // Complete flexible update
    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }
    
    // Show update dialog
    fun showUpdateDialog(
        activity: Activity,
        updateInfo: UpdateInfo,
        onUpdate: () -> Unit,
        onCancel: () -> Unit
    ) {
        val message = if (updateInfo.isFlexibleUpdate) {
            "A small update is available. Download in background?"
        } else {
            "A new version is available. Update now?"
        }
        
        AlertDialog.Builder(activity)
            .setTitle("Update Available")
            .setMessage(message)
            .setPositiveButton("Update") { _, _ ->
                onUpdate()
            }
            .setNegativeButton("Later") { _, _ ->
                onCancel()
            }
            .setCancelable(false)
            .show()
    }
    
    // Install update from APK file
    fun installUpdateFromApk(context: Context, apkUri: Uri) {
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        if (installIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(installIntent)
        } else {
            Log.e(TAG, "No activity found to handle APK installation")
        }
    }
    
    // Get current version code
    fun getCurrentVersionCode(context: Context): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                packageInfo.versionCode
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting version code", e)
            0
        }
    }
    
    data class UpdateInfo(
        val isAvailable: Boolean,
        val isFlexibleUpdate: Boolean,
        val updateType: Int,
        val versionCode: Long
    )
}
