/*
 * SPDX-FileCopyrightText: 2021 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package app.grapheneos.backup.storage.ui.backup

import android.content.Context
import android.net.Uri
import app.grapheneos.backup.storage.api.BackupContentType
import app.grapheneos.backup.storage.api.MediaType

public data class BackupContentItem(
    val uri: Uri,
    val contentType: BackupContentType,
    val enabled: Boolean,
) {
    public fun getName(context: Context): String = when (contentType) {
        is BackupContentType.Custom -> BackupContentType.Custom.getName(uri)
        is MediaType -> context.getString(contentType.nameRes)
    }
}
