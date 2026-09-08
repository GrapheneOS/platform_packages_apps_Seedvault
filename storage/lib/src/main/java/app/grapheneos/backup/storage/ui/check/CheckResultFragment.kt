/*
 * SPDX-FileCopyrightText: 2024 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package app.grapheneos.backup.storage.ui.check

import android.os.Bundle
import android.text.format.Formatter.formatShortFileSize
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import app.grapheneos.backup.storage.R
import app.grapheneos.backup.storage.api.CheckResult
import app.grapheneos.backup.storage.api.SnapshotItem
import app.grapheneos.backup.storage.api.StoredSnapshot
import app.grapheneos.backup.storage.ui.restore.SnapshotAdapter

internal class CheckResultFragment : Fragment() {

    private val activity get() = (requireActivity() as CheckResultActivity)
    private val checkResult get() = activity.storageBackupInt.checkResult

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_check_results, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        when (val result = checkResult) {
            is CheckResult.Success -> onSuccess(result)
            is CheckResult.Error -> onError(result)
            is CheckResult.GeneralError, null -> {
                if (result == null) {
                    val str = getString(R.string.check_error_no_result)
                    val e = NullPointerException(str)
                    val r = CheckResult.GeneralError(e)
                    onGeneralError(r)
                } else {
                    onGeneralError(result as CheckResult.GeneralError)
                }
            }
        }
    }

    private fun onSuccess(result: CheckResult.Success) {
        val intro = getString(
            R.string.check_success_intro,
            result.snapshots.size,
            result.percent,
            formatShortFileSize(requireContext(), result.size),
        )
        requireView().requireViewById<TextView>(R.id.introView).text = intro

        val items = result.snapshots.map { snapshot ->
            SnapshotItem(
                storedSnapshot = StoredSnapshot("doesNotMatter", snapshot.timeStart),
                snapshot = snapshot,
            )
        }.sortedByDescending { it.time }
        val listView = requireView().requireViewById<RecyclerView>(R.id.listView)
        listView.adapter = SnapshotAdapter(activity).apply {
            submitList(items)
        }
    }

    private fun onError(result: CheckResult.Error) {
        val v = requireView()
        v.requireViewById<ImageView>(R.id.imageView).setImageResource(R.drawable.ic_cloud_error)
        v.requireViewById<TextView>(R.id.titleView).setText(R.string.check_error_title)

        val intro = if (result.existingSnapshots == 0) {
            getString(R.string.check_error_no_snapshots)
        } else if (result.snapshots.isEmpty()) {
            getString(
                R.string.check_error_only_broken_snapshots,
                result.existingSnapshots,
            )
        } else {
            getString(R.string.check_error_has_snapshots, result.existingSnapshots)
        }
        v.requireViewById<TextView>(R.id.introView).text = intro

        val items = (result.goodSnapshots.map { snapshot ->
            SnapshotItem(
                storedSnapshot = StoredSnapshot("doesNotMatter", snapshot.timeStart),
                snapshot = snapshot,
                hasError = false,
            )
        } + result.badSnapshots.map { snapshot ->
            SnapshotItem(
                storedSnapshot = StoredSnapshot("doesNotMatter", snapshot.timeStart),
                snapshot = snapshot,
                hasError = true,
            )
        }).sortedByDescending { it.time }
        val listView = v.requireViewById<RecyclerView>(R.id.listView)
        listView.adapter = SnapshotAdapter(activity).apply {
            submitList(items)
        }
    }

    private fun onGeneralError(result: CheckResult.GeneralError) {
        val v = requireView()
        v.requireViewById<ImageView>(R.id.imageView).setImageResource(R.drawable.ic_cloud_error)
        v.requireViewById<TextView>(R.id.titleView).setText(R.string.check_error_title)

        v.requireViewById<TextView>(R.id.introView).text =
            getString(R.string.check_error_no_snapshots)

        v.requireViewById<TextView>(R.id.errorView).text =
            "${result.e.localizedMessage}\n\n${result.e}"
    }
}
