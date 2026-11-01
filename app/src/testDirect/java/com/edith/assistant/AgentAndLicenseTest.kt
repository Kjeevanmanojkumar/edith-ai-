package com.edith.assistant

import com.edith.assistant.action.ContactResolver
import com.edith.assistant.agent.Platform
import com.edith.assistant.agent.SocialAction
import com.edith.assistant.agent.SocialTaskRequest
import com.edith.assistant.agent.TaskMode
import com.edith.assistant.agent.TaskState
import com.edith.assistant.agent.TaskStateMachine
import com.edith.assistant.license.AccessKeyRules
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentAndLicenseTest {

    @Test
    fun testAccessKeyRulesEvaluation() {
        // Community default when no key
        val community = AccessKeyRules.evaluate(
            hasKey = false,
            plan = "",
            expiresAt = 0L,
            blocked = false
        )
        assertTrue(community.active)
        assertEquals("Community", community.plan)
        assertFalse(community.hasRememberedKey)

        // Blocked key
        val blocked = AccessKeyRules.evaluate(
            hasKey = true,
            plan = "Pro",
            expiresAt = System.currentTimeMillis() + 100000L,
            blocked = true
        )
        assertFalse(blocked.active)
        assertTrue(blocked.blocked)

        // Expired key
        val expired = AccessKeyRules.evaluate(
            hasKey = true,
            plan = "Pro",
            expiresAt = 1000L,
            blocked = false,
            currentTime = 2000L
        )
        assertFalse(expired.active)
        assertEquals("License Expired", expired.statusText)

        // Active valid key
        val active = AccessKeyRules.evaluate(
            hasKey = true,
            plan = "Pro Elite",
            expiresAt = 5000L,
            blocked = false,
            currentTime = 2000L
        )
        assertTrue(active.active)
        assertEquals("Pro Elite", active.plan)
    }

    @Test
    fun testTaskStateMachineTransitions() {
        val sm = TaskStateMachine(TaskState.IDLE)
        assertEquals(TaskState.IDLE, sm.currentState)

        sm.moveTo(TaskState.OPENING_APP)
        assertEquals(TaskState.OPENING_APP, sm.currentState)

        sm.moveTo(TaskState.OBSERVING)
        sm.moveTo(TaskState.RESOLVING_TARGET)
        sm.moveTo(TaskState.VALIDATING_TARGET)
        sm.moveTo(TaskState.WAITING_FOR_CONFIRMATION)

        // FAILED is reachable from any non-terminal state
        sm.moveTo(TaskState.FAILED)
        assertEquals(TaskState.FAILED, sm.currentState)
    }

    @Test(expected = IllegalStateException::class)
    fun testTaskStateMachineTerminalExitThrows() {
        val sm = TaskStateMachine(TaskState.IDLE)
        sm.moveTo(TaskState.FAILED)
        // Transitioning from terminal state FAILED must throw
        sm.moveTo(TaskState.OBSERVING)
    }

    @Test
    fun testSocialTaskRequestParsing() {
        val json = JSONObject()
            .put("platform", "instagram")
            .put("action", "create_post")
            .put("caption", "My awesome photo")
            .put("mode", "publish")

        val result = SocialTaskRequest.parse(json)
        assertTrue(result.isSuccess)
        val req = result.getOrThrow()
        assertEquals(Platform.INSTAGRAM, req.platform)
        assertEquals(SocialAction.CREATE_POST, req.action)
        assertEquals(TaskMode.PUBLISH, req.mode)
        assertEquals("My awesome photo", req.caption)
    }

    @Test
    fun testContactResolverPhoneNumberCheck() {
        assertTrue(ContactResolver.isPhoneNumber("+919876543210"))
        assertTrue(ContactResolver.isPhoneNumber("9876543210"))
        assertTrue(ContactResolver.isPhoneNumber("+1 (555) 123-4567"))
        assertFalse(ContactResolver.isPhoneNumber("Mummy"))
        assertFalse(ContactResolver.isPhoneNumber("John Doe"))
    }
}
