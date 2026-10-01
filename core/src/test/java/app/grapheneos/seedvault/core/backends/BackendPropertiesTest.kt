package app.grapheneos.seedvault.core.backends

import at.bitfire.dav4jvm.okhttp.exception.HttpException
import org.junit.Test
import java.io.IOException
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class BackendPropertiesTest {

    private fun httpException(statusCode: Int) =
        HttpException("HTTP $statusCode", null, statusCode, null, null, emptyList())

    @Test
    fun `WebDAV 507 is out of space`() {
        assertTrue(httpException(507).isOutOfSpace())
        assertTrue(IOException(httpException(507)).isOutOfSpace())
        assertFalse(httpException(500).isOutOfSpace())
        assertFalse(IOException(httpException(500)).isOutOfSpace())
    }
}
