package io.github.chalexey.cashpet.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.chalexey.cashpet.app.ui.theme.CoinYellow
import io.github.chalexey.cashpet.core.model.PetMood

@Composable
fun CashPetBottomBar(current: Any, onNavigate: (Any) -> Unit) {
    // This overload is intentionally kept generic; AppNavigation owns the concrete route enum.
    // The actual bar is rendered by the route adapter below.
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding().background(MaterialTheme.colorScheme.surface),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf("⌂" to "Дом", "☷" to "План", "▣" to "Магазин", "🐷" to "Копилка", "✦" to "Задания").forEach { (icon, label) ->
            ColumnItem(icon, label)
        }
    }
}

@Composable
private fun ColumnItem(icon: String, label: String) {
    androidx.compose.foundation.layout.Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 8.dp),
    ) {
        Text(icon, fontSize = 21.sp)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun FeedbackBar(text: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CoinYellow.copy(alpha = .14f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("✓", fontSize = 24.sp)
            Text(text, modifier = Modifier.weight(1f).padding(horizontal = 10.dp), style = MaterialTheme.typography.bodyMedium)
            Text("Понятно", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable(onClick = onDismiss))
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge)
}

@Composable
fun StatChip(icon: String, label: String, value: Int) {
    androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 20.sp)
        Text("$value", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
