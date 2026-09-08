/*
 * SPDX-FileCopyrightText: 2024 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package app.grapheneos.backup.storage.api

import app.grapheneos.backup.storage.backup.BackupSnapshot

public sealed class CheckResult {

    public data class Success(
        val snapshots: List<BackupSnapshot>,
        val percent: Int,
        val size: Long,
    ) : CheckResult()

    public data class Error(
        /**
         * This number is greater than the size of [snapshots],
         * if we could not read/decrypt one or more snapshots.
         */
        val existingSnapshots: Int,
        val snapshots: List<BackupSnapshot>,
        val missingChunkIds: Set<String>,
        val malformedChunkIds: Set<String>,
    ) : CheckResult() {

        val goodSnapshots: List<BackupSnapshot>
        val badSnapshots: List<BackupSnapshot>
        val badChunkIds: Set<String> get() = missingChunkIds + malformedChunkIds

        init {
            val good = mutableListOf<BackupSnapshot>()
            val bad = mutableListOf<BackupSnapshot>()
            snapshots.forEach { snapshot ->
                val chunkIds = (snapshot.mediaFilesList.flatMap { it.chunkIdsList } +
                    snapshot.documentFilesList.flatMap { it.chunkIdsList }).toSet()
                val snapshotBadChunkIds = chunkIds.intersect(badChunkIds)
                if (snapshotBadChunkIds.isEmpty()) {
                    good.add(snapshot)
                } else {
                    bad.add(snapshot)
                }
            }
            goodSnapshots = good
            badSnapshots = bad
        }
    }

    public data class GeneralError(val e: Exception) : CheckResult()
}
