package com.first10.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.security.MessageDigest

class SecurityTest {

    private fun hash(input: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray(Charsets.UTF_8))
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    @Test
    fun testSaltedHashing() {
        val pin = "1234"
        val salt1 = "random_salt_abc"
        val salt2 = "random_salt_xyz"

        val hash1 = hash(pin, salt1)
        val hash2 = hash(pin, salt2)

        // Same PIN with different salts must produce distinct hashes
        assertNotEquals(hash1, hash2)

        // Same PIN with same salt must produce exact match
        assertEquals(hash1, hash(pin, salt1))

        // Different PIN must not match
        assertNotEquals(hash1, hash("5678", salt1))
    }
}
