/*
 * SPDX-FileCopyrightText: 2024 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package app.grapheneos.backup.storage.check

import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.util.Log
import androidx.annotation.CallSuper
import androidx.work.BackoffPolicy.EXPONENTIAL
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy.REPLACE
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.grapheneos.backup.storage.api.StorageBackup
import app.grapheneos.backup.storage.ui.NOTIFICATION_ID_CHECK
import app.grapheneos.backup.storage.ui.Notifications
import java.time.Duration

public abstract class CheckerWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    public companion object {
        private val TAG = CheckerWorker::class.simpleName
        private const val PERCENT = "percent"
        public const val UNIQUE_WORK_NAME: String = "app.grapheneos.backup.storage.FILE_BACKUP_CHECK"

        public fun scheduleNow(
            context: Context,
            percent: Int,
            builder: OneTimeWorkRequest.Builder,
        ) {
            check(percent in 0..100) { "Percent $percent out of bounds." }
            val data = Data.Builder().putInt(PERCENT, percent).build()
            val workRequest = builder
                .setExpedited(RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(EXPONENTIAL, Duration.ofSeconds(10))
                .setInputData(data)
                .build()
            val workManager = WorkManager.getInstance(context)
            Log.i(TAG, "Asking to check $percent% of file backups now...")
            workManager.enqueueUniqueWork(UNIQUE_WORK_NAME, REPLACE, workRequest)
        }
    }

    private val n by lazy { Notifications(applicationContext) }
    protected abstract val storageBackup: StorageBackup
    protected abstract val resultActivityClass: Class<*>

    @CallSuper
    override suspend fun doWork(): Result {
        try {
            setForeground(createForegroundInfo())
        } catch (e: Exception) {
            Log.e(TAG, "Error while running setForeground: ", e)
        }
        val percent = inputData.getInt(PERCENT, -1)
        check(percent in 0..100) { "Percent $percent out of bounds." }

        val observer = NotificationCheckObserver(applicationContext, resultActivityClass)
        if (storageBackup.checkBackups(percent, observer)) return Result.success()
        return Result.retry()
    }

    private fun createForegroundInfo() = ForegroundInfo(
        NOTIFICATION_ID_CHECK,
        n.getCheckNotification().build(),
        FOREGROUND_SERVICE_TYPE_DATA_SYNC,
    )
}
