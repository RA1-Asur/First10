package com.first10.app.ui

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.first10.app.databinding.ActivityMessagesBinding
import com.first10.app.model.MotivationalMessage
import com.first10.app.storage.PreferencesManager
import com.first10.app.ui.adapter.MessageAdapter

class MessagesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMessagesBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var adapter: MessageAdapter
    private var messages: MutableList<MotivationalMessage> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager.getInstance(this)

        setupToolbar()
        setupRotationSwitch()
        setupRecyclerView()
        setupFab()
        loadMessages()
    }

    private fun setupToolbar() {
        binding.toolbarMessages.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRotationSwitch() {
        binding.switchRandomRotation.isChecked = prefs.isRandomMessageRotation
        binding.switchRandomRotation.setOnCheckedChangeListener { _, isChecked ->
            prefs.isRandomMessageRotation = isChecked
        }
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(
            messages,
            onToggleEnabled = { item, isChecked ->
                val index = messages.indexOfFirst { it.id == item.id }
                if (index != -1) {
                    messages[index].isEnabled = isChecked
                    prefs.saveMotivationalMessages(messages)
                }
            },
            onDelete = { item ->
                messages.removeAll { it.id == item.id }
                prefs.saveMotivationalMessages(messages)
                loadMessages()
            }
        )

        binding.rvMessages.layoutManager = LinearLayoutManager(this)
        binding.rvMessages.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddMessage.setOnClickListener {
            showAddMessageDialog()
        }
    }

    private fun loadMessages() {
        messages = prefs.getMotivationalMessages().toMutableList()
        adapter.updateData(messages)
    }

    private fun showAddMessageDialog() {
        val input = EditText(this).apply {
            hint = "e.g. Your morning belongs to you."
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Add Motivational Message")
            .setMessage("Enter a mindful, respectful reminder to interrupt automatic phone usage:")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    val newMessage = MotivationalMessage(text = text)
                    messages.add(newMessage)
                    prefs.saveMotivationalMessages(messages)
                    loadMessages()
                    Toast.makeText(this, "Message added", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
