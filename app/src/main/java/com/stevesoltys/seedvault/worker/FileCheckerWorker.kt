/*
 * SPDX-FileCopyrightText: 2024 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package com.stevesoltys.seedvault.worker

import android.content.Context
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import com.stevesoltys.seedvault.BackupStateManager
import com.stevesoltys.seedvault.ui.check.FileCheckResultActivity
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.first
import app.grapheneos.backup.storage.api.StorageBackup
import app.grapheneos.backup.storage.check.CheckerWorker
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FileCheckerWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CheckerWorker(appContext, workerParams), KoinComponent {

    companion object {
        private val TAG = FileCheckerWorker::class.simpleName
        const val UNIQUE_WORK_NAME = CheckerWorker.UNIQUE_WORK_NAME

        fun scheduleNow(
            context: Context,
            percent: Int,
        ) {
            scheduleNow(context, percent, OneTimeWorkRequestBuilder<FileCheckerWorker>())
        }
    }

    private val log = KotlinLogging.logger {}
    private val backupStateManager: BackupStateManager by inject()
    override val storageBackup: StorageBackup by inject()
    override val resultActivityClass: Class<*> = FileCheckResultActivity::class.java

    override suspend fun doWork(): Result {
        log.info { "Start worker $this ($id)" }
        if (backupStateManager.isBackupRunning.first()) {
            Log.i(TAG, "isBackupRunning was true, so retrying later...")
            return Result.retry()
        }
        return super.doWork()
    }
}
