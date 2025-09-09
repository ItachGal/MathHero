package io.github.galitach.mathhero.ui.archive

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import io.github.galitach.mathhero.data.MathProblem
import io.github.galitach.mathhero.databinding.ItemArchiveProblemBinding

class ArchiveProblemAdapter(private val problems: List<MathProblem>) :
    RecyclerView.Adapter<ArchiveProblemAdapter.ProblemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProblemViewHolder {
        val binding = ItemArchiveProblemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProblemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProblemViewHolder, position: Int) {
        holder.bind(problems[position])
        holder.itemView.alpha = 0f
        holder.itemView.translationY = 50f
        holder.itemView.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(position * 50L)
            .setDuration(400)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    override fun getItemCount(): Int = problems.size

    class ProblemViewHolder(private val binding: ItemArchiveProblemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(problem: MathProblem) {
            binding.problemText.text = problem.question
            binding.answerText.text = problem.answer
        }
    }
}