package com.czagent.core.safety

import com.czagent.core.model.ScreenSnapshot
import com.czagent.core.model.StepType
import com.czagent.core.model.TaskStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyGuardTest {
    private val guard = SafetyGuard()
    private val emptyScreen = ScreenSnapshot(null, null, emptyList())

    @Test
    fun `payment text requires confirmation`() {
        val step = TaskStep(
            id = 1,
            orderIndex = 0,
            type = StepType.CLICK_TEXT,
            label = "确认支付",
            selectorText = "支付",
        )

        val decision = guard.evaluate(step, emptyScreen)

        assertTrue(decision is SafetyDecision.RequiresConfirmation)
    }

    @Test
    fun `password input requires confirmation and redaction`() {
        val step = TaskStep(
            id = 1,
            orderIndex = 0,
            type = StepType.INPUT_TEXT,
            label = "输入密码",
            inputText = "123456",
        )

        val decision = guard.evaluate(step, emptyScreen)

        assertTrue(decision is SafetyDecision.RequiresConfirmation)
        assertEquals("******", (decision as SafetyDecision.RequiresConfirmation).redactedInputText)
    }

    @Test
    fun `safe navigation click is allowed`() {
        val step = TaskStep(
            id = 1,
            orderIndex = 0,
            type = StepType.CLICK_TEXT,
            label = "打开设置",
            selectorText = "设置",
        )

        val decision = guard.evaluate(step, emptyScreen)

        assertEquals(SafetyDecision.Allowed, decision)
    }

    @Test
    fun `explicit confirmation flag requires confirmation`() {
        val step = TaskStep(
            id = 1,
            orderIndex = 0,
            type = StepType.CLICK_COORDINATES,
            label = "tap custom area",
            x = 10,
            y = 20,
            requiresConfirmation = true,
        )

        val decision = guard.evaluate(step, emptyScreen)

        assertTrue(decision is SafetyDecision.RequiresConfirmation)
    }
}
