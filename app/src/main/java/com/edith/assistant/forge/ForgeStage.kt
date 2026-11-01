package com.edith.assistant.forge

enum class ForgeStage(val label: String, val baseProgress: Int) {
    INITIALIZING("Igniting the Forge Reactor", 5),
    SKELETON("Architecting Core DOM & Frameworks", 20),
    STRUCTURE("Forging 3D Hero & Bento Sections", 40),
    VISUALS("Synthesizing 3D Shaders & Canvas", 65),
    STYLING("Infusing Glassmorphism & Micro-Styling", 85),
    MOTION("Calibrating Dynamic Spatial Motion", 95),
    POLISHED("Materializing Polished Web Experience", 100)
}

object ForgeStageDetector {

    fun detect(partialHtml: String): Pair<ForgeStage, Int> {
        val lower = partialHtml.lowercase()
        val len = partialHtml.length

        val hasHtml = lower.contains("<html") || lower.contains("<!doctype")
        val hasHead = lower.contains("<head") || lower.contains("<title")
        val hasBody = lower.contains("<body")
        val hasCanvasOrThree = lower.contains("<canvas") || lower.contains("three") || lower.contains("webgl")
        val hasStyles = lower.contains("<style") || lower.contains("class=") || lower.contains("bg-")
        val hasScripts = lower.contains("<script") || lower.contains("gsap") || lower.contains("requestanimationframe")
        val hasCompleted = lower.contains("</html>")

        if (hasCompleted) {
            return ForgeStage.POLISHED to 100
        }

        if (len < 20 && !hasHtml && !hasHead) {
            return ForgeStage.INITIALIZING to 5
        }

        return when {
            hasScripts && len > 6000 -> {
                val progress = 85 + ((len.coerceAtMost(16000) - 6000) * 12 / 10000)
                ForgeStage.MOTION to progress.coerceIn(85, 98)
            }
            hasStyles && (hasCanvasOrThree || len > 3500) -> {
                val progress = 65 + ((len.coerceAtMost(7000) - 3500) * 18 / 3500)
                ForgeStage.STYLING to progress.coerceIn(65, 84)
            }
            hasCanvasOrThree || len > 2000 -> {
                val progress = 40 + ((len.coerceAtMost(4000) - 2000) * 22 / 2000)
                ForgeStage.VISUALS to progress.coerceIn(40, 64)
            }
            hasBody || len > 600 -> {
                val progress = 20 + ((len.coerceAtMost(2000) - 600) * 18 / 1400)
                ForgeStage.STRUCTURE to progress.coerceIn(20, 39)
            }
            hasHead || hasHtml -> {
                ForgeStage.SKELETON to 18
            }
            else -> {
                ForgeStage.INITIALIZING to 10
            }
        }
    }
}
