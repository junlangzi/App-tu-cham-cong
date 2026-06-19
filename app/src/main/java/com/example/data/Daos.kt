package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserConfigDao {
    @Query("SELECT * FROM user_config WHERE id = 1")
    fun getUserConfigFlow(): Flow<UserConfig?>

    @Query("SELECT * FROM user_config WHERE id = 1")
    suspend fun getUserConfigDirect(): UserConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserConfig(config: UserConfig)
}

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs ORDER BY id ASC")
    fun getAllJobsFlow(): Flow<List<Job>>

    @Query("SELECT * FROM jobs ORDER BY id ASC")
    suspend fun getAllJobsDirect(): List<Job>

    @Query("SELECT * FROM jobs WHERE id = :id")
    suspend fun getJobById(id: Int): Job?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: Job): Long

    @Update
    suspend fun updateJob(job: Job)

    @Query("DELETE FROM jobs WHERE id = :id")
    suspend fun deleteJobById(id: Int)

    @Query("DELETE FROM jobs")
    suspend fun clearAllJobs()
}

@Dao
interface WorkLogDao {
    @Query("SELECT * FROM work_logs ORDER BY date DESC")
    fun getAllWorkLogsFlow(): Flow<List<WorkLog>>

    @Query("SELECT * FROM work_logs ORDER BY date DESC")
    suspend fun getAllWorkLogsDirect(): List<WorkLog>

    @Query("SELECT * FROM work_logs WHERE date LIKE :monthPattern ORDER BY date ASC")
    fun getWorkLogsInMonthFlow(monthPattern: String): Flow<List<WorkLog>>

    @Query("SELECT * FROM work_logs WHERE date = :date")
    suspend fun getWorkLogByDate(date: String): WorkLog?

    @Query("SELECT * FROM work_logs WHERE date = :date")
    fun getWorkLogByDateFlow(date: String): Flow<WorkLog?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkLog(log: WorkLog)

    @Query("DELETE FROM work_logs WHERE date = :date")
    suspend fun deleteWorkLogByDate(date: String)

    @Query("DELETE FROM work_logs")
    suspend fun clearAllWorkLogs()
}

@Dao
interface MonthlySupportDao {
    @Query("SELECT * FROM monthly_supports ORDER BY id ASC")
    fun getAllMonthlySupportsFlow(): Flow<List<MonthlySupport>>

    @Query("SELECT * FROM monthly_supports ORDER BY id ASC")
    suspend fun getAllMonthlySupportsDirect(): List<MonthlySupport>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthlySupport(support: MonthlySupport): Long

    @Update
    suspend fun updateMonthlySupport(support: MonthlySupport)

    @Query("DELETE FROM monthly_supports WHERE id = :id")
    suspend fun deleteMonthlySupportById(id: Int)

    @Query("DELETE FROM monthly_supports")
    suspend fun clearAllMonthlySupports()
}
