package app.grapheneos.seedvault.core.backends.saf

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import app.grapheneos.seedvault.core.backends.AppBackupFileType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

internal class DocumentFileCacheTest {

    private val context: Context = mockk()
    private val baseFile: DocumentFile = mockk()
    private val rootFile: DocumentFile = mockk()
    private val repoFolder: DocumentFile = mockk()
    private val snapshotFile: DocumentFile = mockk()

    private val handle = AppBackupFileType.Snapshot("repoId", "hash")
    private val cache = DocumentFileCache(context, baseFile, "root")

    init {
        mockkStatic("app.grapheneos.seedvault.core.backends.saf.SafHelperKt")
        coEvery { baseFile.getOrCreateDirectory(context, "root") } returns rootFile
        coEvery { rootFile.listFilesBlocking(context) } returns listOf(repoFolder)
        every { repoFolder.name } returns "repoId"
        every { snapshotFile.name } returns handle.name
    }

    @Test
    fun `getFile finds existing file`() = runTest {
        coEvery { repoFolder.listFilesBlocking(context) } returns listOf(snapshotFile)

        assertSame(snapshotFile, cache.getFile(handle))
    }

    @Test
    fun `getFile returns null for missing file`() = runTest {
        coEvery { repoFolder.listFilesBlocking(context) } returns emptyList()

        assertNull(cache.getFile(handle))
    }

    @Test
    fun `getFile throws when listing fails`() = runTest {
        coEvery { repoFolder.listFilesBlocking(context) } throws IOException("timeout")

        assertFailsWith<SafRetryException> { cache.getFile(handle) }
    }
}
