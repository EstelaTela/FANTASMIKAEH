package com.example.fkaeh.ui.components

import com.example.fkaeh.R
import com.example.fkaeh.AppViewModel
import com.example.fkaeh.core.*
import com.example.fkaeh.data.models.*
import com.example.fkaeh.ui.common.*

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class AdaptiveLayout(
    val screenWidthDp: Int,
    val screenHeightDp: Int
) {
    val isCompactWidth: Boolean
        get() = screenWidthDp < 360

    val isVeryCompactWidth: Boolean
        get() = screenWidthDp < 330

    val isShortHeight: Boolean
        get() = screenHeightDp < 700

    val gridColumns: Int
        get() = if (isVeryCompactWidth) 1 else 2

    val horizontalPadding: Dp
        get() = if (isCompactWidth) 12.dp else 16.dp

    val gridSpacing: Dp
        get() = if (isCompactWidth) 12.dp else 16.dp

    val bottomBarHeight: Dp
        get() = if (isCompactWidth) 58.dp else 62.dp

    val bottomBarIconSize: Dp
        get() = if (isCompactWidth) 20.dp else 24.dp

    val heroButtonSize: Dp
        get() = if (isCompactWidth) 66.dp else 74.dp

    val heroIconSize: Dp
        get() = if (isCompactWidth) 30.dp else 34.dp

    val heroButtonInsetEnd: Dp
        get() = if (isCompactWidth) 10.dp else 14.dp

    val heroButtonInsetTop: Dp
        get() = if (isCompactWidth) 10.dp else 14.dp
}

@Composable
fun rememberAdaptiveLayout(): AdaptiveLayout {
    val configuration = LocalConfiguration.current
    return AdaptiveLayout(
        screenWidthDp = configuration.screenWidthDp,
        screenHeightDp = configuration.screenHeightDp
    )
}
