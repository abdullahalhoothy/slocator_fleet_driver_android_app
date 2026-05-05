package com.slocator.fleetdriver.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slocator.fleetdriver.R
import com.slocator.fleetdriver.ui.theme.BrandEmerald
import com.slocator.fleetdriver.ui.theme.BrandPurple
import com.slocator.fleetdriver.ui.theme.BrandPurpleLight
import com.slocator.fleetdriver.ui.theme.DoneBg
import com.slocator.fleetdriver.ui.theme.ObsidianCard
import com.slocator.fleetdriver.ui.theme.ObsidianOutline

/**
 * Premium tactile route button.
 *
 * Layout (LTR):  [ checkbox | label + meta | maps glyph ]
 * In RTL the checkbox naturally moves to the leading edge — i.e. the right side —
 * which matches the user's spec ("checkbox to the right of the button label in Arabic").
 *
 * State:
 *  - default: gradient purple, big shadow, large tap target
 *  - done:    flattened to a quiet emerald-tinted card, button is non-clickable
 */
@Preview
@Composable
fun PartButton(
    modifier: Modifier = Modifier,
    partNumber: Int = 1 ,
    stopCount: Int = 2 ,
    isDone: Boolean = false ,
    onCheckedChange: (Boolean) -> Unit = {},
    onOpenRoute: () -> Unit= {},


) {
    val targetAlpha by animateFloatAsState(if (isDone) 0.55f else 1f, label = "alpha")
    val targetBg by animateColorAsState(
        targetValue = if (isDone) DoneBg else Color.Transparent,
        label = "bg"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .shadow(
                elevation = if (isDone) 0.dp else 12.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = BrandPurple,
                spotColor = BrandPurple
            )
            .clip(RoundedCornerShape(22.dp))
            .background(
                brush = if (isDone) {
                    Brush.linearGradient(listOf(targetBg, targetBg))
                } else {
                    Brush.linearGradient(
                        colors = listOf(BrandPurple, BrandPurpleLight),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                }
            )
            .border(
                width = 1.dp,
                color = if (isDone) BrandEmerald.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        // Checkbox is leading — RTL handles the visual flip automatically.
        DoneCheckbox(
            checked = isDone,
            onCheckedChange = onCheckedChange,
            modifier = Modifier
        )

        Spacer(Modifier.width(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .alpha(targetAlpha)
                .clickable(enabled = !isDone) { onOpenRoute() },
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.routes_part_label, partNumber),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp),
                color = if (isDone) BrandEmerald else Color.White
            )
            Text(
                text = if (stopCount > 0)
                    androidx.compose.ui.res.stringResource(R.string.routes_stops_count, stopCount)
                else
                    androidx.compose.ui.res.stringResource(R.string.routes_open_in_maps),
                style = MaterialTheme.typography.labelMedium,
                color = if (isDone) BrandEmerald.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.85f)
            )
        }

        Spacer(Modifier.width(12.dp))

        Icon(
            imageVector = if (isDone) Icons.Default.Check else Icons.Default.Map,
            contentDescription = null,
            tint = if (isDone) BrandEmerald else Color.White,
            modifier = Modifier.size(28.dp).alpha(targetAlpha).clickable(enabled = !isDone) { onOpenRoute() }
        )
    }
}

@Composable
private fun DoneCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (checked) BrandEmerald else Color.White.copy(alpha = 0.10f))
            .border(
                width = 1.5.dp,
                color = if (checked) BrandEmerald else ObsidianOutline,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = androidx.compose.ui.res.stringResource(R.string.cd_checkbox_done),
                tint = ObsidianCard,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
