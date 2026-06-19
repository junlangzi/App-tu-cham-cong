package com.example.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.platform.testTag
import coil.compose.AsyncImage
import com.example.data.UserConfig
import com.example.data.Job
import com.example.data.MonthlySupport
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

@Composable
fun SettingMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(iconColor.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SettingsScreen(
    viewModel: AttendanceViewModel,
    userConfig: UserConfig,
    onBackToHome: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var activeSettingSection by remember { mutableStateOf<String?>(null) }

    // State bindings utilizing userConfig as refresh key
    var name by remember(userConfig) { mutableStateOf(userConfig.fullName) }
    var occupation by remember(userConfig) { mutableStateOf(userConfig.occupation) }
    var dailySalaryStr by remember(userConfig) { mutableStateOf(userConfig.dailySalary.toInt().toString()) }
    var defaultMealStr by remember(userConfig) { mutableStateOf(userConfig.dailyMealAllowance.toInt().toString()) }
    var selectedAvatarName by remember(userConfig) { mutableStateOf(userConfig.avatarName) }

    var pendingConfigAction by remember { mutableStateOf<PendingActionData?>(null) }
    var pendingJobAction by remember { mutableStateOf<PendingActionData?>(null) }

    var avatarUri by remember(userConfig) { mutableStateOf(userConfig.avatarUri) }
    var avatarScale by remember(userConfig) { mutableStateOf(userConfig.avatarScale) }
    var avatarOffsetX by remember(userConfig) { mutableStateOf(userConfig.avatarOffsetX) }
    var avatarOffsetY by remember(userConfig) { mutableStateOf(userConfig.avatarOffsetY) }
    var isLibraryMode by remember(userConfig) { mutableStateOf(userConfig.avatarUri != null) }

    var selectedColorHex by remember(userConfig) { mutableStateOf(userConfig.selectedColorHex) }
    var selectedFontName by remember(userConfig) { mutableStateOf(userConfig.selectedFontName) }
    var appThemeMode by remember(userConfig) { mutableStateOf(userConfig.appThemeMode) }

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

    // Salary settlement states (used for Advanced Quyết toán)
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1
    var settlementMonth by remember { mutableStateOf(String.format(Locale.US, "%d-%02d", year, month)) }
    var settlementTotalReceivedStr by remember { mutableStateOf("") }
    var settlementCalculatedWage by remember { mutableStateOf<Double?>(null) }
    var settlementSuccessShow by remember { mutableStateOf(false) }

    // Bulk Synchronize states
    var showSyncDialog by remember { mutableStateOf(false) }
    val dayMax = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    var syncFromStr by remember { mutableStateOf(String.format(Locale.US, "%d-%02d-01", year, month)) }
    var syncToStr by remember { mutableStateOf(String.format(Locale.US, "%d-%02d-%02d", year, month, dayMax)) }
    var syncTargetField by remember { mutableStateOf("MEAL") } // "MEAL" or "SALARY"
    var syncTargetAmount by remember { mutableStateOf(0.0) }
    var syncSuccessShow by remember { mutableStateOf(false) }

    // Backup restore states
    var exportConfigChecked by remember { mutableStateOf(true) }
    var exportJobsChecked by remember { mutableStateOf(true) }
    var exportSupportsChecked by remember { mutableStateOf(true) }
    var exportLogsChecked by remember { mutableStateOf(true) }

    var importConfigChecked by remember { mutableStateOf(true) }
    var importJobsChecked by remember { mutableStateOf(true) }
    var importSupportsChecked by remember { mutableStateOf(true) }
    var importLogsChecked by remember { mutableStateOf(true) }

    var showBackupOptionsDialog by remember { mutableStateOf(false) }
    var showRestoreOptionsDialog by remember { mutableStateOf(false) }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }
    var backupSuccessShow by remember { mutableStateOf(false) }
    var backupErrorMsg by remember { mutableStateOf<String?>(null) }
    var restoreSuccessShow by remember { mutableStateOf(false) }
    var restoreErrorMsg by remember { mutableStateOf<String?>(null) }
    var saveSuccessShow by remember { mutableStateOf(false) }

    // Job sub-states
    var showAddJobDialog by remember { mutableStateOf(false) }
    var jobToEdit by remember { mutableStateOf<Job?>(null) }

    // Monthly support sub-states
    var showAddSupportDialog by remember { mutableStateOf(false) }
    var newSupportName by remember { mutableStateOf("") }
    var newSupportAmountStr by remember { mutableStateOf("") }

    // Document Launchers for backup/restore
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*"),
        onResult = { uri ->
            if (uri != null) {
                coroutineScope.launch {
                    try {
                        val json = viewModel.exportBackup(
                            exportConfig = exportConfigChecked,
                            exportJobs = exportJobsChecked,
                            exportSupports = exportSupportsChecked,
                            exportLogs = exportLogsChecked
                        )
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(json.toByteArray(Charsets.UTF_8))
                        }
                        backupSuccessShow = true
                    } catch (e: Exception) {
                        backupErrorMsg = e.localizedMessage
                    }
                }
            }
        }
    )

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bytes = inputStream.readBytes()
                        pendingRestoreJson = String(bytes, Charsets.UTF_8)
                        showRestoreOptionsDialog = true
                    }
                } catch (e: Exception) {
                    restoreErrorMsg = "Không thể đọc file: ${e.localizedMessage}"
                }
            }
        }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen_lazy_col"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (activeSettingSection == null) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                ) {
                    IconButton(onClick = { onBackToHome() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Quay về trang chủ")
                    }
                    Text(
                        "Cài Đặt Hệ Thống",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SettingMenuItem(
                            icon = Icons.Filled.Person,
                            title = "Cá nhân & Tài khoản",
                            description = "Cập nhật tên, ảnh đại diện và chức vụ",
                            iconColor = Color(0xFF1E88E5),
                            onClick = { activeSettingSection = "account" }
                        )
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 14.dp))
                        
                        SettingMenuItem(
                            icon = Icons.Filled.AccountBalanceWallet,
                            title = "Lương & Trợ cấp cơ bản",
                            description = "Sửa mức lương ngày công, tiền ăn trưa mặc định",
                            iconColor = Color(0xFF4CAF50),
                            onClick = { activeSettingSection = "salary" }
                        )
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 14.dp))
                        
                        SettingMenuItem(
                            icon = Icons.Filled.Assignment,
                            title = "Danh mục công việc & Hệ số tỷ lệ",
                            description = "Thêm, sửa, xóa việc chấm công, tỷ lệ công, hỗ trợ khâu",
                            iconColor = Color(0xFF8E24AA),
                            onClick = { activeSettingSection = "jobs" }
                        )
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 14.dp))
                        
                        SettingMenuItem(
                            icon = Icons.Filled.Stars,
                            title = "Phụ cấp & Hỗ trợ tháng",
                            description = "Thiết lập xăng xe, điện thoại cố định hàng tháng",
                            iconColor = Color(0xFFFF9800),
                            onClick = { activeSettingSection = "monthly_supports" }
                        )
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 14.dp))
                        
                        SettingMenuItem(
                            icon = Icons.Filled.Palette,
                            title = "Giao diện & Phông chữ",
                            description = "Chế độ sáng/tối, màu sắc chủ đề và kiểu phông hiển thị",
                            iconColor = Color(0xFFE91E63),
                            onClick = { activeSettingSection = "theme" }
                        )
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 14.dp))
                        
                        SettingMenuItem(
                            icon = Icons.Filled.Build,
                            title = "Công cụ & Tiện ích nâng cao",
                            description = "Quyết toán lương, Đồng bộ chấm công, Sao lưu dữ liệu",
                            iconColor = Color(0xFF795548),
                            onClick = { activeSettingSection = "utilities" }
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
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
                            "Được thiết kế riêng để tối ưu hóa quản lý ngày làm công, trợ cấp xăng xe điện thoại, và tự động quyết toán tiền thực lĩnh cực kỳ thông thái.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Phiên bản v2.0 • Ngô Thế Quân", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
        else if (activeSettingSection == "account") {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    IconButton(onClick = { activeSettingSection = null }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                    Text(
                        "Cá nhân & Tài khoản",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
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
                            modifier = Modifier.fillMaxWidth().testTag("settings_name_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = occupation,
                            onValueChange = { occupation = it },
                            label = { Text("Nghề nghiệp / Chức vụ") },
                            modifier = Modifier.fillMaxWidth().testTag("settings_occupation_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Ảnh đại diện (Avatar)",
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

                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Kích cỡ (Thu phóng): ${String.format(Locale.US, "%.1f", avatarScale)}x", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

            item {
                Button(
                    onClick = {
                        val salary = dailySalaryStr.toDoubleOrNull() ?: userConfig.dailySalary
                        val meal = defaultMealStr.toDoubleOrNull() ?: userConfig.dailyMealAllowance
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
                        activeSettingSection = null
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_account_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Check, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lưu cá nhân & Trở về", fontWeight = FontWeight.Bold)
                }
            }
        }
        else if (activeSettingSection == "salary") {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    IconButton(onClick = { activeSettingSection = null }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                    Text(
                        "Lương & Trợ cấp cơ bản",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Lương ngày công cơ sở (VND)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = dailySalaryStr,
                                    onValueChange = { dailySalaryStr = it },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Tiền hỗ trợ ăn mặc định hàng ngày (VND)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = defaultMealStr,
                                    onValueChange = { defaultMealStr = it },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val salary = dailySalaryStr.toDoubleOrNull() ?: userConfig.dailySalary
                        val meal = defaultMealStr.toDoubleOrNull() ?: userConfig.dailyMealAllowance
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
                        activeSettingSection = null
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_salary_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Check, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lưu mức lương & Trở về", fontWeight = FontWeight.Bold)
                }
            }
        }
        else if (activeSettingSection == "jobs") {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { activeSettingSection = null }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Quay lại")
                        }
                        Text(
                            "Danh mục công việc",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
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
            }

            if (allJobs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                    ) {
                        Text(
                            "Chưa có công việc nào trong danh mục.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(allJobs) { job ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
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
                                            fontSize = 9.sp, 
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Hệ số: x${FormatHelper.formatRatio(job.rate)} công • Trợ cấp: ${FormatHelper.formatVnd(job.supportAmount)}",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
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

            item {
                Button(
                    onClick = { activeSettingSection = null },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Hoàn tất thiết lập công việc", fontWeight = FontWeight.Bold)
                }
            }
        }
        else if (activeSettingSection == "monthly_supports") {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { activeSettingSection = null }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Quay lại")
                        }
                        Text(
                            "Phụ cấp cố định tháng",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showAddSupportDialog = true }) {
                        Icon(Icons.Filled.AddCircle, "Thêm", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    }
                }
            }

            if (allMonthlySupports.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                    ) {
                        Text(
                            "Không tìm thấy phụ cấp cố định nào. Sử dụng nút '+' ở trên để tạo mới xăng xe, điện thoại...",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(allMonthlySupports) { support ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(support.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(2.dp))
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

            item {
                Button(
                    onClick = { activeSettingSection = null },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Hoàn tất thiết lập phụ cấp", fontWeight = FontWeight.Bold)
                }
            }
        }
        else if (activeSettingSection == "theme") {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    IconButton(onClick = { activeSettingSection = null }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                    Text(
                        "Giao diện & Phông chữ",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
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
                                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Text(title, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }

                        Text("Màu thương hiệu chủ đạo", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        val colorPresets = listOf(
                            "#1E88E5" to Color(0xFF1E88E5), // Blue
                            "#2E7D32" to Color(0xFF2E7D32), // Green
                            "#008080" to Color(0xFF008080), // Teal
                            "#7B1FA2" to Color(0xFF7B1FA2), // Purple
                            "#C62828" to Color(0xFFC62828), // Ruby Red
                            "#EF6C00" to Color(0xFFEF6C00), // Orange
                            "#37474F" to Color(0xFF37474F)  // Slate
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            colorPresets.forEach { (colorHex, previewColor) ->
                                val isChosen = selectedColorHex.equals(colorHex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(previewColor)
                                        .border(
                                            width = if (isChosen) 3.dp else 1.dp,
                                            color = if (isChosen) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColorHex = colorHex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isChosen) {
                                        Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        Text("Kiểu chữ hiển thị", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        val fontOptionPairs = listOf(
                            "Sử dụng mặc định" to "Mặc định (System)",
                            "Serif" to "Có chân (Serif)",
                            "SansSerif" to "Không chân (Sans)",
                            "Monospace" to "Độ rộng đều (Mono)"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            fontOptionPairs.forEach { (fontKey, fontLabel) ->
                                val isSelected = selectedFontName == fontKey
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedFontName = fontKey },
                                    label = { Text(fontLabel, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val salary = dailySalaryStr.toDoubleOrNull() ?: userConfig.dailySalary
                        val meal = defaultMealStr.toDoubleOrNull() ?: userConfig.dailyMealAllowance
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
                        activeSettingSection = null
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_theme_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Check, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lưu cấu hình & Trở về", fontWeight = FontWeight.Bold)
                }
            }
        }
        else if (activeSettingSection == "utilities") {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    IconButton(onClick = { activeSettingSection = null }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                    Text(
                        "Công cụ & Tiện ích",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Quyết toán lương
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
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
                                text = "Quyết toán lương thực tế nhận",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Tính toán bình quân lương ngày công từ tổng thực lĩnh",
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
                            OutlinedTextField(
                                value = settlementMonth,
                                onValueChange = { settlementMonth = it },
                                label = { Text("Tháng quyết toán (YYYY-MM)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            IconButton(
                                onClick = {
                                    settlementMonth = String.format(Locale.US, "%d-%02d", year, month)
                                },
                                modifier = Modifier.background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(Icons.Filled.DateRange, "Tháng hiện tại", tint = MaterialTheme.colorScheme.secondary)
                            }
                        }

                        OutlinedTextField(
                            value = settlementTotalReceivedStr,
                            onValueChange = { settlementTotalReceivedStr = it },
                            label = { Text("Tổng số tiền mặt nhận được (VND)") },
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
                                            restoreErrorMsg = "Tháng này chưa chấm ngày làm công nào để tính quyết toán."
                                        }
                                    }
                                } else {
                                    restoreErrorMsg = "Vui lòng nhập số tiền thực lĩnh hợp lệ và tháng YYYY-MM."
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("settlement_btn")
                        ) {
                            Icon(Icons.Filled.Calculate, "Tính toán")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Quyết toán & Cập nhật")
                        }

                        val currentSettleRate = getDailySalaryForMonth(userConfig, settlementMonth)
                        if (currentSettleRate != userConfig.dailySalary) {
                            Text(
                                "✓ Tháng ${settlementMonth} đang áp dụng đơn giá quyết toán: ${FormatHelper.formatVnd(currentSettleRate)}/công.",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Sao lưu dữ liệu
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
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
                                    "Xuất lưu file offline dưới dạng tập tin JSON an toàn (.json)",
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
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp).testTag("backup_btn"),
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
                                    .height(44.dp).testTag("restore_btn"),
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

            item {
                Button(
                    onClick = { activeSettingSection = null },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Quay về thiết lập chung", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Modal dialogs setup supporting modular section triggers
    if (showSyncDialog) {
        AlertDialog(
            onDismissRequest = { showSyncDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Sync, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Đồng Bộ Số Liệu Hàng Loạt", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Đồng bộ ${if (syncTargetField == "MEAL") "tiền ăn" else "lương cơ sở ngày công"} trị giá: ${FormatHelper.formatVnd(syncTargetAmount)} vào toàn bộ ngày ghi chép trong khoảng thời gian:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = syncFromStr,
                        onValueChange = { syncFromStr = it },
                        label = { Text("Từ ngày (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
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
                                        // Standard parameters matching
                                    }
                                }
                                showSyncDialog = false
                                syncSuccessShow = true
                            }
                        } else {
                            restoreErrorMsg = "Định dạng ngày không hợp lệ. Vui lòng ghi rõ YYYY-MM-DD"
                        }
                    }
                ) {
                    Text("Đồng bộ")
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
            text = { Text("Con số hỗ trợ ăn đã được cập nhật thành công cho toàn bộ các ngày làm việc trong phạm vi đã chọn.") },
            confirmButton = {
                Button(onClick = { syncSuccessShow = false }) {
                    Text("Xác nhận")
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
                    Text("Tùy chỉnh các phụ cấp nhận thêm cố định hàng tháng (Xăng xe, ăn ca, chuyên cần...)", style = MaterialTheme.typography.bodySmall)
                    
                    OutlinedTextField(
                        value = newSupportName,
                        onValueChange = { newSupportName = it },
                        label = { Text("Tên hỗ trợ") },
                        placeholder = { Text("Ví dụ: Tiền xăng điện thoại, Quỹ bảo hiểm...") },
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
                        label = { Text("Số tiền (VND, dùng dấu âm '-' để trừ xăng xe...)") },
                        placeholder = { Text("Ví dụ: 300000 hoặc -150000") },
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

    if (showBackupOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showBackupOptionsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Backup, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Lọc Mục Sao Lưu", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Chọn các thành phần dữ liệu bạn muốn sao lưu thành file JSON trữ trên bộ thiết bị:", style = MaterialTheme.typography.bodySmall)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { exportConfigChecked = !exportConfigChecked }.padding(vertical = 4.dp)) {
                            Checkbox(checked = exportConfigChecked, onCheckedChange = { exportConfigChecked = it })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cấu hình & Cài đặt cá nhân", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { exportJobsChecked = !exportJobsChecked }.padding(vertical = 4.dp)) {
                            Checkbox(checked = exportJobsChecked, onCheckedChange = { exportJobsChecked = it })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Danh mục công việc", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { exportSupportsChecked = !exportSupportsChecked }.padding(vertical = 4.dp)) {
                            Checkbox(checked = exportSupportsChecked, onCheckedChange = { exportSupportsChecked = it })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Phụ cấp cố định tháng", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { exportLogsChecked = !exportLogsChecked }.padding(vertical = 4.dp)) {
                            Checkbox(checked = exportLogsChecked, onCheckedChange = { exportLogsChecked = it })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nhật ký chấm công", style = MaterialTheme.typography.bodyMedium)
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
                    Text("Đồng ý")
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.CloudDownload, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Mục Khôi Phục", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Chọn phần dữ liệu bạn muốn nén khôi phục (sẽ ghi đè và thay đổi số liệu hiện tại):", style = MaterialTheme.typography.bodySmall)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { importConfigChecked = !importConfigChecked }.padding(vertical = 4.dp)) {
                            Checkbox(checked = importConfigChecked, onCheckedChange = { importConfigChecked = it })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cấu hình & Cài đặt cá nhân", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { importJobsChecked = !importJobsChecked }.padding(vertical = 4.dp)) {
                            Checkbox(checked = importJobsChecked, onCheckedChange = { importJobsChecked = it })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Danh mục công việc", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { importSupportsChecked = !importSupportsChecked }.padding(vertical = 4.dp)) {
                            Checkbox(checked = importSupportsChecked, onCheckedChange = { importSupportsChecked = it })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Phụ cấp cố định tháng", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { importLogsChecked = !importLogsChecked }.padding(vertical = 4.dp)) {
                            Checkbox(checked = importLogsChecked, onCheckedChange = { importLogsChecked = it })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nhật ký chấm công", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Text("Lưu ý: Thao tác sẽ ghi đè có tính vĩnh viễn.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
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
                                        restoreErrorMsg = "Định dạng file không tương thích."
                                    }
                                } catch (e: Exception) {
                                    restoreErrorMsg = e.localizedMessage
                                } finally {
                                    pendingRestoreJson = null
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = importConfigChecked || importJobsChecked || importSupportsChecked || importLogsChecked
                ) {
                    Text("Khôi phục ngay")
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
            text = { Text("Thông tin cá nhân, định mức lương ngày công và thiết lập giao diện đã được lưu trữ thành công.") },
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
            text = { Text("Dữ liệu của bạn đã được xuất thành file JSON và lưu lại an toàn.") },
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
            text = { Text("Gặp sự cố không thể tạo file: $backupErrorMsg") },
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
            text = { Text("Mọi dữ liệu lựa chọn đã được nạp phục hồi thành công từ file backup và sẵn sàng sử dụng.") },
            confirmButton = {
                TextButton(onClick = { restoreSuccessShow = false }) {
                    Text("Xác nhận")
                }
            }
        )
    }

    if (restoreErrorMsg != null) {
        AlertDialog(
            onDismissRequest = { restoreErrorMsg = null },
            title = { Text("Lỗi Thao Tác", fontWeight = FontWeight.Bold) },
            text = { Text("Gặp sự cố: $restoreErrorMsg") },
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
            title = { Text("Xác Nhận Hành Động", fontWeight = FontWeight.Bold) },
            text = { Text(pendingConfigAction!!.text) },
            confirmButton = {
                Button(
                    onClick = {
                        pendingConfigAction!!.action()
                        pendingConfigAction = null
                    }
                ) {
                    Text("Xác nhận")
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
                    Text("Đồng ý")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingJobAction = null }) {
                    Text("Hủy")
                }
            }
        )
    }
}
