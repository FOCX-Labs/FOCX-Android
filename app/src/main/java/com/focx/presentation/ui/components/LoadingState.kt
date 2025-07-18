package com.focx.presentation.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.focx.presentation.ui.theme.Alpha
import com.focx.presentation.ui.theme.BackgroundDark
import com.focx.presentation.ui.theme.Dimensions
import com.focx.presentation.ui.theme.OnBackground
import com.focx.presentation.ui.theme.OnSurface
import com.focx.presentation.ui.theme.OnSurfaceVariant
import com.focx.presentation.ui.theme.Primary
import com.focx.presentation.ui.theme.Spacing
import com.focx.presentation.ui.theme.SurfaceDark
import com.focx.presentation.ui.theme.TechShapes

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    size: LoadingSize = LoadingSize.MEDIUM,
    color: Color = Primary
) {
    val indicatorSize = when (size) {
        LoadingSize.SMALL -> 24.dp
        LoadingSize.MEDIUM -> 40.dp
        LoadingSize.LARGE -> 56.dp
    }

    CircularProgressIndicator(
        modifier = modifier.size(indicatorSize),
        color = color,
        strokeWidth = 3.dp
    )
}

enum class LoadingSize {
    SMALL,
    MEDIUM,
    LARGE
}

@Composable
fun TechLoadingIndicator(
    modifier: Modifier = Modifier,
    size: LoadingSize = LoadingSize.MEDIUM
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val indicatorSize = when (size) {
        LoadingSize.SMALL -> 24.dp
        LoadingSize.MEDIUM -> 40.dp
        LoadingSize.LARGE -> 56.dp
    }

    Box(
        modifier = modifier.size(indicatorSize),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(indicatorSize)
                .graphicsLayer { rotationZ = rotation },
            color = Primary,
            strokeWidth = 3.dp,
            trackColor = Primary.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun FullScreenLoading(
    modifier: Modifier = Modifier,
    message: String = "Loading..."
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TechLoadingIndicator(size = LoadingSize.LARGE)
            Spacer(modifier = Modifier.height(Spacing.medium))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = OnBackground,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            SurfaceDark.copy(alpha = 0.6f),
            SurfaceDark.copy(alpha = 0.2f),
            SurfaceDark.copy(alpha = 0.6f)
        ),
        start = Offset.Zero,
        end = Offset(x = shimmerTranslateAnim, y = shimmerTranslateAnim)
    )

    Box(
        modifier = modifier
            .background(brush)
    )
}

@Composable
fun ShimmerProductCard(
    modifier: Modifier = Modifier
) {
    TechCard(
        modifier = modifier.fillMaxWidth(),
        style = CardStyle.ELEVATED
    ) {
        // Image placeholder
        ShimmerEffect(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimensions.productImageLarge)
                .clip(TechShapes.productImage)
        )

        Spacer(modifier = Modifier.height(Spacing.small))

        // Title placeholder
        ShimmerEffect(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
        )

        Spacer(modifier = Modifier.height(Spacing.extraSmall))

        // Description placeholder
        ShimmerEffect(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
        )

        Spacer(modifier = Modifier.height(Spacing.small))

        // Price and rating row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ShimmerEffect(
                modifier = Modifier
                    .width(80.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            ShimmerEffect(
                modifier = Modifier
                    .width(60.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
fun ShimmerProductGrid(
    modifier: Modifier = Modifier,
    itemCount: Int = 6
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(Spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        items(itemCount) {
            ShimmerProductCard()
        }
    }
}

@Composable
fun ShimmerProductList(
    modifier: Modifier = Modifier,
    itemCount: Int = 5
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        items(itemCount) {
            ShimmerProductCard()
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(Dimensions.iconExtraLarge * 2),
                tint = OnSurfaceVariant.copy(alpha = Alpha.medium)
            )
            Spacer(modifier = Modifier.height(Spacing.large))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = OnSurface,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )

        if (subtitle != null) {
            Spacer(modifier = Modifier.height(Spacing.small))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(Spacing.large))
            TechButton(
                text = actionText,
                onClick = onActionClick,
                style = TechButtonStyle.PRIMARY
            )
        }
    }
}

@Composable
fun ErrorState(
    title: String = "Something went wrong",
    subtitle: String? = "Please try again later",
    actionText: String = "Retry",
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = title,
        subtitle = subtitle,
        icon = androidx.compose.material.icons.Icons.Default.Error,
        actionText = actionText,
        onActionClick = onActionClick,
        modifier = modifier
    )
}

@Composable
fun NoInternetState(
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Internet Connection",
        subtitle = "Please check your connection and try again",
        icon = androidx.compose.material.icons.Icons.Default.WifiOff,
        actionText = "Retry",
        onActionClick = onRetryClick,
        modifier = modifier
    )
}