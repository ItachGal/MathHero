package io.github.galitach.mathhero.ui.archive

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.galitach.mathhero.R
import io.github.galitach.mathhero.data.MathProblem
import io.github.galitach.mathhero.databinding.DialogArchiveBinding

class ArchiveDialogFragment : DialogFragment() {

    private var _binding: DialogArchiveBinding? = null
    private val binding get() = _binding!!

    private val archivedProblems: List<MathProblem> by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelableArrayList(ARG_PROBLEMS, MathProblem::class.java) ?: emptyList()
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelableArrayList(ARG_PROBLEMS) ?: emptyList()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogArchiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        binding.toolbar.setNavigationOnClickListener { dismiss() }
    }

    private fun setupRecyclerView() {
        binding.archiveRecyclerView.layoutManager = LinearLayoutManager(context)
        context?.let {
            binding.archiveRecyclerView.addItemDecoration(DividerItemDecoration(it))
        }

        if (archivedProblems.isNotEmpty()) {
            binding.archiveRecyclerView.adapter = ArchiveProblemAdapter(archivedProblems)
            binding.emptyState.visibility = View.GONE
            binding.archiveRecyclerView.visibility = View.VISIBLE
        } else {
            binding.emptyState.visibility = View.VISIBLE
            binding.archiveRecyclerView.visibility = View.GONE
        }
    }

    override fun getTheme(): Int {
        return R.style.FullScreenDialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ArchiveDialogFragment"
        private const val ARG_PROBLEMS = "arg_problems"

        fun newInstance(problems: List<MathProblem>): ArchiveDialogFragment {
            return ArchiveDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(ARG_PROBLEMS, ArrayList(problems))
                }
            }
        }
    }
}