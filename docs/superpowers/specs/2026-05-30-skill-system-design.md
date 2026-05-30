# Skill System Design

## Overview

This document specifies the **Skill System** for czAgent — a re-usable, parameterizable, event-driven automation system that integrates with the existing `AutomationTask` framework and adds on-device LLM support for both intent understanding and action decision making.

Skills are the "recipes" for automation. They combine:
- A parameterized step template (what to do)
- Trigger conditions (when to do it)
- Safety guards (guardrails)
- Tags for natural-language matching (how users refer to it)

## Goals

1. **Reuse**: Let users create reusable skills instead of repeating identical tasks
2. **Parameterization**: Allow variables (e.g., `{contact}`, `{message}`) that change per execution
3. **Event-Driven**: Support manual, notification, app-switch, schedule, and network triggers
4. **On-Device AI**: A single local LLM handles both intent recognition *and* screen action decisions
5. **Composability**: Skills work with existing `ExecutionEngine` and `SafetyGuard` infrastructure
6. **Import/Export**: Share skills via JSON files

## Non-Goals

- No cloud LLM dependency (all local)
- No scripting/looping (v1 keeps it linear steps with parameters)
- No user-to-user skill marketplace (v1)

## Core Concepts

### Skill Data Model

```kotlin
data class Skill(
    val id: String,
    val name: String,
    val description: String,
    val tags: List<String>,
    val version: Int,
    val parameters: List<SkillParameter>,
    val steps: List<SkillStep>,
    val triggers: List<Trigger>,
    val enabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

data class SkillParameter(
    val name: String,
    val displayName: String,
    val type: ParamType,
    val defaultValue: String?,
    val required: Boolean,
)

enum class ParamType {
    TEXT, NUMBER, BOOLEAN
}

data class SkillStep(
    val orderIndex: Int,
    val type: StepType,
    val label: String,
    val selectorText: ParamRef?,
    val inputText: ParamRef?,
    val x: Int?,
    val y: Int?,
    val swipeDirection: SwipeDirection?,
    val waitMillis: Long?,
    val requiresConfirmation: Boolean = false,
)

// A ParamRef is a string like "{contact}" to be resolved at runtime
typealias ParamRef = String

sealed class Trigger {
    data object Manual : Trigger()
    data class Notification(val packageName: String, val keywordFilter: String?) : Trigger()
    data class AppSwitch(val packageName: String, val onEntry: Boolean) : Trigger()
    data class DailySchedule(val localTime: String) : Trigger()
    data class NetworkChange(val ssid: String?) : Trigger()
}
```

### On-Device LLM Interface

The single LLM performs two jobs:

```kotlin
interface OnDeviceLLM {
    // Job 1: Understand user intent and extract parameters
    suspend fun understandIntent(
        userInput: String,
        availableSkills: List<Skill>,
    ): SkillMatch?

    // Job 2: Observe screen and decide next action
    suspend fun decideAction(
        screen: ScreenSnapshot,
        taskContext: TaskContext,
    ): AgentDecision
}

data class SkillMatch(
    val skill: Skill,
    val confidence: Float,
    val extractedParams: Map<String, String>,
)
```

The LLM replaces `RuleBasedVisionAnalyzer` for decision making but can fall back to it if needed.

## Architecture

### High-Level Flow

