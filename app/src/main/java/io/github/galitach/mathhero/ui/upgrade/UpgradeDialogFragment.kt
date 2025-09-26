package io.github.galitach.mathhero.ui.upgrade

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.galitach.mathhero.R
import io.github.galitach.mathhero.databinding.DialogUpgradeBinding
import io.github.galitach.mathhero.ui.MainViewModel
import io.github.galitach.mathhero.ui.MainViewModelFactory
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class UpgradeDialogFragment : DialogFragment() {

    private var _binding: DialogUpgradeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels { MainViewModelFactory }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogUpgradeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { dismiss() }
        binding.upgradeButton.setOnClickListener {
            viewModel.initiatePurchaseFlow()
        }
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.map { it.proProductDetails }.distinctUntilChanged().collect { details ->
                    val price = details?.oneTimePurchaseOfferDetails?.formattedPrice
                    if (price != null) {
                        binding.upgradeButton.text = getString(R.string.upgrade_to_pro_price, price)
                        binding.upgradeButton.isEnabled = true
                    } else {
                        binding.upgradeButton.setText(R.string.upgrade_to_pro)
                        binding.upgradeButton.isEnabled = false
                    }
                }
            }
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
        const val TAG = "UpgradeDialogFragment"
        fun newInstance(): UpgradeDialogFragment {
            return UpgradeDialogFragment()
        }
    }
}