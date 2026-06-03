package com.slocator.fleetdriver.ui.screens.routesscreen.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slocator.fleetdriver.R
import com.slocator.fleetdriver.data.ScheduledDay
import com.slocator.fleetdriver.ui.components.BrandLockup
import com.slocator.fleetdriver.ui.components.PartButton
import com.slocator.fleetdriver.ui.screens.routesscreen.doamin.RoutesUiState
import com.slocator.fleetdriver.ui.theme.BrandEmerald
import com.slocator.fleetdriver.ui.theme.BrandPurpleDim
import com.slocator.fleetdriver.ui.theme.Obsidian
import com.slocator.fleetdriver.ui.theme.ObsidianCard
import com.slocator.fleetdriver.ui.theme.TextSecondary
import kotlinx.datetime.number

@Preview
@Composable
fun RoutesScreen(state: RoutesUiState = RoutesUiState()) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(BrandPurpleDim.copy(alpha = 0.25f), Obsidian),
                    center = Offset.Unspecified,
                    radius = 1200f
                )
            )
            .systemBarsPadding()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HeaderBar(
                driverId = state.driverId,
                isRefreshing = state.isRefreshing,
                onRefresh = state.onRefresh,
                onLogout = state.onLogout,
                onToggleLanguage = state.onToggleLanguage,
                languageToggleLabel = state.languageToggleLabel
            )
            DateHeadline(
                day = state.day,
                hasPrevious = state.hasPreviousDay,
                hasNext = state.hasNextDay,
                onPrevious = state.onPreviousDay,
                onNext = state.onNextDay,
            )
            if (state.errorBanner != null) {

                ErrorBanner(text = state.errorBanner)

            }
            ProgressStrip(
                done = state.parts.count(state.isPartDone),
                total = state.parts.size
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {





                if (state.parts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyState(text = stringResource(R.string.routes_no_today))
                        }
                    }
                } else {
                    items(
                        state.parts,
                        key = { "${state.day?.dayLabel}_${it.partNumber}" }
                    ) { part ->
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 7.dp)) {
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
}

@Composable
private fun HeaderBar(
    driverId: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onToggleLanguage: () -> Unit,
    languageToggleLabel: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
//        Box(
//            modifier = Modifier
//                .size(44.dp)
//                .clip(CircleShape)
//                .background(
//                    Brush.linearGradient(listOf(BrandPurpleLight, BrandEmerald))
//                ),
//            contentAlignment = Alignment.Center
//        ) {
            BrandLockup(
                modifier = Modifier
                    .size(44.dp)
            )
//        }
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
        TextButton(onClick = onToggleLanguage) {
            Text(
                text = languageToggleLabel,
                style = MaterialTheme.typography.labelLarge,
                color = BrandEmerald
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
private fun DateHeadline(
    day: ScheduledDay?,
    hasPrevious: Boolean = false,
    hasNext: Boolean = false,
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},

) {
    val date = day?.date
    // Format date text as DD/MM/YYYY
    val dateText = if (date != null) {
        val dd = date.day.toString().padStart(2, '0')
        val mm = date.month.number.toString().padStart(2, '0')
        "$dd/$mm/${date.year}"
    } else {
        day?.dayLabel ?: ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.routes_today_label),
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp),
            color = BrandEmerald
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onPrevious, enabled = hasPrevious) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = "Previous Day",
                    tint = if (hasPrevious) MaterialTheme.colorScheme.onBackground else TextSecondary.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Text(
                text = dateText,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 28.sp),
                color = MaterialTheme.colorScheme.onBackground,
                 textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp).weight(1f)
            )
            
            IconButton(onClick = onNext, enabled = hasNext) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Next Day",
                    tint = if (hasNext) MaterialTheme.colorScheme.onBackground else TextSecondary.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

//private fun formatLocalizedDate(date: LocalDate, locale: Locale): Pair<String, String> {
//    val isArabic = locale.language == "ar"
//
//    val monthNames = if (isArabic) {
//        listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")
//    } else {
//        listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
//    }
//
//    val dayNames = if (isArabic) {
//        listOf("الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد")
//    } else {
//        listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
//    }
//
//    val monthName = monthNames[date.month.ordinal]
//    val dayOfWeekIdx = date.dayOfWeek.ordinal // Monday is 0
//    val dayName = dayNames[dayOfWeekIdx]
//
//    val big = "${date.day} $monthName"
//    val small = "$dayName • ${date.year}"
//
//    return Pair(big, small)
//}

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
//            Box(
//                modifier = Modifier
//                    .size(72.dp)
//                    .clip(CircleShape)
//                    .background(ObsidianCard)
//            )
            BrandLockup(
                modifier = Modifier
                    .size(44.dp)
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
