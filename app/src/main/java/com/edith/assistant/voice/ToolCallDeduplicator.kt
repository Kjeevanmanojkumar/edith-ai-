package com.edith.assistant.voice

class ToolCallDeduplicator(private val maxSize: Int = 100) {
    private val seenIds = LinkedHashSet<String>()

    @Synchronized
    fun isNew(callId: String): Boolean {
        if (callId.isBlank()) return true
        if (seenIds.contains(callId)) {
            return false
        }
        seenIds.add(callId)
        if (seenIds.size > maxSize) {
            val iterator = seenIds.iterator()
            if (iterator.hasNext()) {
                iterator.next()
                iterator.remove()
            }
        }
        return true
    }

    @Synchronized
    fun clear() {
        seenIds.clear()
    }

    @Synchronized
    fun size(): Int = seenIds.size
}
