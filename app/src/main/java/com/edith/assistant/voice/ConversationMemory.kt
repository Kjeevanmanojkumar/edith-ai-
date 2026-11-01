package com.edith.assistant.voice

enum class MemoryRole {
    USER,
    ASSISTANT
}

data class MemoryTurn(
    val role: MemoryRole,
    val text: String
)

class ConversationMemory(private val maxTurns: Int = 12) {
    private val turns = mutableListOf<MemoryTurn>()

    @Synchronized
    fun addTurn(role: MemoryRole, text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        turns.add(MemoryTurn(role, clean))
        while (turns.size > maxTurns) {
            turns.removeAt(0)
        }
    }

    @Synchronized
    fun recap(): String? {
        if (turns.isEmpty()) return null
        val sb = StringBuilder()
        for (turn in turns) {
            val prefix = if (turn.role == MemoryRole.USER) "User: " else "You: "
            sb.append(prefix).append(turn.text).append("\n")
        }
        val result = sb.toString().trim()
        return if (result.length > 1600) {
            result.takeLast(1600)
        } else {
            result.ifEmpty { null }
        }
    }

    @Synchronized
    fun getTurns(): List<MemoryTurn> {
        return turns.toList()
    }

    @Synchronized
    fun clear() {
        turns.clear()
    }
}
