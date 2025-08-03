package com.focx.presentation.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.focx.core.constants.AppConstants
import com.focx.domain.entity.Product
import com.focx.presentation.ui.theme.AnimationDuration
import com.focx.presentation.ui.theme.BackgroundDark
import com.focx.presentation.ui.theme.BorderColor
import com.focx.presentation.ui.theme.Dimensions
import com.focx.presentation.ui.theme.OnSurface
import com.focx.presentation.ui.theme.OnSurfaceVariant
import com.focx.presentation.ui.theme.PriceColor
import com.focx.presentation.ui.theme.Primary
import com.focx.presentation.ui.theme.Spacing
import com.focx.presentation.ui.theme.SurfaceDark
import com.focx.presentation.ui.theme.SurfaceMedium
import com.focx.presentation.ui.theme.TechShapes

enum class CardStyle {
    ELEVATED, OUTLINED, FILLED, GRADIENT
}

@Composable
fun TechCard(
    modifier: Modifier = Modifier,
    style: CardStyle = CardStyle.ELEVATED,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1f,
        animationSpec = tween(AnimationDuration.fast),
        label = "card_scale"
    )

    val cardModifier = modifier
        .scale(scale)
        .clip(TechShapes.card)
        .then(
            when (style) {
                CardStyle.ELEVATED -> Modifier
                    .shadow(
                        elevation = Dimensions.shadowElevationMedium, shape = TechShapes.card
                    )
                    .background(SurfaceDark)

                CardStyle.OUTLINED -> Modifier
                    .border(
                        width = Dimensions.borderWidth, color = BorderColor, shape = TechShapes.card
                    )
                    .background(SurfaceDark)

                CardStyle.FILLED -> Modifier.background(SurfaceMedium)

                CardStyle.GRADIENT -> Modifier.background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            SurfaceDark, SurfaceMedium
                        )
                    )
                )
            }
        )
        .then(
            if (onClick != null) {
                Modifier.clickable {
                    isPressed = true
                    onClick()
                    isPressed = false
                }
            } else Modifier)
        .padding(Spacing.medium)

    Column(
        modifier = cardModifier, content = content
    )
}

@Composable
fun ProductCardExt(
    product: Product,
    onClick: (Product) -> Unit,
    modifier: Modifier = Modifier,
    showFavoriteButton: Boolean = false,
    onFavoriteClick: ((Product) -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.Red)
            .clickable { onClick(product) }) {}
}

@Composable
fun ProductCard(
    product: Product,
    onClick: (Product) -> Unit,
    modifier: Modifier = Modifier,
    showFavoriteButton: Boolean = false,
    onFavoriteClick: ((Product) -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(product) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp, pressedElevation = 8.dp
        ),
        border = BorderStroke(
            width = 0.5.dp, color = Primary.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Box {
                // Product Image with enhanced styling
                AsyncImage(
                    model = product.imageUrls.firstOrNull(),
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )

                // Favorite Button
                if (showFavoriteButton && onFavoriteClick != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(32.dp),
                        shape = CircleShape,
                        color = BackgroundDark.copy(alpha = 0.7f),
                        onClick = { onFavoriteClick(product) }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = OnSurface.copy(alpha = 0.8f),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp)
                        )
                    }
                }

                // Sales Badge
//                if (product.salesCount > 100) {
//                    Surface(
//                        modifier = Modifier
//                            .align(Alignment.TopStart)
//                            .padding(8.dp),
//                        shape = RoundedCornerShape(8.dp),
//                        color = Error.copy(alpha = 0.9f)
//                    ) {
//                        Text(
//                            text = "${product.salesCount}",
//                            style = MaterialTheme.typography.labelSmall,
//                            color = Color.White,
//                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
//                            fontWeight = FontWeight.Bold
//                        )
//                    }
//                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Product Name
            Text(
                text = product.name, style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ), color = OnSurface, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Sales info
//            Text(
//                text = "Sales: ${product.salesCount}",
//                style = MaterialTheme.typography.bodySmall,
//                color = OnSurfaceVariant.copy(alpha = 0.8f)
//            )

            Spacer(modifier = Modifier.height(8.dp))

            // Price with enhanced styling
            Text(
                text = "${String.format("%.2f", product.price.toDouble() / AppConstants.App.TOKEN_DECIMAL)} ${product.currency}", style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ), color = PriceColor
            )
        }
    }
}

@Composable
fun CategoryCard(
    title: String,
    subtitle: String? = null,
    imageUrl: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TechCard(
        modifier = modifier, style = CardStyle.GRADIENT, onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    modifier = Modifier
                        .size(Dimensions.productImageSmall)
                        .clip(TechShapes.categoryCard),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(Spacing.medium))
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface,
                    fontWeight = FontWeight.Bold
                )

                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(Spacing.extraSmall))
                    Text(
                        text = subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.KeyboardArrowRight,
                contentDescription = "Navigate",
                tint = OnSurfaceVariant,
                modifier = Modifier.size(Dimensions.iconMedium)
            )
        }
    }
}

@Composable
fun StatsCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier,
    accentColor: Color = Primary
) {
    TechCard(
        modifier = modifier, style = CardStyle.FILLED
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = accentColor.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)
                        ), contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(Dimensions.iconMedium)
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.medium))
            }

            Column {
                Text(
                    text = title, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant
                )

                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = OnSurface,
                    fontWeight = FontWeight.Bold
                )

                if (subtitle != null) {
                    Text(
                        text = subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant
                    )
                }
            }
        }
    }
}