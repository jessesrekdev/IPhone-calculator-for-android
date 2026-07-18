package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector

// iOS Palette Tokens
val ColorDarkGray = Color(0xFF333333)
val ColorLightGray = Color(0xFFA5A5A5)
val ColorOrange = Color(0xFFFF9F0A)
val ColorBlack = Color(0xFF000000)
val ColorWhite = Color(0xFFFFFFFF)

// Highlight state for active operations
val ColorOrangePressed = Color(0xFFF1C40F)

enum class ButtonType {
    NUMBER,     // Dark Gray background, white text
    UTILITY,    // Light Gray background, black text
    OPERATION,  // Orange background, white text
    SCIENTIFIC  // Dark Slate background, white text (scientific buttons)
}

@Composable
fun CalculatorButton(
    text: String,
    type: ButtonType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActiveOperation: Boolean = false,
    isZeroButton: Boolean = false,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Determine the base background and text colors
    val (baseBgColor, baseTextColor) = when (type) {
        ButtonType.NUMBER -> ColorDarkGray to ColorWhite
        ButtonType.UTILITY -> ColorLightGray to ColorBlack
        ButtonType.SCIENTIFIC -> Color(0xFF252525) to ColorWhite
        ButtonType.OPERATION -> {
            if (isActiveOperation) {
                ColorWhite to ColorOrange
            } else {
                ColorOrange to ColorWhite
            }
        }
    }

    // Interactive button press feedback
    val animatedBgColor by animateColorAsState(
        targetValue = when {
            isPressed -> {
                when (type) {
                    ButtonType.NUMBER -> Color(0xFF555555)
                    ButtonType.UTILITY -> Color(0xFFD5D5D5)
                    ButtonType.SCIENTIFIC -> Color(0xFF444444)
                    ButtonType.OPERATION -> {
                        if (isActiveOperation) Color(0xFFE5E5E5) else Color(0xFFFCDCA2)
                    }
                }
            }
            else -> baseBgColor
        },
        label = "button_bg_color"
    )

    // Dynamic scale feedback for a sleek feeling
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        label = "button_scale"
    )

    Box(
        contentAlignment = if (isZeroButton) Alignment.CenterStart else Alignment.Center,
        modifier = modifier
            .scale(scale)
            .then(
                if (type == ButtonType.NUMBER) {
                    Modifier.shadow(
                        elevation = 4.dp,
                        shape = CircleShape,
                        spotColor = Color.Black.copy(alpha = 0.5f)
                    )
                } else Modifier
            )
            .then(
                if (type == ButtonType.SCIENTIFIC) {
                    Modifier
                        .height(44.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(100.dp))
                } else if (isZeroButton) {
                    Modifier.clip(CircleShape)
                } else {
                    Modifier
                        .aspectRatio(1f)
                        .clip(CircleShape)
                }
            )
            .background(animatedBgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("btn_${text.lowercase().replace("+/-", "sign").replace("%", "percent")}")
    ) {
        val textPaddingModifier = if (isZeroButton) {
            Modifier.fillMaxWidth(0.5f)
        } else {
            Modifier
        }

        Box(
            modifier = textPaddingModifier,
            contentAlignment = Alignment.Center
        ) {
            val fontSize = when {
                type == ButtonType.SCIENTIFIC -> {
                    if (text.length > 3) 14.sp else if (text.length > 1) 16.sp else 18.sp
                }
                text.length > 2 -> 22.sp
                else -> 32.sp
            }
            val fontWeight = if (type == ButtonType.SCIENTIFIC) FontWeight.Normal else FontWeight.Medium

            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = baseTextColor,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Text(
                    text = text,
                    color = baseTextColor,
                    fontSize = fontSize,
                    fontWeight = fontWeight
                )
            }
        }
    }
}

