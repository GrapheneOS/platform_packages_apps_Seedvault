/*
 * SPDX-FileCopyrightText: 2024 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package app.grapheneos.seedvault.core.backends.saf

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Root.COLUMN_ROOT_ID
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.core.database.getStringOrNull
import androidx.documentfile.provider.DocumentFile
import app.grapheneos.seedvault.core.backends.BackendProperties

public data class SafProperties(
    override val config: Uri,
    override val name: String,
    override val isUsb: Boolean,
    override val requiresNetwork: Boolean,
    /**
     * The [COLUMN_ROOT_ID] for the [uri].
     * This is only nullable for historic reasons, because we didn't always store it.
     */
    val rootId: String?,
) : BackendProperties<Uri>() {

    public val uri: Uri = config

    public fun getDocumentFile(context: Context): DocumentFile =
        DocumentFile.fromTreeUri(context, config)
            ?: throw AssertionError("Should only happen on API < 21.")

    /**
     * Returns true if this is USB storage that is not available, false otherwise.
     *
     * Must be run off UI thread (ideally I/O).
     */
    @WorkerThread
    override fun isUnavailableUsb(context: Context): Boolean {
        if (!isUsb) return false
        return if (rootId == null) { // fallback for when we didn't store rootId
            // the document file is not a directory
            !getDocumentFile(context).isDirectory
        } else {
            // retry root check due to SAF bug
            for (i in 1..3) {
                try {
                    // if root isn't present, the usb storage is unavailable
                    return !isRootIdPresent(context)
                } catch (e: Exception) {
                    Log.e("SafProperties", "Error getting root ($i): ", e)
                    continue
                }
            }
            return true // we had three exceptions, so is unavailable
        }
    }

    private fun isRootIdPresent(context: Context): Boolean {
        val rootUri = DocumentsContract.buildRootsUri(uri.authority)
        val projection = arrayOf(COLUMN_ROOT_ID)
        return context.contentResolver.query(
            rootUri, projection, "$COLUMN_ROOT_ID = ?", arrayOf(rootId), null
        )?.use { c ->
            // the selection per root ID doesn't work, so we need to look at cursor rows
            while (c.moveToNext()) {
                val str = c.getStringOrNull(c.getColumnIndex(COLUMN_ROOT_ID))
                if (str == rootId) return true
            }
            return false
        } ?: throw NullPointerException()
    }
}
