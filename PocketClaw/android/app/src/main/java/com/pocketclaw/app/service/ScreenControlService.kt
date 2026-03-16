package com.pocketclaw.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * AccessibilityService that provides screen reading and control capabilities.
 * Must be enabled by the user in Settings > Accessibility.
 */
class ScreenControlService : AccessibilityService() {

    companion object {
        private const val TAG = "ScreenCtrl"
        var instance: ScreenControlService? = null
            private set

        val isRunning: Boolean get() = instance != null
        val isEnabled: Boolean get() = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
        Log.d(TAG, "Accessibility service destroyed")
    }

    fun readScreen(): String {
        val root = rootInActiveWindow ?: return "No active window"
        return buildString {
            append("Package: ${root.packageName}\n")
            appendNodeTree(root, depth = 0)
        }
    }

    private fun StringBuilder.appendNodeTree(node: AccessibilityNodeInfo, depth: Int) {
        if (depth > 6) return

        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()
        val className = node.className?.toString()?.substringAfterLast('.') ?: ""
        val clickable = node.isClickable
        val editable = node.isEditable
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)

        val hasContent = !text.isNullOrBlank() || !desc.isNullOrBlank() || clickable || editable
        if (hasContent) {
            val indent = "  ".repeat(depth)
            val label = text ?: desc ?: ""
            val attrs = mutableListOf<String>()
            if (clickable) attrs.add("clickable")
            if (editable) attrs.add("editable")
            val attrStr = if (attrs.isNotEmpty()) " [${attrs.joinToString(",")}]" else ""
            append("$indent$className: \"$label\"$attrStr (${bounds.centerX()},${bounds.centerY()})\n")
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            appendNodeTree(child, depth + 1)
            child.recycle()
        }
    }

    suspend fun tap(x: Float, y: Float): Boolean {
        val path = Path().apply {
            moveTo(x, y)
            lineTo(x, y + 1f)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 100)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGestureAndWait(gesture)
    }

    suspend fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long = 300): Boolean {
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGestureAndWait(gesture)
    }

    fun inputText(text: String): Boolean {
        val focused = findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        val args = android.os.Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        val result = focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        focused.recycle()
        return result
    }

    fun pressBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)

    fun pressHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)

    fun pressRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)

    private suspend fun dispatchGestureAndWait(gesture: GestureDescription): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        val dispatched = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                deferred.complete(true)
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                deferred.complete(false)
            }
        }, null)

        if (!dispatched) return false
        return withTimeoutOrNull(3000L) { deferred.await() } ?: false
    }
}
