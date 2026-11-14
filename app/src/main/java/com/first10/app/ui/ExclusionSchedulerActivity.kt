package com.first10.app.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.first10.app.R
import com.first10.app.databinding.ActivityExclusionSchedulerBinding
import com.first10.app.model.ExclusionPeriod
import com.first10.app.storage.PreferencesManager
import com.first10.app.ui.adapter.ExclusionAdapter
import java.util.Calendar
import java.util.Locale

class ExclusionSchedulerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExclusionSchedulerBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var adapter: ExclusionAdapter
    private var exclusions: MutableList<ExclusionPeriod> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExclusionSchedulerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager.getInstance(this)

        setupToolbar()
        setupRecyclerView()
        setupFab()
        loadExclusions()
    }

    private fun setupToolbar() {
        binding.toolbarExclusions.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = ExclusionAdapter(
            exclusions,
            onToggleEnabled = { item, isChecked ->
                val index = exclusions.indexOfFirst { it.id == item.id }
                if (index != -1) {
                    exclusions[index].isEnabled = isChecked
                    prefs.saveExclusionPeriods(exclusions)
                }
            },
            onEdit = { item ->
                showAddOrEditDialog(item)
            },
            onDelete = { item ->
                exclusions.removeAll { it.id == item.id }
                prefs.saveExclusionPeriods(exclusions)
                updateListUI()
            }
        )

        binding.rvExclusions.layoutManager = LinearLayoutManager(this)
        binding.rvExclusions.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddExclusion.setOnClickListener {
            showAddOrEditDialog(null)
        }
    }

    private fun loadExclusions() {
        exclusions = prefs.getExclusionPeriods().toMutableList()
        updateListUI()
    }

    private fun updateListUI() {
        adapter.updateData(exclusions)
        if (exclusions.isEmpty()) {
            binding.tvEmptyExclusions.visibility = View.VISIBLE
            binding.rvExclusions.visibility = View.GONE
        } else {
            binding.tvEmptyExclusions.visibility = View.GONE
            binding.rvExclusions.visibility = View.VISIBLE
        }
    }

    private fun showAddOrEditDialog(existing: ExclusionPeriod?) {
        var startH = existing?.startHour ?: 22
        var startM = existing?.startMinute ?: 0
        var endH = existing?.endHour ?: 6
        var endM = existing?.endMinute ?: 0

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 16)
        }

        val etName = EditText(this).apply {
            hint = "Schedule Name (e.g. Night Sleep, Dinner)"
            setText(existing?.name ?: "Night Sleep")
        }
        layout.addView(etName)

        val tvStartTime = TextView(this).apply {
            text = "Start Time: ${formatTime(startH, startM)}"
            textSize = 15f
            setPadding(0, 24, 0, 12)
            setOnClickListener {
                TimePickerDialog(this@ExclusionSchedulerActivity, { _, h, m ->
                    startH = h
                    startM = m
                    text = "Start Time: ${formatTime(startH, startM)}"
                }, startH, startM, false).show()
            }
        }
        layout.addView(tvStartTime)

        val tvEndTime = TextView(this).apply {
            text = "End Time: ${formatTime(endH, endM)}"
            textSize = 15f
            setPadding(0, 12, 0, 24)
            setOnClickListener {
                TimePickerDialog(this@ExclusionSchedulerActivity, { _, h, m ->
                    endH = h
                    endM = m
                    text = "End Time: ${formatTime(endH, endM)}"
                }, endH, endM, false).show()
            }
        }
        layout.addView(tvEndTime)

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Add Exclusion Schedule" else "Edit Exclusion Schedule")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim().ifEmpty { "Exclusion Period" }
                if (existing != null) {
                    existing.name = name
                    existing.startHour = startH
                    existing.startMinute = startM
                    existing.endHour = endH
                    existing.endMinute = endM
                } else {
                    val newPeriod = ExclusionPeriod(
                        name = name,
                        startHour = startH,
                        startMinute = startM,
                        endHour = endH,
                        endMinute = endM
                    )
                    exclusions.add(newPeriod)
                }
                prefs.saveExclusionPeriods(exclusions)
                updateListUI()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.US, "%d:%02d %s", displayHour, minute, amPm)
    }
}
