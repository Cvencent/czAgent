# Mobile Agent Design

## Overview

This project is a pure Android local app for operating phone software through configurable tasks, scheduled jobs, and shortcut commands. The first version is a working local automation foundation rather than a full multimodal AI release. It must run on the phone, keep user data local, and provide a clean path to plug in an on-device vision-language model later.

The original design document focused on automated app check-ins. This spec generalizes that into a phone operation agent. Check-ins remain one use case, but the product should support broader workflows such as opening app pages, collecting rewards, sending predefined text, navigating repeated flows, and running user-defined command sequences.

## Goals

- Build a pure Android local app using Kotlin and Jetpack Compose.
- Let users create tasks from a name, natural-language description, target app, optional schedule, and executable steps.
- Support shortcut commands for frequently used tasks.
- Support scheduled tasks through Android-local scheduling.
- Execute basic phone operations through AccessibilityService.
- Capture screen snapshots through MediaProjection after user authorization.
- Record execution logs, status, success, failure, duration, and failure reason.
- Define a vision/agent interface that can later be backed by MiniCPM-V, llama.cpp, or another local multimodal model.
- Ship the first version with a rule-based analyzer that uses Accessibility UI tree data and configured steps.
- Require explicit confirmation for sensitive actions.

## Non-Goals For Version 1

- No bundled MiniCPM-V model.
- No llama.cpp or JNI model runtime integration.
- No NPU acceleration work.
- No cloud service.
- No remote control console.
- No attempt to bypass Android permissions, app security boundaries, captchas, payment confirmations, or account-protection flows.
- No automatic execution of high-risk operations such as payment, transfer, account authorization, deletion, content publishing, or password entry.

## Product Shape

The app has five main surfaces:

1. Home
   - Shows task list, shortcut commands, and recent execution state.
   - Allows manual task execution.

2. Task Editor
   - Captures task name, description, target app, schedule, and step list.
   - Supports steps such as open app, wait, click text, click coordinates, swipe, input text, back, screenshot, and complete.

3. Execution Monitor
   - Shows current task, current step, logs, recent screenshot, and running status.
   - Provides pause, resume, and stop controls.

4. History
   - Lists past task runs with status, duration, timestamps, and failure reason.

5. Settings
   - Shows Accessibility permission state.
   - Shows screenshot permission state.
   - Configures sensitive-action confirmation.
   - Shows analyzer mode, initially rule-based.

## Architecture

The first version uses a layered Android architecture:

- UI layer: Jetpack Compose screens and ViewModels.
- Task management layer: task storage, shortcut commands, scheduling, and run history.
- Agent layer: task planning, step execution loop, result verification, retry, timeout, cancellation, and safety checks.
- Observation layer: Accessibility UI tree snapshots and MediaProjection screenshots.
- Execution layer: AccessibilityService actions.
- Persistence layer: Room database.

The execution engine depends on interfaces instead of Android framework classes where practical, so the core task loop can be unit tested without a device.

## Core Components

### TaskManager

TaskManager owns task CRUD, shortcut command management, and task lookup. It reads and writes Room entities through repositories and exposes task data to ViewModels.

### TaskScheduler

TaskScheduler uses WorkManager for scheduled tasks. It maps enabled task schedules to unique work names and enqueues or cancels jobs when task schedule settings change.

Version 1 scheduling supports:

- Manual run.
- One-tap shortcut run.
- Daily scheduled run at a configured local time.

### ExecutionEngine

ExecutionEngine runs a task from start to terminal state. It:

- Loads task steps.
- Creates an execution session.
- Checks SafetyGuard before sensitive steps.
- Captures current screen context.
- Asks AgentPlanner or VisionAnalyzer for the next action when needed.
- Executes actions through ActionExecutor.
- Records per-step logs.
- Retries transient failures within configured limits.
- Stops on cancellation, timeout, unrecoverable failure, or completion.

Version 1 limits:

- Maximum 50 steps per run.
- Maximum 3 retries per step.
- Maximum 5 minutes per task run by default.

### ActionExecutor

ActionExecutor wraps AccessibilityService operations:

- Open app by package name.
- Click by visible text.
- Click by coordinates.
- Swipe.
- Input text into focused or matched field.
- Press back.
- Wait.

Click-by-text should prefer Accessibility node matching over coordinates. Coordinates remain available for workflows where UI text is absent.

### ScreenObserver

ScreenObserver provides:

- Current foreground package and activity when available.
- Accessibility node tree snapshot.
- Optional screenshot bitmap after MediaProjection permission is granted.

If screenshot permission is not granted, the execution engine continues with Accessibility data only.

### AgentPlanner

AgentPlanner converts task descriptions and configured steps into executable actions.

Version 1 behavior:

- For explicitly configured steps, follow the step list.
- For natural-language descriptions without explicit steps, generate a simple draft plan only when the requested intent matches supported built-in patterns.
- Otherwise ask the user to define steps before execution.

This keeps the first release predictable and avoids pretending to have full AI behavior before the local model exists.

### VisionAnalyzer

VisionAnalyzer is the future extension point for local multimodal reasoning.

```kotlin
interface VisionAnalyzer {
    suspend fun analyze(
        screen: ScreenSnapshot,
        taskContext: TaskContext
    ): AgentDecision
}
```

Version 1 ships:

```kotlin
class RuleBasedVisionAnalyzer : VisionAnalyzer
```

