package com.slocator.fleetdriver.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slocator.fleetdriver.R
import com.slocator.fleetdriver.ui.theme.BrandEmerald
import com.slocator.fleetdriver.ui.theme.BrandPurpleLight

/**
 * Vector logomark — gradient pin with hollow inner ring.
 * Drawn directly in Compose so we don't pay for a separate vector asset.
 */
@Composable
fun BrandLogo(modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 64.dp) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.50f, h * 0.18f)
            cubicTo(
                w * 0.27f, h * 0.18f,
                w * 0.18f, h * 0.36f,
                w * 0.18f, h * 0.50f
            )
            cubicTo(
                w * 0.18f, h * 0.74f,
                w * 0.50f, h * 0.92f,
                w * 0.50f, h * 0.92f
            )
            cubicTo(
                w * 0.50f, h * 0.92f,
                w * 0.82f, h * 0.74f,
                w * 0.82f, h * 0.50f
            )
            cubicTo(
                w * 0.82f, h * 0.36f,
                w * 0.73f, h * 0.18f,
                w * 0.50f, h * 0.18f
            )
            close()
        }
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(BrandPurpleLight, BrandEmerald),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )
        // inner cutout
        drawCircle(
            color = androidx.compose.ui.graphics.Color(0xFF0E0E12),
            radius = w * 0.13f,
            center = Offset(w * 0.50f, h * 0.46f)
        )
    }
}

@Composable
fun BrandLockup(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BrandLogo(size = 76.dp)
        Spacer(Modifier.height(0.dp))
        Text(
            text = androidx.compose.ui.res.stringResource(id = R.string.app_brand_short),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = androidx.compose.ui.res.stringResource(id = R.string.app_brand_subtitle),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
