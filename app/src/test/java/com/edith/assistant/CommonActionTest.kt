package com.edith.assistant

import com.edith.assistant.action.ContactResolver
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommonActionTest {

    @Test
    fun testContactResolverPhoneNumberCheck() {
        assertTrue(ContactResolver.isPhoneNumber("+919876543210"))
        assertTrue(ContactResolver.isPhoneNumber("9876543210"))
        assertTrue(ContactResolver.isPhoneNumber("+1 (555) 123-4567"))
        assertFalse(ContactResolver.isPhoneNumber("Mummy"))
        assertFalse(ContactResolver.isPhoneNumber("John Doe"))
    }
}
