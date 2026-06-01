package com.example.fkaeh.ui.common

import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle

object AppColors {
    val Primary = Color(0xFF8C52FF)
    val PrimaryAlt = Color(0xFF9C27B0)

    val BackgroundBlack = Color(0xFF0A0A0A)
    val SurfaceBlack = Color(0xFF111111)
    val SurfaceDark = Color(0xFF151515)
    val SurfaceElevated = Color(0xFF171717)
    val SurfaceMuted = Color(0xFF242424)

    val TextPrimary = Color.White
    val TextDark = Color.Black
    val TextMuted = Color(0xFFB7B7B7)
    val TextSoft = Color(0xFFD0D0D0)
    val TextDisabled = Color(0xFF8E8E8E)

    val Error = Color.Red
    val Danger = Color(0xFFD50000)
    val DangerSoft = Color(0xFF8B1E1E)
    val Success = Color(0xFF7FD48B)
    val ErrorSoft = Color(0xFFFF8A80)

    val ProfileCard = Color(0xD9101010)
    val ProfileBorder = Color(0xFF2A2A2A)
    val DialogContainer = SurfaceDark
}

val Purple = AppColors.Primary
val BlackBg = AppColors.BackgroundBlack

val WhiteFieldColors @Composable get() = TextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    errorContainerColor = Color.White,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    errorIndicatorColor = AppColors.Error,
    focusedTextColor = Color.Black,
    unfocusedTextColor = Color.Black,
    focusedPlaceholderColor = Color.Gray,
    unfocusedPlaceholderColor = Color.Gray,
    cursorColor = AppColors.PrimaryAlt
)

val DarkFieldColors @Composable get() = TextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    focusedTextColor = Color.Black,
    unfocusedTextColor = Color.Black,
    focusedPlaceholderColor = Color.Gray,
    unfocusedPlaceholderColor = Color.Gray,
    cursorColor = AppColors.PrimaryAlt
)

val BlackFieldColors @Composable get() = TextFieldDefaults.colors(
    focusedContainerColor = AppColors.SurfaceBlack,
    unfocusedContainerColor = AppColors.SurfaceBlack,
    errorContainerColor = AppColors.SurfaceBlack,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    errorIndicatorColor = AppColors.Error,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedPlaceholderColor = AppColors.TextDisabled,
    unfocusedPlaceholderColor = AppColors.TextDisabled,
    focusedLabelColor = AppColors.TextSoft,
    unfocusedLabelColor = AppColors.TextSoft,
    cursorColor = AppColors.Primary
)

val shadowTextStyle = TextStyle(
    shadow = Shadow(
        color = Color.Black,
        offset = Offset(1f, 1f),
        blurRadius = 4f
    )
)
