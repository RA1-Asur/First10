package com.first10.app.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.first10.app.databinding.ItemTaskBinding
import com.first10.app.model.TaskItem

class TaskAdapter(
    private var tasks: List<TaskItem>,
    private val onToggleEnabled: (TaskItem, Boolean) -> Unit,
    private val onDelete: (TaskItem) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.binding.tvTaskNumber.text = (position + 1).toString()
        holder.binding.tvTaskTitle.text = task.title

        if (task.description.isNotEmpty()) {
            holder.binding.tvTaskDescription.visibility = View.VISIBLE
            holder.binding.tvTaskDescription.text = task.description
        } else {
            holder.binding.tvTaskDescription.visibility = View.GONE
        }

        holder.binding.switchTaskEnabled.setOnCheckedChangeListener(null)
        holder.binding.switchTaskEnabled.isChecked = task.isEnabled

        holder.binding.switchTaskEnabled.setOnCheckedChangeListener { _, isChecked ->
            task.isEnabled = isChecked
            onToggleEnabled(task, isChecked)
        }

        holder.binding.btnDeleteTask.setOnClickListener {
            onDelete(task)
        }
    }

    override fun getItemCount(): Int = tasks.size

    fun updateData(newTasks: List<TaskItem>) {
        tasks = newTasks
        notifyDataSetChanged()
    }
}
