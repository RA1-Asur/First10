package com.first10.app.ui.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.first10.app.databinding.ItemTaskBlockingBinding
import com.first10.app.model.TaskItem

class BlockingTaskAdapter(
    private var tasks: List<TaskItem>,
    private val onCheckChanged: (TaskItem, Boolean) -> Unit
) : RecyclerView.Adapter<BlockingTaskAdapter.BlockingTaskViewHolder>() {

    inner class BlockingTaskViewHolder(val binding: ItemTaskBlockingBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockingTaskViewHolder {
        val binding = ItemTaskBlockingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BlockingTaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BlockingTaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.binding.tvTaskTitle.text = task.title

        holder.binding.cbTaskCompleted.setOnCheckedChangeListener(null)
        holder.binding.cbTaskCompleted.isChecked = task.isCompleted
        updateStrikeThrough(holder, task.isCompleted)

        holder.binding.cbTaskCompleted.setOnCheckedChangeListener { _, isChecked ->
            task.isCompleted = isChecked
            updateStrikeThrough(holder, isChecked)
            onCheckChanged(task, isChecked)
        }

        holder.itemView.setOnClickListener {
            holder.binding.cbTaskCompleted.toggle()
        }
    }

    private fun updateStrikeThrough(holder: BlockingTaskViewHolder, isCompleted: Boolean) {
        if (isCompleted) {
            holder.binding.tvTaskTitle.paintFlags =
                holder.binding.tvTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.binding.tvTaskTitle.alpha = 0.5f
        } else {
            holder.binding.tvTaskTitle.paintFlags =
                holder.binding.tvTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.binding.tvTaskTitle.alpha = 1.0f
        }
    }

    override fun getItemCount(): Int = tasks.size

    fun updateData(newTasks: List<TaskItem>) {
        tasks = newTasks
        notifyDataSetChanged()
    }
}
