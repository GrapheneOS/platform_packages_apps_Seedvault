/*
 * SPDX-FileCopyrightText: 2020 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package com.stevesoltys.seedvault.storage

import com.stevesoltys.seedvault.backend.BackendManager
import com.stevesoltys.seedvault.crypto.KeyManager
import app.grapheneos.backup.storage.api.StorageBackup
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val storageModule = module {
    single { StorageBackup(androidContext(), get<BackendManager>(), get<KeyManager>()) }
}
