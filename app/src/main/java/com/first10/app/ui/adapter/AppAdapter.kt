package com.first10.app.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.first10.app.databinding.ItemAppBinding
import com.first10.app.model.AppItem

class AppAdapter(
    private var apps: List<AppItem>,
    private val isRestrictedMode: Boolean,
    private val onToggle: (AppItem, Boolean) -> Unit
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    inner class AppViewHolder(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.binding.tvAppName.text = app.appName
        holder.binding.tvAppPackage.text = app.packageName

        if (app.icon != null) {
            holder.binding.ivAppIcon.setImageDrawable(app.icon)
        } else {
            holder.binding.ivAppIcon.setImageResource(com.first10.app.R.drawable.ic_launcher_foreground)
        }

        // Avoid triggering listener when binding
        holder.binding.switchApp.setOnCheckedChangeListener(null)
        holder.binding.switchApp.isChecked = if (isRestrictedMode) app.isRestricted else app.isAlwaysAllowed

        holder.binding.switchApp.setOnCheckedChangeListener { _, isChecked ->
            if (isRestrictedMode) {
                app.isRestricted = isChecked
            } else {
                app.isAlwaysAllowed = isChecked
            }
            onToggle(app, isChecked)
        }

        holder.itemView.setOnClickListener {
            holder.binding.switchApp.toggle()
        }
    }

    override fun getItemCount(): Int = apps.size

    fun updateData(newApps: List<AppItem>) {
        apps = newApps
        notifyDataSetChanged()
    }
}
