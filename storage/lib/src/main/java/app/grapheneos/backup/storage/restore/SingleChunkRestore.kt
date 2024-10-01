/*
 * SPDX-FileCopyrightText: 2021 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package app.grapheneos.backup.storage.restore

import android.util.Log
import app.grapheneos.backup.storage.api.RestoreObserver
import app.grapheneos.backup.storage.api.StoredSnapshot
import app.grapheneos.backup.storage.crypto.StreamCrypto
import app.grapheneos.seedvault.core.backends.Backend

private const val TAG = "SingleChunkRestore"

internal class SingleChunkRestore(
    backendGetter: () -> Backend,
    fileRestore: FileRestore,
    streamCrypto: StreamCrypto,
    streamKey: ByteArray,
) : AbstractChunkRestore(backendGetter, fileRestore, streamCrypto, streamKey) {

    suspend fun restore(
        version: Int,
        storedSnapshot: StoredSnapshot,
        chunks: Collection<RestorableChunk>,
        observer: RestoreObserver?,
    ): Int {
        var restoredFiles = 0
        chunks.forEach { chunk ->
            check(chunk.files.size == 1)
            val file = chunk.files[0]
            try {
                getAndDecryptChunk(version, storedSnapshot, chunk.chunkId) { decryptedStream ->
                    restoreFile(file, observer, "M") { outputStream ->
                        decryptedStream.copyTo(outputStream)
                    }
                }
                restoredFiles++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore single chunk file ${file.path}", e)
                observer?.onFileRestoreError(file, e)
                // we try to continue to restore as many files as possible
            }
        }
        return restoredFiles
    }

}
