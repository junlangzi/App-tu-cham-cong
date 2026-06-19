package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_config")
data class UserConfig(
    @PrimaryKey val id: Int = 1,
    val fullName: String = "Nhân viên",
    val avatarName: String = "AVATAR_1",
    val dailySalary: Double = 300000.0,
    val dailyMealAllowance: Double = 20000.0,
    val isDarkMode: Boolean = false,
    val avatarUri: String? = null,
    val avatarScale: Float = 1.0f,
    val avatarOffsetX: Float = 0.0f,
    val avatarOffsetY: Float = 0.0f,
    val occupation: String = "Công nhân",
    val selectedColorHex: String = "#1E88E5",
    val selectedFontName: String = "Sử dụng mặc định",
    val appThemeMode: String = "SYSTEM",
    val monthActualSalaries: String = "{}"
)

@Entity(tableName = "jobs")
data class Job(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val rate: Double = 1.0, // Tỉ lệ công (e.g., 1.0, 1.1, 1.2, 0.5)
    val supportAmount: Double = 0.0, // Tiền hỗ trợ khâu (VND)
    val isDefault: Boolean = false
)

@Entity(tableName = "monthly_supports")
data class MonthlySupport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val amount: Double
)

@Entity(tableName = "work_logs")
data class WorkLog(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val jobId1: Int? = null,
    val ratio1: Double = 1.0, // Tỷ lệ ngày công cho việc 1 (e.g., 0.5, 1.0)
    val jobId2: Int? = null,
    val ratio2: Double = 0.0, // Tỷ lệ ngày công cho việc 2 (e.g., 0.5)
    val mealAllowance: Double = 20000.0, // Tiền ăn của ngày đó
    val otherAmount: Double = 0.0, // Tiền khác tùy chỉnh
    val otherNote: String = "", // Ghi chú tiền khác
    val note: String = "" // Ghi chú chung
)
