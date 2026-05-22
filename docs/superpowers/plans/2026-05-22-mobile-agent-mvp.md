# Mobile Agent MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first version of the pure Android local phone-operation agent described in `docs/superpowers/specs/2026-05-22-mobile-agent-design.md`.

**Architecture:** Create a Kotlin + Jetpack Compose Android app with a testable core automation engine isolated from Android framework APIs. Android services and UI depend on interfaces so the execution loop, safety checks, rule-based analyzer, and scheduling mappings can be unit tested.

**Tech Stack:** Kotlin, Android Gradle Plugin, Jetpack Compose, Room, WorkManager, Kotlin coroutines, JUnit.

---

## Environment Notes

This workstation currently has no `java`, `gradle`, `ANDROID_HOME`, or `adb` on PATH. Use a local `.local/` tools directory for downloaded JDK, Gradle, or Android SDK artifacts if verification is required. Keep `.local/` out of Git.

## File Structure

- `settings.gradle.kts` configures the Gradle project.
- `build.gradle.kts` configures shared plugin versions.
- `gradle.properties` configures AndroidX, Kotlin, and Gradle behavior.
- `app/build.gradle.kts` defines Android app dependencies.
- `app/src/main/AndroidManifest.xml` declares the app, AccessibilityService, and permissions.
- `app/src/main/res/xml/mobile_agent_accessibility_service.xml` configures AccessibilityService.
- `app/src/main/java/com/czagent/MainActivity.kt` hosts Compose UI.
- `app/src/main/java/com/czagent/core/model/Models.kt` defines task, step, action, screen, and run domain models.
- `app/src/main/java/com/czagent/core/safety/SafetyGuard.kt` classifies sensitive actions.
- `app/src/main/java/com/czagent/core/vision/VisionAnalyzer.kt` defines analyzer contracts and rule-based implementation.
- `app/src/main/java/com/czagent/core/engine/ExecutionEngine.kt` runs tasks through testable interfaces.
- `app/src/main/java/com/czagent/core/scheduler/ScheduleCalculator.kt` calculates daily schedule delays.
- `app/src/main/java/com/czagent/data/Entities.kt` defines Room entities.
- `app/src/main/java/com/czagent/data/Dao.kt` defines Room DAO contracts.
- `app/src/main/java/com/czagent/data/AppDatabase.kt` creates Room database.
- `app/src/main/java/com/czagent/android/automation/MobileAgentAccessibilityService.kt` performs Accessibility actions.
- `app/src/main/java/com/czagent/android/automation/AndroidActionExecutor.kt` adapts AccessibilityService to `ActionExecutor`.
- `app/src/main/java/com/czagent/android/observation/AndroidScreenObserver.kt` adapts Accessibility and screenshot state to `ScreenObserver`.
- `app/src/main/java/com/czagent/android/scheduler/TaskRunWorker.kt` is the WorkManager entry point.
- `app/src/test/java/com/czagent/core/safety/SafetyGuardTest.kt` covers sensitive action detection.
- `app/src/test/java/com/czagent/core/vision/RuleBasedVisionAnalyzerTest.kt` covers rule-based screen matching.
- `app/src/test/java/com/czagent/core/engine/ExecutionEngineTest.kt` covers success, retry, safety, timeout, and cancellation.
- `app/src/test/java/com/czagent/core/scheduler/ScheduleCalculatorTest.kt` covers daily schedule delay calculation.

## Task 1: Project Scaffolding

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `.gitignore`

- [ ] **Step 1: Create Gradle Android project files**

Use an Android app module named `app`, package namespace `com.czagent`, min SDK 26, target/compile SDK 35, Kotlin JVM target 17, Compose enabled, and dependencies for Compose, lifecycle, Room, WorkManager, coroutines, and JUnit.

- [ ] **Step 2: Ignore local tool downloads and build outputs**

Ensure `.gitignore` contains:

```gitignore
.claude/settings.local.json
.gradle/
.local/
build/
app/build/
local.properties
*.apk
*.aab
```

- [ ] **Step 3: Verify project files are present**

Run: `git status --short`

Expected: Gradle project files appear as untracked or modified files.

## Task 2: Domain Models And SafetyGuard

**Files:**
- Create: `app/src/main/java/com/czagent/core/model/Models.kt`
- Create: `app/src/main/java/com/czagent/core/safety/SafetyGuard.kt`
- Create: `app/src/test/java/com/czagent/core/safety/SafetyGuardTest.kt`

