/*
 * SPDX-FileCopyrightText: 2024 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package app.grapheneos.seedvault.core.backends.saf

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import app.grapheneos.seedvault.core.backends.AppBackupFileType
import app.grapheneos.seedvault.core.backends.FileBackupFileType
import app.grapheneos.seedvault.core.backends.FileHandle
import app.grapheneos.seedvault.core.backends.LegacyAppBackupFile
import app.grapheneos.seedvault.core.backends.TopLevelFolder
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

internal class DocumentFileCache(
    private val context: Context,
    private val baseFile: DocumentFile,
    private val root: String,
) {

    private val cache = ConcurrentHashMap<String, DocumentFile>()

    internal suspend fun getRootFile(): DocumentFile {
        return cache.getOrPut(root) {
            baseFile.getOrCreateDirectory(context, root)
        }
    }

    internal suspend fun getOrCreateFile(fh: FileHandle): DocumentFile = try {
        when (fh) {
            is TopLevelFolder -> cache.getOrPut("$root/${fh.relativePath}") {
                getRootFile().getOrCreateDirectory(context, fh.name)
            }

            is AppBackupFileType.Blob -> {
                val subFolderName = fh.name.substring(0, 2)
                cache.getOrPut("$root/${fh.topLevelFolder.name}/$subFolderName") {
                    getOrCreateFile(fh.topLevelFolder).getOrCreateDirectory(context, subFolderName)
                }.getOrCreateFile(context, fh.name)
            }

            is AppBackupFileType.Snapshot -> {
                getOrCreateFile(fh.topLevelFolder).getOrCreateFile(context, fh.name)
            }

            is FileBackupFileType.Blob -> {
                val subFolderName = fh.name.substring(0, 2)
                cache.getOrPut("$root/${fh.topLevelFolder.name}/$subFolderName") {
                    getOrCreateFile(fh.topLevelFolder).getOrCreateDirectory(context, subFolderName)
                }.getOrCreateFile(context, fh.name)
            }

            is FileBackupFileType.Snapshot -> {
                getOrCreateFile(fh.topLevelFolder).getOrCreateFile(context, fh.name)
            }

            is LegacyAppBackupFile -> cache.getOrPut("$root/${fh.relativePath}") {
                getOrCreateFile(fh.topLevelFolder).getOrCreateFile(context, fh.name)
            }
        }
    } catch (e: IllegalArgumentException) {
        throw e.toRetryIfMissingFile()
    }

    /**
     * Like [getOrCreateFile], but returns null instead of creating what doesn't exist.
     * Folders found are cached, so loading many blobs doesn't re-list their parents each time.
     */
    internal suspend fun getFile(fh: FileHandle): DocumentFile? = try {
        when (fh) {
            is TopLevelFolder -> getOrPutIfFound("$root/${fh.relativePath}") {
                getRootFile().findExistingFile(fh.name)
            }

            is AppBackupFileType.Blob -> {
                val subFolderName = fh.name.substring(0, 2)
                getOrPutIfFound("$root/${fh.topLevelFolder.name}/$subFolderName") {
                    getFile(fh.topLevelFolder)?.findExistingFile(subFolderName)
                }?.findExistingFile(fh.name)
            }

            is AppBackupFileType.Snapshot -> {
                getFile(fh.topLevelFolder)?.findExistingFile(fh.name)
            }

            is FileBackupFileType.Blob -> {
                val subFolderName = fh.name.substring(0, 2)
                getOrPutIfFound("$root/${fh.topLevelFolder.name}/$subFolderName") {
                    getFile(fh.topLevelFolder)?.findExistingFile(subFolderName)
                }?.findExistingFile(fh.name)
            }

            is FileBackupFileType.Snapshot -> {
                getFile(fh.topLevelFolder)?.findExistingFile(fh.name)
            }

            is LegacyAppBackupFile -> cache.getOrElse("$root/${fh.relativePath}") {
                getFile(fh.topLevelFolder)?.findExistingFile(fh.name)
            }
        }
    } catch (e: IllegalArgumentException) {
        throw e.toRetryIfMissingFile()
    }

    /**
     * Like [findFileBlocking], but throws when listing fails,
     * so that doesn't get mistaken for the file not existing.
     */
    private suspend fun DocumentFile.findExistingFile(name: String): DocumentFile? = try {
        listFilesBlocking(context).find { it.name == name }
    } catch (e: IOException) {
        throw e as? SafRetryException ?: SafRetryException(e)
    }

    private inline fun getOrPutIfFound(key: String, find: () -> DocumentFile?): DocumentFile? {
        return cache[key] ?: find()?.also { cache[key] = it }
    }

    private fun IllegalArgumentException.toRetryIfMissingFile(): Exception {
        return if (message?.contains("Missing file for") == true) {
            clearAll() // clear cache as files may have been messed with, need re-caching
            SafRetryException(this)
        } else this
    }

    internal fun removeFromCache(fh: FileHandle) {
        cache.remove("$root/${fh.relativePath}")
    }

    internal fun clearAll() {
        cache.clear()
    }
}
