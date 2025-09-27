package io.github.galitach.mathhero.ui.ranks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import io.github.galitach.mathhero.R
import io.github.galitach.mathhero.data.HeroType
import io.github.galitach.mathhero.data.Rank
import io.github.galitach.mathhero.databinding.DialogRanksBinding

class RanksDialogFragment : DialogFragment() {

    private var _binding: DialogRanksBinding? = null
    private val binding get() = _binding!!

    private val highestStreak: Int by lazy {
        arguments?.getInt(ARG_HIGHEST_STREAK) ?: 0
    }

    private val heroType: HeroType by lazy {
        val typeName = arguments?.getString(ARG_HERO_TYPE) ?: HeroType.A.name
        HeroType.valueOf(typeName)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogRanksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { dismiss() }
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        binding.ranksRecyclerView.adapter = RankAdapter(Rank.allRanks, highestStreak, heroType)
    }

    override fun getTheme(): Int {
        return R.style.FullScreenDialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "RanksDialogFragment"
        private const val ARG_HIGHEST_STREAK = "arg_highest_streak"
        private const val ARG_HERO_TYPE = "arg_hero_type"

        fun newInstance(highestStreak: Int, heroType: HeroType): RanksDialogFragment {
            return RanksDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_HIGHEST_STREAK, highestStreak)
                    putString(ARG_HERO_TYPE, heroType.name)
                }
            }
        }
    }
}