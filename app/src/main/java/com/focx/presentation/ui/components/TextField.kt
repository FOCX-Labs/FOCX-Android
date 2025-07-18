package com.focx.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.focx.presentation.ui.theme.Alpha
import com.focx.presentation.ui.theme.AnimationDuration
import com.focx.presentation.ui.theme.BorderColor
import com.focx.presentation.ui.theme.Dimensions
import com.focx.presentation.ui.theme.Error
import com.focx.presentation.ui.theme.OnSurface
import com.focx.presentation.ui.theme.OnSurfaceVariant
import com.focx.presentation.ui.theme.Primary
import com.focx.presentation.ui.theme.Spacing
import com.focx.presentation.ui.theme.SurfaceMedium
import com.focx.presentation.ui.theme.TechShapes

enum class TextFieldStyle {
    OUTLINED,
    FILLED,
    UNDERLINED
}

@Composable
fun TechTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    style: TextFieldStyle = TextFieldStyle.OUTLINED
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> Error
            isFocused -> Primary
            else -> BorderColor
        },
        animationSpec = tween(AnimationDuration.fast),
        label = "border_color"
    )

    val backgroundColor = when (style) {
        TextFieldStyle.OUTLINED -> Color.Transparent
        TextFieldStyle.FILLED -> SurfaceMedium
        TextFieldStyle.UNDERLINED -> Color.Transparent
    }

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isError) Error else OnSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.extraSmall)
            )
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused },
            enabled = enabled,
            readOnly = readOnly,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = if (enabled) OnSurface else OnSurfaceVariant
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    keyboardActions.onDone?.invoke(this)
                },
                onNext = keyboardActions.onNext,
                onPrevious = keyboardActions.onPrevious,
                onSearch = keyboardActions.onSearch,
                onSend = keyboardActions.onSend,
                onGo = keyboardActions.onGo
            ),
            visualTransformation = visualTransformation,
            singleLine = singleLine,
            maxLines = maxLines,
            cursorBrush = SolidColor(Primary),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimensions.textFieldHeight)
                        .clip(
                            when (style) {
                                TextFieldStyle.OUTLINED -> TechShapes.button
                                TextFieldStyle.FILLED -> TechShapes.button
                                TextFieldStyle.UNDERLINED -> RoundedCornerShape(0.dp)
                            }
                        )
                        .background(backgroundColor)
                        .then(
                            when (style) {
                                TextFieldStyle.OUTLINED -> Modifier.border(
                                    width = if (isFocused) Dimensions.borderWidthThick else Dimensions.borderWidth,
                                    color = borderColor,
                                    shape = TechShapes.button
                                )

                                TextFieldStyle.UNDERLINED -> Modifier.border(
                                    width = if (isFocused) Dimensions.borderWidthThick else Dimensions.borderWidth,
                                    color = borderColor,
                                    shape = RoundedCornerShape(0.dp)
                                )

                                else -> Modifier
                            }
                        )
                        .padding(horizontal = Spacing.medium),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (leadingIcon != null) {
                            Icon(
                                imageVector = leadingIcon,
                                contentDescription = null,
                                tint = if (isFocused) Primary else OnSurfaceVariant,
                                modifier = Modifier.size(Dimensions.iconMedium)
                            )
                            Spacer(modifier = Modifier.width(Spacing.small))
                        }

                        Box(
                            modifier = Modifier.weight(1f)
                        ) {
                            if (value.isEmpty() && placeholder != null) {
                                Text(
                                    text = placeholder,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = OnSurfaceVariant.copy(alpha = Alpha.medium)
                                )
                            }
                            innerTextField()
                        }

                        if (trailingIcon != null) {
                            Spacer(modifier = Modifier.width(Spacing.small))
                            TechIconButton(
                                icon = trailingIcon,
                                onClick = { onTrailingIconClick?.invoke() },
                                size = TechButtonSize.SMALL,
                                style = TechButtonStyle.GHOST
                            )
                        }
                    }
                }
            }
        )

        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = Error,
                modifier = Modifier.padding(top = Spacing.extraSmall)
            )
        }
    }
}

@Composable
fun TechSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search...",
    enabled: Boolean = true,
    leadingIcon: ImageVector = androidx.compose.material.icons.Icons.Default.Search,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    TechTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        onTrailingIconClick = onTrailingIconClick,
        enabled = enabled,
        singleLine = true,
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Search,
        keyboardActions = KeyboardActions(
            onSearch = {
                onSearch(query)
                keyboardController?.hide()
            }
        ),
        style = TextFieldStyle.FILLED
    )
}

@Composable
fun TechPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true
) {
    var passwordVisible by remember { mutableStateOf(false) }

    TechTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        leadingIcon = androidx.compose.material.icons.Icons.Default.Lock,
        trailingIcon = if (passwordVisible) {
            androidx.compose.material.icons.Icons.Default.VisibilityOff
        } else {
            androidx.compose.material.icons.Icons.Default.Visibility
        },
        onTrailingIconClick = { passwordVisible = !passwordVisible },
        isError = isError,
        errorMessage = errorMessage,
        enabled = enabled,
        keyboardType = KeyboardType.Password,
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        }
    )
}

@Composable
fun TechTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    minLines: Int = 3,
    maxLines: Int = 6
) {
    TechTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        isError = isError,
        errorMessage = errorMessage,
        enabled = enabled,
        singleLine = false,
        maxLines = maxLines,
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Default,
        style = TextFieldStyle.OUTLINED
    )
}