```
┌─────────────────────────────────────────────────────────────┐
│                         User                                │
└────────┬────────────────────────────────────────────────────┘
         │ 1. "帮我给张三发微信说今晚回家"
         │ 2. Notification arrives
         │ 3. App switch happens
         │ 4. Scheduled time
         ▼
┌─────────────────────────────────────────────────────────────┐
│                    SkillTriggerManager                       │
│  (listens to events, finds matching skills)                 │
└────────┬────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│                      OnDeviceLLM                            │
│  ┌──────────────────────────────────────────────────┐     │
│  │ understandIntent() → matches skill + extracts    │     │
│  │                    params from user input        │     │
│  └──────────────────────────────────────────────────┘     │
│  ┌──────────────────────────────────────────────────┐     │
│  │ decideAction() → decides next click/input/etc   │     │
│  │                   from screen snapshot           │     │
│  └──────────────────────────────────────────────────┘     │
└────────┬────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│                    SkillResolver                            │
│  (resolves Skill + params → AutomationTask)                │
└────────┬────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│                    ExecutionEngine                          │
│  (existing code, unchanged!)                                │
│  ┌──────────────────┐  ┌──────────────────┐               │
│  │ SafetyGuard      │  │ ActionExecutor   │               │
│  └──────────────────┘  └──────────────────┘               │
└─────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Responsibility |
|-----------|----------------|
| [SkillRepository](file:///f:/czAgent/app/src/main/java/com/czagent/data/TaskRepository.kt) | CRUD for skills (extends existing Room) |
| SkillResolver | Skill + params → AutomationTask |
| SkillTriggerManager | Listens to events, triggers matching skills |
| OnDeviceLLM | Intent recognition + action decisions |
| ExecutionEngine | Unchanged, runs the resolved task |
| SafetyGuard | Unchanged, applies safety checks |

### Skill → Task Resolution

```kotlin
class SkillResolver {
    fun resolve(skill: Skill, params: Map<String, String>): AutomationTask {
        // Validate all required params are present
        // Resolve {param} placeholders in steps
        // Wrap in AutomationTask with OPEN_APP and COMPLETE steps
        // Return ready-to-run task
    }
}
```

## Persistence

### Room Schema

```kotlin
@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val tags: String, // JSON [ "tag1", "tag2" ]
    val version: Int,
    val stepsJson: String, // JSON serialized steps
    val triggersJson: String, // JSON serialized triggers
    val enabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "skill_parameters")
data class SkillParameterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val skillId: String,
    val name: String,
    val displayName: String,
    val type: String,
    val defaultValue: String?,
    val required: Boolean,
)
```

JSON is used for steps/triggers because their structure varies and doesn't need querying.

### JSON Export Format

```json
{
  "name": "发送微信消息",
  "description": "给指定联系人发送微信消息",
  "tags": ["微信", "消息", "聊天", "whatsapp"],
  "version": 1,
  "parameters": [
    {
      "name": "contact",
      "displayName": "联系人",
      "type": "TEXT",
      "required": true
    },
    {
      "name": "message",
      "displayName": "消息内容",
      "type": "TEXT",
      "required": true
    }
  ],
  "steps": [
    {"orderIndex": 0, "type": "OPEN_APP", "label": "打开微信"},
    {"orderIndex": 1, "type": "CLICK_TEXT", "label": "搜索联系人", "selectorText": "搜索"},
    {"orderIndex": 2, "type": "INPUT_TEXT", "label": "输入联系人", "inputText": "{contact}"},
    {"orderIndex": 3, "type": "CLICK_TEXT", "label": "点击联系人", "selectorText": "{contact}"},
    {"orderIndex": 4, "type": "CLICK_TEXT", "label": "点击输入框", "selectorText": "输入框"},
    {"orderIndex": 5, "type": "INPUT_TEXT", "label": "输入消息", "inputText": "{message}"},
    {"orderIndex": 6, "type": "CLICK_TEXT", "label": "发送", "selectorText": "发送"}
  ],
  "triggers": [{"type": "MANUAL"}]
}
```

## UI Integration

### New Screens

| Screen | Purpose |
|--------|---------|
| `SkillListScreen` | Skill library, search, natural language input, enable/disable |
| `SkillEditorScreen` | Visual step editor, parameter config, trigger setup, JSON import/export |

### Navigation Flow

```
HomeScreen
  ├── TaskEditorScreen     (existing, for fixed tasks)
  ├── SkillListScreen      (NEW)
  │     └── SkillEditorScreen  (NEW)
  ├── ExecutionMonitorScreen (existing)
  ├── HistoryScreen         (existing)
  └── SettingsScreen        (existing, + LLM model config)
