/*
 * SPDX-FileCopyrightText: 2020 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package com.stevesoltys.seedvault.storage

import app.grapheneos.backup.storage.api.RestoreObserver
import app.grapheneos.backup.storage.api.StorageBackup
import app.grapheneos.backup.storage.restore.NotificationRestoreObserver
import app.grapheneos.backup.storage.restore.RestoreService
import app.grapheneos.backup.storage.ui.restore.FileSelectionManager
import org.koin.android.ext.android.inject

internal class StorageRestoreService : RestoreService() {
    override val storageBackup: StorageBackup by inject()
    override val fileSelectionManager: FileSelectionManager by inject()

    // use lazy delegate because context isn't available during construction time
    override val restoreObserver: RestoreObserver by lazy {
        NotificationRestoreObserver(applicationContext)
    }
}
