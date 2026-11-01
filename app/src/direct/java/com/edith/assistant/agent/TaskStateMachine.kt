package com.edith.assistant.agent

enum class TaskState {
    IDLE,
    OPENING_APP,
    OBSERVING,
    RESOLVING_TARGET,
    VALIDATING_TARGET,
    EXECUTING_ACTION,
    WAITING_FOR_UI,
    VERIFYING,
    NEXT_STEP,
    RETRY,
    REOBSERVE,
    WAITING_FOR_CONFIRMATION,
    WAITING_FOR_USER,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class ExecPhase {
    PLANNING,
    NAVIGATING,
    INPUTTING,
    VERIFYING,
    CONFIRMING
}

enum class ActionMethod {
    ACCESSIBILITY_CLICK,
    GESTURE_ON_ELEMENT_BOUNDS,
    VISION_TAP,
    SET_TEXT
}

enum class Blocker {
    LOGIN,
    CAPTCHA,
    TWO_FACTOR,
    SECURITY_CHECK,
    PERMISSION_PROMPT
}

enum class FailureReason {
    APP_NOT_INSTALLED,
    TARGET_NOT_FOUND,
    VERIFICATION_FAILED,
    STALE_SCREEN,
    TIMEOUT,
    REJECTED_BY_USER
}

enum class Platform {
    INSTAGRAM,
    FACEBOOK
}

enum class SocialAction {
    CREATE_POST,
    CREATE_REEL,
    CREATE_STORY,
    TEXT_POST
}

enum class TaskMode {
    PUBLISH,
    DRAFT
}

enum class TaskStatus {
    RUNNING,
    PAUSED_CONFIRMATION,
    PAUSED_BLOCKER,
    COMPLETED,
    SUBMITTED_UNVERIFIED,
    FAILED,
    CANCELLED
}

enum class ControlCommand {
    CONFIRM,
    REJECT,
    RESUME,
    CANCEL,
    STATUS
}

class TaskStateMachine(initialState: TaskState = TaskState.IDLE) {
    var currentState: TaskState = initialState
        private set

    private val _history = mutableListOf(initialState)
    val history: List<TaskState> get() = _history.toList()

    private val terminalStates = setOf(TaskState.COMPLETED, TaskState.FAILED, TaskState.CANCELLED)

    @Synchronized
    fun moveTo(newState: TaskState) {
        if (currentState in terminalStates) {
            throw IllegalStateException("Cannot transition from terminal state $currentState to $newState")
        }

        // FAILED and CANCELLED reachable from all non-terminal states
        if (newState == TaskState.FAILED || newState == TaskState.CANCELLED) {
            currentState = newState
            _history.add(newState)
            return
        }

        val isValid = when (currentState) {
            TaskState.IDLE -> newState == TaskState.OPENING_APP || newState == TaskState.OBSERVING
            TaskState.OPENING_APP -> newState == TaskState.OBSERVING || newState == TaskState.WAITING_FOR_UI
            TaskState.WAITING_FOR_UI -> newState == TaskState.OBSERVING || newState == TaskState.VERIFYING
            TaskState.OBSERVING -> newState == TaskState.RESOLVING_TARGET || newState == TaskState.WAITING_FOR_USER
            TaskState.RESOLVING_TARGET -> newState == TaskState.VALIDATING_TARGET || newState == TaskState.REOBSERVE
            TaskState.VALIDATING_TARGET -> newState == TaskState.EXECUTING_ACTION || newState == TaskState.WAITING_FOR_CONFIRMATION
            TaskState.WAITING_FOR_CONFIRMATION -> newState == TaskState.EXECUTING_ACTION || newState == TaskState.CANCELLED
            TaskState.WAITING_FOR_USER -> newState == TaskState.OBSERVING || newState == TaskState.REOBSERVE
            TaskState.EXECUTING_ACTION -> newState == TaskState.WAITING_FOR_UI || newState == TaskState.VERIFYING
            TaskState.VERIFYING -> newState == TaskState.NEXT_STEP || newState == TaskState.RETRY || newState == TaskState.COMPLETED
            TaskState.NEXT_STEP -> newState == TaskState.OBSERVING || newState == TaskState.RESOLVING_TARGET || newState == TaskState.COMPLETED
            TaskState.RETRY -> newState == TaskState.OBSERVING || newState == TaskState.RESOLVING_TARGET
            TaskState.REOBSERVE -> newState == TaskState.OBSERVING
            else -> false
        }

        if (!isValid) {
            throw IllegalStateException("Illegal state transition from $currentState to $newState")
        }

        currentState = newState
        _history.add(newState)
    }
}
