package com.czagent.android.automation

import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.czagent.core.engine.ActionExecutor
import com.czagent.core.model.ActionResult
import com.czagent.core.model.AgentAction
import com.czagent.core.model.SwipeDirection
import kotlinx.coroutines.delay

class AndroidActionExecutor(
    private val context: Context,
) : ActionExecutor {
    override suspend fun execute(action: AgentAction): ActionResult {
        val service = MobileAgentAccessibilityService.instance
        return when (action) {
            is AgentAction.OpenApp -> openApp(action.packageName)
            is AgentAction.ClickText -> clickText(service, action.text)
            is AgentAction.ClickCoordinates -> clickCoordinates(service, action.x, action.y)
            is AgentAction.Swipe -> swipe(service, action.direction)
            is AgentAction.InputText -> inputText(service, action.text, action.targetText)
            AgentAction.Back -> if (service?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK) == true) ActionResult.Success else ActionResult.Failure("Back action failed")
            is AgentAction.Wait -> {
                delay(action.millis)
                ActionResult.Success
            }
            AgentAction.Screenshot -> ActionResult.Success
            AgentAction.Complete -> ActionResult.Success
        }
    }

    private fun openApp(packageName: String): ActionResult {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return ActionResult.Failure("Launch intent not found for $packageName", transient = false)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return ActionResult.Success
    }

    private fun clickText(service: MobileAgentAccessibilityService?, text: String): ActionResult {
        val root = service?.rootInActiveWindow ?: return ActionResult.Failure("Accessibility root unavailable")
        val node = root.findAccessibilityNodeInfosByText(text).firstOrNull()
            ?: return ActionResult.Failure("Text node not found: $text")
        return if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            ActionResult.Success
        } else {
            ActionResult.Failure("Click text failed: $text")
        }
    }

    private fun clickCoordinates(service: MobileAgentAccessibilityService?, x: Int, y: Int): ActionResult {
        service ?: return ActionResult.Failure("Accessibility service unavailable")
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80))
            .build()
        return if (service.dispatchGesture(gesture, null, null)) ActionResult.Success else ActionResult.Failure("Coordinate click failed")
    }

    private fun swipe(service: MobileAgentAccessibilityService?, direction: SwipeDirection): ActionResult {
        service ?: return ActionResult.Failure("Accessibility service unavailable")
        val (startX, startY, endX, endY) = when (direction) {
            SwipeDirection.UP -> listOf(540f, 1800f, 540f, 600f)
            SwipeDirection.DOWN -> listOf(540f, 600f, 540f, 1800f)
            SwipeDirection.LEFT -> listOf(900f, 1200f, 180f, 1200f)
            SwipeDirection.RIGHT -> listOf(180f, 1200f, 900f, 1200f)
        }
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 350))
            .build()
        return if (service.dispatchGesture(gesture, null, null)) ActionResult.Success else ActionResult.Failure("Swipe failed")
    }

    private fun inputText(service: MobileAgentAccessibilityService?, text: String, targetText: String?): ActionResult {
        val root = service?.rootInActiveWindow ?: return ActionResult.Failure("Accessibility root unavailable")
        val node = targetText?.let { root.findAccessibilityNodeInfosByText(it).firstOrNull() } ?: root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        node ?: return ActionResult.Failure("Input target unavailable")
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return if (node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) ActionResult.Success else ActionResult.Failure("Input text failed")
    }
}
