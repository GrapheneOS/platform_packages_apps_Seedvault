/*
 * SPDX-FileCopyrightText: 2021 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package de.grobox.storagebackuptester.plugin

import android.content.Context
import android.net.Uri
import app.grapheneos.seedvault.core.backends.Backend
import app.grapheneos.seedvault.core.backends.FileHandle
import app.grapheneos.seedvault.core.backends.FileInfo
import app.grapheneos.seedvault.core.backends.TopLevelFolder
import app.grapheneos.seedvault.core.backends.saf.SafBackend
import app.grapheneos.seedvault.core.backends.saf.SafProperties
import java.io.InputStream
import java.io.OutputStream
import kotlin.reflect.KClass

class TestSafBackend(
    private val appContext: Context,
    private val getLocationUri: () -> Uri?,
) : Backend {

    private val safProperties
        get() = SafProperties(
            config = getLocationUri() ?: error("no uri"),
            name = "foo",
            isUsb = false,
            requiresNetwork = false,
            rootId = "bar",
        )
    private val delegate: SafBackend get() = SafBackend(appContext, safProperties)

    private val nullStream = object : OutputStream() {
        override fun write(b: Int) {
            // oops
        }
    }

    override suspend fun test(): Boolean = delegate.test()

    override suspend fun getFreeSpace(): Long? = delegate.getFreeSpace()

    override suspend fun save(handle: FileHandle): OutputStream {
        if (getLocationUri() == null) return nullStream
        return delegate.save(handle)
    }

    override suspend fun load(handle: FileHandle): InputStream {
        return delegate.load(handle)
    }

    override suspend fun list(
        topLevelFolder: TopLevelFolder?,
        vararg fileTypes: KClass<out FileHandle>,
        callback: (FileInfo) -> Unit,
    ) = delegate.list(topLevelFolder, *fileTypes, callback = callback)

    override suspend fun remove(handle: FileHandle) = delegate.remove(handle)

    override suspend fun rename(from: TopLevelFolder, to: TopLevelFolder) {
        delegate.rename(from, to)
    }

    override suspend fun removeAll() = delegate.removeAll()

    override val providerPackageName: String? get() = delegate.providerPackageName

}
