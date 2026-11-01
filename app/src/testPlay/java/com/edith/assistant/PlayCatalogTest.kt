package com.edith.assistant

import com.edith.assistant.action.edithToolCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PlayCatalogTest {

    @Test
    fun testPlayCatalogContainsOnlySafeTools() {
        val declarations = edithToolCatalog.declarations()
        val names = mutableSetOf<String>()
        for (i in 0 until declarations.length()) {
            names.add(declarations.getJSONObject(i).getString("name"))
        }

        // Play edition must NOT contain screen reading or social media automation tools
        assertFalse(names.contains("read_screen"))
        assertFalse(names.contains("tap_text"))
        assertFalse(names.contains("execute_social_media_task"))

        // Play edition contains 4 safe tools: build_website, open_app, call_contact, message_contact
        assertEquals(4, names.size)
    }
}
