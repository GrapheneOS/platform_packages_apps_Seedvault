/*
 * SPDX-FileCopyrightText: 2020 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package com.stevesoltys.seedvault.ui.files

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import app.grapheneos.backup.storage.api.StorageBackup
import app.grapheneos.backup.storage.ui.backup.BackupContentViewModel

class FileSelectionViewModel(
    app: Application,
    override val storageBackup: StorageBackup,
) : BackupContentViewModel(app) {

    init {
        viewModelScope.launch { loadContent() }
    }

}