```

### SkillListScreen

```
┌─────────────────────────────┐
│  🔍 输入你想要做的事...      │  ← Natural language input
├─────────────────────────────┤
│  📦 我的技能                 │
│  ┌─────────────────────────┐│
│  │ 📱 发送微信消息     [启用] ││
│  │ 给指定联系人发送微信消息   ││
│  │ 触发: 手动 / 通知到达     ││
│  └─────────────────────────┘│
│  [+ 新建技能]  [📥 导入]    │
├─────────────────────────────┤
│  🏠  📦  📊  ⚙️             │
└─────────────────────────────┘
```

### SkillEditorScreen

```
┌─────────────────────────────┐
│  编辑技能              [保存]│
├─────────────────────────────┤
│  名称:  [发送微信消息      ] │
│  描述:  [给指定联系人发消息 ] │
│  标签:  [微信] [消息] [+添加]│
├─────────────────────────────┤
│  📋 参数                    │
│  ┌──────┬──────┬─────┬─────┐│
│  │ 名称 │ 显示名│ 类型│ 必填││
│  ├──────┼──────┼─────┼─────┤│
│  │contact│联系人│ 文本│  ✅ ││
│  │message│消息  │ 文本│  ✅ ││
│  └──────┴──────┴─────┴─────┘│
├─────────────────────────────┤
│  📝 步骤                    │
│  ① 打开微信                  │
│  ② 点击"搜索"               │
│  ③ 输入 {contact}           │
│  [+ 添加步骤]               │
├─────────────────────────────┤
│  ⚡ 触发器                  │
│  ☑ 手动触发                 │
│  ☑ 通知到达 (微信)          │
├─────────────────────────────┤
│  [📤 导出 JSON]  [📥 导入]  │
└─────────────────────────────┘
```

### Settings Updates

Add "端侧大模型" section:
- Model selection (Gemma 4, Qwen 2.5, Llama 3, etc.)
- Model status (downloading / ready)
- GPU acceleration toggle

## LLM Integration

### Model Loading

Use one of:
- [MLC LLM](https://github.com/mlc-ai/mlc-android) (recommended)
- MediaPipe LLM
- Custom ONNX Runtime

Models download on first run and stay local.

### Tool Calling Protocol

LLM outputs structured actions matching existing `AgentAction`:
- `click(x, y)` or `click(text, bounds)`
- `swipe(direction)` or `swipe(x1,y1,x2,y2)`
- `type(text)`
- `back()`
- `wait(ms)`
- `screenshot()`
- `complete()`

### Prompting Strategy

```
[SYSTEM] You are a phone automation assistant. Use these tools: tap, swipe, type, back, wait, screenshot, complete.

[SCREEN] Current package: com.tencent.mm
[UI_NODES] ... (simplified accessibility tree)
[SCREENSHOT] (base64 or pixel data)
[TASK] Send message "{message}" to "{contact}"

[THOUGHT] I need to find search button...
[ACTION] tap("搜索", bounds: {...})
```

## Migration Strategy

The skill system *extends* rather than replaces the existing `AutomationTask` system.

1. Keep `AutomationTask`/`TaskStep`/`ExecutionEngine` exactly as-is
2. Add `Skill`/`SkillStep`/`OnDeviceLLM`/`SkillTriggerManager` as new layers
3. Add `LLMBasedVisionAnalyzer` as alternative to `RuleBasedVisionAnalyzer`
4. Users can choose to use fixed tasks *or* skills

## Safety & Privacy

- All LLM inference on-device, no data leaves the phone
- Reuse existing `SafetyGuard` for sensitive operations
- User confirmation before skill execution (especially event-triggered ones)
- Model stays private to the user, no model upload

## Implementation Order (Prioritized)

1. Core data model + Room persistence
2. SkillResolver (Skill → AutomationTask)
3. Basic UI (SkillListScreen, SkillEditorScreen)
4. JSON import/export
5. Manual trigger support
6. SkillTriggerManager for notification/app-switch/schedule/network
7. OnDeviceLLM interface + initial rule-based fallback
8. Actual LLM integration (MLC)
9. Natural language intent matching with LLM
