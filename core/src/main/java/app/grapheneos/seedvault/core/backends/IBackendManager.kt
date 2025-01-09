/*
 * SPDX-FileCopyrightText: 2024 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package app.grapheneos.seedvault.core.backends

public interface IBackendManager {
    public val backend: Backend
    public val isOnRemovableDrive: Boolean
    public val requiresNetwork: Boolean
}
