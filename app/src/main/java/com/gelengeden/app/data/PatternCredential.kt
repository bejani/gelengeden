package com.gelengeden.app.data

/**
 * Validation and stable serialization for a 3×3 app-lock pattern.
 * The returned value is only passed to the credential hash function; raw patterns are never persisted.
 */
object PatternCredential {
    const val MIN_NODES = 4
    private const val NODE_COUNT = 9

    fun canonicalize(nodes: List<Int>): String? {
        if (nodes.size < MIN_NODES) return null
        if (nodes.any { it !in 0 until NODE_COUNT }) return null
        if (nodes.distinct().size != nodes.size) return null
        return nodes.joinToString(separator = ",")
    }
}
