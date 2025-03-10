/*
 * SPDX-FileCopyrightText: 2024 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package app.grapheneos.backup.storage.ui.check

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import app.grapheneos.backup.storage.R
import app.grapheneos.backup.storage.api.CheckResult
import app.grapheneos.backup.storage.api.SnapshotItem
import app.grapheneos.backup.storage.ui.restore.FilesAdapter

internal const val SNAPSHOT_TIMESTAMP = "snapshotTimestamp"

public class SnapshotFilesFragment : Fragment() {

    private val activity get() = (requireActivity() as CheckResultActivity)
    private val checkResult get() = activity.storageBackupInt.checkResult
    private val fileSelectionManager get() = activity.fileSelectionManagerInt

    private lateinit var list: RecyclerView
    private lateinit var adapter: FilesAdapter

    internal companion object {
        fun new(snapshotItem: SnapshotItem) = SnapshotFilesFragment().apply {
            arguments = Bundle().apply {
                putLong(SNAPSHOT_TIMESTAMP, snapshotItem.time)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val v = inflater.inflate(R.layout.fragment_select_files, container, false)
        val topStub: ViewStub = v.requireViewById(R.id.topStub)
        topStub.layoutResource = R.layout.header_snapshot_files
        topStub.inflate()
        list = v.requireViewById(R.id.list)

        return v
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val timestamp = requireArguments().getLong(SNAPSHOT_TIMESTAMP, -1L)
        val snapshot = when (val result = checkResult) {
            is CheckResult.Success -> result.snapshots.find { it.timeStart == timestamp }
            is CheckResult.Error -> result.snapshots.find { it.timeStart == timestamp }
            else -> null
        }
        if (snapshot == null) {
            Toast.makeText(requireContext(), R.string.check_error_no_result, LENGTH_LONG).show()
            parentFragmentManager.popBackStack()
        } else {
            val badChunkIds = (checkResult as? CheckResult.Error)?.badChunkIds
            fileSelectionManager.onSnapshotChosen(snapshot, badChunkIds)

            val textView: TextView = requireView().requireViewById(R.id.textView)
            textView.text = getString(R.string.check_files_text, snapshot.name)
            adapter = FilesAdapter(fileSelectionManager::onExpandClicked, null)
            list.adapter = adapter
            lifecycleScope.launch {
                fileSelectionManager.files.flowWithLifecycle(lifecycle, State.STARTED).collect {
                    adapter.submitList(it)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fileSelectionManager.clearState()
    }
}
