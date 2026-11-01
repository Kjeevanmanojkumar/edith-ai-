package com.edith.assistant

import com.edith.assistant.forge.ForgeHtml
import com.edith.assistant.forge.ForgeIntent
import com.edith.assistant.forge.ForgeStage
import com.edith.assistant.forge.ForgeStageDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForgeTest {

    @Test
    fun testForgeIntentMatching() {
        // Matches valid creation prompts
        assertNotNull(ForgeIntent.extract("ek cafe ki website banao"))
        assertNotNull(ForgeIntent.extract("create a portfolio website"))
        assertNotNull(ForgeIntent.extract("make a landing page for my bakery"))
        assertNotNull(ForgeIntent.extract("build a website for personal trainer"))

        // Ignores pure questions
        assertNull(ForgeIntent.extract("what is a website?"))
        assertNull(ForgeIntent.extract("how does a website work?"))
        assertNull(ForgeIntent.extract("can you explain what a landing page is?"))

        // Ignores general unrelated queries
        assertNull(ForgeIntent.extract("what's the weather today"))
        assertNull(ForgeIntent.extract("call mum on phone"))
    }

    @Test
    fun testForgeHtmlStrippingAndDetection() {
        val fenced = "```html\n<!DOCTYPE html><html><head></head><body>Hello</body></html>\n```"
        val stripped = ForgeHtml.stripFences(fenced)
        assertFalse(stripped.startsWith("```"))
        assertFalse(stripped.endsWith("```"))
        assertTrue(ForgeHtml.looksLikeHtml(stripped))
    }

    @Test
    fun testForgeHtmlRepairTruncated() {
        val truncated = "<html><head><style>body { color: red; }</style><script>console.log('hi');"
        val repaired = ForgeHtml.repairTruncated(truncated)
        assertTrue(repaired.contains("</script>"))
        assertTrue(repaired.contains("</body>"))
        assertTrue(repaired.contains("</html>"))
    }

    @Test
    fun testForgeHtmlSafetyNetIdempotent() {
        val baseHtml = "<!DOCTYPE html><html><body><h1>Hello World</h1></body></html>"
        val withNet1 = ForgeHtml.withSafetyNet(baseHtml)
        assertTrue(withNet1.contains("forge-safety-net"))

        // Applying again should retain safety net script gracefully
        val withNet2 = ForgeHtml.withSafetyNet(withNet1)
        assertTrue(withNet2.contains("forge-safety-net"))
    }

    @Test
    fun testForgeStageDetectorProgression() {
        val (stage1, _) = ForgeStageDetector.detect("<!DOCTYPE html><html><head><title>Cafe</title></head>")
        assertEquals(ForgeStage.SKELETON, stage1)

        val fullHtml = "<!DOCTYPE html><html><head></head><body><h1>Done</h1></body></html>"
        val (stageDone, progDone) = ForgeStageDetector.detect(fullHtml)
        assertEquals(ForgeStage.POLISHED, stageDone)
        assertEquals(100, progDone)
    }
}
