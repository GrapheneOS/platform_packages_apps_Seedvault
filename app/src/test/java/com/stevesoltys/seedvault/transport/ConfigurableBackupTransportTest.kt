package com.stevesoltys.seedvault.transport

import android.app.backup.BackupTransport.TRANSPORT_OK
import android.app.backup.BackupTransport.TRANSPORT_PACKAGE_REJECTED
import android.content.Context
import android.content.pm.PackageInfo
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stevesoltys.seedvault.TestApp
import com.stevesoltys.seedvault.transport.backup.BackupCoordinator
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(
    sdk = [35], // TODO: Drop once robolectric supports 36
    application = TestApp::class
)
internal class ConfigurableBackupTransportTest {

    private val backupCoordinator: BackupCoordinator = mockk()
    private val fd: ParcelFileDescriptor = mockk {
        every { close() } just Runs
    }
    private val packageInfo = PackageInfo().apply { packageName = "org.example" }

    private val transport = run {
        loadKoinModules(module { single { backupCoordinator } })
        ConfigurableBackupTransport(ApplicationProvider.getApplicationContext<Context>())
    }

    @After
    fun afterEachTest() {
        stopKoin()
    }

    @Test
    fun `K-V backup always closes the file descriptor`() {
        every { backupCoordinator.performIncrementalBackup(packageInfo, fd, 0) } returns TRANSPORT_OK

        assertEquals(TRANSPORT_OK, transport.performBackup(packageInfo, fd, 0))
        verify { fd.close() }
    }

    @Test
    fun `accepted full backup keeps socket open for sendBackupData`() {
        every { backupCoordinator.performFullBackup(packageInfo, fd, 0) } returns TRANSPORT_OK

        assertEquals(TRANSPORT_OK, transport.performFullBackup(packageInfo, fd, 0))
        verify(exactly = 0) { fd.close() }
    }

    @Test
    fun `rejected or failed full backup closes socket`() {
        every {
            backupCoordinator.performFullBackup(packageInfo, fd, 0)
        } returns TRANSPORT_PACKAGE_REJECTED andThenThrows IllegalStateException()

        assertEquals(TRANSPORT_PACKAGE_REJECTED, transport.performFullBackup(packageInfo, fd, 0))
        assertThrows(IllegalStateException::class.java) {
            transport.performFullBackup(packageInfo, fd, 0)
        }
        verify(exactly = 2) { fd.close() }
    }
}
