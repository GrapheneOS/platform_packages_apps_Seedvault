/*
 * SPDX-FileCopyrightText: 2024 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package app.grapheneos.seedvault.core.backends.webdav

import kotlinx.coroutines.runBlocking
import app.grapheneos.seedvault.core.backends.AppBackupFileType
import app.grapheneos.seedvault.core.backends.Backend
import app.grapheneos.seedvault.core.backends.BackendSaver
import app.grapheneos.seedvault.core.backends.BackendTest
import app.grapheneos.seedvault.core.backends.FileHandle
import app.grapheneos.seedvault.core.backends.TopLevelFolder
import app.grapheneos.seedvault.core.toHexString
import org.junit.Test
import java.io.OutputStream
import kotlin.random.Random
import kotlin.test.assertContentEquals

internal class WebDavBackendTest : BackendTest() {
    override val backend: Backend = WebDavBackend(WebDavTestConfig.getConfig(), ".SeedvaultTest")

    @Test
    fun `test write, list, read, rename, delete`(): Unit = runBlocking {
        testWriteListReadRenameDelete()
    }

    @Test
    fun `test remove, create, write file`(): Unit = runBlocking {
        testRemoveCreateWriteFile()
    }

    @Test
    fun `test, free space and create app blob without root folder`(): Unit = runBlocking {
        testTestFreeSpaceAndCreateBlob()
    }

    @Test
    fun `test write after repo was removed externally`(): Unit = runBlocking {
        val otherBackend = WebDavBackend(WebDavTestConfig.getConfig(), ".SeedvaultTest")
        val repoId = Random.nextBytes(32).toHexString()
        val blob = AppBackupFileType.Blob(repoId, Random.nextBytes(32).toHexString())
        val bytes = Random.nextBytes(2342)
        try {
            backend.save(blob, getSaver(bytes))
            otherBackend.remove(TopLevelFolder(repoId))
            backend.list(TopLevelFolder(repoId), AppBackupFileType.Blob::class) {}
            backend.save(blob, getSaver(bytes))
            assertContentEquals(bytes, backend.load(blob as FileHandle).readAllBytes())
        } finally {
            backend.remove(TopLevelFolder(repoId))
        }
    }

    private fun getSaver(bytes: ByteArray) = object : BackendSaver {
        override val size: Long = bytes.size.toLong()
        override val sha256: String? = null

        override fun save(outputStream: OutputStream): Long {
            outputStream.write(bytes)
            return size
        }
    }
}
