package com.monnhuitne.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monnhuitne.app.ui.theme.Success
import com.monnhuitne.app.ui.theme.ErrorColor
import com.monnhuitne.app.ui.theme.TextMuted
import com.monnhuitne.app.ui.theme.Warning

@Composable
fun StatusBadge(status: String) {
    val (label, color) = when (status) {
        "success" -> "Succès" to Success
        "error" -> "Échec" to ErrorColor
        "running" -> "En cours" to Warning
        "waiting" -> "En attente" to Warning
        "inactive" -> "Inactif" to TextMuted
        else -> status to TextMuted
    }
    Text(
        text = label,
        color = color,
        fontSize = 12.sp,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
