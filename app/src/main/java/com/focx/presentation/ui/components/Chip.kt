package com.focx.presentation.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focx.presentation.ui.theme.AnimationDuration
import com.focx.presentation.ui.theme.TechColors
import com.focx.presentation.ui.theme.TechShapes

enum class TechChipStyle {
    PRIMARY,
    SECONDARY,
    OUTLINE,
    GHOST
}

enum class TechChipSize {
    SMALL,
    MEDIUM,
    LARGE
}

@Composable
fun TechChip(
    text: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    style: TechChipStyle = TechChipStyle.OUTLINE,
    size: TechChipSize = TechChipSize.MEDIUM,
    selected: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(AnimationDuration.fast),
        label = "chip_scale"
    )

    val chipColors = getChipColors(style, selected)
    val chipSizes = getChipSizes(size)

    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .scale(scale)
            .height(chipSizes.height)
            .clip(TechShapes.chip)
            .background(chipColors.backgroundColor)
            .border(
                width = chipColors.borderWidth,
                color = chipColors.borderColor,
                shape = TechShapes.chip
            )
            .then(
                if (onClick != null && enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        onClick()
                    }
                } else Modifier
            )
            .padding(
                horizontal = chipSizes.horizontalPadding,
                vertical = chipSizes.verticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Leading icon
        leadingIcon?.let { icon ->
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(chipSizes.iconSize),
                tint = chipColors.contentColor
            )
            Spacer(modifier = Modifier.width(chipSizes.iconSpacing))
        }

        // Text
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = chipSizes.fontSize,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = chipColors.contentColor
        )

        // Trailing icon
        trailingIcon?.let { icon ->
            Spacer(modifier = Modifier.width(chipSizes.iconSpacing))
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(chipSizes.iconSize)
                    .then(
                        if (onTrailingIconClick != null && enabled) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onTrailingIconClick()
                            }
                        } else Modifier
                    ),
                tint = chipColors.contentColor
            )
        }
    }
}

@Composable
fun TechFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    TechChip(
        text = text,
        onClick = onClick,
        modifier = modifier,
        style = if (selected) TechChipStyle.PRIMARY else TechChipStyle.OUTLINE,
        selected = selected,
        enabled = enabled,
        leadingIcon = leadingIcon
    )
}

@Composable
fun TechInputChip(
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    TechChip(
        text = text,
        onClick = null,
        modifier = modifier,
        style = TechChipStyle.SECONDARY,
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = Icons.Default.Close,
        onTrailingIconClick = onDismiss
    )
}

@Composable
fun TechActionChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    TechChip(
        text = text,
        onClick = onClick,
        modifier = modifier,
        style = TechChipStyle.GHOST,
        enabled = enabled,
        leadingIcon = leadingIcon
    )
}

private data class ChipColors(
    val backgroundColor: Color,
    val contentColor: Color,
    val borderColor: Color,
    val borderWidth: androidx.compose.ui.unit.Dp
)

private data class ChipSizes(
    val height: androidx.compose.ui.unit.Dp,
    val horizontalPadding: androidx.compose.ui.unit.Dp,
    val verticalPadding: androidx.compose.ui.unit.Dp,
    val fontSize: androidx.compose.ui.unit.TextUnit,
    val iconSize: androidx.compose.ui.unit.Dp,
    val iconSpacing: androidx.compose.ui.unit.Dp
)

@Composable
private fun getChipColors(style: TechChipStyle, selected: Boolean): ChipColors {
    return when (style) {
        TechChipStyle.PRIMARY -> ChipColors(
            backgroundColor = if (selected) TechColors.primary else TechColors.primary.copy(alpha = 0.1f),
            contentColor = if (selected) TechColors.onPrimary else TechColors.primary,
            borderColor = TechColors.primary,
            borderWidth = 1.dp
        )

        TechChipStyle.SECONDARY -> ChipColors(
            backgroundColor = if (selected) TechColors.secondary else TechColors.secondary.copy(alpha = 0.1f),
            contentColor = if (selected) TechColors.onSecondary else TechColors.secondary,
            borderColor = TechColors.secondary,
            borderWidth = 1.dp
        )

        TechChipStyle.OUTLINE -> ChipColors(
            backgroundColor = if (selected) TechColors.primary.copy(alpha = 0.1f) else Color.Transparent,
            contentColor = if (selected) TechColors.primary else TechColors.onSurface,
            borderColor = if (selected) TechColors.primary else TechColors.outline,
            borderWidth = 1.dp
        )

        TechChipStyle.GHOST -> ChipColors(
            backgroundColor = if (selected) TechColors.surfaceVariant else Color.Transparent,
            contentColor = if (selected) TechColors.onSurfaceVariant else TechColors.onSurface,
            borderColor = Color.Transparent,
            borderWidth = 0.dp
        )
    }
}

@Composable
private fun getChipSizes(size: TechChipSize): ChipSizes {
    return when (size) {
        TechChipSize.SMALL -> ChipSizes(
            height = 24.dp,
            horizontalPadding = 8.dp,
            verticalPadding = 4.dp,
            fontSize = 12.sp,
            iconSize = 12.dp,
            iconSpacing = 4.dp
        )

        TechChipSize.MEDIUM -> ChipSizes(
            height = 32.dp,
            horizontalPadding = 12.dp,
            verticalPadding = 6.dp,
            fontSize = 14.sp,
            iconSize = 16.dp,
            iconSpacing = 6.dp
        )

        TechChipSize.LARGE -> ChipSizes(
            height = 40.dp,
            horizontalPadding = 16.dp,
            verticalPadding = 8.dp,
            fontSize = 16.sp,
            iconSize = 20.dp,
            iconSpacing = 8.dp
        )
    }
}