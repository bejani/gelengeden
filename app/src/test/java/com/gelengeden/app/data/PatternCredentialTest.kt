package com.gelengeden.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PatternCredentialTest {

    @Test
    fun `accepts a valid four-node pattern and serializes its order`() {
        assertEquals("0,1,4,8", PatternCredential.canonicalize(listOf(0, 1, 4, 8)))
    }

    @Test
    fun `rejects a pattern shorter than four nodes`() {
        assertNull(PatternCredential.canonicalize(listOf(0, 1, 2)))
    }

    @Test
    fun `rejects duplicate nodes`() {
        assertNull(PatternCredential.canonicalize(listOf(0, 1, 1, 4)))
    }

    @Test
    fun `rejects nodes outside the three by three grid`() {
        assertNull(PatternCredential.canonicalize(listOf(0, 1, 4, 9)))
    }
}
