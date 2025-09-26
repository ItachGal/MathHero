package io.github.galitach.mathhero.ui.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.galitach.mathhero.databinding.ItemOnboardingPageBinding

class OnboardingAdapter(private val pages: List<OnboardingPage>) :
    RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val binding = ItemOnboardingPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OnboardingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(pages[position])
    }

    override fun getItemCount(): Int = pages.size

    class OnboardingViewHolder(private val binding: ItemOnboardingPageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(page: OnboardingPage) {
            binding.onboardingImage.setImageResource(page.imageRes)
            binding.onboardingTitle.setText(page.titleRes)
            binding.onboardingDescription.setText(page.descriptionRes)
        }
    }
}