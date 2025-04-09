/*
 * SPDX-FileCopyrightText: 2021 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package app.grapheneos.backup.storage

import android.net.Uri
import app.grapheneos.backup.storage.api.MediaType
import app.grapheneos.backup.storage.api.mediaItems
import app.grapheneos.backup.storage.backup.BackupMediaFile
import app.grapheneos.backup.storage.db.StoredUri

internal fun Uri.toStoredUri(): StoredUri = StoredUri(this)

internal fun Uri.getMediaType(): MediaType? {
    val str = toString()
    for (item in mediaItems) {
        if (str.startsWith(item.contentUri.toString())) return item
    }
    return null
}

internal fun Uri.getBackupMediaType(): BackupMediaFile.MediaType? {
    return getMediaType()?.backupType
}
