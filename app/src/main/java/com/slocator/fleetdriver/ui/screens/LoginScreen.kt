package com.slocator.fleetdriver.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.slocator.fleetdriver.R
import com.slocator.fleetdriver.ui.components.BrandLockup
import com.slocator.fleetdriver.ui.theme.BrandEmerald
import com.slocator.fleetdriver.ui.theme.BrandPurple
import com.slocator.fleetdriver.ui.theme.BrandPurpleDim
import com.slocator.fleetdriver.ui.theme.BrandPurpleLight
import com.slocator.fleetdriver.ui.theme.Obsidian
import com.slocator.fleetdriver.ui.theme.ObsidianCard
import com.slocator.fleetdriver.ui.theme.ObsidianOutline
import com.slocator.fleetdriver.ui.theme.TextSecondary

@Composable
fun LoginScreen(
    initialPhone: String = "",
    isLoading: Boolean = false,
    errorText: String? = null,
    onSubmit: (String) -> Unit,
    onToggleLanguage: () -> Unit,
    languageToggleLabel: String
) {
    val focus = LocalFocusManager.current
    var phone by remember { mutableStateOf(initialPhone) }
    val emptyError = stringResource(R.string.login_error_empty)
    var localErr by remember { mutableStateOf<String?>(null) }
    val visibleError = errorText ?: localErr

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
    ) {
        // Top-end language toggle. End is right in LTR, left in RTL.
        TextButton(
            onClick = onToggleLanguage,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            Text(
                text = languageToggleLabel,
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .padding(top = 60.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(40.dp))
            BrandLockup()
            Spacer(Modifier.height(56.dp))

            Text(
                text = stringResource(R.string.login_welcome),
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.login_instruction),
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
            Spacer(Modifier.height(32.dp))

            // Phone number field — premium dark variant
            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    if (localErr != null) localErr = null
                },
                singleLine = true,
                label = { Text(stringResource(R.string.login_phone_label)) },
                placeholder = { Text(stringResource(R.string.login_phone_hint)) },
                textStyle = TextStyle(
                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                    fontWeight = FontWeight.SemiBold
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Go
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clip(RoundedCornerShape(16.dp)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ObsidianCard,
                    unfocusedContainerColor = ObsidianCard,
                    focusedBorderColor = BrandPurpleLight,
                    unfocusedBorderColor = ObsidianOutline,
                    cursorColor = BrandEmerald,
                    focusedLabelColor = BrandPurpleLight,
                    unfocusedLabelColor = TextSecondary
                )
            )

            AnimatedVisibility(visible = visibleError != null) {
                Text(
                    text = visibleError.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                )
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    val v = phone.trim()
                    if (v.isEmpty()) localErr = emptyError
                    else { focus.clearFocus(); onSubmit(v) }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPurple,
                    contentColor = Color.White,
                    disabledContainerColor = BrandPurple.copy(alpha = 0.4f),
                    disabledContentColor = Color.White.copy(alpha = 0.7f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                    )
                } else {
                    Text(
                        text = stringResource(R.string.login_button),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.login_offline_hint),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }
    }
}
