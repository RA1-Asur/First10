package com.first10.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.first10.app.databinding.ActivityTaskEditorBinding
import com.first10.app.model.TaskItem
import com.first10.app.storage.PreferencesManager

class TaskEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskEditorBinding
    private lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager.getInstance(this)

        setupToolbar()
        setupSaveButton()
    }

    private fun setupToolbar() {
        binding.toolbarTaskEditor.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupSaveButton() {
        binding.btnSaveTask.setOnClickListener {
            val title = binding.etTaskTitle.text.toString().trim()
            val desc = binding.etTaskDescription.text.toString().trim()

            if (title.isEmpty()) {
                Toast.makeText(this, "Please enter a task title", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val routines = prefs.getRoutines()
            val defaultRoutine = routines.firstOrNull { it.isDefault } ?: routines.first()

            val newTask = TaskItem(
                title = title,
                description = desc,
                orderIndex = defaultRoutine.tasks.size
            )
            defaultRoutine.tasks.add(newTask)

            prefs.saveRoutines(routines)
            Toast.makeText(this, "Task added to routine", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
