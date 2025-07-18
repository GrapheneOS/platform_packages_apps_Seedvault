/*
 * SPDX-FileCopyrightText: 2021 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package app.grapheneos.backup.storage.prune

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import app.grapheneos.backup.storage.SnapshotRetriever
import app.grapheneos.backup.storage.api.StoredSnapshot
import app.grapheneos.backup.storage.backup.BackupDocumentFile
import app.grapheneos.backup.storage.backup.BackupMediaFile
import app.grapheneos.backup.storage.backup.BackupSnapshot
import app.grapheneos.backup.storage.crypto.StreamCrypto
import app.grapheneos.backup.storage.db.CachedChunk
import app.grapheneos.backup.storage.db.ChunksCache
import app.grapheneos.backup.storage.db.Db
import app.grapheneos.backup.storage.getCurrentBackupSnapshots
import app.grapheneos.backup.storage.getRandomString
import app.grapheneos.backup.storage.mockLog
import app.grapheneos.seedvault.core.backends.Backend
import app.grapheneos.seedvault.core.backends.FileBackupFileType.Blob
import app.grapheneos.seedvault.core.backends.IBackendManager
import app.grapheneos.seedvault.core.crypto.CoreCrypto.ALGORITHM_HMAC
import app.grapheneos.seedvault.core.crypto.CoreCrypto.KEY_SIZE_BYTES
import app.grapheneos.seedvault.core.crypto.KeyManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

internal class PrunerTest {

    private val db: Db = mockk()
    private val chunksCache: ChunksCache = mockk()
    private val backendManager: IBackendManager = mockk()
    private val androidId: String = getRandomString()
    private val keyManager: KeyManager = mockk()
    private val backend: Backend = mockk()
    private val snapshotRetriever: SnapshotRetriever = mockk()
    private val retentionManager: RetentionManager = mockk()
    private val streamCrypto: StreamCrypto = mockk()
    private val streamKey = "This is a backup key for testing".toByteArray()
    private val mainKey = SecretKeySpec(streamKey, 0, KEY_SIZE_BYTES, ALGORITHM_HMAC)

    init {
        mockLog(false)
        mockkStatic("app.grapheneos.backup.storage.SnapshotRetrieverKt")
        every { backendManager.backend } returns backend
        every { db.getChunksCache() } returns chunksCache
        every { keyManager.getMainKey() } returns mainKey
        every { streamCrypto.deriveStreamKey(mainKey) } returns streamKey
    }

    private val pruner = Pruner(
        db = db,
        retentionManager = retentionManager,
        backendManager = backendManager,
        androidId = androidId,
        keyManager = keyManager,
        snapshotRetriever = snapshotRetriever,
        streamCrypto = streamCrypto,
    )

    @Test
    fun test() = runBlocking {
        val chunk1 = getRandomString(6)
        val chunk2 = getRandomString(6)
        val chunk3 = getRandomString(6)
        val chunk4 = getRandomString(6)
        val snapshot1 = BackupSnapshot.newBuilder()
            .setTimeStart(Random.nextLong())
            .addMediaFiles(BackupMediaFile.newBuilder().addChunkIds(chunk1))
            .addMediaFiles(BackupMediaFile.newBuilder().addChunkIds(chunk2))
            .addDocumentFiles(BackupDocumentFile.newBuilder().addChunkIds(chunk1))
            .addDocumentFiles(BackupDocumentFile.newBuilder().addChunkIds(chunk3))
            .build()
        val snapshot2 = BackupSnapshot.newBuilder()
            .setTimeStart(Random.nextLong())
            .addMediaFiles(BackupMediaFile.newBuilder().addChunkIds(chunk1))
            .addMediaFiles(BackupMediaFile.newBuilder().addChunkIds(chunk2))
            .addDocumentFiles(BackupDocumentFile.newBuilder().addChunkIds(chunk4))
            .build()
        val storedSnapshot1 = StoredSnapshot("foo", snapshot1.timeStart)
        val storedSnapshot2 = StoredSnapshot("bar", snapshot2.timeStart)
        val storedSnapshots = listOf(storedSnapshot1, storedSnapshot2)
        val expectedChunks = listOf(chunk1, chunk2, chunk3)
        val actualChunks = slot<Collection<String>>()
        val actualChunks2 = slot<Collection<String>>()
        val cachedChunk3 = CachedChunk(chunk3, 0, 0)

        coEvery { backend.getCurrentBackupSnapshots(androidId) } returns storedSnapshots
        every {
            retentionManager.getSnapshotsToDelete(storedSnapshots)
        } returns listOf(storedSnapshot1)
        coEvery { snapshotRetriever.getSnapshot(streamKey, storedSnapshot1) } returns snapshot1
        coEvery { backend.remove(storedSnapshot1.snapshotHandle) } just Runs
        every {
            db.applyInParts(capture(actualChunks), captureLambda())
        } answers {
            secondArg<(Collection<String>) -> Unit>().invoke(actualChunks.captured)
        }
        every { chunksCache.decrementRefCount(capture(actualChunks2)) } just Runs
        every { chunksCache.getUnreferencedChunks() } returns listOf(cachedChunk3)
        coEvery { backend.remove(Blob(androidId, chunk3)) } just Runs
        every { chunksCache.deleteChunks(listOf(cachedChunk3)) } just Runs

        pruner.prune(null)

        assertTrue(actualChunks.isCaptured)
        assertTrue(actualChunks2.isCaptured)
        assertEquals(expectedChunks.sorted(), actualChunks.captured.sorted())
        assertEquals(expectedChunks.sorted(), actualChunks2.captured.sorted())
    }

}
