package com.czagent.android.automation

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class MobileAgentAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        currentPackageName = event?.packageName?.toString()
        currentActivityName = event?.className?.toString()
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    companion object {
        @Volatile var instance: MobileAgentAccessibilityService? = null
            private set
        @Volatile var currentPackageName: String? = null
            private set
        @Volatile var currentActivityName: String? = null
            private set
    }
}
