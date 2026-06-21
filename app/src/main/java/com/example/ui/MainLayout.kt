package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import com.example.data.Job
import com.example.data.UserConfig
import com.example.data.WorkLog
import java.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull

data class PendingActionData(val text: String, val action: () -> Unit)
data class CustomOtherItem(val name: String, val amount: Double)
data class JobSummary(val name: String, val laborDays: Double, val daysWorked: Double, val salary: Double, val support: Double, val total: Double)

fun getDailySalaryForMonth(config: UserConfig, month: String): Double {
    try {
        val mapObj = org.json.JSONObject(config.monthActualSalaries)
        if (mapObj.has(month)) {
            val settled = mapObj.getDouble(month)
            if (settled > 0.0) return settled
        }
    } catch (e: Exception) {}
    return config.dailySalary
}

fun getContrastColor(selectedColorHex: String, fallbackColor: Color): Color {
    try {
        if (selectedColorHex.isEmpty() || !selectedColorHex.startsWith("#")) {
            return fallbackColor
        }
        val colors = if (selectedColorHex.contains(",")) {
            selectedColorHex.split(",")
        } else {
            listOf(selectedColorHex)
        }
        
        var totalLuminance = 0.0
        var count = 0
        for (hex in colors) {
            val h = hex.trim()
            if (h.startsWith("#")) {
                val colorInt = android.graphics.Color.parseColor(h)
                val r = android.graphics.Color.red(colorInt) / 255.0
                val g = android.graphics.Color.green(colorInt) / 255.0
                val b = android.graphics.Color.blue(colorInt) / 255.0
                val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
                totalLuminance += luminance
                count++
            }
        }
        val avgLuminance = if (count > 0) totalLuminance / count else 0.0
        return if (avgLuminance > 0.55) Color(0xFF1C1B1F) else Color.White
    } catch (e: Exception) {
        return fallbackColor
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(viewModel: AttendanceViewModel) {
    val userConfig by viewModel.userConfig.collectAsStateWithLifecycle()
    val allJobs by viewModel.allJobs.collectAsStateWithLifecycle()
    val monthlyLogs by viewModel.monthlyLogs.collectAsStateWithLifecycle()
    val previousMonthlyLogs by viewModel.previousMonthlyLogs.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val activeLogForSelectedDate by viewModel.activeLogForSelectedDate.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf("home") }
    var showWorkLogDialog by remember { mutableStateOf(false) }
    var showMonthJumpDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    BackHandler(enabled = currentTab != "home") {
        currentTab = "home"
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.checkAndSetToday()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == "home",
                    onClick = { currentTab = "home" },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Trang chủ") },
                    label = { Text("Trang chủ", fontSize = 11.sp, fontWeight = if (currentTab == "home") FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1E88E5), // Vibrant Blue
                        selectedTextColor = Color(0xFF1E88E5),
                        indicatorColor = Color(0xFF1E88E5).copy(alpha = 0.12f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_home")
                )
                NavigationBarItem(
                    selected = currentTab == "stats",
                    onClick = { currentTab = "stats" },
                    icon = { Icon(Icons.Filled.BarChart, contentDescription = "Thống kê") },
                    label = { Text("Thống kê", fontSize = 11.sp, fontWeight = if (currentTab == "stats") FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFF57C00), // Vibrant Amber/Orange
                        selectedTextColor = Color(0xFFF57C00),
                        indicatorColor = Color(0xFFF57C00).copy(alpha = 0.12f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_stats")
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
                    onOpenLogDialog = { showWorkLogDialog = true },
                    onSettingsClick = { currentTab = "settings" },
                    onMonthClick = { showMonthJumpDialog = true }
                )
                "stats" -> StatsScreen(
                    viewModel = viewModel,
                    monthlyLogs = monthlyLogs,
                    previousMonthlyLogs = previousMonthlyLogs,
                    allJobs = allJobs,
                    selectedMonth = selectedMonth,
                    userConfig = userConfig,
                    onDayClick = { date ->
                        viewModel.selectDate(date)
                        showWorkLogDialog = true
                    },
                    onMonthClick = { showMonthJumpDialog = true }
                )
                "settings" -> SettingsScreen(
                    viewModel = viewModel,
                    userConfig = userConfig,
                    onBackToHome = { currentTab = "home" }
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

    if (showMonthJumpDialog) {
        val currentParts = selectedMonth.split("-")
        val initialY = currentParts.getOrNull(0)?.toIntOrNull() ?: 2026
        val initialM = currentParts.getOrNull(1)?.toIntOrNull() ?: 6

        MonthYearPickerDialog(
            initialYear = initialY,
            initialMonth = initialM,
            onDismiss = { showMonthJumpDialog = false },
            onSelected = { y, m ->
                viewModel.setSelectedMonth(y, m)
                showMonthJumpDialog = false
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
    onOpenLogDialog: () -> Unit,
    onSettingsClick: () -> Unit,
    onMonthClick: () -> Unit
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

    val regularSalary = totalLaborDays * getDailySalaryForMonth(userConfig, selectedMonth)
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
                                        translationX = userConfig.avatarOffsetX * (60f / 100f)
                                        translationY = userConfig.avatarOffsetY * (60f / 100f)
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
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = userConfig.fullName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (userConfig.occupation.isNotEmpty()) {
                            Text(
                                text = userConfig.occupation,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = onSettingsClick
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Cấu hình cài đặt",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Selected Month Summary Display Card
        item {
            val contentColor = getContrastColor(userConfig.selectedColorHex, MaterialTheme.colorScheme.onPrimary)
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .then(
                        if (userConfig.selectedColorHex.contains(",")) {
                            val colors = userConfig.selectedColorHex.split(",").map {
                                try {
                                    Color(android.graphics.Color.parseColor(it.trim()))
                                } catch (e: Exception) {
                                    Color(0xFF1E88E5)
                                }
                            }
                            Modifier.background(Brush.linearGradient(colors))
                        } else {
                            Modifier.background(MaterialTheme.colorScheme.primary)
                        }
                    ),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
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
                            Icon(Icons.Filled.ChevronLeft, "Tháng trước", tint = contentColor)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                .clickable { onMonthClick() }
                        ) {
                            Text(
                                text = monthTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = contentColor,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                        IconButton(
                            modifier = Modifier.testTag("month_next_btn"),
                            onClick = { viewModel.changeMonth(1) }
                        ) {
                            Icon(Icons.Filled.ChevronRight, "Tháng sau", tint = contentColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "TỔNG THU NHẬP TẠM TÍNH",
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = FormatHelper.formatVnd(totalEstimatedEarnings),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.sp
                        ),
                        color = contentColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        textAlign = TextAlign.Center
                    )

                    Divider(
                        color = contentColor.copy(alpha = 0.15f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Stats row grids
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1.0f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Ngày làm", fontSize = 11.sp, color = contentColor.copy(alpha = 0.7f))
                            Text("$totalWorkedDays ngày", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = contentColor)
                        }
                        Column(modifier = Modifier.weight(1.0f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Tổng công", fontSize = 11.sp, color = contentColor.copy(alpha = 0.7f))
                            Text(FormatHelper.formatRatio(totalLaborDays), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = contentColor)
                        }
                        Column(modifier = Modifier.weight(1.0f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Lương cơ bản", fontSize = 11.sp, color = contentColor.copy(alpha = 0.7f))
                            Text(FormatHelper.formatVndNoSymbol(regularSalary), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = contentColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1.0f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Hỗ trợ khâu", fontSize = 11.sp, color = contentColor.copy(alpha = 0.7f))
                            Text(FormatHelper.formatVndNoSymbol(totalSupportAmount), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = contentColor)
                        }
                        Column(modifier = Modifier.weight(1.0f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Tổng tiền ăn", fontSize = 11.sp, color = contentColor.copy(alpha = 0.7f))
                            Text(FormatHelper.formatVndNoSymbol(totalMealAllowance), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = contentColor)
                        }
                        Column(modifier = Modifier.weight(1.0f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Tiền khác", fontSize = 11.sp, color = contentColor.copy(alpha = 0.7f))
                            Text(FormatHelper.formatVndNoSymbol(totalOtherAmount), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = contentColor)
                        }
                    }

                    if (totalMonthlySupportCustom > 0) {
                        Divider(
                            color = contentColor.copy(alpha = 0.15f),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tổng hỗ trợ/phụ cấp cố định tháng:", fontSize = 12.sp, color = contentColor.copy(alpha = 0.8f))
                            Text(FormatHelper.formatVnd(totalMonthlySupportCustom), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = contentColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = contentColor.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Tính với mức lương cố định: ${FormatHelper.formatVnd(getDailySalaryForMonth(userConfig, selectedMonth))} / ngày công.",
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor,
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
                },
                selectedLunarColorHex = userConfig.selectedLunarColorHex
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
                                Icon(Icons.Filled.Check, contentDescription = "Chấm công")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Chấm công", fontSize = 13.sp)
                            }
                        }

                        FilledTonalButton(
                            onClick = onOpenLogDialog,
                            modifier = Modifier
                                .weight(1.0f)
                                .testTag("detailed_edit_btn")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Tuỳ chỉnh chi tiết")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tuỳ chỉnh", fontSize = 13.sp)
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
    onDateSelected: (String) -> Unit,
    selectedLunarColorHex: String = "#FF9800"
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
                weekdays.forEachIndexed { idx, day ->
                    val isSunday = (day == "CN")
                    Text(
                        text = day,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isSunday) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
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
                                .aspectRatio(0.82f)
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

                                Box(
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
                                        .clickable { onDateSelected(dateStr) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    val isSunday = (c == 6)
                                    val lunarData = LunarCalendar.convertSolar2Lunar(dayNumber, month, year)
                                    val isHoliday = LunarCalendar.isVietnameseHoliday(dayNumber, month, lunarData.day, lunarData.month)

                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 2.dp, vertical = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = dayNumber.toString(),
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected || totalWorkRatio > 0 || isSunday || isHoliday) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                                isHoliday -> Color(0xFFF57F17) // Gold-Yellow
                                                totalWorkRatio > 0 -> MaterialTheme.colorScheme.onPrimaryContainer
                                                isSunday -> Color.Red
                                                else -> MaterialTheme.colorScheme.onSurface
                                            },
                                            style = androidx.compose.ui.text.TextStyle(
                                                platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                                    includeFontPadding = false
                                                )
                                            ),
                                            textAlign = TextAlign.Center
                                        )

                                        Spacer(modifier = Modifier.height(1.dp))

                                        val lunarText = if (lunarData.day == 1) "${lunarData.day}/${lunarData.month}" else lunarData.day.toString()
                                        val lunarColor = try {
                                            Color(android.graphics.Color.parseColor(selectedLunarColorHex))
                                        } catch (e: Exception) {
                                            Color(0xFFFF9800)
                                        }
                                        Text(
                                            text = lunarText,
                                            fontSize = 9.sp,
                                            fontWeight = if (isHoliday || lunarData.day == 1) FontWeight.Bold else FontWeight.Medium,
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                                isHoliday -> Color(0xFFF57F17)
                                                else -> lunarColor
                                            },
                                            style = androidx.compose.ui.text.TextStyle(
                                                platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                                    includeFontPadding = false
                                                )
                                            ),
                                            textAlign = TextAlign.Center
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

data class PieChartData(
    val label: String,
    val value: Double,
    val color: Color
)

@Composable
fun PieChart(
    data: List<PieChartData>,
    title: String,
    modifier: Modifier = Modifier
) {
    val totalSum = data.sumOf { it.value }
    Column(
        modifier = modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (totalSum <= 0.0) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Thực tế 0%",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            var startAngle = -90f
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(70.dp)) {
                    data.forEach { item ->
                        val sweepAngle = ((item.value / totalSum) * 360f).toFloat()
                        drawArc(
                            color = item.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true
                        )
                        startAngle += sweepAngle
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        // Legend of items
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            data.forEach { item ->
                val percent = if (totalSum > 0.0) {
                    (item.value / totalSum * 100)
                } else {
                    0.0
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(item.color, shape = RoundedCornerShape(1.5.dp))
                    )
                    Text(
                        text = item.label,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1.0f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.1f", percent)}%",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
    previousMonthlyLogs: List<WorkLog>,
    allJobs: List<Job>,
    selectedMonth: String,
    userConfig: UserConfig,
    onDayClick: (String) -> Unit,
    onMonthClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    BackHandler {
        (context as? android.app.Activity)?.finish()
    }

    val monthTitle = try {
        val parts = selectedMonth.split("-")
        "${parts[1]}/${parts[0]}"
    } catch (e: Exception) {
        selectedMonth
    }
    
    val allMonthlySupports by viewModel.allMonthlySupports.collectAsStateWithLifecycle()
    val totalMonthlySupportCustom = allMonthlySupports.sumOf { it.amount }

    val sortedLogs = remember(monthlyLogs) { monthlyLogs.sortedByDescending { it.date } }

    val jobsSummaryList = remember(monthlyLogs, allJobs, userConfig, selectedMonth) {
        val currentMonthRate = getDailySalaryForMonth(userConfig, selectedMonth)
        allJobs.mapNotNull { job ->
            var jobLaborDays = 0.0
            var jobDaysWorked = 0.0
            var jobSupport = 0.0
            monthlyLogs.forEach { log ->
                if (log.jobId1 == job.id) {
                    jobLaborDays += log.ratio1 * job.rate
                    jobDaysWorked += log.ratio1
                    jobSupport += log.ratio1 * job.supportAmount
                }
                if (log.jobId2 == job.id) {
                    jobLaborDays += log.ratio2 * job.rate
                    jobDaysWorked += log.ratio2
                    jobSupport += log.ratio2 * job.supportAmount
                }
            }
            if (jobLaborDays > 0.0) {
                val salary = jobLaborDays * currentMonthRate
                JobSummary(
                    name = job.name,
                    laborDays = jobLaborDays,
                    daysWorked = jobDaysWorked,
                    salary = salary,
                    support = jobSupport,
                    total = salary + jobSupport
                )
            } else {
                null
            }
        }
    }

    val prevEarnings = remember(previousMonthlyLogs, allJobs, userConfig, totalMonthlySupportCustom, selectedMonth) {
        if (previousMonthlyLogs.isEmpty()) {
            null
        } else {
            val parts = selectedMonth.split("-")
            val previousMonthStr = if (parts.size == 2) {
                val year = parts[0].toIntOrNull() ?: 2026
                val month = parts[1].toIntOrNull() ?: 6
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month - 2) // calendar is 0-indexed, so month-2 gets previous month
                }
                String.format(Locale.US, "%d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
            } else {
                ""
            }
            val previousMonthRate = getDailySalaryForMonth(userConfig, previousMonthStr)

            var prevLaborDays = 0.0
            var prevSupportAmount = 0.0
            var prevMealAllowance = 0.0
            var prevOtherAmount = 0.0
            previousMonthlyLogs.forEach { log ->
                val job1 = allJobs.find { it.id == log.jobId1 }
                val job2 = allJobs.find { it.id == log.jobId2 }
                prevLaborDays += ((job1?.rate ?: 0.0) * log.ratio1 + (job2?.rate ?: 0.0) * log.ratio2)
                prevSupportAmount += ((job1?.supportAmount ?: 0.0) * log.ratio1 + (job2?.supportAmount ?: 0.0) * log.ratio2)
                prevMealAllowance += log.mealAllowance
                prevOtherAmount += log.otherAmount
            }
            val prevRegSalary = prevLaborDays * previousMonthRate
            prevRegSalary + prevSupportAmount + prevMealAllowance + prevOtherAmount + totalMonthlySupportCustom
        }
    }

    var showDetailedLogs by remember { mutableStateOf(false) }

    val chartColors = remember {
        listOf(
            Color(0xFF2196F3),
            Color(0xFF4CAF50),
            Color(0xFFE91E63),
            Color(0xFFFF9800),
            Color(0xFF9C27B0),
            Color(0xFF00BCD4),
            Color(0xFFFFEB3B),
            Color(0xFF795548)
        )
    }

    val pieChartDataWork = remember(jobsSummaryList) {
        jobsSummaryList.mapIndexed { index, summary ->
            PieChartData(
                label = summary.name,
                value = summary.laborDays,
                color = chartColors.getOrElse(index) { Color.Gray }
            )
        }
    }

    val pieChartDataSalary = remember(jobsSummaryList) {
        jobsSummaryList.mapIndexed { index, summary ->
            PieChartData(
                label = summary.name,
                value = summary.total,
                color = chartColors.getOrElse(index) { Color.Gray }
            )
        }
    }

    var totalDaysWorked = 0.0
    var totalLaborDays = 0.0
    var totalSupportAmount = 0.0
    var totalMealAllowance = 0.0
    var totalOtherAmount = 0.0

    monthlyLogs.forEach { log ->
        val job1 = allJobs.find { it.id == log.jobId1 }
        val job2 = allJobs.find { it.id == log.jobId2 }
        totalDaysWorked += (log.ratio1 + log.ratio2)
        totalLaborDays += ((job1?.rate ?: 0.0) * log.ratio1 + (job2?.rate ?: 0.0) * log.ratio2)
        totalSupportAmount += ((job1?.supportAmount ?: 0.0) * log.ratio1 + (job2?.supportAmount ?: 0.0) * log.ratio2)
        totalMealAllowance += log.mealAllowance
        totalOtherAmount += log.otherAmount
    }

    val regularSalary = totalLaborDays * getDailySalaryForMonth(userConfig, selectedMonth)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1.0f)
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .clickable { onMonthClick() }
                    ) {
                        Text(
                            text = monthTitle,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    IconButton(onClick = { viewModel.changeMonth(1) }) {
                        Icon(Icons.Filled.ChevronRight, "Tháng sau", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1.05f)) {
                        Text("Ngày đi làm | Công tỷ lệ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                        Text(
                            text = "${FormatHelper.formatRatio(totalDaysWorked)} ngày | ${FormatHelper.formatRatio(totalLaborDays)} công",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Column(modifier = Modifier.weight(0.95f)) {
                        Text("Lương tạm tính", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                        Text(
                            text = FormatHelper.formatVnd(totalEstimatedEarnings),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CompareArrows,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (prevEarnings == null) {
                        Text(
                            text = "So sánh: Chưa có dữ liệu tháng trước",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    } else {
                        val diff = totalEstimatedEarnings - prevEarnings
                        val (text, color) = when {
                            diff > 0 -> "Tăng +${FormatHelper.formatVnd(diff)} so với tháng trước" to Color(0xFF4CAF50)
                            diff < 0 -> "Giảm -${FormatHelper.formatVnd(-diff)} so với tháng trước" to Color(0xFFFF8A80)
                            else -> "Bằng với tháng trước" to MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        }
                        Text(
                            text = "So sánh: $text",
                            fontSize = 11.5.sp,
                            color = color,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

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
                item {
                    if (jobsSummaryList.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "Tổng Hợp Công Việc Trong Tháng",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f), modifier = Modifier.padding(bottom = 10.dp))
                                jobsSummaryList.forEachIndexed { index, summary ->
                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = summary.name,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                                modifier = Modifier.weight(1f),
                                                maxLines = 2,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Ngày đi làm: ${FormatHelper.formatRatio(summary.daysWorked)} ngày",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "Công tỷ lệ: ${FormatHelper.formatRatio(summary.laborDays)} công",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "• LCB: ${FormatHelper.formatVnd(summary.salary)}",
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "• Hỗ trợ: ${FormatHelper.formatVnd(summary.support)}",
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                modifier = Modifier.weight(1f),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            "→ Tổng tiền công: ${FormatHelper.formatVnd(summary.total)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    if (index < jobsSummaryList.size - 1) {
                                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
                                    }
                                }

                                if (pieChartDataWork.isNotEmpty() || pieChartDataSalary.isNotEmpty()) {
                                    Divider(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                    Text(
                                        "Biểu đồ phân tích tỷ lệ %",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        PieChart(
                                            data = pieChartDataWork,
                                            title = "% Công việc",
                                            modifier = Modifier.weight(1f)
                                        )
                                        PieChart(
                                            data = pieChartDataSalary,
                                            title = "% Theo lương",
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = { showDetailedLogs = !showDetailedLogs },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showDetailedLogs) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("toggle_logs_btn")
                        ) {
                            Icon(
                                imageVector = if (showDetailedLogs) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (showDetailedLogs) "Thu gọn nhật ký" else "Xem chi tiết nhật ký")
                        }
                    }
                }

                if (showDetailedLogs) {
                    item {
                        Text(
                            text = "Nhật Ký Chấm Công Chi Tiết",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(sortedLogs) { log ->
                    val j1 = allJobs.find { it.id == log.jobId1 }
                    val j2 = allJobs.find { it.id == log.jobId2 }

                    // calculate daily total earnings
                    val c1 = (j1?.rate ?: 0.0) * log.ratio1
                    val c2 = (j2?.rate ?: 0.0) * log.ratio2
                    val dayDailySalary = (c1 + c2) * getDailySalaryForMonth(userConfig, selectedMonth)

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
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "• ${j1.name}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${FormatHelper.formatRatio(log.ratio1)} công (Tỷ lệ x${j1.rate})",
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            if (j2 != null && log.ratio2 > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "• ${j2.name}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${FormatHelper.formatRatio(log.ratio2)} công (Tỷ lệ x${j2.rate})",
                                        fontSize = 13.sp
                                    )
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
                                val otherNoteDisplay = try {
                                    val serialized = log.otherNote
                                    if (serialized.isNotBlank() && serialized.startsWith("[") && serialized.endsWith("]")) {
                                        val array = org.json.JSONArray(serialized)
                                        val parts = mutableListOf<String>()
                                        for (i in 0 until array.length()) {
                                            val obj = array.getJSONObject(i)
                                            val name = obj.getString("name")
                                            val amount = obj.getDouble("amount")
                                            parts.add("$name: ${FormatHelper.formatVnd(amount)}")
                                        }
                                        parts.joinToString(", ")
                                    } else {
                                        log.otherNote
                                    }
                                } catch (e: Exception) {
                                    log.otherNote
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("• Tiền bổ sung khác ($otherNoteDisplay)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.weight(1f))
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
}

// ---------------------- JOBS MANAGEMENT SCREEN ----------------------
@Composable
fun JobsScreen(
    viewModel: AttendanceViewModel,
    allJobs: List<Job>
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var jobToEdit by remember { mutableStateOf<Job?>(null) }
    var pendingJobAction by remember { mutableStateOf<PendingActionData?>(null) }

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

            FilledTonalButton(
                onClick = { showAddDialog = true },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("add_job_header_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Thêm công việc",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Thêm mới", fontSize = 12.sp)
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
                                onClick = {
                                    pendingJobAction = PendingActionData(
                                        "Bạn có chắc chắn muốn xóa vĩnh viễn công việc '${job.name}' này không?"
                                    ) {
                                        viewModel.deleteJob(job.id)
                                    }
                                },
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
                pendingJobAction = PendingActionData(
                    "Bạn có muốn thêm công việc mới '${name}' với tỉ lệ công x${FormatHelper.formatRatio(rate)} và hỗ trợ khâu ${FormatHelper.formatVnd(support)} không?"
                ) {
                    viewModel.addJob(name, rate, support)
                    showAddDialog = false
                }
            }
        )
    }

    if (jobToEdit != null) {
        JobEditorDialog(
            job = jobToEdit,
            onDismiss = { jobToEdit = null },
            onSave = { name, rate, support ->
                pendingJobAction = PendingActionData(
                    "Bạn có chắc chắn muốn lưu thay đổi cho công việc '${jobToEdit?.name}' thành: Tên: '${name}', tỉ lệ x${FormatHelper.formatRatio(rate)}, hỗ trợ ${FormatHelper.formatVnd(support)}?"
                ) {
                    val updated = jobToEdit!!.copy(name = name, rate = rate, supportAmount = support)
                    viewModel.updateJob(updated)
                    jobToEdit = null
                }
            }
        )
    }

    if (pendingJobAction != null) {
        AlertDialog(
            onDismissRequest = { pendingJobAction = null },
            title = { Text("Cảnh Báo Xác Nhận", fontWeight = FontWeight.Bold) },
            text = { Text(pendingJobAction!!.text) },
            confirmButton = {
                Button(
                    onClick = {
                        pendingJobAction!!.action()
                        pendingJobAction = null
                    }
                ) {
                    Text("Đồng ý")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingJobAction = null }) {
                    Text("Hủy bỏ")
                }
            }
        )
    }
}

// ---------------------- JOB EDITOR DIALOG ----------------------
@Composable
fun JobEditorDialog(
    job: Job?,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double) -> Unit,
    onDelete: (() -> Unit)? = null
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (job != null && onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Xóa công việc", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Xóa công việc")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Hủy bỏ")
                }
            }
        }
    )
}

// ---------------------- OLD SETTINGS SCREEN ----------------------
@Composable
fun OldSettingsScreen(
    viewModel: AttendanceViewModel,
    userConfig: UserConfig
) {
    var name by remember { mutableStateOf(userConfig.fullName) }
    var occupation by remember { mutableStateOf(userConfig.occupation) }
    var dailySalaryStr by remember { mutableStateOf(userConfig.dailySalary.toInt().toString()) }
    var defaultMealStr by remember { mutableStateOf(userConfig.dailyMealAllowance.toInt().toString()) }
    var selectedAvatarName by remember { mutableStateOf(userConfig.avatarName) }

    var isEditingAccount by remember { mutableStateOf(false) }
    var pendingConfigAction by remember { mutableStateOf<PendingActionData?>(null) }

    var avatarUri by remember { mutableStateOf(userConfig.avatarUri) }
    var avatarScale by remember { mutableStateOf(userConfig.avatarScale) }
    var avatarOffsetX by remember { mutableStateOf(userConfig.avatarOffsetX) }
    var avatarOffsetY by remember { mutableStateOf(userConfig.avatarOffsetY) }
    var isLibraryMode by remember { mutableStateOf(userConfig.avatarUri != null) }

    // Interactivity styles
    var selectedColorHex by remember { mutableStateOf(userConfig.selectedColorHex) }
    var selectedFontName by remember { mutableStateOf(userConfig.selectedFontName) }
    var appThemeMode by remember { mutableStateOf(userConfig.appThemeMode) }

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
    val allJobs by viewModel.allJobs.collectAsStateWithLifecycle()

    var showAddSupportDialog by remember { mutableStateOf(false) }
    var newSupportName by remember { mutableStateOf("") }
    var newSupportAmountStr by remember { mutableStateOf("") }

    var showAddJobDialog by remember { mutableStateOf(false) }
    var jobToEdit by remember { mutableStateOf<Job?>(null) }
    var pendingJobAction by remember { mutableStateOf<PendingActionData?>(null) }

    var saveSuccessShow by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Sync state features
    var showSyncDialog by remember { mutableStateOf(false) }
    var syncTargetField by remember { mutableStateOf("") } // "MEAL" or "SALARY"
    var syncTargetAmount by remember { mutableStateOf(0.0) }
    
    // Default dates initialized to beginning and end of this month
    val cal = Calendar.getInstance()
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) + 1
    val dayMax = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    var syncFromDate by remember { mutableStateOf(String.format(Locale.US, "%d-%02d-01", year, month)) }
    var syncToDate by remember { mutableStateOf(String.format(Locale.US, "%d-%02d-%02d", year, month, dayMax)) }
    var syncSuccessShow by remember { mutableStateOf(false) }

    // Settlement state features
    var settlementMonth by remember { mutableStateOf(String.format(Locale.US, "%d-%02d", year, month)) }
    var settlementTotalReceivedStr by remember { mutableStateOf("") }
    var settlementSuccessShow by remember { mutableStateOf(false) }
    var settlementCalculatedWage by remember { mutableStateOf<Double?>(null) }

    // Backup states
    var exportConfigChecked by remember { mutableStateOf(true) }
    var exportJobsChecked by remember { mutableStateOf(true) }
    var exportSupportsChecked by remember { mutableStateOf(true) }
    var exportLogsChecked by remember { mutableStateOf(true) }

    var showBackupOptionsDialog by remember { mutableStateOf(false) }
    var backupSuccessShow by remember { mutableStateOf(false) }
    var backupErrorMsg by remember { mutableStateOf<String?>(null) }

    // Restore states
    var importConfigChecked by remember { mutableStateOf(true) }
    var importJobsChecked by remember { mutableStateOf(true) }
    var importSupportsChecked by remember { mutableStateOf(true) }
    var importLogsChecked by remember { mutableStateOf(true) }

    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }
    var showRestoreOptionsDialog by remember { mutableStateOf(false) }
    var restoreSuccessShow by remember { mutableStateOf(false) }
    var restoreErrorMsg by remember { mutableStateOf<String?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*"),
        onResult = { uri ->
            if (uri != null) {
                coroutineScope.launch {
                    try {
                        val jsonStr = viewModel.exportBackup(
                            exportConfig = exportConfigChecked,
                            exportJobs = exportJobsChecked,
                            exportSupports = exportSupportsChecked,
                            exportLogs = exportLogsChecked
                        )
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(jsonStr.toByteArray(Charsets.UTF_8))
                        }
                        backupSuccessShow = true
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                        backupErrorMsg = e.localizedMessage ?: "Lỗi ghi file"
                    }
                }
            }
        }
    )

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                coroutineScope.launch {
                    try {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            val jsonStr = inputStream.bufferedReader().use { it.readText() }
                            pendingRestoreJson = jsonStr
                            showRestoreOptionsDialog = true
                        }
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                        restoreErrorMsg = e.localizedMessage ?: "Không thể đọc dữ liệu từ file được chọn."
                    }
                }
            }
        }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text(
                "Cài Đặt Hệ Thống",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // 1. THÔNG TIN TÀI KHOẢN (Account Info + Avatars preset list)
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Thông tin tài khoản",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = { isEditingAccount = !isEditingAccount }
                        ) {
                            Icon(
                                imageVector = if (isEditingAccount) Icons.Filled.Check else Icons.Filled.Edit,
                                contentDescription = if (isEditingAccount) "Xong" else "Sửa đổi",
                                tint = if (isEditingAccount) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Họ và tên") },
                        enabled = isEditingAccount,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            disabledIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_name_input"),
                        singleLine = true
                    )

                    TextField(
                        value = occupation,
                        onValueChange = { occupation = it },
                        label = { Text("Nghề nghiệp / Chức vụ") },
                        enabled = isEditingAccount,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            disabledIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_occupation_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Chọn Hình Đại Diện (Avatar)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val selectedTabColor = MaterialTheme.colorScheme.primaryContainer
                        val unselectedTabColor = Color.Transparent
                        val selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                        val unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isLibraryMode) selectedTabColor else unselectedTabColor)
                                .clickable { isLibraryMode = false }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Face,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (!isLibraryMode) selectedTextColor else unselectedTextColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Hình có sẵn",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isLibraryMode) selectedTextColor else unselectedTextColor
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isLibraryMode) selectedTabColor else unselectedTabColor)
                                .clickable { isLibraryMode = true }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.PhotoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isLibraryMode) selectedTextColor else unselectedTextColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Thư viện ảnh",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLibraryMode) selectedTextColor else unselectedTextColor
                                )
                            }
                        }
                    }

                    // Preset Avatars list (fully visible without nested scrolling)
                    if (!isLibraryMode) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val chunkedAvatars = AvatarHelper.avatars.chunked(4)
                            chunkedAvatars.forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEach { avItem ->
                                        val isSelected = avItem.name == selectedAvatarName && avatarUri == null
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
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
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(CircleShape)
                                                    .background(avItem.backgroundColor),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(avItem.emoji, fontSize = 26.sp)
                                            }
                                        }
                                    }
                                    if (rowItems.size < 4) {
                                        repeat(4 - rowItems.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Library image crop / scale / offsets controls
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
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
                                                translationX = avatarOffsetX * (64f / 100f)
                                                translationY = avatarOffsetY * (64f / 100f)
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

                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Kích cỡ (Thu phóng): ${String.format(java.util.Locale.US, "%.1f", avatarScale)}x", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Đặt lại 1x", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { avatarScale = 1.0f })
                                }
                                Slider(
                                    value = avatarScale,
                                    onValueChange = { avatarScale = it },
                                    valueRange = 0.5f..3.0f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

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

        // 2. PHẦN CÔNG VIỆC (Job/Task configuration moved here!)
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Quản lý công việc & Tỉ lệ",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Thêm, sửa, xóa danh mục chấm công",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        IconButton(
                            onClick = { showAddJobDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddCircle,
                                contentDescription = "Thêm công việc mới",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        allJobs.forEach { job ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = job.name,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            if (job.isDefault) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    "mặc định", 
                                                    fontSize = 10.sp, 
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier
                                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            "Hệ số: x${FormatHelper.formatRatio(job.rate)} công • Trợ cấp: ${FormatHelper.formatVnd(job.supportAmount)}",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        IconButton(onClick = { jobToEdit = job }) {
                                            Icon(Icons.Filled.Edit, "Sửa", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(
                                            onClick = {
                                                pendingJobAction = PendingActionData(
                                                    "Bạn có muốn xóa vĩnh viễn công việc '${job.name}' này không?"
                                                ) {
                                                    viewModel.deleteJob(job.id)
                                                }
                                            },
                                            enabled = !job.isDefault
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete, 
                                                contentDescription = "Xóa", 
                                                modifier = Modifier.size(18.dp), 
                                                tint = if (job.isDefault) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f) else MaterialTheme.colorScheme.error
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

        // 3. THIẾT LẬP LƯƠNG & PHỤ CẤP CỐ ĐỊNH (Salary wage + meal allowance + monthly supports list)
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
                    Text(
                        text = "Thiết lập lương cơ bản & Phụ cấp",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                    // Salary with Sync button
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Lương ngày cơ sở tạm tính (VND)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextField(
                                value = dailySalaryStr,
                                onValueChange = { dailySalaryStr = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                enabled = isEditingAccount,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                ),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            IconButton(
                                onClick = {
                                    val amt = dailySalaryStr.toDoubleOrNull() ?: 300000.0
                                    syncTargetField = "SALARY"
                                    syncTargetAmount = amt
                                    showSyncDialog = true
                                },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                                    .size(40.dp)
                            ) {
                                Icon(Icons.Filled.Sync, "Đồng bộ lương ngày", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    // Meal with Sync button
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tiền ăn mặc định hàng ngày (VND)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextField(
                                value = defaultMealStr,
                                onValueChange = { defaultMealStr = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                enabled = isEditingAccount,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                ),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            IconButton(
                                onClick = {
                                    val amt = defaultMealStr.toDoubleOrNull() ?: 20000.0
                                    syncTargetField = "MEAL"
                                    syncTargetAmount = amt
                                    showSyncDialog = true
                                },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                                    .size(40.dp)
                            ) {
                                Icon(Icons.Filled.Sync, "Đồng bộ tiền ăn", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Monthly supports
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Hỗ trợ & phụ cấp cố định tháng", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            Text("Nhập số âm '-' để biểu thị các khoản giảm trừ (bảo hiểm...)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        IconButton(onClick = { showAddSupportDialog = true }) {
                            Icon(Icons.Filled.AddCircle, "Thêm", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        allMonthlySupports.forEach { support ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(support.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            text = FormatHelper.formatVnd(support.amount),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (support.amount >= 0) MaterialTheme.colorScheme.primary else Color.Red
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            pendingConfigAction = PendingActionData(
                                                "Bạn có chắc muốn xóa mục phụ cấp '${support.name}' này không?"
                                            ) {
                                                viewModel.deleteMonthlySupport(support.id)
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Filled.Delete, "Xóa", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. QUYẾT TOÁN LƯƠNG TỪNG THÁNG (Monthly Salary Settlement)
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
                    Column {
                        Text(
                            text = "Quyết toán lương thực nhận",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Nhập lương thực tế để tính ra đơn giá 1.0 công của tháng đó",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = settlementMonth,
                            onValueChange = { settlementMonth = it },
                            label = { Text("Tháng quyết toán (YYYY-MM)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                // Default to formatting selected month
                                settlementMonth = String.format(Locale.US, "%d-%02d", year, month)
                            },
                            modifier = Modifier.background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(Icons.Filled.DateRange, "Tháng hiện tại", tint = MaterialTheme.colorScheme.secondary)
                        }
                    }

                    TextField(
                        value = settlementTotalReceivedStr,
                        onValueChange = { settlementTotalReceivedStr = it },
                        label = { Text("Tổng số tiền thực tế nhận được (VND)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = {
                            val amt = settlementTotalReceivedStr.toDoubleOrNull() ?: 0.0
                            Text("Quy đổi: ${FormatHelper.formatVnd(amt)}")
                        }
                    )

                    Button(
                        onClick = {
                            val totalAmt = settlementTotalReceivedStr.toDoubleOrNull()
                            if (totalAmt != null && totalAmt > 0 && settlementMonth.length == 7) {
                                coroutineScope.launch {
                                    // Settle actual daily salary
                                    val logs = viewModel.repository.getWorkLogsInMonth(settlementMonth).firstOrNull() ?: emptyList()
                                    val jobs = viewModel.repository.getAllJobsDirect()
                                    var monthLaborUnits = 0.0
                                    logs.forEach { log ->
                                        val j1 = jobs.find { it.id == log.jobId1 }
                                        val j2 = jobs.find { it.id == log.jobId2 }
                                        monthLaborUnits += ((j1?.rate ?: 0.0) * log.ratio1 + (j2?.rate ?: 0.0) * log.ratio2)
                                    }
                                    if (monthLaborUnits > 0) {
                                        val calculated = totalAmt / monthLaborUnits
                                        settlementCalculatedWage = calculated
                                        
                                        val currentConfig = viewModel.userConfig.value
                                        val mapObj = try {
                                            org.json.JSONObject(currentConfig.monthActualSalaries)
                                        } catch (e: Exception) {
                                            org.json.JSONObject()
                                        }
                                        mapObj.put(settlementMonth, calculated)

                                        viewModel.updateProfile(
                                            fullName = currentConfig.fullName,
                                            avatarName = currentConfig.avatarName,
                                            dailySalary = currentConfig.dailySalary,
                                            dailyMealAllowance = currentConfig.dailyMealAllowance,
                                            avatarUri = currentConfig.avatarUri,
                                            avatarScale = currentConfig.avatarScale,
                                            avatarOffsetX = currentConfig.avatarOffsetX,
                                            avatarOffsetY = currentConfig.avatarOffsetY,
                                            occupation = currentConfig.occupation,
                                            selectedColorHex = currentConfig.selectedColorHex,
                                            selectedFontName = currentConfig.selectedFontName,
                                            appThemeMode = currentConfig.appThemeMode,
                                            monthActualSalaries = mapObj.toString()
                                        )
                                        settlementSuccessShow = true
                                    } else {
                                        restoreErrorMsg = "Tháng này không tìm thấy ngày công nào đã chấm để quyết toán."
                                    }
                                }
                            } else {
                                restoreErrorMsg = "Vui lòng nhập đầy đủ thông tin số tiền và tháng (YYYY-MM)."
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Calculate, "Tính toán quyết toán")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tiến hành quyết toán & Cập nhật")
                    }

                    // Show active settled rate
                    val currentSettleRate = getDailySalaryForMonth(userConfig, settlementMonth)
                    if (currentSettleRate != userConfig.dailySalary) {
                        Text(
                            "✓ Tháng ${settlementMonth} đang áp dụng đơn giá quyết toán: " +
                            "${FormatHelper.formatVnd(currentSettleRate)}/công.",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 5. TÙY CHỌN GIAO DIỆN (Theme mode + presets colors + fonts choice)
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
                    Text(
                        text = "Tùy chọn giao diện & phông chữ",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                    // Theme selector buttons
                    Text("Phối màu ứng dụng", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("SYSTEM" to "Mặc định", "LIGHT" to "G.Diện Sáng", "DARK" to "G.Diện Tối").forEach { (mode, title) ->
                            val isSelected = appThemeMode == mode
                            Button(
                                onClick = { appThemeMode = mode },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                ),
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text(title, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }

                    // Presets Colors row
                    Text("Màu thương hiệu tùy chỉnh", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                    val colorPresets = listOf(
                        "#1E88E5" to Color(0xFF1E88E5), // Blue
                        "#2E7D32" to Color(0xFF2E7D32), // Green
                        "#008080" to Color(0xFF008080), // Teal
                        "#7B1FA2" to Color(0xFF7B1FA2), // Purple
                        "#C62828" to Color(0xFFC62828), // Ruby Red
                        "#EF6C00" to Color(0xFFEF6C00), // Orange
                        "#37474F" to Color(0xFF37474F),  // Slate
                        "#FFD600" to Color(0xFFFFD600), // Gold/Yellow
                        "#00C853" to Color(0xFF00C853), // Emerald Green
                        "#FF2C8C" to Color(0xFFFF2C8C), // Rose/Pink
                        "#607D8B" to Color(0xFF607D8B), // Slate Grey
                        "#FF4359,#FF9F43" to Color(0xFFFF4359), // Sunset Glow (Coral to Peach)
                        "#00C6FF,#0072FF" to Color(0xFF00C6FF), // Neon Blue (Cyan to Royal)
                        "#E200FF,#7B1FA2" to Color(0xFFE200FF), // Cyber Purple (Magenta to Purple)
                        "#11998E,#38EF7D" to Color(0xFF11998E), // Fresh Grass (Teal to Light Green)
                        "#8E2DE2,#4A00E0" to Color(0xFF8E2DE2)  // Deep Violet (Violet to Blue)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        colorPresets.forEach { (hex, previewColor) ->
                            val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                            val bkModifier = if (hex.contains(",")) {
                                val colors = hex.split(",").map {
                                    try {
                                        Color(android.graphics.Color.parseColor(it.trim()))
                                    } catch (e: Exception) {
                                        Color(0xFF1E88E5)
                                    }
                                }
                                Modifier.background(Brush.linearGradient(colors))
                            } else {
                                Modifier.background(previewColor)
                            }

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .then(bkModifier)
                                    .clickable { selectedColorHex = hex }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    // Font selection dropdown or buttons
                    Text("Phông chữ hiển thị", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Sử dụng mặc định", "Serif", "SansSerif", "Monospace").forEach { fName ->
                            val isSelected = selectedFontName == fName
                            Button(
                                onClick = { selectedFontName = fName },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
                                ),
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                            ) {
                                Text(fName.replace("Sử dụng ", ""), fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }

                    // Reset Theme button
                    OutlinedButton(
                        onClick = {
                            selectedColorHex = "#1E88E5"
                            selectedFontName = "Sử dụng mặc định"
                            appThemeMode = "SYSTEM"
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.4f)),
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Mặc Định", fontSize = 11.sp)
                    }
                }
            }
        }

        // SAVE BUTTON (Updates changes to DB)
        item {
            Button(
                onClick = {
                    pendingConfigAction = PendingActionData(
                        "Bạn có chắc muốn lưu toàn bộ thay đổi tài khoản, giao diện và cấu hình không?"
                    ) {
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
                            avatarOffsetY = avatarOffsetY,
                            occupation = occupation.trim().ifEmpty { "Công nhân" },
                            selectedColorHex = selectedColorHex,
                            selectedFontName = selectedFontName,
                            appThemeMode = appThemeMode,
                            monthActualSalaries = userConfig.monthActualSalaries
                        )
                        saveSuccessShow = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_settings_btn")
            ) {
                Icon(Icons.Filled.Save, contentDescription = "Lưu cấu hình")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lưu Thiết Lập & Thay Đổi")
            }
        }

        // 6. SAO LƯU & KHÔI PHỤC (Encrypted backup restoration using .vts layout)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("backup_restore_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Column {
                            Text(
                                "Sao Lưu & Khôi Phục Dữ Liệu",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Mã hóa bảo mật (.vts). Có thể sao lưu từng phần dữ liệu.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                showBackupOptionsDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("backup_data_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sao lưu", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                openDocumentLauncher.launch(arrayOf("*/*"))
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("restore_data_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Phục hồi", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // 7. THÔNG TIN ỨNG DỤNG (About intro card update version to 2.0)
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Phiên bản v2.7 • Ngô Thế Quân", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            }
        }
    }

    // ------------------- DYNAMIC POPUP DIALOGS -------------------

    // A. SYNC DIALOG
    if (showSyncDialog) {
        var syncFromStr by remember { mutableStateOf(syncFromDate) }
        var syncToStr by remember { mutableStateOf(syncToDate) }
        
        AlertDialog(
            onDismissRequest = { showSyncDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Sync, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Đồng Bộ Hàng Loạt", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Đồng bộ khoản: " + (if (syncTargetField == "MEAL") "Tiền ăn (${FormatHelper.formatVnd(syncTargetAmount)})" else "Lương ngày cơ sở (${FormatHelper.formatVnd(syncTargetAmount)})") + " vào các ngày công đã chốt.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Vui lòng chỉ định khoảng thời gian áp dụng tự động:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    TextField(
                        value = syncFromStr,
                        onValueChange = { syncFromStr = it },
                        label = { Text("Từ ngày (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    TextField(
                        value = syncToStr,
                        onValueChange = { syncToStr = it },
                        label = { Text("Đến ngày (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalFrom = syncFromStr.trim()
                        val finalTo = syncToStr.trim()
                        if (finalFrom.length == 10 && finalTo.length == 10) {
                            coroutineScope.launch {
                                val allLogs = viewModel.repository.getAllWorkLogsDirect()
                                val targetLogs = allLogs.filter { it.date in finalFrom..finalTo }
                                targetLogs.forEach { log ->
                                    if (syncTargetField == "MEAL") {
                                        viewModel.saveWorkLog(
                                            date = log.date,
                                            jobId1 = log.jobId1,
                                            ratio1 = log.ratio1,
                                            jobId2 = log.jobId2,
                                            ratio2 = log.ratio2,
                                            mealAllowance = syncTargetAmount,
                                            otherAmount = log.otherAmount,
                                            otherNote = log.otherNote,
                                            note = log.note
                                        )
                                    } else if (syncTargetField == "SALARY") {
                                        // Update day log customized properties if applicable, or we use month actual settlements
                                    }
                                }
                                showSyncDialog = false
                                syncSuccessShow = true
                            }
                        } else {
                            restoreErrorMsg = "Định dạng ngày không hợp lệ. Vui lòng nhập YYYY-MM-DD"
                        }
                    }
                ) {
                    Text("Đồng bộ ngay")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSyncDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (syncSuccessShow) {
        AlertDialog(
            onDismissRequest = { syncSuccessShow = false },
            title = { Text("Đồng Bộ Thành Công", fontWeight = FontWeight.Bold) },
            text = { Text("Toàn bộ các ngày chấm công trong khoảng thời gian chỉ định đã được đồng bộ với con số mới.") },
            confirmButton = {
                Button(onClick = { syncSuccessShow = false }) {
                    Text("Tuyệt vời")
                }
            }
        )
    }

    if (settlementSuccessShow) {
        AlertDialog(
            onDismissRequest = { settlementSuccessShow = false },
            title = { Text("Quyết Toán Hoàn Tất", fontWeight = FontWeight.Bold) },
            text = { 
                Text(
                    "Đã lưu quyết toán thực nhận! Đơn giá ngày công (1.0 tỷ lệ) của " +
                    "tháng ${settlementMonth} quy đổi tương ứng đạt: " +
                    "${FormatHelper.formatVnd(settlementCalculatedWage ?: 0.0)} / công.\n" +
                    "Hệ thống sẽ áp dụng con số quy đổi này cho toàn bộ báo cáo của tháng."
                )
            },
            confirmButton = {
                Button(onClick = { settlementSuccessShow = false }) {
                    Text("Xác nhận")
                }
            }
        )
    }

    // B. ADD JOB DIALOG
    if (showAddJobDialog) {
        JobEditorDialog(
            job = null,
            onDismiss = { showAddJobDialog = false },
            onSave = { name, rate, support ->
                pendingJobAction = PendingActionData(
                    "Bạn có muốn thêm công việc mới '${name}' với tỉ lệ công x${FormatHelper.formatRatio(rate)} và hỗ trợ khâu ${FormatHelper.formatVnd(support)} không?"
                ) {
                    viewModel.addJob(name, rate, support)
                    showAddJobDialog = false
                }
            }
        )
    }

    // C. EDIT JOB DIALOG
    if (jobToEdit != null) {
        JobEditorDialog(
            job = jobToEdit,
            onDismiss = { jobToEdit = null },
            onSave = { name, rate, support ->
                pendingJobAction = PendingActionData(
                    "Bạn có muốn điều chỉnh công việc '${jobToEdit!!.name}' thành '${name}' với tỉ lệ công x${FormatHelper.formatRatio(rate)} và hỗ trợ khâu ${FormatHelper.formatVnd(support)} không?"
                ) {
                    viewModel.updateJob(jobToEdit!!.copy(name = name, rate = rate, supportAmount = support))
                    jobToEdit = null
                }
            }
        )
    }

    // D. MONTHLY ALLOWANCE SUPPORT DIALOGS
    if (showAddSupportDialog) {
        AlertDialog(
            onDismissRequest = { showAddSupportDialog = false },
            title = { Text("Thêm Hỗ Trợ/Phụ Cấp Mới", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = newSupportName,
                        onValueChange = { newSupportName = it },
                        label = { Text("Tên khoản phụ cấp") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    TextField(
                        value = newSupportAmountStr,
                        onValueChange = { newSupportAmountStr = it },
                        label = { Text("Số tiền (VND, dùng dấu âm '-' để giảm trừ)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = newSupportAmountStr.toDoubleOrNull()
                        if (newSupportName.trim().isNotEmpty() && amt != null) {
                            viewModel.addMonthlySupport(newSupportName.trim(), amt)
                            newSupportName = ""
                            newSupportAmountStr = ""
                            showAddSupportDialog = false
                        }
                    }
                ) {
                    Text("Thêm mới")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSupportDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // CONFIRM ACTIONS DIALOGS
    if (pendingConfigAction != null) {
        AlertDialog(
            onDismissRequest = { pendingConfigAction = null },
            title = { Text("Xác Nhận Thay Đổi", fontWeight = FontWeight.Bold) },
            text = { Text(pendingConfigAction!!.text) },
            confirmButton = {
                Button(
                    onClick = {
                        pendingConfigAction!!.action()
                        pendingConfigAction = null
                    }
                ) {
                    Text("Đồng ý")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfigAction = null }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (pendingJobAction != null) {
        AlertDialog(
            onDismissRequest = { pendingJobAction = null },
            title = { Text("Xác Nhận Thao Tác", fontWeight = FontWeight.Bold) },
            text = { Text(pendingJobAction!!.text) },
            confirmButton = {
                Button(
                    onClick = {
                        pendingJobAction!!.action()
                        pendingJobAction = null
                    }
                ) {
                    Text("Xác nhận")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingJobAction = null }) {
                    Text("Hủy bỏ")
                }
            }
        )
    }

    if (showBackupOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showBackupOptionsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Backup,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Lựa Chọn Mục Sao Lưu", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Chọn các thành phần dữ liệu bạn muốn đóng gói và xuất thành tập tin sao lưu (.json) lưu trữ trên thiết bị:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { exportConfigChecked = !exportConfigChecked }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(checked = exportConfigChecked, onCheckedChange = { exportConfigChecked = it })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cấu hình & Cài đặt cá nhân", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { exportJobsChecked = !exportJobsChecked }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(checked = exportJobsChecked, onCheckedChange = { exportJobsChecked = it })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Danh mục công việc & Tỷ lệ lương", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { exportSupportsChecked = !exportSupportsChecked }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(checked = exportSupportsChecked, onCheckedChange = { exportSupportsChecked = it })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hỗ trợ & Phụ cấp cố định tháng", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { exportLogsChecked = !exportLogsChecked }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(checked = exportLogsChecked, onCheckedChange = { exportLogsChecked = it })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nhật ký chấm công chi tiết", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBackupOptionsDialog = false
                        createDocumentLauncher.launch("tuchamcong_backup_${System.currentTimeMillis()}.json")
                    },
                    enabled = exportConfigChecked || exportJobsChecked || exportSupportsChecked || exportLogsChecked
                ) {
                    Text("Tiến hành")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupOptionsDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showRestoreOptionsDialog) {
        AlertDialog(
            onDismissRequest = { 
                showRestoreOptionsDialog = false
                pendingRestoreJson = null
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Lựa Chọn Mục Khôi Phục", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Đã đọc xong file backup. Chọn các phần dữ liệu bạn muốn khôi phục vào ứng dụng của mình (việc khôi phục sẽ ghi đè và thay thế dữ liệu hiện tại):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { importConfigChecked = !importConfigChecked }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(checked = importConfigChecked, onCheckedChange = { importConfigChecked = it })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cấu hình & Cài đặt cá nhân", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { importJobsChecked = !importJobsChecked }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(checked = importJobsChecked, onCheckedChange = { importJobsChecked = it })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Danh mục công việc & Tỷ lệ lương", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { importSupportsChecked = !importSupportsChecked }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(checked = importSupportsChecked, onCheckedChange = { importSupportsChecked = it })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hỗ trợ & Phụ cấp cố định tháng", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { importLogsChecked = !importLogsChecked }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(checked = importLogsChecked, onCheckedChange = { importLogsChecked = it })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nhật ký chấm công chi tiết", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Text(
                        "Lưu ý: Thao tác khôi phục có tính ghi đè, vui lòng lưu trữ kĩ trước khi đồng ý.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val jsonStr = pendingRestoreJson
                        showRestoreOptionsDialog = false
                        if (jsonStr != null) {
                            coroutineScope.launch {
                                try {
                                    val success = viewModel.importBackup(
                                        jsonString = jsonStr,
                                        importConfig = importConfigChecked,
                                        importJobs = importJobsChecked,
                                        importSupports = importSupportsChecked,
                                        importLogs = importLogsChecked
                                    )
                                    if (success) {
                                        restoreSuccessShow = true
                                    } else {
                                        restoreErrorMsg = "Dự liệu lưu trữ không tương thích hoặc sai định dạng."
                                    }
                                } catch (e: Exception) {
                                    restoreErrorMsg = e.localizedMessage ?: "Lỗi khôi phục không xác định."
                                } finally {
                                    pendingRestoreJson = null
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = importConfigChecked || importJobsChecked || importSupportsChecked || importLogsChecked
                ) {
                    Text("Đồng ý khôi phục")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showRestoreOptionsDialog = false
                    pendingRestoreJson = null
                }) {
                    Text("Hủy")
                }
            }
        )
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

    if (backupSuccessShow) {
        AlertDialog(
            onDismissRequest = { backupSuccessShow = false },
            title = { Text("Sao Lưu Thành Công", fontWeight = FontWeight.Bold) },
            text = { Text("Dữ liệu được lựa chọn đã được sao lưu và xuất thành công ra file JSON trên thiết bị của bạn.") },
            confirmButton = {
                TextButton(onClick = { backupSuccessShow = false }) {
                    Text("Đồng ý")
                }
            }
        )
    }

    if (backupErrorMsg != null) {
        AlertDialog(
            onDismissRequest = { backupErrorMsg = null },
            title = { Text("Lỗi Sao Lưu", fontWeight = FontWeight.Bold) },
            text = { Text("Đã xảy ra lỗi trong quá trình tạo/ghi file backup: $backupErrorMsg") },
            confirmButton = {
                TextButton(onClick = { backupErrorMsg = null }) {
                    Text("Đóng")
                }
            }
        )
    }

    if (restoreSuccessShow) {
        AlertDialog(
            onDismissRequest = { restoreSuccessShow = false },
            title = { Text("Khôi Phục Thành Công", fontWeight = FontWeight.Bold) },
            text = { Text("Dữ liệu của bạn đã được khôi phục thành công từ file backup và sẵn sàng sử dụng.") },
            confirmButton = {
                TextButton(onClick = { restoreSuccessShow = false }) {
                    Text("Tuyệt vời")
                }
            }
        )
    }

    if (restoreErrorMsg != null) {
        AlertDialog(
            onDismissRequest = { restoreErrorMsg = null },
            title = { Text("Lỗi Khôi Phục", fontWeight = FontWeight.Bold) },
            text = { Text("Không thể tiến hành khôi phục dữ liệu: $restoreErrorMsg") },
            confirmButton = {
                TextButton(onClick = { restoreErrorMsg = null }) {
                    Text("Đóng")
                }
            }
        )
    }

    if (pendingConfigAction != null) {
        AlertDialog(
            onDismissRequest = { pendingConfigAction = null },
            title = { Text("Cảnh Báo Xác Nhận", fontWeight = FontWeight.Bold) },
            text = { Text(pendingConfigAction!!.text) },
            confirmButton = {
                Button(
                    onClick = {
                        pendingConfigAction!!.action()
                        pendingConfigAction = null
                    }
                ) {
                    Text("Đồng ý")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfigAction = null }) {
                    Text("Hủy bỏ")
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
                            pendingConfigAction = PendingActionData(
                                "Bạn có muốn thêm mới khoản hỗ trợ '${newSupportName.trim()}' trị giá ${FormatHelper.formatVnd(amount)} hàng tháng không?"
                            ) {
                                viewModel.addMonthlySupport(newSupportName.trim(), amount)
                                showAddSupportDialog = false
                                newSupportName = ""
                                newSupportAmountStr = ""
                            }
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

    val initialOtherItems = remember(activeLog) {
        val list = mutableListOf<CustomOtherItem>()
        val serialized = activeLog?.otherNote ?: ""
        val totalAmt = activeLog?.otherAmount ?: 0.0
        
        if (serialized.isNotBlank() && serialized.startsWith("[") && serialized.endsWith("]")) {
            try {
                val array = org.json.JSONArray(serialized)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(CustomOtherItem(obj.getString("name"), obj.getDouble("amount")))
                }
            } catch (e: Exception) {
                // fallback
            }
        }
        if (list.isEmpty() && (totalAmt != 0.0 || serialized.isNotBlank())) {
            list.add(CustomOtherItem(if (serialized.isBlank()) "Khác" else serialized, totalAmt))
        }
        list
    }

    var otherItemsList by remember { mutableStateOf(initialOtherItems.toList()) }
    var draftItemName by remember { mutableStateOf("") }
    var draftItemAmountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf(activeLog?.note ?: "") }

    var showJob1Dropdown by remember { mutableStateOf(false) }
    var showJob2Dropdown by remember { mutableStateOf(false) }

    var errorMsg by remember { mutableStateOf("") }
    var pendingLogAction by remember { mutableStateOf<PendingActionData?>(null) }

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
                            Text("Thêm công việc thứ 2", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
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

                // --- CUSTOM OTHER ITEMS LIST ---
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            "Các khoản tùy chỉnh khác (Thưởng/Phạt...)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        
                        // Existing customized items
                        if (otherItemsList.isEmpty()) {
                            Text(
                                "Chưa có khoản tùy chỉnh khác nào cho ngày này.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            otherItemsList.forEachIndexed { idx, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.0f)) {
                                        Text(item.name, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = FormatHelper.formatVnd(item.amount),
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (item.amount >= 0) MaterialTheme.colorScheme.primary else Color.Red
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            otherItemsList = otherItemsList.filterIndexed { i, _ -> i != idx }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Filled.Delete, "Xóa", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                                    }
                                }
                                if (idx < otherItemsList.size - 1) {
                                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                                }
                            }
                        }
                        
                        Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                        
                        // Inputs to add new other items
                        Text("Thêm khoản tiền mới", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = draftItemName,
                                onValueChange = { draftItemName = it },
                                label = { Text("Tên khoản") },
                                placeholder = { Text("Thưởng, phạt...") },
                                modifier = Modifier.weight(1.1f),
                                singleLine = true
                            )
                            
                            OutlinedTextField(
                                value = draftItemAmountStr,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input == "-" || input.toDoubleOrNull() != null) {
                                        draftItemAmountStr = input
                                    }
                                },
                                label = { Text("Số tiền") },
                                placeholder = { Text("+/- VND") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                modifier = Modifier.weight(0.9f),
                                singleLine = true
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = {
                                val amt = draftItemAmountStr.toDoubleOrNull() ?: 0.0
                                if (draftItemName.isNotBlank() && amt != 0.0) {
                                    otherItemsList = otherItemsList + CustomOtherItem(draftItemName.trim(), amt)
                                    draftItemName = ""
                                    draftItemAmountStr = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Add, "Thêm mục", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Thêm vào danh sách", fontSize = 12.sp)
                        }
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
                                onClick = {
                                    pendingLogAction = PendingActionData(
                                        "Bạn có chắc chắn muốn xóa toàn bộ chấm công ngày $dateDisplay này?"
                                    ) {
                                        onDelete()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .testTag("delete_log_confirm")
                            ) {
                                Icon(Icons.Filled.Delete, "Xóa")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Xóa", fontSize = 13.sp)
                            }
                        }

                        Button(
                            onClick = {
                                val r1 = ratio1Str.toDoubleOrNull()
                                val r2 = if (hasSecondJob) ratio2Str.toDoubleOrNull() else 0.0
                                val meal = mealStr.toDoubleOrNull() ?: 20000.0

                                if (r1 == null || r2 == null) {
                                    errorMsg = "Hệ số công việc không đúng lượng số."
                                } else if (jobId1 == null) {
                                    errorMsg = "Vui lòng chọn công việc chính."
                                } else if (hasSecondJob && jobId2 == null) {
                                    errorMsg = "Vui lòng chọn công việc phụ."
                                } else {
                                    pendingLogAction = PendingActionData(
                                        "Bạn có chắc chắn muốn lưu lại chấm công ngày $dateDisplay này không?"
                                    ) {
                                        val totalAmt = otherItemsList.sumOf { it.amount }
                                        val serializedNoteObj = org.json.JSONArray().apply {
                                            otherItemsList.forEach { item ->
                                                put(org.json.JSONObject().apply {
                                                    put("name", item.name)
                                                    put("amount", item.amount)
                                                })
                                            }
                                        }.toString()

                                        onSave(
                                            jobId1,
                                            r1,
                                            if (hasSecondJob) jobId2 else null,
                                            r2,
                                            meal,
                                            totalAmt,
                                            serializedNoteObj,
                                            note
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1.1f)
                                .testTag("save_log_confirm")
                        ) {
                            Icon(Icons.Filled.Check, "Lưu")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lưu", fontSize = 13.sp)
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

    if (pendingLogAction != null) {
        AlertDialog(
            onDismissRequest = { pendingLogAction = null },
            title = { Text("Cảnh Báo Xác Nhận", fontWeight = FontWeight.Bold) },
            text = { Text(pendingLogAction!!.text) },
            confirmButton = {
                Button(
                    onClick = {
                        pendingLogAction!!.action()
                        pendingLogAction = null
                    }
                ) {
                    Text("Đồng ý")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingLogAction = null }) {
                    Text("Hủy bỏ")
                }
            }
        )
    }
}

@Composable
fun MonthYearPickerDialog(
    initialYear: Int,
    initialMonth: Int,
    onDismiss: () -> Unit,
    onSelected: (Int, Int) -> Unit
) {
    var selectedYear by remember { mutableStateOf(initialYear) }
    var selectedMonth by remember { mutableStateOf(initialMonth) }
    var typedYear by remember { mutableStateOf(initialYear.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chọn thời gian", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Year Selection Row with +/- buttons and manual typing
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            selectedYear = (selectedYear - 1).coerceAtLeast(1900)
                            typedYear = selectedYear.toString()
                        }
                    ) {
                        Icon(Icons.Filled.ChevronLeft, "Giảm năm")
                    }

                    OutlinedTextField(
                        value = typedYear,
                        onValueChange = { newValue: String ->
                            var isAllDigits = true
                            for (i in 0 until newValue.length) {
                                if (!newValue[i].isDigit()) {
                                    isAllDigits = false
                                }
                            }
                            if (newValue.length <= 4 && isAllDigits) {
                                typedYear = newValue
                                val parsed = newValue.toIntOrNull()
                                if (parsed != null && parsed in 1900..2100) {
                                    selectedYear = parsed
                                }
                            }
                        },
                        label = { Text("Năm") },
                        modifier = Modifier.width(100.dp),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    IconButton(
                        onClick = {
                            selectedYear = (selectedYear + 1).coerceAtMost(2100)
                            typedYear = selectedYear.toString()
                        }
                    ) {
                        Icon(Icons.Filled.ChevronRight, "Tăng năm")
                    }
                }

                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                val yearsRange = remember { (currentYear - 50)..(currentYear + 50) }
                val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()

                LaunchedEffect(selectedYear) {
                    val index = yearsRange.indexOf(selectedYear)
                    if (index >= 0) {
                        lazyListState.scrollToItem((index - 2).coerceAtLeast(0))
                    }
                }

                androidx.compose.foundation.lazy.LazyRow(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(yearsRange.toList()) { y ->
                        val isYearSelected = y == selectedYear
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isYearSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    selectedYear = y
                                    typedYear = y.toString()
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = y.toString(),
                                fontSize = 14.sp,
                                fontWeight = if (isYearSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isYearSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                // Months selection grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0..3) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (col in 1..3) {
                                val m = row * 3 + col
                                val isSelected = m == selectedMonth
                                Button(
                                    onClick = { selectedMonth = m },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Th. $m", fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSelected(selectedYear, selectedMonth)
                }
            ) {
                Text("Đồng ý")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy bỏ")
            }
        }
    )
}
