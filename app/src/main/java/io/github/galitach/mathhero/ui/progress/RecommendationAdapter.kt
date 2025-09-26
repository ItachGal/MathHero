package io.github.galitach.mathhero.ui.progress

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.galitach.mathhero.data.Recommendation
import io.github.galitach.mathhero.databinding.ItemRecommendationBinding

class RecommendationAdapter(
    private val onClick: (Recommendation) -> Unit
) : ListAdapter<Recommendation, RecommendationAdapter.RecommendationViewHolder>(RecommendationDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendationViewHolder {
        val binding = ItemRecommendationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecommendationViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: RecommendationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RecommendationViewHolder(
        private val binding: ItemRecommendationBinding,
        private val onClick: (Recommendation) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private var currentRecommendation: Recommendation? = null

        init {
            binding.root.setOnClickListener {
                currentRecommendation?.let { onClick(it) }
            }
        }

        fun bind(recommendation: Recommendation) {
            currentRecommendation = recommendation
            binding.recommendationIcon.setImageResource(recommendation.iconRes)
            binding.recommendationTitle.setText(recommendation.titleRes)
            binding.recommendationDescription.text = recommendation.description
        }
    }
}

object RecommendationDiffCallback : DiffUtil.ItemCallback<Recommendation>() {
    override fun areItemsTheSame(oldItem: Recommendation, newItem: Recommendation): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Recommendation, newItem: Recommendation): Boolean {
        return oldItem == newItem
    }
}