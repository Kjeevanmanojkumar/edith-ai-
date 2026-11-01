package com.edith.assistant.agent

import android.graphics.Rect

data class UiElement(
    val id: String,
    val text: String,
    val contentDescription: String,
    val className: String,
    val bounds: Rect,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val isScrollable: Boolean
)

data class ScreenObservation(
    val observationId: String,
    val packageName: String,
    val windowClass: String,
    val elements: List<UiElement>,
    val screenWidth: Int,
    val screenHeight: Int,
    val windowStamp: Long
)

data class CapturedScreen(
    val rawElements: List<UiElement>,
    val packageName: String,
    val timestamp: Long
)

enum class DriverResult {
    OK,
    FAILED,
    STALE,
    NOT_FOUND,
    UNSUPPORTED
}

interface ScreenDriver {
    suspend fun capture(): ScreenObservation?
    suspend fun clickElement(obs: ScreenObservation, element: UiElement): DriverResult
    suspend fun gestureTap(x: Float, y: Float): DriverResult
    suspend fun setText(text: String): DriverResult
    suspend fun scroll(direction: String): DriverResult
    suspend fun back(): DriverResult
    suspend fun home(): DriverResult
}
