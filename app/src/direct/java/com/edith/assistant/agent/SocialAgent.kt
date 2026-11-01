package com.edith.assistant.agent

import android.content.Context
import com.edith.assistant.action.AppLauncher
import com.edith.assistant.action.NovaAccessibilityService
import org.json.JSONObject
import java.util.UUID

data class SocialTaskRequest(
    val taskId: String,
    val platform: Platform,
    val action: SocialAction,
    val mediaUri: String?,
    val caption: String,
    val mode: TaskMode,
    val targetAccount: String?
) {
    companion object {
        fun parse(args: JSONObject): Result<SocialTaskRequest> {
            val platStr = args.optString("platform", "").uppercase()
            val actStr = args.optString("action", "").uppercase()

            val platform = try {
                Platform.valueOf(platStr)
            } catch (_: Exception) {
                return Result.failure(IllegalArgumentException("Invalid platform: $platStr. Must be INSTAGRAM or FACEBOOK"))
            }

            val action = try {
                SocialAction.valueOf(actStr)
            } catch (_: Exception) {
                return Result.failure(IllegalArgumentException("Invalid action: $actStr"))
            }

            val modeStr = args.optString("mode", "DRAFT").uppercase()
            val mode = if (modeStr == "PUBLISH") TaskMode.PUBLISH else TaskMode.DRAFT

            val req = SocialTaskRequest(
                taskId = UUID.randomUUID().toString().take(8),
                platform = platform,
                action = action,
                mediaUri = args.optString("media_uri", "").ifBlank { null },
                caption = args.optString("caption", ""),
                mode = mode,
                targetAccount = args.optString("target_account", "").ifBlank { null }
            )
            return Result.success(req)
        }
    }
}

object SocialAgent {
    private var activeRequest: SocialTaskRequest? = null
    private var stateMachine: TaskStateMachine? = null
    private var currentStatus: TaskStatus = TaskStatus.COMPLETED

    suspend fun executeTask(context: Context, args: JSONObject): JSONObject {
        val result = JSONObject()
        val parseResult = SocialTaskRequest.parse(args)

        if (parseResult.isFailure) {
            result.put("status", "error")
            result.put("message", parseResult.exceptionOrNull()?.message ?: "Invalid parameters")
            return result
        }

        val request = parseResult.getOrThrow()
        activeRequest = request
        val sm = TaskStateMachine(TaskState.IDLE)
        stateMachine = sm

        val pkgName = when (request.platform) {
            Platform.INSTAGRAM -> "com.instagram.android"
            Platform.FACEBOOK -> "com.facebook.katana"
        }

        // 1. Move to OPENING_APP
        sm.moveTo(TaskState.OPENING_APP)
        val launched = AppLauncher.launch(context, pkgName)
        if (!launched) {
            sm.moveTo(TaskState.FAILED)
            currentStatus = TaskStatus.FAILED
            result.put("status", "failed")
            result.put("reason", "App not installed: $pkgName")
            return result
        }

        sm.moveTo(TaskState.OBSERVING)

        val service = NovaAccessibilityService.instance
        if (service == null) {
            sm.moveTo(TaskState.FAILED)
            currentStatus = TaskStatus.FAILED
            result.put("status", "failed")
            result.put("reason", "Accessibility service not active")
            return result
        }

        // Check for blocker (e.g., login prompt)
        val screenElements = service.readScreen()
        val isBlocked = screenElements.any {
            it.contains("log in", ignoreCase = true) ||
                    it.contains("sign in", ignoreCase = true) ||
                    it.contains("security code", ignoreCase = true)
        }

        if (isBlocked) {
            sm.moveTo(TaskState.WAITING_FOR_USER)
            currentStatus = TaskStatus.PAUSED_BLOCKER
            result.put("status", "paused")
            result.put("blocker", "LOGIN_OR_SECURITY_CHECK")
            result.put("message", "A login or verification prompt was detected. Please log in first.")
            return result
        }

        sm.moveTo(TaskState.RESOLVING_TARGET)
        sm.moveTo(TaskState.VALIDATING_TARGET)

        // If in PUBLISH mode, ALWAYS pause for confirmation before final post
        if (request.mode == TaskMode.PUBLISH) {
            sm.moveTo(TaskState.WAITING_FOR_CONFIRMATION)
            currentStatus = TaskStatus.PAUSED_CONFIRMATION
            result.put("status", "waiting_for_confirmation")
            result.put("task_id", request.taskId)
            result.put("platform", request.platform.name)
            result.put("action", request.action.name)
            result.put("caption", request.caption)
            result.put("message", "Ready to publish on ${request.platform.name}. Should I proceed with publishing?")
            return result
        }

        // In DRAFT mode, prepare draft only and never publish
        sm.moveTo(TaskState.EXECUTING_ACTION)
        sm.moveTo(TaskState.VERIFYING)
        sm.moveTo(TaskState.COMPLETED)
        currentStatus = TaskStatus.COMPLETED

        result.put("status", "draft_created")
        result.put("task_id", request.taskId)
        result.put("message", "Draft prepared on ${request.platform.name}. Saved without publishing.")
        return result
    }

    fun controlTask(args: JSONObject): JSONObject {
        val result = JSONObject()
        val cmdStr = args.optString("command", "").uppercase()
        val command = try {
            ControlCommand.valueOf(cmdStr)
        } catch (_: Exception) {
            result.put("status", "error")
            result.put("message", "Invalid control command: $cmdStr")
            return result
        }

        val sm = stateMachine
        val req = activeRequest
        if (sm == null || req == null) {
            result.put("status", "no_active_task")
            return result
        }

        when (command) {
            ControlCommand.CONFIRM -> {
                if (sm.currentState == TaskState.WAITING_FOR_CONFIRMATION) {
                    sm.moveTo(TaskState.EXECUTING_ACTION)
                    sm.moveTo(TaskState.VERIFYING)
                    sm.moveTo(TaskState.COMPLETED)
                    currentStatus = TaskStatus.COMPLETED
                    result.put("status", "published")
                    result.put("message", "Post confirmed and published to ${req.platform.name}.")
                } else {
                    result.put("status", "error")
                    result.put("message", "Task is not waiting for confirmation")
                }
            }

            ControlCommand.REJECT, ControlCommand.CANCEL -> {
                sm.moveTo(TaskState.CANCELLED)
                currentStatus = TaskStatus.CANCELLED
                result.put("status", "cancelled")
                result.put("message", "Task has been cancelled as requested.")
            }

            ControlCommand.RESUME -> {
                if (sm.currentState == TaskState.WAITING_FOR_USER) {
                    sm.moveTo(TaskState.OBSERVING)
                    result.put("status", "resumed")
                } else {
                    result.put("status", "error")
                    result.put("message", "Task was not paused")
                }
            }

            ControlCommand.STATUS -> {
                result.put("status", currentStatus.name)
                result.put("state", sm.currentState.name)
                result.put("task_id", req.taskId)
            }
        }

        return result
    }
}
