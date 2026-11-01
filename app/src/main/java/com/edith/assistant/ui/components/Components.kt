package com.edith.assistant.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edith.assistant.ui.theme.NovaTheme
import com.edith.assistant.ui.theme.depthSurface
import kotlinx.coroutines.launch

enum class ButtonVariant {
    PRIMARY,
    SECONDARY,
    TEXT
}

@Composable
fun NovaGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    } else Modifier

    Box(
        modifier = modifier
            .depthSurface(shape, elevation)
            .then(clickModifier)
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun NovaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    testTag: String = "nova_button"
) {
    val colors = NovaTheme.colors
    val reduced = NovaTheme.reducedMotion
    val scope = rememberCoroutineScope()
    val pressScale = remember { Animatable(1f) }

    val bgBrush = when (variant) {
        ButtonVariant.PRIMARY -> Brush.horizontalGradient(listOf(colors.accent, colors.accentSecondary))
        ButtonVariant.SECONDARY -> Brush.horizontalGradient(listOf(colors.surfaceRaised, colors.surface))
        ButtonVariant.TEXT -> Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    }

    val contentColor = when (variant) {
        ButtonVariant.PRIMARY -> colors.onAccent
        ButtonVariant.SECONDARY -> colors.textPrimary
        ButtonVariant.TEXT -> colors.accent
    }

    Box(
        modifier = modifier
            .testTag(testTag)
            .heightIn(min = 48.dp)
            .scale(if (reduced) 1f else pressScale.value)
            .clip(RoundedCornerShape(24.dp))
            .background(bgBrush)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        if (!reduced) {
                            scope.launch { pressScale.animateTo(0.96f, tween(80)) }
                        }
                        tryAwaitRelease()
                        if (!reduced) {
                            scope.launch { pressScale.animateTo(1f, tween(120)) }
                        }
                    },
                    onTap = { if (enabled) onClick() }
                )
            }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
fun NovaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null,
    testTag: String = "nova_text_field"
) {
    val colors = NovaTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(placeholder, color = colors.textTertiary)
        },
        trailingIcon = trailingIcon,
        modifier = modifier
            .testTag(testTag)
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .depthSurface(RoundedCornerShape(20.dp), elevation = 4.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = Color.Transparent,
            cursorColor = colors.accent,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(20.dp),
        singleLine = true
    )
}

@Composable
fun NovaTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null
) {
    val colors = NovaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("top_bar_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textPrimary
                )
            }
        } else {
            Spacer(modifier = Modifier.width(16.dp))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(color = colors.textPrimary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        actions?.invoke()
    }
}

@Composable
fun NovaSectionHeader(title: String, modifier: Modifier = Modifier) {
    val colors = NovaTheme.colors
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium.copy(color = colors.textPrimary),
        modifier = modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun NovaSettingsRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    value: String? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    testTag: String = "settings_row"
) {
    val colors = NovaTheme.colors
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    } else Modifier

    Row(
        modifier = Modifier
            .testTag(testTag)
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .then(clickableModifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceRaised),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(color = colors.textPrimary)
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
                )
            }
        }

        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(color = colors.accent)
            )
        }

        if (checked != null && onCheckedChange != null) {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.onAccent,
                    checkedTrackColor = colors.accent,
                    uncheckedThumbColor = colors.textTertiary,
                    uncheckedTrackColor = colors.surfaceRaised
                )
            )
        }
    }
}

@Composable
fun NovaTypingDots() {
    val colors = NovaTheme.colors
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dot1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
        label = "d1"
    )
    val dot2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 150, easing = LinearEasing), RepeatMode.Reverse),
        label = "d2"
    )
    val dot3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 300, easing = LinearEasing), RepeatMode.Reverse),
        label = "d3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(dot1, dot2, dot3).forEach { d ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .scale(0.8f + d * 0.4f)
                    .clip(CircleShape)
                    .background(colors.accent)
            )
        }
    }
}
