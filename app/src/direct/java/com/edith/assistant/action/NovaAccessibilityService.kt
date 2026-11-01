package com.edith.assistant.action

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.LinkedList
import java.util.Queue

class NovaAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: NovaAccessibilityService? = null
            private set

        fun isEnabled(context: Context): Boolean {
            val expectedServiceName = "${context.packageName}/${NovaAccessibilityService::class.java.name}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.split(":").any { it.equals(expectedServiceName, ignoreCase = true) }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Events monitored for UI state changes
    }

    override fun onInterrupt() {
        // Interrupted
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
        }
    }

    fun readScreen(maxElements: Int = 40, maxLabelLength: Int = 40): List<String> {
        val root = rootInActiveWindow ?: return emptyList()
        val elements = mutableListOf<String>()
        val queue: Queue<AccessibilityNodeInfo> = LinkedList()
        queue.add(root)

        val seen = mutableSetOf<String>()

        while (queue.isNotEmpty() && elements.size < maxElements) {
            val node = queue.poll() ?: continue

            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()
            val label = (if (!text.isNullOrEmpty()) text else desc) ?: ""

            if (label.isNotEmpty() && label.length in 1..maxLabelLength) {
                val type = when {
                    node.isEditable -> "input"
                    node.isClickable -> "button"
                    node.isCheckable -> "checkbox"
                    else -> "text"
                }
                val line = "[$type] $label"
                if (seen.add(line)) {
                    elements.add(line)
                }
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    queue.add(child)
                }
            }
        }

        return elements
    }

    fun tapText(targetText: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val cleanTarget = targetText.lowercase().trim()

        val matchingNodes = mutableListOf<AccessibilityNodeInfo>()
        findNodesMatching(root, cleanTarget, matchingNodes)

        for (node in matchingNodes) {
            var curr: AccessibilityNodeInfo? = node
            while (curr != null) {
                if (curr.isClickable) {
                    val clicked = curr.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (clicked) return true
                }
                curr = curr.parent
            }
        }

        // If clickable action not supported, attempt gesture tap on bounds
        for (node in matchingNodes) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            if (!bounds.isEmpty) {
                val tapped = performGestureTap(bounds.centerX().toFloat(), bounds.centerY().toFloat())
                if (tapped) return true
            }
        }

        return false
    }

    private fun findNodesMatching(
        node: AccessibilityNodeInfo?,
        target: String,
        results: MutableList<AccessibilityNodeInfo>
    ) {
        if (node == null) return
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""

        if (text.contains(target) || desc.contains(target)) {
            results.add(node)
        }

        for (i in 0 until node.childCount) {
            findNodesMatching(node.getChild(i), target, results)
        }
    }

    fun typeText(textToType: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focusedNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: findFirstEditable(root)
            ?: return false

        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToType)
        }
        return focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun findFirstEditable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val editable = findFirstEditable(node.getChild(i))
            if (editable != null) return editable
        }
        return null
    }

    fun scrollScreen(direction: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrollAction = if (direction.equals("up", ignoreCase = true)) {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        }

        val scrollableNode = findFirstScrollable(root) ?: return false
        return scrollableNode.performAction(scrollAction)
    }

    private fun findFirstScrollable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val scrollable = findFirstScrollable(node.getChild(i))
            if (scrollable != null) return scrollable
        }
        return null
    }

    fun performBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun performHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun performGestureTap(x: Float, y: Float): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()
        return dispatchGesture(gesture, null, null)
    }
}
