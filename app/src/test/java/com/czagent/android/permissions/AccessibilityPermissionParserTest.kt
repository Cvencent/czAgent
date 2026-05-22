package com.czagent.android.permissions

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityPermissionParserTest {
    @Test
    fun `enabled service list containing component is enabled`() {
        val enabledServices = "com.other/.Service:com.czagent/com.czagent.android.automation.MobileAgentAccessibilityService"

        val result = AccessibilityPermissionParser.isServiceEnabled(
            enabledServices = enabledServices,
            packageName = "com.czagent",
            serviceClassName = "com.czagent.android.automation.MobileAgentAccessibilityService",
        )

        assertTrue(result)
    }

    @Test
    fun `relative service name in enabled service list is enabled`() {
        val enabledServices = "com.czagent/.android.automation.MobileAgentAccessibilityService"

        val result = AccessibilityPermissionParser.isServiceEnabled(
            enabledServices = enabledServices,
            packageName = "com.czagent",
            serviceClassName = "com.czagent.android.automation.MobileAgentAccessibilityService",
        )

        assertTrue(result)
    }

    @Test
    fun `missing service is disabled`() {
        val result = AccessibilityPermissionParser.isServiceEnabled(
            enabledServices = "com.other/.Service",
            packageName = "com.czagent",
            serviceClassName = "com.czagent.android.automation.MobileAgentAccessibilityService",
        )

        assertFalse(result)
    }
}
