package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class AttendanceRepository(private val database: AppDatabase) {
    private val userConfigDao = database.userConfigDao()
    private val jobDao = database.jobDao()
    private val workLogDao = database.workLogDao()

    val userConfig: Flow<UserConfig?> = userConfigDao.getUserConfigFlow()
    val allJobs: Flow<List<Job>> = jobDao.getAllJobsFlow()
    val allWorkLogs: Flow<List<WorkLog>> = workLogDao.getAllWorkLogsFlow()
    val allMonthlySupports: Flow<List<MonthlySupport>> = database.monthlySupportDao().getAllMonthlySupportsFlow()

    fun getWorkLogsInMonth(month: String): Flow<List<WorkLog>> {
        // month comes in format "YYYY-MM"
        return workLogDao.getWorkLogsInMonthFlow("$month%")
    }

    suspend fun initializeDefaultsIfEmpty() {
        // Seed default config
        val currentConfig = userConfigDao.getUserConfigDirect()
        if (currentConfig == null) {
            userConfigDao.insertUserConfig(UserConfig())
        }

        // Seed default monthly supports
        val currentSupports = database.monthlySupportDao().getAllMonthlySupportsFlow()
        // (will initialize some defaults in viewModel scale if needed, but keeping it simple)

        // Seed some standard jobs
        val currentJobs = jobDao.getAllJobsDirect()
        if (currentJobs.isEmpty()) {
            jobDao.insertJob(Job(name = "Công việc Hành chính", rate = 1.0, supportAmount = 0.0, isDefault = true))
            jobDao.insertJob(Job(name = "Sản xuất tổ thường", rate = 1.1, supportAmount = 25000.0))
            jobDao.insertJob(Job(name = "Sản xuất độc hại", rate = 1.2, supportAmount = 50000.0))
            jobDao.insertJob(Job(name = "Tăng ca ngoài giờ", rate = 1.5, supportAmount = 10000.0))
            jobDao.insertJob(Job(name = "Hỗ trợ lắp ráp", rate = 0.5, supportAmount = 15000.0))
        }
    }

    suspend fun saveUserConfig(
        fullName: String,
        avatarName: String,
        dailySalary: Double,
        dailyMealAllowance: Double,
        isDarkMode: Boolean,
        avatarUri: String? = null,
        avatarScale: Float = 1.0f,
        avatarOffsetX: Float = 0.0f,
        avatarOffsetY: Float = 0.0f
    ) {
        userConfigDao.insertUserConfig(
            UserConfig(
                fullName = fullName,
                avatarName = avatarName,
                dailySalary = dailySalary,
                dailyMealAllowance = dailyMealAllowance,
                isDarkMode = isDarkMode,
                avatarUri = avatarUri,
                avatarScale = avatarScale,
                avatarOffsetX = avatarOffsetX,
                avatarOffsetY = avatarOffsetY
            )
        )
    }

    suspend fun toggleDarkMode(enabled: Boolean) {
        val current = userConfigDao.getUserConfigDirect() ?: UserConfig()
        userConfigDao.insertUserConfig(current.copy(isDarkMode = enabled))
    }

    suspend fun addJob(job: Job): Long = jobDao.insertJob(job)
    suspend fun updateJob(job: Job) = jobDao.updateJob(job)
    suspend fun deleteJob(jobId: Int) = jobDao.deleteJobById(jobId)

    suspend fun addMonthlySupport(support: MonthlySupport): Long = database.monthlySupportDao().insertMonthlySupport(support)
    suspend fun updateMonthlySupport(support: MonthlySupport) = database.monthlySupportDao().updateMonthlySupport(support)
    suspend fun deleteMonthlySupport(id: Int) = database.monthlySupportDao().deleteMonthlySupportById(id)

    suspend fun getWorkLogByDate(date: String): WorkLog? = workLogDao.getWorkLogByDate(date)
    fun getWorkLogByDateFlow(date: String): Flow<WorkLog?> = workLogDao.getWorkLogByDateFlow(date)
    suspend fun saveWorkLog(log: WorkLog) = workLogDao.insertWorkLog(log)
    suspend fun deleteWorkLog(date: String) = workLogDao.deleteWorkLogByDate(date)
}
