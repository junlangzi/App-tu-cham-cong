package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import com.example.data.Job
import com.example.data.UserConfig
import com.example.data.WorkLog
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(viewModel: AttendanceViewModel) {
    val userConfig by viewModel.userConfig.collectAsStateWithLifecycle()
    val allJobs by viewModel.allJobs.collectAsStateWithLifecycle()
    val monthlyLogs by viewModel.monthlyLogs.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val activeLogForSelectedDate by viewModel.activeLogForSelectedDate.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf("home") }
    var showWorkLogDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == "home",
                    onClick = { currentTab = "home" },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Trang chủ") },
                    label = { Text("Trang chủ", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_home")
                )
                NavigationBarItem(
                    selected = currentTab == "stats",
                    onClick = { currentTab = "stats" },
                    icon = { Icon(Icons.Filled.Assessment, contentDescription = "Thống kê") },
                    label = { Text("Thống kê", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_stats")
                )
                NavigationBarItem(
                    selected = currentTab == "jobs",
                    onClick = { currentTab = "jobs" },
                    icon = { Icon(Icons.Filled.Work, contentDescription = "Công việc") },
                    label = { Text("Công việc", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_jobs")
                )
                NavigationBarItem(
                    selected = currentTab == "settings",
                    onClick = { currentTab = "settings" },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Cài đặt") },
                    label = { Text("Cài đặt", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentTab) {
                "home" -> HomeScreen(
                    viewModel = viewModel,
                    userConfig = userConfig,
                    allJobs = allJobs,
                    monthlyLogs = monthlyLogs,
                    selectedMonth = selectedMonth,
                    selectedDate = selectedDate,
                    activeLog = activeLogForSelectedDate,
                    onOpenLogDialog = { showWorkLogDialog = true }
                )
                "stats" -> StatsScreen(
                    viewModel = viewModel,
                    monthlyLogs = monthlyLogs,
                    allJobs = allJobs,
                    selectedMonth = selectedMonth,
                    userConfig = userConfig,
                    onDayClick = { date ->
                        viewModel.selectDate(date)
                        showWorkLogDialog = true
                    }
                )
                "jobs" -> JobsScreen(
                    viewModel = viewModel,
                    allJobs = allJobs
                )
                "settings" -> SettingsScreen(
                    viewModel = viewModel,
                    userConfig = userConfig
                )
            }
        }
    }

    if (showWorkLogDialog) {
        WorkLogEditorDialog(
            date = selectedDate,
            activeLog = activeLogForSelectedDate,
            allJobs = allJobs,
            defaultMealAllowance = userConfig.dailyMealAllowance,
            onDismiss = { showWorkLogDialog = false },
            onSave = { jobId1, ratio1, jobId2, ratio2, meal, otherAmount, otherNote, note ->
                viewModel.saveWorkLog(
                    date = selectedDate,
                    jobId1 = jobId1,
                    ratio1 = ratio1,
                    jobId2 = jobId2,
                    ratio2 = ratio2,
                    mealAllowance = meal,
                    otherAmount = otherAmount,
                    otherNote = otherNote,
                    note = note
                )
                showWorkLogDialog = false
            },
            onDelete = {
                viewModel.deleteWorkLog(selectedDate)
                showWorkLogDialog = false
            }
        )
    }
}

// ---------------------- HOME SCREEN ----------------------
@Composable
fun HomeScreen(
    viewModel: AttendanceViewModel,
    userConfig: UserConfig,
    allJobs: List<Job>,
    monthlyLogs: List<WorkLog>,
    selectedMonth: String,
    selectedDate: String,
    activeLog: WorkLog?,
    onOpenLogDialog: () -> Unit
) {
    val avatar = AvatarHelper.getAvatar(userConfig.avatarName)
    val allMonthlySupports by viewModel.allMonthlySupports.collectAsStateWithLifecycle()
    var showQuickLogDropdown by remember { mutableStateOf(false) }

    // Parse month for displaying formatted string
    val monthTitle = try {
        val parts = selectedMonth.split("-")
        "Tháng ${parts[1]}/${parts[0]}"
    } catch (e: Exception) {
        selectedMonth
    }

    // Calculations based on month's logs
    val totalWorkedDays = monthlyLogs.size

    var totalLaborDays = 0.0
    var totalSupportAmount = 0.0
    var totalMealAllowance = 0.0
    var totalOtherAmount = 0.0

    monthlyLogs.forEach { log ->
        val job1 = allJobs.find { it.id == log.jobId1 }
        val job2 = allJobs.find { it.id == log.jobId2 }

        // Labor days calculation: (Job1 rate * Job1 Day ratio) + (Job2 rate * Job2 Day ratio)
        val l1 = (job1?.rate ?: 0.0) * log.ratio1
        val l2 = (job2?.rate ?: 0.0) * log.ratio2
        totalLaborDays += (l1 + l2)

        // Support money calculation: (Job1 support * Job1 ratio) + (Job2 support * Job2 ratio)
        val s1 = (job1?.supportAmount ?: 0.0) * log.ratio1
        val s2 = (job2?.supportAmount ?: 0.0) * log.ratio2
        totalSupportAmount += (s1 + s2)

        totalMealAllowance += log.mealAllowance
        totalOtherAmount += log.otherAmount
    }

    val regularSalary = totalLaborDays * userConfig.dailySalary
    val totalMonthlySupportCustom = allMonthlySupports.sumOf { it.amount }
    val totalEstimatedEarnings = regularSalary + totalSupportAmount + totalMealAllowance + totalOtherAmount + totalMonthlySupportCustom

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_scroll"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming Profile Header
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var imageLoadFailed by remember(userConfig.avatarUri) { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(if (userConfig.avatarUri != null && !imageLoadFailed) MaterialTheme.colorScheme.primaryContainer else avatar.backgroundColor),
                        contentAlignment = Alignment.Center
                    ) {
                        if (userConfig.avatarUri != null && !imageLoadFailed) {
                            val modelToLoad: Any = if (userConfig.avatarUri!!.startsWith("content://") || userConfig.avatarUri!!.startsWith("http://") || userConfig.avatarUri!!.startsWith("https://")) {
                                userConfig.avatarUri!!
                            } else {
                                java.io.File(userConfig.avatarUri!!)
                            }
                            AsyncImage(
                                model = modelToLoad,
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = userConfig.avatarScale
                                        scaleY = userConfig.avatarScale
                                        translationX = userConfig.avatarOffsetX
                                        translationY = userConfig.avatarOffsetY
                                    },
                                onError = {
                                    imageLoadFailed = true
                                }
                            )
                        } else {
                            Text(avatar.emoji, fontSize = 32.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1.0f)) {
                        Text(
                            text = "Xin chào,",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = userConfig.fullName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleDarkMode(!userConfig.isDarkMode) }
                    ) {
                        Icon(
                            imageVector = if (userConfig.isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "Chuyển giao diện sáng tối",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Selected Month Summary Display Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            modifier = Modifier.testTag("month_prev_btn"),
                            onClick = { viewModel.changeMonth(-1) }
                        ) {
                            Icon(Icons.Filled.ChevronLeft, "Tháng trước", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text(
                            text = monthTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                        IconButton(
                            modifier = Modifier.testTag("month_next_btn"),
                            onClick = { viewModel.changeMonth(1) }
                        ) {
                            Icon(Icons.Filled.ChevronRight, "Tháng sau", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "TỔNG THU NHẬP TẠM TÍNH",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = FormatHelper.formatVnd(totalEstimatedEarnings),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        textAlign = TextAlign.Center
                    )

                    Divider(
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Stats row grids
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1.0f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Ngày làm", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            Text("$totalWorkedDays ngày", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Column(modifier = Modifier.weight(1.0f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Tổng công", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            Text(FormatHelper.formatRatio(totalLaborDays), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Column(modifier = Modifier.weight(1.0f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Lương cơ bản", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            Text(FormatHelper.formatVndNoSymbol(regularSalary), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1.0f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Hỗ trợ khâu", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            Text(FormatHelper.formatVndNoSymbol(totalSupportAmount), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Column(modifier = Modifier.weight(1.0f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Tổng tiền ăn", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            Text(FormatHelper.formatVndNoSymbol(totalMealAllowance), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Column(modifier = Modifier.weight(1.0f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Tiền khác", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            Text(FormatHelper.formatVndNoSymbol(totalOtherAmount), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }

                    if (totalMonthlySupportCustom > 0) {
                        Divider(
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tổng hỗ trợ/phụ cấp cố định tháng:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                            Text(FormatHelper.formatVnd(totalMonthlySupportCustom), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Tính với mức lương cố định: ${FormatHelper.formatVnd(userConfig.dailySalary)} / ngày công.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Calendar-Based Selection Widget
        item {
            Text(
                text = "Lịch Chấm Công",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        item {
            CalendarGridWidget(
                selectedMonth = selectedMonth,
                selectedDate = selectedDate,
                monthlyLogs = monthlyLogs,
                onDateSelected = { date ->
                    viewModel.selectDate(date)
                }
            )
        }

        // Logger quick control block for the focus date
        item {
            val dateLabel = try {
                val parts = selectedDate.split("-")
                "Ngày ${parts[2]}/${parts[1]}/${parts[0]}"
            } catch (e: Exception) {
                selectedDate
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (activeLog != null) {
                        // Display active info
                        val j1 = allJobs.find { it.id == activeLog.jobId1 }
                        val j2 = allJobs.find { it.id == activeLog.jobId2 }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Trạng thái:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                Text("Đã chấm công ✅", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            if (j1 != null) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(j1.name, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${FormatHelper.formatRatio(activeLog.ratio1)} công (x${j1.rate})", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (j2 != null && activeLog.ratio2 > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(j2.name, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${FormatHelper.formatRatio(activeLog.ratio2)} công (x${j2.rate})", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tiền ăn:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                Text(FormatHelper.formatVnd(activeLog.mealAllowance), fontSize = 13.sp)
                            }
                            if (activeLog.otherAmount != 0.0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Tiền khác: (${activeLog.otherNote})", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                    Text(FormatHelper.formatVnd(activeLog.otherAmount), fontSize = 13.sp)
                                }
                            }
                            if (activeLog.note.isNotEmpty()) {
                                Text(
                                    text = "Ghi chú: ${activeLog.note}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Hôm nay chưa thực hiện chấm công.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (activeLog == null) {
                            Button(
                                onClick = {
                                    showQuickLogDropdown = true
                                },
                                modifier = Modifier
                                    .weight(1.0f)
                                    .testTag("quick_log_btn")
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = "Chấm công nhanh")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Chấm nhanh 1 công", fontSize = 13.sp)
                            }
                        }

                        FilledTonalButton(
                            onClick = onOpenLogDialog,
                            modifier = Modifier
                                .weight(1.0f)
                                .testTag("detailed_edit_btn")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Chỉnh sửa chi tiết")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (activeLog == null) "Chấm chi tiết" else "Sửa đổi", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    if (showQuickLogDropdown) {
        AlertDialog(
            onDismissRequest = { showQuickLogDropdown = false },
            title = { Text("Chọn Nhanh Công Việc", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            text = {
                if (allJobs.isEmpty()) {
                    Text("Chưa có danh sách công việc nào. Hãy tạo thêm trong mục 'Công việc'.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(allJobs) { jb ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.saveWorkLog(
                                            date = selectedDate,
                                            jobId1 = jb.id,
                                            ratio1 = 1.0,
                                            mealAllowance = userConfig.dailyMealAllowance
                                        )
                                        showQuickLogDropdown = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(jb.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            "Tỷ lệ: ${FormatHelper.formatRatio(jb.rate)} • Phụ cấp khâu: ${FormatHelper.formatVnd(jb.supportAmount)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showQuickLogDropdown = false }) {
                    Text("Hủy bỏ")
                }
            }
        )
    }
}

// ---------------------- CALENDAR GRID COMPONENT ----------------------
@Composable
fun CalendarGridWidget(
    selectedMonth: String,
    selectedDate: String,
    monthlyLogs: List<WorkLog>,
    onDateSelected: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Parse selected year and month
    val parts = selectedMonth.split("-")
    val year = parts[0].toIntOrNull() ?: 2026
    val month = parts[1].toIntOrNull() ?: 6

    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday

    // Align start offsets so we place starting weekday correctly: (converting Sunday to last or offset)
    // 1 (Sun)-> 6, 2 (Mon)-> 0, 3 (Tue)-> 1, 4 (Wed)-> 2, 5 (Thu)-> 3, 6 (Fri)-> 4, 7 (Sat)-> 5
    val emptyPrecedingDays = when (firstDayOfWeek) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6
        else -> 0
    }

    val weekdays = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Weekdays Row
            Row(modifier = Modifier.fillMaxWidth()) {
                weekdays.forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier
                            .weight(1.0f)
                            .padding(vertical = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Days Grid
            val totalCells = emptyPrecedingDays + daysInMonth
            val rows = (totalCells + 6) / 7

            for (r in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (c in 0 until 7) {
                        val index = r * 7 + c
                        val dayNumber = index - emptyPrecedingDays + 1

                        Box(
                            modifier = Modifier
                                .weight(1.0f)
                                .aspectRatio(1.0f)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayNumber in 1..daysInMonth) {
                                val dateStr = String.format(Locale.US, "%d-%02d-%02d", year, month, dayNumber)
                                val isSelected = dateStr == selectedDate
                                val logForDay = monthlyLogs.find { it.date == dateStr }

                                var totalWorkRatio = 0.0
                                if (logForDay != null) {
                                    totalWorkRatio = logForDay.ratio1 + logForDay.ratio2
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                totalWorkRatio > 0 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable { onDateSelected(dateStr) }
                                        .padding(2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = dayNumber.toString(),
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected || totalWorkRatio > 0) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            totalWorkRatio > 0 -> MaterialTheme.colorScheme.onPrimaryContainer
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )

                                    if (totalWorkRatio > 0) {
                                        Text(
                                            text = "${FormatHelper.formatRatio(totalWorkRatio)}c",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- STATS SCREEN ----------------------
@Composable
fun StatsScreen(
    viewModel: AttendanceViewModel,
    monthlyLogs: List<WorkLog>,
    allJobs: List<Job>,
    selectedMonth: String,
    userConfig: UserConfig,
    onDayClick: (String) -> Unit
) {
    val monthTitle = try {
        val parts = selectedMonth.split("-")
        "Tháng ${parts[1]}/${parts[0]}"
    } catch (e: Exception) {
        selectedMonth
    }
    
    val allMonthlySupports by viewModel.allMonthlySupports.collectAsStateWithLifecycle()
    val totalMonthlySupportCustom = allMonthlySupports.sumOf { it.amount }

    var totalLaborDays = 0.0
    var totalSupportAmount = 0.0
    var totalMealAllowance = 0.0
    var totalOtherAmount = 0.0

    monthlyLogs.forEach { log ->
        val job1 = allJobs.find { it.id == log.jobId1 }
        val job2 = allJobs.find { it.id == log.jobId2 }
        totalLaborDays += ((job1?.rate ?: 0.0) * log.ratio1 + (job2?.rate ?: 0.0) * log.ratio2)
        totalSupportAmount += ((job1?.supportAmount ?: 0.0) * log.ratio1 + (job2?.supportAmount ?: 0.0) * log.ratio2)
        totalMealAllowance += log.mealAllowance
        totalOtherAmount += log.otherAmount
    }

    val regularSalary = totalLaborDays * userConfig.dailySalary
    val totalEstimatedEarnings = regularSalary + totalSupportAmount + totalMealAllowance + totalOtherAmount + totalMonthlySupportCustom

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("stats_screen")
    ) {
        // Upper stats banner
        Card(
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.changeMonth(-1) }) {
                        Icon(Icons.Filled.ChevronLeft, "Tháng trước", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Text(
                        text = "Thống Kê $monthTitle",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    IconButton(onClick = { viewModel.changeMonth(1) }) {
                        Icon(Icons.Filled.ChevronRight, "Tháng sau", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1.0f)) {
                        Text("Tổng ngày công", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                        Text(
                            text = "${FormatHelper.formatRatio(totalLaborDays)} công",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Column(modifier = Modifier.weight(1.0f)) {
                        Text("Lương tạm tính", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                        Text(
                            text = FormatHelper.formatVnd(totalEstimatedEarnings),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Nhật Ký Chấm Công Chi Tiết",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 4.dp)
        )

        if (monthlyLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = "Trống",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Không có dữ liệu công trong tháng này.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
                    .testTag("detailed_logs_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(monthlyLogs) { log ->
                    val j1 = allJobs.find { it.id == log.jobId1 }
                    val j2 = allJobs.find { it.id == log.jobId2 }

                    // calculate daily total earnings
                    val c1 = (j1?.rate ?: 0.0) * log.ratio1
                    val c2 = (j2?.rate ?: 0.0) * log.ratio2
                    val dayDailySalary = (c1 + c2) * userConfig.dailySalary

                    val daySupport = (j1?.supportAmount ?: 0.0) * log.ratio1 + (j2?.supportAmount ?: 0.0) * log.ratio2
                    val dayTotal = dayDailySalary + daySupport + log.mealAllowance + log.otherAmount

                    val displayDate = try {
                        val bits = log.date.split("-")
                        "${bits[2]}/${bits[1]}/${bits[0]}"
                    } catch (e: Exception) {
                        log.date
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDayClick(log.date) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.DateRange,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = displayDate,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "+${FormatHelper.formatVnd(dayTotal)}",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Jobs info
                            if (j1 != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("• ${j1.name}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${FormatHelper.formatRatio(log.ratio1)} công (Tỷ lệ x${j1.rate})", fontSize = 13.sp)
                                }
                            }

                            if (j2 != null && log.ratio2 > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("• ${j2.name}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${FormatHelper.formatRatio(log.ratio2)} công (Tỷ lệ x${j2.rate})", fontSize = 13.sp)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• Tiền ăn trưa/tối", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                Text(FormatHelper.formatVnd(log.mealAllowance), fontSize = 13.sp)
                            }

                            if (log.otherAmount != 0.0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("• Tiền bổ sung khác (${log.otherNote})", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                    Text(FormatHelper.formatVnd(log.otherAmount), fontSize = 13.sp)
                                }
                            }

                            if (log.note.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Ghi chú: ${log.note}",
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(6.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- JOBS MANAGEMENT SCREEN ----------------------
@Composable
fun JobsScreen(
    viewModel: AttendanceViewModel,
    allJobs: List<Job>
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var jobToEdit by remember { mutableStateOf<Job?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("jobs_screen")
    ) {
        // Screen Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Quản Lý Công Việc",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_job_header_btn")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Thêm công việc")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Thêm mới")
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("jobs_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(allJobs) { job ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1.0f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = job.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (job.isDefault) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text("Mặc định", fontSize = 10.sp) },
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    text = "Tỉ lệ: x${FormatHelper.formatRatio(job.rate)} công",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "Hỗ trợ: ${FormatHelper.formatVnd(job.supportAmount)}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { jobToEdit = job },
                                modifier = Modifier.testTag("edit_job_${job.id}")
                            ) {
                                Icon(Icons.Filled.Edit, "Chỉnh sửa", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = { viewModel.deleteJob(job.id) },
                                modifier = Modifier.testTag("delete_job_${job.id}"),
                                enabled = !job.isDefault // simple safety: can't delete basic admin job
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    "Xóa bỏ",
                                    tint = if (job.isDefault) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        JobEditorDialog(
            job = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, rate, support ->
                viewModel.addJob(name, rate, support)
                showAddDialog = false
            }
        )
    }

    if (jobToEdit != null) {
        JobEditorDialog(
            job = jobToEdit,
            onDismiss = { jobToEdit = null },
            onSave = { name, rate, support ->
                val updated = jobToEdit!!.copy(name = name, rate = rate, supportAmount = support)
                viewModel.updateJob(updated)
                jobToEdit = null
            }
        )
    }
}

// ---------------------- JOB EDITOR DIALOG ----------------------
@Composable
fun JobEditorDialog(
    job: Job?,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf(job?.name ?: "") }
    var rateStr by remember { mutableStateOf(job?.rate?.toString() ?: "1.0") }
    var supportStr by remember { mutableStateOf(job?.supportAmount?.toInt()?.toString() ?: "0") }

    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (job == null) "Thêm Công Việc Mới" else "Sửa Thông Tin Công Việc", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên công việc") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("job_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = rateStr,
                    onValueChange = { rateStr = it },
                    label = { Text("Tỉ lệ ngày công (Ví dụ: 1.0, 1.2, 0.5)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("job_rate_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = supportStr,
                    onValueChange = { supportStr = it },
                    label = { Text("Tiền hỗ trợ / phụ cấp khâu (VND)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("job_support_input"),
                    singleLine = true,
                    supportingText = {
                        val amount = supportStr.toDoubleOrNull() ?: 0.0
                        Text("Phụ cấp: ${FormatHelper.formatVnd(amount)}")
                    }
                )

                if (isError) {
                    Text(
                        "Vui lòng điền đúng thông tin số hợp lệ.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rate = rateStr.toDoubleOrNull()
                    val support = supportStr.toDoubleOrNull()
                    if (name.trim().isNotEmpty() && rate != null && support != null) {
                        onSave(name, rate, support)
                    } else {
                        isError = true
                    }
                },
                modifier = Modifier.testTag("save_job_confirm")
            ) {
                Text("Lưu lại")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy bỏ")
            }
        }
    )
}

// ---------------------- SETTINGS SCREEN ----------------------
@Composable
fun SettingsScreen(
    viewModel: AttendanceViewModel,
    userConfig: UserConfig
) {
    var name by remember { mutableStateOf(userConfig.fullName) }
    var dailySalaryStr by remember { mutableStateOf(userConfig.dailySalary.toInt().toString()) }
    var defaultMealStr by remember { mutableStateOf(userConfig.dailyMealAllowance.toInt().toString()) }
    var selectedAvatarName by remember { mutableStateOf(userConfig.avatarName) }

    var avatarUri by remember { mutableStateOf(userConfig.avatarUri) }
    var avatarScale by remember { mutableStateOf(userConfig.avatarScale) }
    var avatarOffsetX by remember { mutableStateOf(userConfig.avatarOffsetX) }
    var avatarOffsetY by remember { mutableStateOf(userConfig.avatarOffsetY) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val file = java.io.File(context.filesDir, "custom_avatar_${System.currentTimeMillis()}.jpg")
                        file.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                        avatarUri = file.absolutePath
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    avatarUri = uri.toString()
                }
            }
        }
    )

    val allMonthlySupports by viewModel.allMonthlySupports.collectAsStateWithLifecycle()
    
    var showAddSupportDialog by remember { mutableStateOf(false) }
    var newSupportName by remember { mutableStateOf("") }
    var newSupportAmountStr by remember { mutableStateOf("") }

    var saveSuccessShow by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text(
                "Cấu Hình Tài Khoản",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Name & salary text fields
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Họ và tên") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_name_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = dailySalaryStr,
                        onValueChange = { dailySalaryStr = it },
                        label = { Text("Mức lương hàng ngày (VND / ngày công)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_salary_input"),
                        singleLine = true,
                        supportingText = {
                            val sal = dailySalaryStr.toDoubleOrNull() ?: 0.0
                            Text("Hiện tại: ${FormatHelper.formatVnd(sal)}")
                        }
                    )

                    OutlinedTextField(
                        value = defaultMealStr,
                        onValueChange = { defaultMealStr = it },
                        label = { Text("Tiền ăn mặc định hàng ngày (VND)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_meal_input"),
                        singleLine = true,
                        supportingText = {
                            val m = defaultMealStr.toDoubleOrNull() ?: 0.0
                            Text("Hiện tại: ${FormatHelper.formatVnd(m)}")
                        }
                    )
                }
            }
        }

        // Avatar selector list
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Chọn Hình Đại Diện (Avatar)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(AvatarHelper.avatars) { avItem ->
                                val isSelected = avItem.name == selectedAvatarName && avatarUri == null
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1.0f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else Color.Transparent
                                        )
                                        .clickable { 
                                            selectedAvatarName = avItem.name 
                                            avatarUri = null
                                        }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(avItem.backgroundColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(avItem.emoji, fontSize = 24.sp)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            avItem.label,
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            textAlign = TextAlign.Center,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Divider(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        // Library Image Picker section
                        Text(
                            text = "Hoặc chọn từ Thư viện ảnh cá nhân:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            var previewLoadFailed by remember(avatarUri) { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(if (avatarUri != null && !previewLoadFailed) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (avatarUri != null && !previewLoadFailed) {
                                    val modelToLoad: Any = if (avatarUri!!.startsWith("content://") || avatarUri!!.startsWith("http://") || avatarUri!!.startsWith("https://")) {
                                        avatarUri!!
                                    } else {
                                        java.io.File(avatarUri!!)
                                    }
                                    AsyncImage(
                                        model = modelToLoad,
                                        contentDescription = "Avatar Preview",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                scaleX = avatarScale
                                                scaleY = avatarScale
                                                translationX = avatarOffsetX
                                                translationY = avatarOffsetY
                                            },
                                        onError = {
                                            previewLoadFailed = true
                                        }
                                    )
                                } else {
                                    val fallbackAvatar = AvatarHelper.getAvatar(selectedAvatarName)
                                    Text(fallbackAvatar.emoji, fontSize = 32.sp)
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Chọn từ Album", fontSize = 12.sp)
                                }

                                if (avatarUri != null) {
                                    OutlinedButton(
                                        onClick = {
                                            avatarUri = null
                                            avatarScale = 1.0f
                                            avatarOffsetX = 0f
                                            avatarOffsetY = 0f
                                        },
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Xóa ảnh", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        if (avatarUri != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                "Căn chỉnh vị trí & kích cỡ:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Scale controller slider (0.5 to 3.0)
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Kích cỡ (Thu phóng): ${String.format("%.1f", avatarScale)}x", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Đặt lại 1x", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { avatarScale = 1.0f })
                                }
                                Slider(
                                    value = avatarScale,
                                    onValueChange = { avatarScale = it },
                                    valueRange = 0.5f..3.0f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Offset X controller slider (-100 to 100)
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Căn chỉnh ngang (X): ${avatarOffsetX.toInt()} px", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Đặt lại", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { avatarOffsetX = 0f })
                                }
                                Slider(
                                    value = avatarOffsetX,
                                    onValueChange = { avatarOffsetX = it },
                                    valueRange = -100f..100f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Offset Y controller slider (-100 to 100)
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Căn chỉnh dọc (Y): ${avatarOffsetY.toInt()} px", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Đặt lại", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { avatarOffsetY = 0f })
                                }
                                Slider(
                                    value = avatarOffsetY,
                                    onValueChange = { avatarOffsetY = it },
                                    valueRange = -100f..100f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }

        // Monthly custom support/allowances list section
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Hỗ Trợ & Phụ Cấp Hàng Tháng",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Các khoản hỗ trợ nhận cố định hàng tháng (Xăng xe, nhà ở...)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(
                        onClick = { showAddSupportDialog = true }
                    ) {
                        Icon(Icons.Filled.AddCircle, contentDescription = "Thêm hỗ trợ cố định", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (allMonthlySupports.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAddSupportDialog = true }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddCircle,
                                contentDescription = "Thêm hỗ trợ cố định",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                "Chưa có mục hỗ trợ cố định nào được cấu hình.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Text(
                                "Nhấn vào đây để thêm khoản đầu tiên",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            allMonthlySupports.forEachIndexed { index, support ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = support.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = FormatHelper.formatVnd(support.amount),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteMonthlySupport(support.id) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Xóa phụ cấp",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                                if (index < allMonthlySupports.size - 1) {
                                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Toggle switches & save logic
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleDarkMode(!userConfig.isDarkMode) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (userConfig.isDarkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Giao Diện Tối (Dark Mode)", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                        Text("Chuyển đổi phối màu ứng dụng", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
                Switch(
                    checked = userConfig.isDarkMode,
                    onCheckedChange = { viewModel.toggleDarkMode(it) },
                    modifier = Modifier.testTag("dark_mode_switch")
                )
            }
        }

        item {
            Button(
                onClick = {
                    val salary = dailySalaryStr.toDoubleOrNull() ?: 300000.0
                    val meal = defaultMealStr.toDoubleOrNull() ?: 20000.0
                    viewModel.updateProfile(
                        fullName = name.trim().ifEmpty { "Nhân viên" },
                        avatarName = selectedAvatarName,
                        dailySalary = salary,
                        dailyMealAllowance = meal,
                        avatarUri = avatarUri,
                        avatarScale = avatarScale,
                        avatarOffsetX = avatarOffsetX,
                        avatarOffsetY = avatarOffsetY
                    )
                    saveSuccessShow = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_settings_btn")
            ) {
                Icon(Icons.Filled.Save, contentDescription = "Lưu cấu hình")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lưu Thay Đổi")
            }
        }

        // Info card / Giới thiệu thông tin
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Ứng Dụng Tự Chấm Công",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Hỗ trợ ghi nhận ngày công, tự động nhân tỉ lệ, phụ cấp khâu phát sinh và hỗ trợ ăn uống tiện lợi hàng ngày cho người lao động.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Phiên bản v1.0.0 • AI Studio", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            }
        }
    }

    if (saveSuccessShow) {
        AlertDialog(
            onDismissRequest = { saveSuccessShow = false },
            title = { Text("Thành Công", fontWeight = FontWeight.Bold) },
            text = { Text("Thông tin cá nhân, ảnh đại diện và thiết lập lương đã được lưu trữ thành công.") },
            confirmButton = {
                TextButton(onClick = { saveSuccessShow = false }) {
                    Text("Đồng ý")
                }
            }
        )
    }

    if (showAddSupportDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddSupportDialog = false
                newSupportName = ""
                newSupportAmountStr = ""
            },
            title = { Text("Thêm Khoản Hỗ Trợ/Phụ Cấp", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Người dùng tùy chỉnh các khoản hỗ trợ nhận thêm cố định hàng tháng (Xăng xe, điện thoại, chuyên cần...)", style = MaterialTheme.typography.bodySmall)
                    
                    OutlinedTextField(
                        value = newSupportName,
                        onValueChange = { newSupportName = it },
                        label = { Text("Tên hỗ trợ (khoản phụ cấp hoặc khấu trừ)") },
                        placeholder = { Text("Ví dụ: Đóng Bảo hiểm xã hội, xăng xe...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newSupportAmountStr,
                        onValueChange = { input ->
                            if (input.isEmpty() || input == "-" || input.toDoubleOrNull() != null) {
                                newSupportAmountStr = input
                            }
                        },
                        label = { Text("Số tiền (VND, dùng dấu âm '-' để trừ/khấu trừ)") },
                        placeholder = { Text("Ví dụ: -150000 hoặc 300000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = newSupportAmountStr.toDoubleOrNull() ?: 0.0
                        if (newSupportName.isNotBlank() && amount != 0.0) {
                            viewModel.addMonthlySupport(newSupportName.trim(), amount)
                            showAddSupportDialog = false
                            newSupportName = ""
                            newSupportAmountStr = ""
                        }
                    }
                ) {
                    Text("Thêm mới")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showAddSupportDialog = false
                        newSupportName = ""
                        newSupportAmountStr = ""
                    }
                ) {
                    Text("Hủy")
                }
            }
        )
    }
}

// ---------------------- WORK LOG EDITOR DIALOG ----------------------
@Composable
fun WorkLogEditorDialog(
    date: String,
    activeLog: WorkLog?,
    allJobs: List<Job>,
    defaultMealAllowance: Double,
    onDismiss: () -> Unit,
    onSave: (Int?, Double, Int?, Double, Double, Double, String, String) -> Unit,
    onDelete: () -> Unit
) {
    // Standard jobs available
    val activeJob1 = remember(activeLog, allJobs) {
        allJobs.find { it.id == activeLog?.jobId1 } ?: allJobs.find { it.isDefault } ?: allJobs.firstOrNull()
    }
    val activeJob2 = remember(activeLog, allJobs) {
        allJobs.find { it.id == activeLog?.jobId2 }
    }

    var jobId1 by remember { mutableStateOf(activeJob1?.id) }
    var ratio1Str by remember { mutableStateOf(activeLog?.ratio1?.toString() ?: "1.0") }

    var hasSecondJob by remember { mutableStateOf(activeJob2 != null) }
    var jobId2 by remember { mutableStateOf(activeJob2?.id ?: allJobs.firstOrNull()?.id) }
    var ratio2Str by remember { mutableStateOf(activeLog?.ratio2?.toString() ?: "0.5") }

    var mealStr by remember { mutableStateOf((activeLog?.mealAllowance ?: defaultMealAllowance).toInt().toString()) }

    var otherAmtStr by remember { mutableStateOf((activeLog?.otherAmount ?: 0.0).toInt().toString()) }
    var otherNote by remember { mutableStateOf(activeLog?.otherNote ?: "") }
    var note by remember { mutableStateOf(activeLog?.note ?: "") }

    var showJob1Dropdown by remember { mutableStateOf(false) }
    var showJob2Dropdown by remember { mutableStateOf(false) }

    var errorMsg by remember { mutableStateOf("") }

    val dateDisplay = try {
        val s = date.split("-")
        "${s[2]}/${s[1]}/${s[0]}"
    } catch (e: Exception) {
        date
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Chấm công ngày $dateDisplay",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // --- JOB 1 SELECTION ---
                item {
                    Column {
                        Text("Công việc chính", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable { showJob1Dropdown = true }
                                .padding(14.dp)
                        ) {
                            val selectedJobName = allJobs.find { it.id == jobId1 }?.name ?: "Chọn công việc"
                            Text(selectedJobName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            DropdownMenu(
                                expanded = showJob1Dropdown,
                                onDismissRequest = { showJob1Dropdown = false },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                allJobs.forEach { job ->
                                    DropdownMenuItem(
                                        text = { Text("${job.name} (Tỉ lệ x${FormatHelper.formatRatio(job.rate)})") },
                                        onClick = {
                                            jobId1 = job.id
                                            showJob1Dropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Column {
                        Text("Hệ số công việc 1 (Mặc định 1.0 hoặc 0.5)", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("1.0", "0.5", "1.2", "1.5").forEach { quickRatio ->
                                FilterChip(
                                    selected = ratio1Str == quickRatio,
                                    onClick = { ratio1Str = quickRatio },
                                    label = { Text(quickRatio) },
                                    modifier = Modifier.weight(1.0f)
                                )
                            }
                        }
                        OutlinedTextField(
                            value = ratio1Str,
                            onValueChange = { ratio1Str = it },
                            label = { Text("Hệ số ngày công tùy chỉnh (Ví dụ: 1.0)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                // --- JOB 2 TOGGLE ---
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = hasSecondJob,
                            onCheckedChange = { hasSecondJob = it }
                        )
                        Column(modifier = Modifier.clickable { hasSecondJob = !hasSecondJob }) {
                            Text("Chấm thêm công việc thứ 2", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text("Dành cho ngày làm 2 công việc khác nhau", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }
                }

                if (hasSecondJob) {
                    item {
                        Column {
                            Text("Công việc phụ thứ 2", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .clickable { showJob2Dropdown = true }
                                    .padding(14.dp)
                            ) {
                                val selectedJobName = allJobs.find { it.id == jobId2 }?.name ?: "Chọn công việc"
                                Text(selectedJobName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                DropdownMenu(
                                    expanded = showJob2Dropdown,
                                    onDismissRequest = { showJob2Dropdown = false },
                                    modifier = Modifier.fillMaxWidth(0.8f)
                                ) {
                                    allJobs.forEach { job ->
                                        DropdownMenuItem(
                                            text = { Text("${job.name} (Tỉ lệ x${FormatHelper.formatRatio(job.rate)})") },
                                            onClick = {
                                                jobId2 = job.id
                                                showJob2Dropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column {
                            Text("Hệ số công việc thứ 2 (Nên dùng 0.5)", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("0.5", "1.0", "0.2").forEach { quickRatio ->
                                    FilterChip(
                                        selected = ratio2Str == quickRatio,
                                        onClick = { ratio2Str = quickRatio },
                                        label = { Text(quickRatio) },
                                        modifier = Modifier.weight(1.0f)
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = ratio2Str,
                                onValueChange = { ratio2Str = it },
                                label = { Text("Hạ số công việc 2") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }

                // --- MEAL MONEY ---
                item {
                    OutlinedTextField(
                        value = mealStr,
                        onValueChange = { mealStr = it },
                        label = { Text("Tiền ăn trong ngày (mặc định 20,000 VND)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = {
                            val amount = mealStr.toDoubleOrNull() ?: 0.0
                            Text("Hiện tại: ${FormatHelper.formatVnd(amount)}")
                        },
                        leadingIcon = { Icon(Icons.Filled.LocalDining, null, tint = MaterialTheme.colorScheme.primary) }
                    )
                }

                // --- OTHER MONEY & NOTE ---
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = otherAmtStr,
                            onValueChange = { otherAmtStr = it },
                            label = { Text("Tiền khác tùy chỉnh") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.1f),
                            singleLine = true,
                            supportingText = {
                                val amount = otherAmtStr.toDoubleOrNull() ?: 0.0
                                Text(FormatHelper.formatVnd(amount))
                            },
                            leadingIcon = { Icon(Icons.Filled.Payments, null, tint = MaterialTheme.colorScheme.primary) }
                        )

                        OutlinedTextField(
                            value = otherNote,
                            onValueChange = { otherNote = it },
                            label = { Text("Mô tả tiền khác") },
                            placeholder = { Text("Tăng ca, thưởng...") },
                            modifier = Modifier.weight(0.9f),
                            singleLine = true
                        )
                    }
                }

                // --- COMMON NOTE ---
                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Ghi chú ngày công (Tùy chọn)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }

                if (errorMsg.isNotEmpty()) {
                    item {
                        Text(
                            text = errorMsg,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                // --- ACTION CONTROLS ---
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (activeLog != null) {
                            Button(
                                onClick = onDelete,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .testTag("delete_log_confirm")
                            ) {
                                Icon(Icons.Filled.Delete, "Xóa chấm công")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Xóa công", fontSize = 13.sp)
                            }
                        }

                        Button(
                            onClick = {
                                val r1 = ratio1Str.toDoubleOrNull()
                                val r2 = if (hasSecondJob) ratio2Str.toDoubleOrNull() else 0.0
                                val meal = mealStr.toDoubleOrNull() ?: 20000.0
                                val otherAmt = otherAmtStr.toDoubleOrNull() ?: 0.0

                                if (r1 == null || r2 == null) {
                                    errorMsg = "Hệ số công việc không đúng lượng số."
                                } else if (jobId1 == null) {
                                    errorMsg = "Vui lòng chọn công việc chính."
                                } else if (hasSecondJob && jobId2 == null) {
                                    errorMsg = "Vui lòng chọn công việc phụ."
                                } else {
                                    onSave(
                                        jobId1,
                                        r1,
                                        if (hasSecondJob) jobId2 else null,
                                        r2,
                                        meal,
                                        otherAmt,
                                        otherNote,
                                        note
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1.1f)
                                .testTag("save_log_confirm")
                        ) {
                            Icon(Icons.Filled.Check, "Lưu thông tin")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lưu chấm", fontSize = 13.sp)
                        }
                    }
                }

                item {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Bỏ qua", textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
