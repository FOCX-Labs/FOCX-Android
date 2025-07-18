package com.focx.presentation.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.focx.presentation.ui.theme.Alpha
import com.focx.presentation.ui.theme.AnimationDuration
import com.focx.presentation.ui.theme.Dimensions
import com.focx.presentation.ui.theme.OnPrimary
import com.focx.presentation.ui.theme.OnSecondary
import com.focx.presentation.ui.theme.Primary
import com.focx.presentation.ui.theme.Secondary
import com.focx.presentation.ui.theme.Spacing
import com.focx.presentation.ui.theme.TechPurple
import com.focx.presentation.ui.theme.TechShapes

enum class TechButtonStyle {
    PRIMARY,
    SECONDARY,
    OUTLINE,
    GHOST,
    GRADIENT
}

enum class TechButtonSize {
    SMALL,
    MEDIUM,
    LARGE
}

@Composable
fun TechButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TechButtonStyle = TechButtonStyle.PRIMARY,
    size: TechButtonSize = TechButtonSize.MEDIUM,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    iconPosition: IconPosition = IconPosition.START
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(AnimationDuration.fast),
        label = "button_scale"
    )

    val interactionSource = remember { MutableInteractionSource() }

    val buttonHeight = when (size) {
        TechButtonSize.SMALL -> Dimensions.buttonHeightSmall
        TechButtonSize.MEDIUM -> Dimensions.buttonHeight
        TechButtonSize.LARGE -> Dimensions.buttonHeightLarge
    }

    val horizontalPadding = when (size) {
        TechButtonSize.SMALL -> Spacing.medium
        TechButtonSize.MEDIUM -> Spacing.large
        TechButtonSize.LARGE -> Spacing.extraLarge
    }

    val textStyle = when (size) {
        TechButtonSize.SMALL -> MaterialTheme.typography.labelMedium
        TechButtonSize.MEDIUM -> MaterialTheme.typography.labelLarge
        TechButtonSize.LARGE -> MaterialTheme.typography.titleMedium
    }.copy(fontWeight = FontWeight.Bold)

    val (backgroundColor, contentColor, borderColor) = getButtonColors(style, enabled)

    Box(
        modifier = modifier
            .scale(scale)
            .height(buttonHeight)
            .clip(TechShapes.button)
            .then(
                if (style == TechButtonStyle.GRADIENT) {
                    Modifier.background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Primary, TechPurple)
                        )
                    )
                } else {
                    Modifier.background(backgroundColor)
                }
            )
            .then(
                if (style == TechButtonStyle.OUTLINE) {
                    Modifier.border(
                        width = Dimensions.borderWidth,
                        color = borderColor,
                        shape = TechShapes.button
                    )
                } else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !loading
            ) {
                isPressed = true
                onClick()
                isPressed = false
            }
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(Dimensions.iconMedium),
                color = contentColor,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null && iconPosition == IconPosition.START) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(Dimensions.iconMedium)
                    )
                    Spacer(modifier = Modifier.width(Spacing.small))
                }

                Text(
                    text = text,
                    style = textStyle,
                    color = contentColor
                )

                if (icon != null && iconPosition == IconPosition.END) {
                    Spacer(modifier = Modifier.width(Spacing.small))
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(Dimensions.iconMedium)
                    )
                }
            }
        }
    }
}

enum class IconPosition {
    START,
    END
}

@Composable
private fun getButtonColors(
    style: TechButtonStyle,
    enabled: Boolean
): Triple<Color, Color, Color> {
    val alpha = if (enabled) Alpha.full else Alpha.disabled

    return when (style) {
        TechButtonStyle.PRIMARY -> Triple(
            Primary.copy(alpha = alpha),
            OnPrimary,
            Color.Transparent
        )

        TechButtonStyle.SECONDARY -> Triple(
            Secondary.copy(alpha = alpha),
            OnSecondary,
            Color.Transparent
        )

        TechButtonStyle.OUTLINE -> Triple(
            Color.Transparent,
            Primary.copy(alpha = alpha),
            Primary.copy(alpha = alpha)
        )

        TechButtonStyle.GHOST -> Triple(
            Color.Transparent,
            Primary.copy(alpha = alpha),
            Color.Transparent
        )

        TechButtonStyle.GRADIENT -> Triple(
            Color.Transparent, // Will be overridden by gradient
            Color.White,
            Color.Transparent
        )
    }
}

@Composable
fun TechIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: TechButtonStyle = TechButtonStyle.GHOST,
    size: TechButtonSize = TechButtonSize.MEDIUM,
    contentDescription: String? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = tween(AnimationDuration.fast),
        label = "icon_button_scale"
    )

    val buttonSize = when (size) {
        TechButtonSize.SMALL -> 32.dp
        TechButtonSize.MEDIUM -> 40.dp
        TechButtonSize.LARGE -> 48.dp
    }

    val iconSize = when (size) {
        TechButtonSize.SMALL -> Dimensions.iconSmall
        TechButtonSize.MEDIUM -> Dimensions.iconMedium
        TechButtonSize.LARGE -> Dimensions.iconLarge
    }

    val (backgroundColor, contentColor, borderColor) = getButtonColors(style, enabled)

    Box(
        modifier = modifier
            .scale(scale)
            .size(buttonSize)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .then(
                if (style == TechButtonStyle.OUTLINE) {
                    Modifier.border(
                        width = Dimensions.borderWidth,
                        color = borderColor,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else Modifier
            )
            .clickable(
                enabled = enabled
            ) {
                isPressed = true
                onClick()
                isPressed = false
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(iconSize)
        )
    }
}