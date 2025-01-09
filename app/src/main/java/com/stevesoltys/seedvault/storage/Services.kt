/*
 * SPDX-FileCopyrightText: 2020 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package com.stevesoltys.seedvault.storage

import android.content.Intent
import com.stevesoltys.seedvault.backend.BackendManager
import com.stevesoltys.seedvault.worker.AppBackupWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import app.grapheneos.backup.storage.api.BackupObserver
import app.grapheneos.backup.storage.api.RestoreObserver
import app.grapheneos.backup.storage.api.StorageBackup
import app.grapheneos.backup.storage.backup.BackupJobService
import app.grapheneos.backup.storage.backup.BackupService
import app.grapheneos.backup.storage.backup.NotificationBackupObserver
import app.grapheneos.backup.storage.restore.NotificationRestoreObserver
import app.grapheneos.backup.storage.restore.RestoreService
import app.grapheneos.backup.storage.ui.restore.FileSelectionManager
import org.koin.android.ext.android.inject

/*
test and debug with

  adb shell dumpsys jobscheduler |
  grep -A 23 -B 4 "Service: com.stevesoltys.seedvault/.storage.StorageBackupJobService"

force running with:

  adb shell cmd jobscheduler run -f com.stevesoltys.seedvault 0

 */

internal class StorageBackupJobService : BackupJobService(StorageBackupService::class.java)

internal class StorageBackupService : BackupService() {

    companion object {
        internal const val EXTRA_START_APP_BACKUP = "startAppBackup"
        private val mIsRunning = MutableStateFlow(false)
        val isRunning = mIsRunning.asStateFlow()
    }

    override val storageBackup: StorageBackup by inject()
    private val backendManager: BackendManager by inject()

    // use lazy delegate because context isn't available during construction time
    override val backupObserver: BackupObserver by lazy {
        NotificationBackupObserver(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        mIsRunning.value = true
    }

    override fun onDestroy() {
        super.onDestroy()
        mIsRunning.value = false
    }

    override fun onBackupFinished(intent: Intent, success: Boolean) {
        if (intent.getBooleanExtra(EXTRA_START_APP_BACKUP, false)) {
            val isUsb = backendManager.backendProperties?.isUsb ?: false
            AppBackupWorker.scheduleNow(applicationContext, reschedule = !isUsb)
        }
    }
}

internal class StorageRestoreService : RestoreService() {
    override val storageBackup: StorageBackup by inject()
    override val fileSelectionManager: FileSelectionManager by inject()

    // use lazy delegate because context isn't available during construction time
    override val restoreObserver: RestoreObserver by lazy {
        NotificationRestoreObserver(applicationContext)
    }
}
