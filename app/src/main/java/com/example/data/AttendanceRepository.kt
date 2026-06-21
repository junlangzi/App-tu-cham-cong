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
        avatarOffsetY: Float = 0.0f,
        occupation: String = "Công nhân",
        selectedColorHex: String = "#1E88E5",
        selectedFontName: String = "Sử dụng mặc định",
        appThemeMode: String = "SYSTEM",
        monthActualSalaries: String = "{}",
        selectedLunarColorHex: String = "#FF9800"
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
                avatarOffsetY = avatarOffsetY,
                occupation = occupation,
                selectedColorHex = selectedColorHex,
                selectedFontName = selectedFontName,
                appThemeMode = appThemeMode,
                monthActualSalaries = monthActualSalaries,
                selectedLunarColorHex = selectedLunarColorHex
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
    suspend fun getAllJobsDirect(): List<Job> = jobDao.getAllJobsDirect()
    suspend fun getAllWorkLogsDirect(): List<WorkLog> = workLogDao.getAllWorkLogsDirect()

    suspend fun addMonthlySupport(support: MonthlySupport): Long = database.monthlySupportDao().insertMonthlySupport(support)
    suspend fun updateMonthlySupport(support: MonthlySupport) = database.monthlySupportDao().updateMonthlySupport(support)
    suspend fun deleteMonthlySupport(id: Int) = database.monthlySupportDao().deleteMonthlySupportById(id)

    suspend fun getWorkLogByDate(date: String): WorkLog? = workLogDao.getWorkLogByDate(date)
    fun getWorkLogByDateFlow(date: String): Flow<WorkLog?> = workLogDao.getWorkLogByDateFlow(date)
    suspend fun saveWorkLog(log: WorkLog) = workLogDao.insertWorkLog(log)
    suspend fun deleteWorkLog(date: String) = workLogDao.deleteWorkLogByDate(date)

    // Symmetric encryption with key "vts"
    private fun encryptBackup(plainText: String): String {
        try {
            val key = "vts"
            val keyBytes = key.toByteArray(Charsets.UTF_8)
            val dataBytes = plainText.toByteArray(Charsets.UTF_8)
            val result = ByteArray(dataBytes.size)
            for (i in dataBytes.indices) {
                result[i] = (dataBytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
            }
            return android.util.Base64.encodeToString(result, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            return plainText
        }
    }

    private fun decryptBackup(cipherText: String): String {
        try {
            val decoded = android.util.Base64.decode(cipherText, android.util.Base64.NO_WRAP)
            val key = "vts"
            val keyBytes = key.toByteArray(Charsets.UTF_8)
            val result = ByteArray(decoded.size)
            for (i in decoded.indices) {
                result[i] = (decoded[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
            }
            return String(result, Charsets.UTF_8)
        } catch (e: Exception) {
            // failed to decrypt, return original text (allows backward compatibility with standard JSON plaintext)
            return cipherText
        }
    }

    suspend fun exportDataJson(
        exportConfig: Boolean,
        exportJobs: Boolean,
        exportSupports: Boolean,
        exportLogs: Boolean
    ): String {
        val root = org.json.JSONObject()

        if (exportConfig) {
            val config = userConfigDao.getUserConfigDirect() ?: UserConfig()
            val cObj = org.json.JSONObject().apply {
                put("fullName", config.fullName)
                put("avatarName", config.avatarName)
                put("dailySalary", config.dailySalary)
                put("dailyMealAllowance", config.dailyMealAllowance)
                put("isDarkMode", config.isDarkMode)
                put("avatarUri", config.avatarUri ?: org.json.JSONObject.NULL)
                put("avatarScale", config.avatarScale.toDouble())
                put("avatarOffsetX", config.avatarOffsetX.toDouble())
                put("avatarOffsetY", config.avatarOffsetY.toDouble())
                put("occupation", config.occupation)
                put("selectedColorHex", config.selectedColorHex)
                put("selectedFontName", config.selectedFontName)
                put("appThemeMode", config.appThemeMode)
                put("monthActualSalaries", config.monthActualSalaries)
                put("selectedLunarColorHex", config.selectedLunarColorHex)
            }
            root.put("user_config", cObj)
        }

        if (exportJobs) {
            val jobs = jobDao.getAllJobsDirect()
            val jArr = org.json.JSONArray()
            for (job in jobs) {
                val jObj = org.json.JSONObject().apply {
                    put("id", job.id)
                    put("name", job.name)
                    put("rate", job.rate)
                    put("supportAmount", job.supportAmount)
                    put("isDefault", job.isDefault)
                }
                jArr.put(jObj)
            }
            root.put("jobs", jArr)
        }

        if (exportSupports) {
            val supports = database.monthlySupportDao().getAllMonthlySupportsDirect()
            val sArr = org.json.JSONArray()
            for (sup in supports) {
                val sObj = org.json.JSONObject().apply {
                    put("id", sup.id)
                    put("name", sup.name)
                    put("amount", sup.amount)
                }
                sArr.put(sObj)
            }
            root.put("monthly_supports", sArr)
        }

        if (exportLogs) {
            val logs = workLogDao.getAllWorkLogsDirect()
            val lArr = org.json.JSONArray()
            for (log in logs) {
                val lObj = org.json.JSONObject().apply {
                    put("date", log.date)
                    put("jobId1", log.jobId1 ?: org.json.JSONObject.NULL)
                    put("ratio1", log.ratio1)
                    put("jobId2", log.jobId2 ?: org.json.JSONObject.NULL)
                    put("ratio2", log.ratio2)
                    put("mealAllowance", log.mealAllowance)
                    put("otherAmount", log.otherAmount)
                    put("otherNote", log.otherNote)
                    put("note", log.note)
                }
                lArr.put(lObj)
            }
            root.put("work_logs", lArr)
        }

        return encryptBackup(root.toString())
    }

    suspend fun importDataJson(
        jsonString: String,
        importConfig: Boolean,
        importJobs: Boolean,
        importSupports: Boolean,
        importLogs: Boolean
    ): Boolean {
        try {
            val decryptedString = decryptBackup(jsonString)
            val root = org.json.JSONObject(decryptedString)

            if (importConfig && root.has("user_config")) {
                val cObj = root.getJSONObject("user_config")
                val config = UserConfig(
                    id = 1,
                    fullName = cObj.optString("fullName", "Nhân viên"),
                    avatarName = cObj.optString("avatarName", "AVATAR_1"),
                    dailySalary = cObj.optDouble("dailySalary", 300000.0),
                    dailyMealAllowance = cObj.optDouble("dailyMealAllowance", 20000.0),
                    isDarkMode = cObj.optBoolean("isDarkMode", false),
                    avatarUri = if (cObj.isNull("avatarUri")) null else cObj.optString("avatarUri"),
                    avatarScale = cObj.optDouble("avatarScale", 1.0).toFloat(),
                    avatarOffsetX = cObj.optDouble("avatarOffsetX", 0.0).toFloat(),
                    avatarOffsetY = cObj.optDouble("avatarOffsetY", 0.0).toFloat(),
                    occupation = cObj.optString("occupation", "Công nhân"),
                    selectedColorHex = cObj.optString("selectedColorHex", "#1E88E5"),
                    selectedFontName = cObj.optString("selectedFontName", "Sử dụng mặc định"),
                    appThemeMode = cObj.optString("appThemeMode", "SYSTEM"),
                    monthActualSalaries = cObj.optString("monthActualSalaries", "{}"),
                    selectedLunarColorHex = cObj.optString("selectedLunarColorHex", "#FF9800")
                )
                userConfigDao.insertUserConfig(config)
            }

            if (importJobs && root.has("jobs")) {
                val jArr = root.getJSONArray("jobs")
                jobDao.clearAllJobs()
                for (i in 0 until jArr.length()) {
                    val jObj = jArr.getJSONObject(i)
                    val job = Job(
                        id = jObj.optInt("id", 0),
                        name = jObj.optString("name", ""),
                        rate = jObj.optDouble("rate", 1.0),
                        supportAmount = jObj.optDouble("supportAmount", 0.0),
                        isDefault = jObj.optBoolean("isDefault", false)
                    )
                    jobDao.insertJob(job)
                }
            }

            if (importSupports && root.has("monthly_supports")) {
                val sArr = root.getJSONArray("monthly_supports")
                database.monthlySupportDao().clearAllMonthlySupports()
                for (i in 0 until sArr.length()) {
                    val sObj = sArr.getJSONObject(i)
                    val support = MonthlySupport(
                        id = sObj.optInt("id", 0),
                        name = sObj.optString("name", ""),
                        amount = sObj.optDouble("amount", 0.0)
                    )
                    database.monthlySupportDao().insertMonthlySupport(support)
                }
            }

            if (importLogs && root.has("work_logs")) {
                val lArr = root.getJSONArray("work_logs")
                workLogDao.clearAllWorkLogs()
                for (i in 0 until lArr.length()) {
                    val lObj = lArr.getJSONObject(i)
                    val log = WorkLog(
                        date = lObj.optString("date", ""),
                        jobId1 = if (lObj.isNull("jobId1")) null else lObj.optInt("jobId1"),
                        ratio1 = lObj.optDouble("ratio1", 1.0),
                        jobId2 = if (lObj.isNull("jobId2")) null else lObj.optInt("jobId2"),
                        ratio2 = lObj.optDouble("ratio2", 0.0),
                        mealAllowance = lObj.optDouble("mealAllowance", 20000.0),
                        otherAmount = lObj.optDouble("otherAmount", 0.0),
                        otherNote = lObj.optString("otherNote", ""),
                        note = lObj.optString("note", "")
                    )
                    workLogDao.insertWorkLog(log)
                }
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
