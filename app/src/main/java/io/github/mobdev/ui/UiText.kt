package io.github.mobdev.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

data class UiText(
    @get:StringRes val resId: Int,
    val args: List<Any> = emptyList(),
)

@Composable
fun UiText.asString(): String {
    return stringResource(id = resId, *args.toTypedArray())
}
