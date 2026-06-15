package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AttendanceRepository
import com.example.data.Job
import com.example.data.UserConfig
import com.example.data.WorkLog
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AttendanceRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AttendanceRepository(database)
        viewModelScope.launch {
            repository.initializeDefaultsIfEmpty()
        }
    }

    // Selected Month context, e.g., "2026-06"
    private val _selectedMonth = MutableStateFlow(getCurrentMonthString())
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    // Config, Job, and Log Flows
    val userConfig: StateFlow<UserConfig> = repository.userConfig
        .filterNotNull()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserConfig()
        )

    val allJobs: StateFlow<List<Job>> = repository.allJobs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allMonthlySupports: StateFlow<List<com.example.data.MonthlySupport>> = repository.allMonthlySupports
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Logged entries for the chosen month
    val monthlyLogs: StateFlow<List<WorkLog>> = _selectedMonth
        .flatMapLatest { month ->
            repository.getWorkLogsInMonth(month)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Currently focused date for detail/edit, defaults to today "YYYY-MM-DD"
    private val _selectedDate = MutableStateFlow(getCurrentDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Active log for the selected date
    val activeLogForSelectedDate: StateFlow<WorkLog?> = _selectedDate
        .flatMapLatest { date ->
            repository.getWorkLogByDateFlow(date)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Navigate months
    fun changeMonth(offset: Int) {
        val current = _selectedMonth.value
        val parts = current.split("-")
        if (parts.size == 2) {
            val year = parts[0].toIntOrNull() ?: 2026
            val month = parts[1].toIntOrNull() ?: 6 // 1-indexed

            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1 + offset) // Calendar is 0-indexed
            }

            val nextYear = calendar.get(Calendar.YEAR)
            val nextMonth = calendar.get(Calendar.MONTH) + 1
            _selectedMonth.value = String.format(Locale.US, "%d-%02d", nextYear, nextMonth)
        }
    }

    fun selectDate(date: String) {
        _selectedDate.value = date
        // Extract month from date to auto-scroll if needed
        val parts = date.split("-")
        if (parts.size == 3) {
            _selectedMonth.value = "${parts[0]}-${parts[1]}"
        }
    }

    // User Profile CRUD
    fun updateProfile(
        fullName: String,
        avatarName: String,
        dailySalary: Double,
        dailyMealAllowance: Double,
        avatarUri: String? = null,
        avatarScale: Float = 1.0f,
        avatarOffsetX: Float = 0.0f,
        avatarOffsetY: Float = 0.0f
    ) {
        viewModelScope.launch {
            val current = userConfig.value
            repository.saveUserConfig(
                fullName = fullName,
                avatarName = avatarName,
                dailySalary = dailySalary,
                dailyMealAllowance = dailyMealAllowance,
                isDarkMode = current.isDarkMode,
                avatarUri = avatarUri,
                avatarScale = avatarScale,
                avatarOffsetX = avatarOffsetX,
                avatarOffsetY = avatarOffsetY
            )
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleDarkMode(enabled)
        }
    }

    // Monthly support/allowance CRUD
    fun addMonthlySupport(name: String, amount: Double) {
        viewModelScope.launch {
            repository.addMonthlySupport(com.example.data.MonthlySupport(name = name, amount = amount))
        }
    }

    fun updateMonthlySupport(support: com.example.data.MonthlySupport) {
        viewModelScope.launch {
            repository.updateMonthlySupport(support)
        }
    }

    fun deleteMonthlySupport(id: Int) {
        viewModelScope.launch {
            repository.deleteMonthlySupport(id)
        }
    }

    // Job CRUD
    fun addJob(name: String, rate: Double, supportAmount: Double) {
        viewModelScope.launch {
            repository.addJob(Job(name = name, rate = rate, supportAmount = supportAmount))
        }
    }

    fun updateJob(job: Job) {
        viewModelScope.launch {
            repository.updateJob(job)
        }
    }

    fun deleteJob(jobId: Int) {
        viewModelScope.launch {
            repository.deleteJob(jobId)
        }
    }

    // WorkLog CRUD
    fun saveWorkLog(
        date: String,
        jobId1: Int?,
        ratio1: Double,
        jobId2: Int? = null,
        ratio2: Double = 0.0,
        mealAllowance: Double = 20000.0,
        otherAmount: Double = 0.0,
        otherNote: String = "",
        note: String = ""
    ) {
        viewModelScope.launch {
            val log = WorkLog(
                date = date,
                jobId1 = jobId1,
                ratio1 = ratio1,
                jobId2 = jobId2,
                ratio2 = ratio2,
                mealAllowance = mealAllowance,
                otherAmount = otherAmount,
                otherNote = otherNote,
                note = note
            )
            repository.saveWorkLog(log)
        }
    }

    fun deleteWorkLog(date: String) {
        viewModelScope.launch {
            repository.deleteWorkLog(date)
        }
    }

    // Helpers
    private fun getCurrentMonthString(): String {
        val cal = Calendar.getInstance()
        return String.format(Locale.US, "%d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    private fun getCurrentDateString(): String {
        val cal = Calendar.getInstance()
        return String.format(Locale.US, "%d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }

    // Factory Class
    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AttendanceViewModel(application) as T
                }
            }
    }
}
