package com.first10.app.storage

import android.content.Context
import android.content.SharedPreferences
import com.first10.app.model.ExclusionPeriod
import com.first10.app.model.MotivationalMessage
import com.first10.app.model.Routine
import com.first10.app.model.TamperEvent
import com.first10.app.model.TaskItem
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "first10_prefs"

        private const val KEY_IS_ACTIVE = "key_is_active"
        private const val KEY_START_TIMESTAMP = "key_start_timestamp"
        private const val KEY_EXPIRATION_TIMESTAMP = "key_expiration_timestamp"
        private const val KEY_LAST_ACTIVATION_DATE = "key_last_activation_date"
        private const val KEY_DURATION_MINUTES = "key_duration_minutes"

        private const val KEY_RESTRICTED_PACKAGES = "key_restricted_packages"
        private const val KEY_ALLOWED_PACKAGES = "key_allowed_packages"

        private const val KEY_ROUTINES_JSON = "key_routines_json"
        private const val KEY_EXCLUSIONS_JSON = "key_exclusions_json"
        private const val KEY_MESSAGES_JSON = "key_messages_json"
        private const val KEY_TAMPER_LOG_JSON = "key_tamper_log_json"

        private const val KEY_PIN_HASH = "key_pin_hash"
        private const val KEY_PIN_SALT = "key_pin_salt"
        private const val KEY_RECOVERY_QUESTION = "key_recovery_question"
        private const val KEY_RECOVERY_ANSWER_HASH = "key_recovery_answer_hash"
        private const val KEY_FAILED_PIN_ATTEMPTS = "key_failed_pin_attempts"
        private const val KEY_LOCKOUT_UNTIL = "key_lockout_until"

        private const val KEY_STEALTH_MODE = "key_stealth_mode"
        private const val KEY_RANDOM_MESSAGES = "key_random_messages"
        private const val KEY_APP_ENABLED = "key_app_enabled"

        @Volatile
        private var instance: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }

    // Restriction State
    var isAppEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_APP_ENABLED, value).apply()

    var isRestrictionActive: Boolean
        get() {
            val active = prefs.getBoolean(KEY_IS_ACTIVE, false)
            val expires = expirationTimestamp
            return active && System.currentTimeMillis() < expires
        }
        set(value) = prefs.edit().putBoolean(KEY_IS_ACTIVE, value).apply()

    var startTimestamp: Long
        get() = prefs.getLong(KEY_START_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_START_TIMESTAMP, value).apply()

    var expirationTimestamp: Long
        get() = prefs.getLong(KEY_EXPIRATION_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_EXPIRATION_TIMESTAMP, value).apply()

    var lastActivationDate: String
        get() = prefs.getString(KEY_LAST_ACTIVATION_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_ACTIVATION_DATE, value).apply()

    var restrictionDurationMinutes: Int
        get() = prefs.getInt(KEY_DURATION_MINUTES, 10)
        set(value) = prefs.edit().putInt(KEY_DURATION_MINUTES, value).apply()

    // Apps Management
    var restrictedPackages: Set<String>
        get() = prefs.getStringSet(KEY_RESTRICTED_PACKAGES, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_RESTRICTED_PACKAGES, HashSet(value)).apply()

    var alwaysAllowedPackages: Set<String>
        get() = prefs.getStringSet(KEY_ALLOWED_PACKAGES, defaultAlwaysAllowedPackages()) ?: defaultAlwaysAllowedPackages()
        set(value) = prefs.edit().putStringSet(KEY_ALLOWED_PACKAGES, HashSet(value)).apply()

    private fun defaultAlwaysAllowedPackages(): Set<String> {
        return setOf(
            "com.android.phone",
            "com.android.dialer",
            "com.google.android.dialer",
            "com.android.server.telecom",
            "com.android.incallui",
            "com.google.android.incallui",
            "com.first10.app",
            "com.first10.app.debug"
        )
    }

    // Stealth Mode
    var isLauncherHidden: Boolean
        get() = prefs.getBoolean(KEY_STEALTH_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_STEALTH_MODE, value).apply()

    // Message rotation
    var isRandomMessageRotation: Boolean
        get() = prefs.getBoolean(KEY_RANDOM_MESSAGES, true)
        set(value) = prefs.edit().putBoolean(KEY_RANDOM_MESSAGES, value).apply()

    // PIN & Security
    var pinHash: String?
        get() = prefs.getString(KEY_PIN_HASH, null)
        set(value) = prefs.edit().putString(KEY_PIN_HASH, value).apply()

    var pinSalt: String?
        get() = prefs.getString(KEY_PIN_SALT, null)
        set(value) = prefs.edit().putString(KEY_PIN_SALT, value).apply()

    var recoveryQuestion: String
        get() = prefs.getString(KEY_RECOVERY_QUESTION, "What is your primary morning focus goal?") ?: "What is your primary morning focus goal?"
        set(value) = prefs.edit().putString(KEY_RECOVERY_QUESTION, value).apply()

    var recoveryAnswerHash: String?
        get() = prefs.getString(KEY_RECOVERY_ANSWER_HASH, null)
        set(value) = prefs.edit().putString(KEY_RECOVERY_ANSWER_HASH, value).apply()

    var failedPinAttempts: Int
        get() = prefs.getInt(KEY_FAILED_PIN_ATTEMPTS, 0)
        set(value) = prefs.edit().putInt(KEY_FAILED_PIN_ATTEMPTS, value).apply()

    var lockoutUntil: Long
        get() = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        set(value) = prefs.edit().putLong(KEY_LOCKOUT_UNTIL, value).apply()

    val hasPinConfigured: Boolean
        get() = !pinHash.isNullOrEmpty()

    // Routines & Tasks Serialization
    fun getRoutines(): List<Routine> {
        val jsonStr = prefs.getString(KEY_ROUTINES_JSON, null)
        if (jsonStr.isNullOrEmpty()) {
            val defaultList = listOf(createDefaultRoutine())
            saveRoutines(defaultList)
            return defaultList
        }
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<Routine>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id")
                val name = obj.optString("name")
                val isDefault = obj.optBoolean("isDefault", false)
                val tasksArray = obj.optJSONArray("tasks") ?: JSONArray()
                val tasks = mutableListOf<TaskItem>()
                for (j in 0 until tasksArray.length()) {
                    val tObj = tasksArray.getJSONObject(j)
                    tasks.add(
                        TaskItem(
                            id = tObj.optString("id"),
                            title = tObj.optString("title"),
                            description = tObj.optString("description"),
                            isCompleted = tObj.optBoolean("isCompleted", false),
                            isEnabled = tObj.optBoolean("isEnabled", true),
                            orderIndex = tObj.optInt("orderIndex", j)
                        )
                    )
                }
                list.add(Routine(id = id, name = name, isDefault = isDefault, tasks = tasks))
            }
            if (list.isEmpty()) listOf(createDefaultRoutine()) else list
        } catch (e: Exception) {
            listOf(createDefaultRoutine())
        }
    }

    fun saveRoutines(routines: List<Routine>) {
        val jsonArray = JSONArray()
        for (r in routines) {
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("name", r.name)
            obj.put("isDefault", r.isDefault)
            val tArr = JSONArray()
            for (t in r.tasks) {
                val tObj = JSONObject()
                tObj.put("id", t.id)
                tObj.put("title", t.title)
                tObj.put("description", t.description)
                tObj.put("isCompleted", t.isCompleted)
                tObj.put("isEnabled", t.isEnabled)
                tObj.put("orderIndex", t.orderIndex)
                tArr.put(tObj)
            }
            obj.put("tasks", tArr)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_ROUTINES_JSON, jsonArray.toString()).apply()
    }

    fun getDefaultRoutine(): Routine {
        val routines = getRoutines()
        return routines.firstOrNull { it.isDefault } ?: routines.firstOrNull() ?: createDefaultRoutine()
    }

    fun updateTaskCompletion(taskId: String, isCompleted: Boolean) {
        val routines = getRoutines()
        for (r in routines) {
            for (t in r.tasks) {
                if (t.id == taskId) {
                    t.isCompleted = isCompleted
                }
            }
        }
        saveRoutines(routines)
    }

    fun resetDailyTasksIfNewDay() {
        val today = getTodayDateString()
        val lastDate = lastActivationDate
        if (today != lastDate && !isRestrictionActive) {
            val routines = getRoutines()
            for (r in routines) {
                r.resetDailyCompletion()
            }
            saveRoutines(routines)
        }
    }

    private fun createDefaultRoutine(): Routine {
        val defaultTasks = mutableListOf(
            TaskItem(title = "Brush your teeth", orderIndex = 0),
            TaskItem(title = "Freshen up", orderIndex = 1),
            TaskItem(title = "Take a bath", orderIndex = 2),
            TaskItem(title = "Drink a glass of water", orderIndex = 3),
            TaskItem(title = "Get dressed", orderIndex = 4),
            TaskItem(title = "Have breakfast", orderIndex = 5),
            TaskItem(title = "Start studying / focused work", orderIndex = 6)
        )
        return Routine(
            name = "Morning Focus Routine",
            isDefault = true,
            tasks = defaultTasks
        )
    }

    // Exclusion Schedules Serialization
    fun getExclusionPeriods(): List<ExclusionPeriod> {
        val jsonStr = prefs.getString(KEY_EXCLUSIONS_JSON, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<ExclusionPeriod>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val daysArr = obj.optJSONArray("daysOfWeek") ?: JSONArray()
                val days = mutableSetOf<Int>()
                for (j in 0 until daysArr.length()) {
                    days.add(daysArr.getInt(j))
                }
                list.add(
                    ExclusionPeriod(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        startHour = obj.optInt("startHour"),
                        startMinute = obj.optInt("startMinute"),
                        endHour = obj.optInt("endHour"),
                        endMinute = obj.optInt("endMinute"),
                        daysOfWeek = if (days.isEmpty()) setOf(1, 2, 3, 4, 5, 6, 7) else days,
                        isEnabled = obj.optBoolean("isEnabled", true)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveExclusionPeriods(periods: List<ExclusionPeriod>) {
        val jsonArray = JSONArray()
        for (p in periods) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("startHour", p.startHour)
            obj.put("startMinute", p.startMinute)
            obj.put("endHour", p.endHour)
            obj.put("endMinute", p.endMinute)
            obj.put("isEnabled", p.isEnabled)
            val daysArr = JSONArray()
            p.daysOfWeek.forEach { daysArr.put(it) }
            obj.put("daysOfWeek", daysArr)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_EXCLUSIONS_JSON, jsonArray.toString()).apply()
    }

    // Motivational Messages Serialization
    fun getMotivationalMessages(): List<MotivationalMessage> {
        val jsonStr = prefs.getString(KEY_MESSAGES_JSON, null)
        if (jsonStr.isNullOrEmpty()) {
            val defaultList = defaultMotivationalMessages()
            saveMotivationalMessages(defaultList)
            return defaultList
        }
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<MotivationalMessage>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    MotivationalMessage(
                        id = obj.optString("id"),
                        text = obj.optString("text"),
                        isEnabled = obj.optBoolean("isEnabled", true)
                    )
                )
            }
            if (list.isEmpty()) defaultMotivationalMessages() else list
        } catch (e: Exception) {
            defaultMotivationalMessages()
        }
    }

    fun saveMotivationalMessages(messages: List<MotivationalMessage>) {
        val jsonArray = JSONArray()
        for (m in messages) {
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("text", m.text)
            obj.put("isEnabled", m.isEnabled)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_MESSAGES_JSON, jsonArray.toString()).apply()
    }

    fun getActiveMotivationalMessage(): String {
        val messages = getMotivationalMessages().filter { it.isEnabled }
        if (messages.isEmpty()) {
            return "WAIT A MOMENT.\nWhat are you looking for?\nYou picked up your phone automatically.\nYour morning belongs to you."
        }
        return if (isRandomMessageRotation) {
            messages.random().text
        } else {
            messages.first().text
        }
    }

    private fun defaultMotivationalMessages(): List<MotivationalMessage> {
        return listOf(
            MotivationalMessage(
                text = "WAIT A MOMENT.\nWhat are you looking for?\nYou picked up your phone automatically.\nYour morning belongs to you."
            ),
            MotivationalMessage(text = "You don't need to check everything right now."),
            MotivationalMessage(text = "Give your mind a head start."),
            MotivationalMessage(text = "Your goals deserve the first 10 minutes."),
            MotivationalMessage(text = "Don't trade your morning for scrolling."),
            MotivationalMessage(text = "Take a moment. You have something better to do.")
        )
    }

    // Tamper Log Serialization
    fun logTamperEvent(eventType: String, details: String) {
        val currentLogs = getTamperLogs().toMutableList()
        currentLogs.add(0, TamperEvent(eventType = eventType, details = details))
        // Cap at 100 most recent logs
        val trimmed = if (currentLogs.size > 100) currentLogs.subList(0, 100) else currentLogs

        val jsonArray = JSONArray()
        for (event in trimmed) {
            val obj = JSONObject()
            obj.put("id", event.id)
            obj.put("timestamp", event.timestamp)
            obj.put("eventType", event.eventType)
            obj.put("details", event.details)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_TAMPER_LOG_JSON, jsonArray.toString()).apply()
    }

    fun getTamperLogs(): List<TamperEvent> {
        val jsonStr = prefs.getString(KEY_TAMPER_LOG_JSON, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<TamperEvent>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    TamperEvent(
                        id = obj.optString("id"),
                        timestamp = obj.optLong("timestamp"),
                        eventType = obj.optString("eventType"),
                        details = obj.optString("details")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}
