package com.slocator.fleetdriver.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slocator.fleetdriver.R
import com.slocator.fleetdriver.data.RoutePart
import com.slocator.fleetdriver.data.ScheduledDay
import com.slocator.fleetdriver.ui.components.PartButton
import com.slocator.fleetdriver.ui.theme.BrandEmerald
import com.slocator.fleetdriver.ui.theme.BrandPurpleDim
import com.slocator.fleetdriver.ui.theme.BrandPurpleLight
import com.slocator.fleetdriver.ui.theme.ObsidianCard
import com.slocator.fleetdriver.ui.theme.TextSecondary
import java.time.format.DateTimeFormatter
import java.util.Locale

data class RoutesUiState(
    val driverId: String,
    val day: ScheduledDay?,
    val parts: List<RoutePart>,
    val isRefreshing: Boolean,
    val errorBanner: String?,
    val isPartDone: (RoutePart) -> Boolean,
    val onTogglePart: (RoutePart, Boolean) -> Unit,
    val onOpenRoute: (RoutePart) -> Unit,
    val onRefresh: () -> Unit,
    val onLogout: () -> Unit
)

@Composable
fun RoutesScreen(state: RoutesUiState) {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
        ?: Locale.getDefault()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HeaderBar(
                driverId = state.driverId,
                isRefreshing = state.isRefreshing,
                onRefresh = state.onRefresh,
                onLogout = state.onLogout
            )

            DateHeadline(day = state.day, locale = locale)

            if (state.errorBanner != null) {
                ErrorBanner(text = state.errorBanner)
            }

            ProgressStrip(
                done = state.parts.count(state.isPartDone),
                total = state.parts.size
            )

            if (state.parts.isEmpty()) {
                EmptyState(text = stringResource(R.string.routes_no_today))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = 8.dp, bottom = 32.dp
                    )
                ) {
                    items(state.parts, key = { it.partNumber }) { part ->
                        PartButton(
                            partNumber = part.partNumber,
                            stopCount = part.stopCount,
                            isDone = state.isPartDone(part),
                            onCheckedChange = { state.onTogglePart(part, it) },
                            onOpenRoute = { state.onOpenRoute(part) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderBar(
    driverId: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(BrandPurpleLight, BrandEmerald))
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = driverId.firstOrNull()?.uppercase() ?: "•",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_brand_short),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                color = TextSecondary
            )
            Text(
                text = driverId,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        IconButton(onClick = onRefresh, enabled = !isRefreshing) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    color = BrandEmerald, strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.routes_refresh),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        IconButton(onClick = onLogout) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = stringResource(R.string.routes_logout),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun DateHeadline(day: ScheduledDay?, locale: Locale) {
    val date = day?.date
    val (big, small) = when {
        date != null -> {
            // Big = "28 April"  /  Small = "Tuesday • 2026"
            val dayMonthFmt = DateTimeFormatter.ofPattern("d MMMM", locale)
            val dowYearFmt = DateTimeFormatter.ofPattern("EEEE • yyyy", locale)
            date.format(dayMonthFmt) to date.format(dowYearFmt)
        }
        day != null -> day.dayLabel to ""
        else -> stringResource(R.string.routes_today_label) to ""
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.routes_today_label),
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp),
            color = BrandEmerald
        )
        Spacer(Modifier.size(2.dp))
        Text(
            text = big,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (small.isNotBlank()) {
            Spacer(Modifier.size(2.dp))
            Text(
                text = small,
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ProgressStrip(done: Int, total: Int) {
    if (total <= 0) return
    val ratio = if (total == 0) 0f else done.toFloat() / total
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.routes_completion_progress, done, total),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
        Spacer(Modifier.size(6.dp))
        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(8.dp)),
            color = BrandEmerald,
            trackColor = BrandPurpleDim.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun ErrorBanner(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
            .padding(12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(ObsidianCard)
            )
            Spacer(Modifier.size(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )
        }
    }
}
