package com.czagent.core.safety

import com.czagent.core.model.ScreenSnapshot
import com.czagent.core.model.StepType
import com.czagent.core.model.TaskStep

sealed class SafetyDecision {
    data object Allowed : SafetyDecision()
    data class RequiresConfirmation(
        val reason: String,
        val redactedInputText: String? = null,
    ) : SafetyDecision()
}

class SafetyGuard(
    private val sensitiveKeywords: Set<String> = DEFAULT_SENSITIVE_KEYWORDS,
) {
    fun evaluate(step: TaskStep, screen: ScreenSnapshot): SafetyDecision {
        if (step.requiresConfirmation) {
            return SafetyDecision.RequiresConfirmation("Step is marked as requiring confirmation", step.redactedInput())
        }

        val haystack = buildString {
            append(step.label)
            append(' ')
            append(step.selectorText.orEmpty())
            append(' ')
            append(step.inputText.orEmpty())
            append(' ')
            append(screen.visibleText())
        }.lowercase()

        val matched = sensitiveKeywords.firstOrNull { it.lowercase() in haystack }
        if (matched != null) {
            return SafetyDecision.RequiresConfirmation("Sensitive operation detected: $matched", step.redactedInput())
        }

        if (step.type == StepType.INPUT_TEXT && looksSecret(step.inputText.orEmpty())) {
            return SafetyDecision.RequiresConfirmation("Sensitive text input detected", step.redactedInput())
        }

        return SafetyDecision.Allowed
    }

    private fun TaskStep.redactedInput(): String? {
        val text = inputText ?: return null
        return if (text.isBlank()) "" else "******"
    }

    private fun looksSecret(text: String): Boolean {
        val normalized = text.trim()
        if (normalized.length >= 6 && normalized.all { it.isDigit() }) return true
        return listOf("password", "密码", "验证码", "verification code", "token").any {
            it in normalized.lowercase()
        }
    }

    companion object {
        val DEFAULT_SENSITIVE_KEYWORDS = setOf(
            "支付",
            "付款",
            "转账",
            "购买",
            "删除",
            "授权",
            "登录授权",
            "发布",
            "发送动态",
            "密码",
            "验证码",
            "pay",
            "payment",
            "transfer",
            "purchase",
            "delete",
            "authorize",
            "password",
            "verification code",
            "publish",
        )
    }
}