- [ ] **Step 1: Write failing SafetyGuard tests**

Cover these behaviors:

```kotlin
@Test fun `payment text requires confirmation`()
@Test fun `password input requires confirmation and redaction`()
@Test fun `safe navigation click is allowed`()
@Test fun `explicit confirmation flag requires confirmation`()
```

- [ ] **Step 2: Run test and verify red**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.czagent.core.safety.SafetyGuardTest`

Expected: FAIL because `SafetyGuard` and domain models do not exist yet.

- [ ] **Step 3: Implement domain models and SafetyGuard**

Define task, step, screen, action, and run status models. Implement `SafetyGuard.evaluate(step, screen)` returning `SafetyDecision.Allowed` or `SafetyDecision.RequiresConfirmation(reason, redactedText)`.

- [ ] **Step 4: Run test and verify green**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.czagent.core.safety.SafetyGuardTest`

Expected: PASS.

## Task 3: Rule-Based Vision Analyzer

**Files:**
- Create: `app/src/main/java/com/czagent/core/vision/VisionAnalyzer.kt`
- Create: `app/src/test/java/com/czagent/core/vision/RuleBasedVisionAnalyzerTest.kt`

- [ ] **Step 1: Write failing analyzer tests**

Cover:

```kotlin
@Test fun `click text step resolves matching accessibility node`()
@Test fun `coordinate step resolves coordinates without node`()
@Test fun `missing text returns target not found`()
```

- [ ] **Step 2: Run test and verify red**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.czagent.core.vision.RuleBasedVisionAnalyzerTest`

Expected: FAIL because analyzer classes do not exist yet.

- [ ] **Step 3: Implement VisionAnalyzer and RuleBasedVisionAnalyzer**

Define `VisionAnalyzer`, `AgentDecision`, and `RuleBasedVisionAnalyzer`. Prefer visible-text matching against `ScreenNode.text` and return target-not-found when no node matches.

- [ ] **Step 4: Run test and verify green**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.czagent.core.vision.RuleBasedVisionAnalyzerTest`

Expected: PASS.

## Task 4: Execution Engine

**Files:**
- Create: `app/src/main/java/com/czagent/core/engine/ExecutionEngine.kt`
- Create: `app/src/test/java/com/czagent/core/engine/ExecutionEngineTest.kt`

- [ ] **Step 1: Write failing ExecutionEngine tests**

Cover:

```kotlin
@Test fun `runs steps to completion and records logs`()
@Test fun `retries transient target not found three times`()
@Test fun `stops when safety confirmation is required`()
@Test fun `stops when max steps is exceeded`()
@Test fun `honors cancellation before next step`()
```

- [ ] **Step 2: Run test and verify red**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.czagent.core.engine.ExecutionEngineTest`

Expected: FAIL because `ExecutionEngine` does not exist yet.

- [ ] **Step 3: Implement ExecutionEngine and test fakes**

Define interfaces:

```kotlin
interface ScreenObserver { suspend fun observe(): ScreenSnapshot }
interface ActionExecutor { suspend fun execute(action: AgentAction): ActionResult }
interface RunLogger { suspend fun log(entry: StepLog); suspend fun finish(status: RunStatus, reason: String?) }
interface CancellationSignal { fun isCancellationRequested(): Boolean }
```

Implement sequential execution with safety checks, analyzer resolution, action execution, retry for transient failures, max-step guard, and cancellation.

- [ ] **Step 4: Run test and verify green**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.czagent.core.engine.ExecutionEngineTest`

Expected: PASS.

## Task 5: Persistence Layer

**Files:**
- Create: `app/src/main/java/com/czagent/data/Entities.kt`
- Create: `app/src/main/java/com/czagent/data/Dao.kt`
- Create: `app/src/main/java/com/czagent/data/AppDatabase.kt`

- [ ] **Step 1: Add Room entities and DAO contracts**

Create entities from the spec: task, task step, task schedule, shortcut, task run, and step log. Use enum values stored as strings through type converters.

- [ ] **Step 2: Wire Room database**

Create `AppDatabase` with version 1 and DAOs for task and run data.

- [ ] **Step 3: Compile**

Run: `.\gradlew.bat :app:compileDebugKotlin`

Expected: PASS.

## Task 6: Android Automation Adapters

