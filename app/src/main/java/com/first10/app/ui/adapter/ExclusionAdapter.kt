package com.first10.app.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.first10.app.databinding.ItemExclusionBinding
import com.first10.app.model.ExclusionPeriod

class ExclusionAdapter(
    private var exclusions: List<ExclusionPeriod>,
    private val onToggleEnabled: (ExclusionPeriod, Boolean) -> Unit,
    private val onEdit: (ExclusionPeriod) -> Unit,
    private val onDelete: (ExclusionPeriod) -> Unit
) : RecyclerView.Adapter<ExclusionAdapter.ExclusionViewHolder>() {

    inner class ExclusionViewHolder(val binding: ItemExclusionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExclusionViewHolder {
        val binding = ItemExclusionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExclusionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExclusionViewHolder, position: Int) {
        val item = exclusions[position]
        holder.binding.tvExclusionName.text = item.name
        holder.binding.tvExclusionTime.text = item.formattedTimeRange()
        holder.binding.tvExclusionDays.text = item.formattedDays()

        holder.binding.switchExclusion.setOnCheckedChangeListener(null)
        holder.binding.switchExclusion.isChecked = item.isEnabled

        holder.binding.switchExclusion.setOnCheckedChangeListener { _, isChecked ->
            item.isEnabled = isChecked
            onToggleEnabled(item, isChecked)
        }

        holder.binding.btnEditExclusion.setOnClickListener {
            onEdit(item)
        }

        holder.binding.btnDeleteExclusion.setOnClickListener {
            onDelete(item)
        }
    }

    override fun getItemCount(): Int = exclusions.size

    fun updateData(newExclusions: List<ExclusionPeriod>) {
        exclusions = newExclusions
        notifyDataSetChanged()
    }
}
