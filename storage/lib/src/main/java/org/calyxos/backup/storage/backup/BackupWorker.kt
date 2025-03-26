/*
 * SPDX-FileCopyrightText: 2024 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.backup.storage.backup

import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.util.Log
import androidx.annotation.CallSuper
import androidx.work.BackoffPolicy.EXPONENTIAL
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy.REPLACE
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.calyxos.backup.storage.R
import org.calyxos.backup.storage.api.BackupObserver
import org.calyxos.backup.storage.api.StorageBackup
import org.calyxos.backup.storage.ui.NOTIFICATION_ID_BACKUP
import org.calyxos.backup.storage.ui.Notifications
import java.time.Duration

public abstract class BackupWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    public companion object {
        private val TAG = BackupWorker::class.simpleName
        public const val UNIQUE_WORK_NAME: String = "org.calyxos.backup.storage.FILE_BACKUP"

        public fun scheduleNow(
            context: Context,
            builder: OneTimeWorkRequest.Builder,
        ) {
            val workRequest = builder
                .setExpedited(RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(EXPONENTIAL, Duration.ofSeconds(10))
                .build()
            val workManager = WorkManager.getInstance(context)
            Log.i(TAG, "Asking to do file backups now...")
            workManager.enqueueUniqueWork(UNIQUE_WORK_NAME, REPLACE, workRequest)
        }
    }

    private val n by lazy { Notifications(applicationContext) }
    protected abstract val storageBackup: StorageBackup
    protected abstract val backupObserver: BackupObserver?

    @CallSuper
    override suspend fun doWork(): Result {
        try {
            setForeground(createForegroundInfo())
        } catch (e: Exception) {
            Log.e(TAG, "Error while running setForeground: ", e)
        }
        val success = storageBackup.runBackup(backupObserver)
        return if (success) {
            // only prune old backups when backup run was successful
            n.showPruneNotification()
            storageBackup.pruneOldBackups(backupObserver)
            Result.success()
        } else {
            Result.retry()
        }
    }

    private fun createForegroundInfo() = ForegroundInfo(
        NOTIFICATION_ID_BACKUP,
        n.getBackupNotification(R.string.notification_backup_scanning),
        FOREGROUND_SERVICE_TYPE_DATA_SYNC,
    )
}