**Files:**
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/xml/mobile_agent_accessibility_service.xml`
- Create: `app/src/main/java/com/czagent/android/automation/MobileAgentAccessibilityService.kt`
- Create: `app/src/main/java/com/czagent/android/automation/AndroidActionExecutor.kt`
- Create: `app/src/main/java/com/czagent/android/observation/AndroidScreenObserver.kt`

- [ ] **Step 1: Declare permissions and AccessibilityService**

Manifest must declare AccessibilityService and foreground-service-related permissions where needed.

- [ ] **Step 2: Implement AccessibilityService state holder**

Track foreground package/activity and expose the active root node to Android adapters.

- [ ] **Step 3: Implement AndroidActionExecutor**

Support open app, click text, click coordinates, swipe, input text, back, wait, screenshot marker, and complete marker.

- [ ] **Step 4: Implement AndroidScreenObserver**

Create `ScreenSnapshot` from current package, activity, and flattened Accessibility node tree. Screenshot capture status is represented by `latestScreenshotUri: String?`, which remains `null` until MediaProjection capture is implemented in a later task.

- [ ] **Step 5: Compile**

Run: `.\gradlew.bat :app:compileDebugKotlin`

Expected: PASS.

## Task 7: Scheduler

**Files:**
- Create: `app/src/main/java/com/czagent/core/scheduler/ScheduleCalculator.kt`
- Create: `app/src/test/java/com/czagent/core/scheduler/ScheduleCalculatorTest.kt`
- Create: `app/src/main/java/com/czagent/android/scheduler/TaskRunWorker.kt`

- [ ] **Step 1: Write failing schedule tests**

Cover same-day future time and next-day rollover.

- [ ] **Step 2: Run test and verify red**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.czagent.core.scheduler.ScheduleCalculatorTest`

Expected: FAIL because `ScheduleCalculator` does not exist yet.

- [ ] **Step 3: Implement schedule delay calculator and WorkManager worker skeleton**

Add pure Kotlin daily-delay calculation and a `CoroutineWorker` that looks up `taskId` input data. If repository wiring is not ready, return failure with a clear message.

- [ ] **Step 4: Run test and verify green**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.czagent.core.scheduler.ScheduleCalculatorTest`

Expected: PASS.

## Task 8: Compose MVP UI

**Files:**
- Create: `app/src/main/java/com/czagent/MainActivity.kt`
- Create: `app/src/main/java/com/czagent/ui/AppState.kt`
- Create: `app/src/main/java/com/czagent/ui/MobileAgentApp.kt`
- Create: `app/src/main/java/com/czagent/ui/screens/HomeScreen.kt`
- Create: `app/src/main/java/com/czagent/ui/screens/TaskEditorScreen.kt`
- Create: `app/src/main/java/com/czagent/ui/screens/ExecutionMonitorScreen.kt`
- Create: `app/src/main/java/com/czagent/ui/screens/HistoryScreen.kt`
- Create: `app/src/main/java/com/czagent/ui/screens/SettingsScreen.kt`

- [ ] **Step 1: Build Compose shell**

Create a single-activity app with bottom navigation for Home, Tasks, Monitor, History, and Settings.

- [ ] **Step 2: Add task editing form**

Support task name, description, package name, quick step creation, schedule toggle, and save into in-memory app state for the MVP shell.

- [ ] **Step 3: Add execution monitor and history panels backed by app state**

Show current status, step logs, and recent runs. Avoid fake AI claims; analyzer label should read `Rule-based`.

- [ ] **Step 4: Add settings permission actions**

Add buttons to open Accessibility settings and show screenshot permission as not connected until MediaProjection capture is fully implemented.

- [ ] **Step 5: Compile**

Run: `.\gradlew.bat :app:assembleDebug`

Expected: PASS.

## Task 9: Final Verification And Commit

**Files:**
- Modify as needed based on verification failures.

- [ ] **Step 1: Run unit tests**

Run: `.\gradlew.bat :app:testDebugUnitTest`

Expected: PASS.

- [ ] **Step 2: Build debug APK**

Run: `.\gradlew.bat :app:assembleDebug`

Expected: PASS and APK at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 3: Check Git status**

Run: `git status --short`

Expected: only intended files are modified or added.

- [ ] **Step 4: Commit**

Run:

```bash
git add .
git commit -m "feat: scaffold mobile agent mvp"
```

Expected: commit succeeds.

- [ ] **Step 5: Push**

Run: `git push`

Expected: branch `main` is pushed to `origin/main`.
