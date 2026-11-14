package com.first10.app.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.first10.app.core.AppDetectionManager
import com.first10.app.databinding.ActivityAppManagerBinding
import com.first10.app.model.AppItem
import com.first10.app.storage.PreferencesManager
import com.first10.app.ui.adapter.AppAdapter
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppManagerBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var appDetectionManager: AppDetectionManager
    private lateinit var adapter: AppAdapter

    private var allApps: List<AppItem> = emptyList()
    private var displayedApps: List<AppItem> = emptyList()
    private var isRestrictedTab = true

    companion object {
        const val EXTRA_TAB = "extra_tab"
        const val TAB_RESTRICTED = 0
        const val TAB_ALLOWED = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager.getInstance(this)
        appDetectionManager = AppDetectionManager(this)

        val initialTab = intent.getIntExtra(EXTRA_TAB, TAB_RESTRICTED)
        isRestrictedTab = (initialTab == TAB_RESTRICTED)

        setupToolbar()
        setupTabs(initialTab)
        setupSearch()
        setupBatchActions()
        loadInstalledApps()
    }

    private fun setupToolbar() {
        binding.toolbarAppManager.setNavigationOnClickListener {
            finish()
        }
        updateTitle()
    }

    private fun updateTitle() {
        binding.toolbarAppManager.title = if (isRestrictedTab) {
            "Restricted Apps (Blocked)"
        } else {
            "Always Allowed Apps"
        }
    }

    private fun setupTabs(initialTab: Int) {
        val tab = binding.tabLayoutApps.getTabAt(initialTab)
        tab?.select()

        binding.tabLayoutApps.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                isRestrictedTab = (tab?.position == TAB_RESTRICTED)
                updateTitle()
                filterAndDisplayApps()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupSearch() {
        binding.etSearchApps.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterAndDisplayApps()
            }
        })

        binding.btnAddCustomPackage.setOnClickListener {
            showAddCustomPackageDialog()
        }
    }

    private fun setupBatchActions() {
        binding.btnSelectAll.setOnClickListener {
            val currentPackages = (if (isRestrictedTab) prefs.restrictedPackages else prefs.alwaysAllowedPackages).toMutableSet()
            displayedApps.forEach { app ->
                currentPackages.add(app.packageName)
                if (isRestrictedTab) app.isRestricted = true else app.isAlwaysAllowed = true
            }
            if (isRestrictedTab) {
                prefs.restrictedPackages = currentPackages
                // Remove from allowed
                val allowed = prefs.alwaysAllowedPackages.toMutableSet()
                allowed.removeAll(currentPackages)
                prefs.alwaysAllowedPackages = allowed
            } else {
                prefs.alwaysAllowedPackages = currentPackages
                // Remove from restricted
                val restricted = prefs.restrictedPackages.toMutableSet()
                restricted.removeAll(currentPackages)
                prefs.restrictedPackages = restricted
            }
            adapter.notifyDataSetChanged()
            updateCountText()
        }

        binding.btnDeselectAll.setOnClickListener {
            val currentPackages = (if (isRestrictedTab) prefs.restrictedPackages else prefs.alwaysAllowedPackages).toMutableSet()
            displayedApps.forEach { app ->
                currentPackages.remove(app.packageName)
                if (isRestrictedTab) app.isRestricted = false else app.isAlwaysAllowed = false
            }
            if (isRestrictedTab) {
                prefs.restrictedPackages = currentPackages
            } else {
                prefs.alwaysAllowedPackages = currentPackages
            }
            adapter.notifyDataSetChanged()
            updateCountText()
        }
    }

    private fun loadInstalledApps() {
        binding.progressLoadingApps.visibility = View.VISIBLE
        binding.rvApps.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            val apps = appDetectionManager.getInstalledApps()
            withContext(Dispatchers.Main) {
                allApps = apps
                binding.progressLoadingApps.visibility = View.GONE
                binding.rvApps.visibility = View.VISIBLE

                adapter = AppAdapter(emptyList(), isRestrictedTab) { app, isChecked ->
                    onAppToggled(app, isChecked)
                }
                binding.rvApps.layoutManager = LinearLayoutManager(this@AppManagerActivity)
                binding.rvApps.adapter = adapter

                filterAndDisplayApps()
            }
        }
    }

    private fun filterAndDisplayApps() {
        val query = binding.etSearchApps.text.toString().trim().lowercase()
        val restrictedSet = prefs.restrictedPackages
        val allowedSet = prefs.alwaysAllowedPackages

        // Update active flags
        allApps.forEach {
            it.isRestricted = restrictedSet.contains(it.packageName)
            it.isAlwaysAllowed = allowedSet.contains(it.packageName)
        }

        displayedApps = if (query.isEmpty()) {
            allApps
        } else {
            allApps.filter {
                it.appName.lowercase().contains(query) || it.packageName.lowercase().contains(query)
            }
        }

        adapter = AppAdapter(displayedApps, isRestrictedTab) { app, isChecked ->
            onAppToggled(app, isChecked)
        }
        binding.rvApps.adapter = adapter

        updateCountText()
    }

    private fun onAppToggled(app: AppItem, isChecked: Boolean) {
        val restricted = prefs.restrictedPackages.toMutableSet()
        val allowed = prefs.alwaysAllowedPackages.toMutableSet()

        if (isRestrictedTab) {
            if (isChecked) {
                restricted.add(app.packageName)
                allowed.remove(app.packageName)
                app.isAlwaysAllowed = false
            } else {
                restricted.remove(app.packageName)
            }
        } else {
            if (isChecked) {
                allowed.add(app.packageName)
                restricted.remove(app.packageName)
                app.isRestricted = false
            } else {
                allowed.remove(app.packageName)
            }
        }

        prefs.restrictedPackages = restricted
        prefs.alwaysAllowedPackages = allowed
        updateCountText()
    }

    private fun updateCountText() {
        val selectedCount = if (isRestrictedTab) prefs.restrictedPackages.size else prefs.alwaysAllowedPackages.size
        val label = if (isRestrictedTab) "restricted" else "allowed"
        binding.tvAppCount.text = "${displayedApps.size} apps found ($selectedCount $label)"
    }

    private fun showAddCustomPackageDialog() {
        val input = EditText(this).apply {
            hint = "e.g. com.instagram.android"
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Add Package Manually")
            .setMessage("Enter the exact Android package name to add to the ${if (isRestrictedTab) "Restricted" else "Always Allowed"} list:")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val pkgName = input.text.toString().trim()
                if (pkgName.isNotEmpty() && pkgName.contains(".")) {
                    val targetSet = (if (isRestrictedTab) prefs.restrictedPackages else prefs.alwaysAllowedPackages).toMutableSet()
                    targetSet.add(pkgName)
                    if (isRestrictedTab) {
                        prefs.restrictedPackages = targetSet
                    } else {
                        prefs.alwaysAllowedPackages = targetSet
                    }
                    Toast.makeText(this, "Added $pkgName", Toast.LENGTH_SHORT).show()
                    loadInstalledApps()
                } else {
                    Toast.makeText(this, "Invalid package name format. Must contain a dot (e.g. com.example)", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
