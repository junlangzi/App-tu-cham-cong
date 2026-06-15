package com.example.ui

import androidx.compose.ui.graphics.Color

data class AvatarItem(
    val name: String,
    val emoji: String,
    val label: String,
    val backgroundColor: Color
)

object AvatarHelper {
    val avatars = listOf(
        AvatarItem("AVATAR_1", "👷‍♂️", "Thợ Căng Tràn", Color(0xFFFF9800)),
        AvatarItem("AVATAR_2", "👨‍💻", "Kỹ Thuật Viên", Color(0xFF2196F3)),
        AvatarItem("AVATAR_3", "👩‍💼", "Nhân Viên Văn Phòng", Color(0xFF9C27B0)),
        AvatarItem("AVATAR_4", "👨‍🍳", "Hỗ Trợ / Bếp", Color(0xFFE91E63)),
        AvatarItem("AVATAR_5", "🦸‍♂️", "Chiến Binh Đi Làm", Color(0xFF00BCD4)),
        AvatarItem("AVATAR_6", "👩‍⚕️", "Chuyên Viên An Toàn", Color(0xFF4CAF50)),
        AvatarItem("AVATAR_7", "🐯", "Cọp Chăm Chỉ", Color(0xFFFF5722)),
        AvatarItem("AVATAR_8", "🚀", "Tăng Tốc Làm Việc", Color(0xFF673AB7))
    )

    fun getAvatar(name: String): AvatarItem {
        return avatars.find { it.name == name } ?: avatars[0]
    }
}
