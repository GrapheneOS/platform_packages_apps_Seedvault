/*
 * SPDX-FileCopyrightText: 2024 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package app.grapheneos.seedvault.core.backends.webdav

public data class WebDavConfig(
    val url: String,
    val username: String,
    val password: String,
)
