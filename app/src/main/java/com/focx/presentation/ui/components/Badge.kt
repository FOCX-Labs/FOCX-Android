package com.focx.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.focx.presentation.ui.theme.CyberYellow
import com.focx.presentation.ui.theme.Dimensions
import com.focx.presentation.ui.theme.DiscountColor
import com.focx.presentation.ui.theme.Error
import com.focx.presentation.ui.theme.NeonGreen
import com.focx.presentation.ui.theme.OnPrimary
import com.focx.presentation.ui.theme.OnSurfaceVariant
import com.focx.presentation.ui.theme.Primary
import com.focx.presentation.ui.theme.Secondary
import com.focx.presentation.ui.theme.Spacing
import com.focx.presentation.ui.theme.Success
import com.focx.presentation.ui.theme.TechBlue
import com.focx.presentation.ui.theme.TechPurple
import com.focx.presentation.ui.theme.TechShapes
import com.focx.presentation.ui.theme.Warning

enum class BadgeStyle {
    FILLED,
    OUTLINED,
    SOFT
}

enum class BadgeSize {
    SMALL,
    MEDIUM,
    LARGE
}

@Composable
fun TechBadge(
    text: String,
    modifier: Modifier = Modifier,
    style: BadgeStyle = BadgeStyle.FILLED,
    size: BadgeSize = BadgeSize.MEDIUM,
    backgroundColor: Color = Primary,
    textColor: Color = OnPrimary,
    icon: ImageVector? = null
) {
    val (height, horizontalPadding, textStyle, iconSize) = when (size) {
        BadgeSize.SMALL -> {
            Tuple4(
                Dimensions.badgeSizeSmall,
                Spacing.small,
                MaterialTheme.typography.labelSmall,
                Dimensions.iconSmall
            )
        }

        BadgeSize.MEDIUM -> {
            Tuple4(
                Dimensions.badgeSize,
                Spacing.small,
                MaterialTheme.typography.labelMedium,
                Dimensions.iconSmall
            )
        }

        BadgeSize.LARGE -> {
            Tuple4(
                24.dp,
                Spacing.medium,
                MaterialTheme.typography.labelLarge,
                Dimensions.iconMedium
            )
        }
    }

    val (bgColor, contentColor, borderColor) = when (style) {
        BadgeStyle.FILLED -> Triple(
            backgroundColor,
            textColor,
            Color.Transparent
        )

        BadgeStyle.OUTLINED -> Triple(
            Color.Transparent,
            backgroundColor,
            backgroundColor
        )

        BadgeStyle.SOFT -> Triple(
            backgroundColor.copy(alpha = 0.1f),
            backgroundColor,
            Color.Transparent
        )
    }

    Box(
        modifier = modifier
            .height(height)
            .clip(TechShapes.statusBadge)
            .background(bgColor)
            .then(
                if (style == BadgeStyle.OUTLINED) {
                    Modifier.border(
                        width = Dimensions.borderWidth,
                        color = borderColor,
                        shape = TechShapes.statusBadge
                    )
                } else Modifier
            )
            .padding(horizontal = horizontalPadding),
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
                    modifier = Modifier.size(iconSize)
                )
                if (text.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(Spacing.extraSmall))
                }
            }

            if (text.isNotEmpty()) {
                Text(
                    text = text,
                    style = textStyle.copy(fontWeight = FontWeight.Medium),
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier,
    size: BadgeSize = BadgeSize.MEDIUM
) {
    val (backgroundColor, textColor) = when (status.lowercase()) {
        "active", "completed", "success", "approved" -> Pair(Success, Color.Black)
        "pending", "processing", "in_progress" -> Pair(Warning, Color.Black)
        "failed", "rejected", "cancelled", "error" -> Pair(Error, Color.White)
        "draft", "inactive", "paused" -> Pair(OnSurfaceVariant, Color.White)
        else -> Pair(Primary, OnPrimary)
    }

    TechBadge(
        text = status.replace("_", " ").uppercase(),
        modifier = modifier,
        style = BadgeStyle.FILLED,
        size = size,
        backgroundColor = backgroundColor,
        textColor = textColor
    )
}

@Composable
fun CategoryBadge(
    category: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val backgroundColor = when (category.lowercase()) {
        "electronics" -> TechBlue
        "fashion" -> TechPurple
        "home" -> NeonGreen
        "sports" -> CyberYellow
        "books" -> Primary
        else -> Secondary
    }

    TechBadge(
        text = category,
        modifier = modifier.then(
            if (onClick != null) {
                Modifier.clickable { onClick() }
            } else Modifier
        ),
        style = BadgeStyle.SOFT,
        size = BadgeSize.SMALL,
        backgroundColor = backgroundColor,
        textColor = backgroundColor
    )
}

@Composable
fun PriceBadge(
    originalPrice: Double,
    discountedPrice: Double,
    currency: String = "$",
    modifier: Modifier = Modifier
) {
    val discountPercentage = ((originalPrice - discountedPrice) / originalPrice * 100).toInt()

    if (discountPercentage > 0) {
        TechBadge(
            text = "-$discountPercentage%",
            modifier = modifier,
            style = BadgeStyle.FILLED,
            size = BadgeSize.SMALL,
            backgroundColor = DiscountColor,
            textColor = Color.Black
        )
    }
}

@Composable
fun NotificationBadge(
    count: Int,
    modifier: Modifier = Modifier,
    maxCount: Int = 99
) {
    if (count > 0) {
        val displayText = if (count > maxCount) "$maxCount+" else count.toString()

        TechBadge(
            text = displayText,
            modifier = modifier,
            style = BadgeStyle.FILLED,
            size = BadgeSize.SMALL,
            backgroundColor = Error,
            textColor = Color.White
        )
    }
}

@Composable
fun OnlineBadge(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    TechBadge(
        text = if (isOnline) "Online" else "Offline",
        modifier = modifier,
        style = BadgeStyle.SOFT,
        size = BadgeSize.SMALL,
        backgroundColor = if (isOnline) Success else OnSurfaceVariant,
        textColor = if (isOnline) Success else OnSurfaceVariant,
        icon = if (isOnline) {
            androidx.compose.material.icons.Icons.Default.Circle
        } else {
            androidx.compose.material.icons.Icons.Default.Circle
        }
    )
}

// Helper data class for tuple
data class Tuple4<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)