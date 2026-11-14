package com.first10.app.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.first10.app.databinding.ItemMessageBinding
import com.first10.app.model.MotivationalMessage

class MessageAdapter(
    private var messages: List<MotivationalMessage>,
    private val onToggleEnabled: (MotivationalMessage, Boolean) -> Unit,
    private val onDelete: (MotivationalMessage) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val item = messages[position]
        holder.binding.tvMessageText.text = item.text

        holder.binding.switchMessageEnabled.setOnCheckedChangeListener(null)
        holder.binding.switchMessageEnabled.isChecked = item.isEnabled

        holder.binding.switchMessageEnabled.setOnCheckedChangeListener { _, isChecked ->
            item.isEnabled = isChecked
            onToggleEnabled(item, isChecked)
        }

        holder.binding.btnDeleteMessage.setOnClickListener {
            onDelete(item)
        }
    }

    override fun getItemCount(): Int = messages.size

    fun updateData(newMessages: List<MotivationalMessage>) {
        messages = newMessages
        notifyDataSetChanged()
    }
}
