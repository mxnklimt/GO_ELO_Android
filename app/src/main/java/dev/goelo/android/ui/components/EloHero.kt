package dev.goelo.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable fun EloHero(name: String, elo: Double, games: Int) = Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
    Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(String.format(Locale.US, "%.1f", elo), style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
    Text(if (games == 0) "记下第一盘，开始积累你的棋力轨迹" else "已记录 $games 盘", style = MaterialTheme.typography.bodyMedium)
}
