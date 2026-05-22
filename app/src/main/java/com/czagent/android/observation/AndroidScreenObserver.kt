package com.czagent.android.observation

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.czagent.android.automation.MobileAgentAccessibilityService
import com.czagent.core.engine.ScreenObserver
import com.czagent.core.model.RectBounds
import com.czagent.core.model.ScreenNode
import com.czagent.core.model.ScreenSnapshot

class AndroidScreenObserver : ScreenObserver {
    override suspend fun observe(): ScreenSnapshot {
        val service = MobileAgentAccessibilityService.instance
        val root = service?.rootInActiveWindow
        return ScreenSnapshot(
            packageName = MobileAgentAccessibilityService.currentPackageName,
            activityName = MobileAgentAccessibilityService.currentActivityName,
            rootNodes = listOfNotNull(root?.toScreenNode()),
            latestScreenshotUri = null,
        )
    }

    private fun AccessibilityNodeInfo.toScreenNode(): ScreenNode {
        val rect = Rect()
        getBoundsInScreen(rect)
        return ScreenNode(
            text = text?.toString(),
            contentDescription = contentDescription?.toString(),
            className = className?.toString(),
            bounds = RectBounds(rect.left, rect.top, rect.width(), rect.height()),
            clickable = isClickable,
            editable = isEditable,
            children = (0 until childCount).mapNotNull { getChild(it)?.toScreenNode() },
        )
    }
}
