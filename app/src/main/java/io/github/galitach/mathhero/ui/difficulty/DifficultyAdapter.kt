package io.github.galitach.mathhero.ui.difficulty

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.galitach.mathhero.data.DifficultyLevel
import io.github.galitach.mathhero.databinding.ItemDifficultyLevelBinding

class DifficultyAdapter(
    private val onDifficultySelected: (DifficultyLevel) -> Unit
) : RecyclerView.Adapter<DifficultyAdapter.DifficultyViewHolder>() {

    private val difficultyLevels = DifficultyLevel.values()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DifficultyViewHolder {
        val binding = ItemDifficultyLevelBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DifficultyViewHolder(binding, onDifficultySelected)
    }

    override fun onBindViewHolder(holder: DifficultyViewHolder, position: Int) {
        holder.bind(difficultyLevels[position])
    }

    override fun getItemCount(): Int = difficultyLevels.size

    class DifficultyViewHolder(
        private val binding: ItemDifficultyLevelBinding,
        private val onDifficultySelected: (DifficultyLevel) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(level: DifficultyLevel) {
            val context = binding.root.context
            binding.difficultyTitle.text = context.getString(level.titleRes)
            binding.difficultyDescription.text = context.getString(level.descriptionRes)
            binding.root.setOnClickListener {
                onDifficultySelected(level)
            }
        }
    }
}