package com.first10.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.first10.app.databinding.ActivityRoutineManagerBinding
import com.first10.app.model.Routine
import com.first10.app.storage.PreferencesManager
import com.first10.app.ui.adapter.TaskAdapter

class RoutineManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoutineManagerBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var adapter: TaskAdapter
    private lateinit var currentRoutine: Routine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoutineManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager.getInstance(this)

        setupToolbar()
        setupRecyclerView()
        setupFab()
    }

    override fun onResume() {
        super.onResume()
        loadRoutineTasks()
    }

    private fun setupToolbar() {
        binding.toolbarRoutines.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        currentRoutine = prefs.getDefaultRoutine()
        adapter = TaskAdapter(
            currentRoutine.tasks,
            onToggleEnabled = { task, isChecked ->
                val routines = prefs.getRoutines()
                val r = routines.firstOrNull { it.id == currentRoutine.id } ?: currentRoutine
                val t = r.tasks.firstOrNull { it.id == task.id }
                t?.isEnabled = isChecked
                prefs.saveRoutines(routines)
            },
            onDelete = { task ->
                val routines = prefs.getRoutines()
                val r = routines.firstOrNull { it.id == currentRoutine.id } ?: currentRoutine
                r.tasks.removeAll { it.id == task.id }
                prefs.saveRoutines(routines)
                loadRoutineTasks()
            }
        )

        binding.rvRoutineTasks.layoutManager = LinearLayoutManager(this)
        binding.rvRoutineTasks.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddTask.setOnClickListener {
            startActivity(Intent(this, TaskEditorActivity::class.java))
        }
    }

    private fun loadRoutineTasks() {
        currentRoutine = prefs.getDefaultRoutine()
        adapter.updateData(currentRoutine.tasks)
    }
}
