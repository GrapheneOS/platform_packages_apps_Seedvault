/*
 * SPDX-FileCopyrightText: 2024 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package app.grapheneos.seedvault.core.backends

import android.content.Context
import app.grapheneos.seedvault.core.backends.saf.SafBackend
import app.grapheneos.seedvault.core.backends.saf.SafProperties
import app.grapheneos.seedvault.core.backends.webdav.WebDavBackend
import app.grapheneos.seedvault.core.backends.webdav.WebDavConfig

public class BackendFactory {
    public fun createSafBackend(context: Context, config: SafProperties): Backend =
        SafBackend(context, config)

    public fun createWebDavBackend(config: WebDavConfig): Backend = WebDavBackend(config)
}
