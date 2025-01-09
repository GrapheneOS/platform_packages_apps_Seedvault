/*
 * SPDX-FileCopyrightText: 2021 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package de.grobox.storagebackuptester

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.util.Log
import com.google.android.material.color.DynamicColors
import de.grobox.storagebackuptester.crypto.KeyManager
import de.grobox.storagebackuptester.plugin.TestSafBackend
import de.grobox.storagebackuptester.settings.SettingsManager
import app.grapheneos.backup.storage.api.StorageBackup
import app.grapheneos.backup.storage.ui.restore.FileSelectionManager
import app.grapheneos.seedvault.core.backends.Backend
import app.grapheneos.seedvault.core.backends.IBackendManager

class App : Application() {

    private val backendManager = object : IBackendManager {
        private val plugin = TestSafBackend(this@App) { settingsManager.getBackupLocation() }
        override val backend: Backend get() = plugin
        override val isOnRemovableDrive: Boolean = false
        override val requiresNetwork: Boolean = false
    }
    val settingsManager: SettingsManager by lazy { SettingsManager(applicationContext) }
    val storageBackup: StorageBackup by lazy {
        StorageBackup(this, backendManager, KeyManager)
    }
    val fileSelectionManager: FileSelectionManager get() = FileSelectionManager()

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.e("TEST", "ON LOW MEMORY!!!")
    }

}
