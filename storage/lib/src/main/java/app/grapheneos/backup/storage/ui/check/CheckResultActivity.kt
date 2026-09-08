/*
 * SPDX-FileCopyrightText: 2024 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package app.grapheneos.backup.storage.ui.check

import android.app.NotificationManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import app.grapheneos.backup.storage.R
import app.grapheneos.backup.storage.api.SnapshotItem
import app.grapheneos.backup.storage.api.StorageBackup
import app.grapheneos.backup.storage.ui.Notifications.Companion.onCheckCompleteNotificationSeen
import app.grapheneos.backup.storage.ui.restore.FileSelectionManager
import app.grapheneos.backup.storage.ui.restore.SnapshotClickListener

internal const val ACTION_FINISHED = "FINISHED"
internal const val ACTION_SHOW = "SHOW"
private val TAG = CheckResultActivity::class.simpleName

public abstract class CheckResultActivity : AppCompatActivity(), SnapshotClickListener {

    protected abstract val storageBackup: StorageBackup
    protected abstract val fileSelectionManager: FileSelectionManager
    internal val storageBackupInt get() = storageBackup
    internal val fileSelectionManagerInt get() = fileSelectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_results)

        if (savedInstanceState == null) when (intent.action) {
            ACTION_FINISHED -> {
                val nm = getSystemService(NotificationManager::class.java)
                onCheckCompleteNotificationSeen(nm)
                finish() // result will be cleared in onDestroy()
            }
            ACTION_SHOW -> {
                val nm = getSystemService(NotificationManager::class.java)
                onCheckCompleteNotificationSeen(nm)
            }
            else -> {
                Log.e(TAG, "Unknown action: ${intent.action}")
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) storageBackup.clearCheckResult()
    }

    override fun onSnapshotClicked(item: SnapshotItem) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, SnapshotFilesFragment.new(item))
            .addToBackStack(SnapshotFilesFragment::class.simpleName)
            .commit()
    }
}