RuleBasedVisionAnalyzer uses the Accessibility tree, visible text, task step metadata, and simple state rules. It does not require a downloaded model.

### SafetyGuard

SafetyGuard detects sensitive actions before execution. It blocks or requests user confirmation for:

- Payment.
- Transfer.
- Purchase.
- Deletion.
- Login authorization.
- Permission authorization.
- Publishing public content.
- Password or verification-code input.

Sensitive matching uses action type, step label, text to input, and visible screen text. When in doubt, require confirmation.

### MemoryStore

MemoryStore records successful and failed runs. Version 1 stores:

- Task run metadata.
- Step logs.
- Last successful path for a task.
- Failure reason.
- Basic screen/action transition data.

The memory system is intentionally simple in version 1. It prepares the database shape for future path reuse but does not try to optimize agent behavior aggressively.

## Data Model

Room stores the following entities:

- TaskEntity
  - id
  - name
  - description
  - targetPackage
  - enabled
  - createdAt
  - updatedAt

- TaskStepEntity
  - id
  - taskId
  - orderIndex
  - type
  - selectorText
  - x
  - y
  - inputText
  - swipeDirection
  - waitMillis
  - requiresConfirmation

- TaskScheduleEntity
  - id
  - taskId
  - enabled
  - type
  - localTime

- ShortcutEntity
  - id
  - taskId
  - label
  - sortOrder

- TaskRunEntity
  - id
  - taskId
  - status
  - startedAt
  - endedAt
  - failureReason

- StepLogEntity
  - id
  - runId
  - stepId
  - status
  - message
  - timestamp

## Execution Flow

1. User starts a task manually, through a shortcut, or through WorkManager.
2. ExecutionEngine creates a TaskRunEntity with status `RUNNING`.
3. ExecutionEngine validates permissions required by the task.
4. For each step:
   - SafetyGuard checks whether confirmation is required.
   - ScreenObserver collects current screen context.
   - VisionAnalyzer resolves the action target if the step needs screen matching.
   - ActionExecutor executes the action.
   - ExecutionEngine waits for UI settle time.
   - ScreenObserver collects updated context.
   - ExecutionEngine records step result.
5. On failure, ExecutionEngine retries the step up to the retry limit if the error is transient.
6. On completion, cancellation, timeout, or unrecoverable failure, ExecutionEngine finalizes the run and writes logs.

## Error Handling

Version 1 error categories:

- PermissionMissing
  - The user has not enabled AccessibilityService or screenshot permission required by the task.

- TargetNotFound
  - The configured text, node, or package cannot be found.

- ActionFailed
  - Accessibility action returned failure or did not produce a detectable state change.

- SafetyConfirmationRequired
  - A sensitive action requires user approval.

- Timeout
  - The task exceeded the configured maximum runtime.

- Cancelled
  - The user stopped the task.

Recovery rules:

- Retry TargetNotFound and ActionFailed up to 3 times with a short delay and fresh screen observation.
- Stop immediately on PermissionMissing and show the required permission.
- Pause for explicit confirmation on SafetyConfirmationRequired.
- Stop on Timeout and Cancelled.

## Permissions And Safety

The app must clearly explain why each permission is needed before sending users to Android settings.

Required permissions:

- AccessibilityService for UI operations.
- MediaProjection for screenshots, only when screenshot-based observation is enabled.
- Notification permission where needed for foreground execution visibility.

Safety principles:

- Never hide that automation is running.
- Never execute sensitive actions silently.
- Never store passwords or verification codes in logs.
- Redact sensitive input text from StepLogEntity.
- Make stopping a running task easy from the execution monitor.

## Testing Strategy

Unit tests:

- ExecutionEngine state transitions.
- Retry and timeout behavior.
- SafetyGuard keyword and action classification.
- RuleBasedVisionAnalyzer matching against synthetic screen snapshots.
- Task schedule mapping.

Instrumented tests:

- Room migrations and DAO behavior.
- Compose task editor basic flows.
- Accessibility action wrapper where Android test environment allows it.

Manual device validation:

- Enable AccessibilityService.
- Create a task that opens an app and clicks visible text.
- Create a shortcut command for the same task.
- Create a daily schedule and verify WorkManager enqueues it.
- Run a task that requires screenshot permission.
- Trigger a sensitive action and verify confirmation is required.

## Version 1 Acceptance Criteria

- The app builds and launches on an Android device or emulator.
- Users can create, edit, delete, and run tasks.
- Users can define a sequence of supported local automation steps.
- Users can create shortcut commands.
- Users can configure daily scheduled tasks.
- AccessibilityService can perform supported actions after permission is enabled.
- The app can request screenshot permission and display the latest captured screenshot in the execution monitor.
- Execution logs are stored and visible in history.
- Sensitive actions require confirmation before execution.
- The execution engine is covered by unit tests for success, failure, retry, timeout, cancellation, and safety confirmation.

## Future Phases

Phase 2 integrates a local multimodal model through VisionAnalyzer:

- MiniCPM-V or equivalent model packaging.
- llama.cpp or compatible Android runtime.
- JNI bridge.
- Screenshot preprocessing.
- JSON decision parsing.
- Performance profiling.

Phase 3 improves agent autonomy:

- Manager/Worker/Reflector split.
- Replanning.
- Loop detection.
- More advanced memory reuse.
- Cross-app reusable subtask library.

Phase 4 broadens product quality:

- Multi-app validation.
- Battery profiling.
- Long-running reliability tests.
- Better permission education.
- Import/export of task recipes.
