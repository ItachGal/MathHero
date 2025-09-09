package io.github.galitach.mathhero.ui.ranks

import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import io.github.galitach.mathhero.R
import io.github.galitach.mathhero.data.Rank
import io.github.galitach.mathhero.databinding.ItemRankBinding

class RankAdapter(
    private val ranks: List<Rank>,
    private val highestStreak: Int
) : RecyclerView.Adapter<RankAdapter.RankViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RankViewHolder {
        val binding = ItemRankBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RankViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RankViewHolder, position: Int) {
        holder.bind(ranks[position], highestStreak)
    }

    override fun getItemCount(): Int = ranks.size

    class RankViewHolder(private val binding: ItemRankBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(rank: Rank, highestStreak: Int) {
            val isUnlocked = highestStreak >= rank.requiredStreak
            val context = binding.root.context

            binding.rankImage.setImageResource(rank.imageRes)
            binding.rankName.text = if (isUnlocked) {
                context.getString(rank.nameRes)
            } else {
                context.getString(R.string.rank_locked)
            }
            binding.root.alpha = if (isUnlocked) 1.0f else 0.5f

            if (isUnlocked) {
                binding.rankImage.clearColorFilter()
            } else {
                val silhouetteColor = MaterialColors.getColor(
                    context,
                    com.google.android.material.R.attr.colorOnSurface,
                    0
                )
                binding.rankImage.setColorFilter(silhouetteColor, PorterDuff.Mode.SRC_IN)
            }
        }
    }
}