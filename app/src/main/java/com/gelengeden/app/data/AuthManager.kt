package com.gelengeden.app.data

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Local app-lock password stored as a salted PBKDF2 hash in SharedPreferences.
 * No network or account is involved — password only protects access on this device.
 */
class AuthManager(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isPasswordSet(): Boolean =
        prefs.contains(KEY_HASH) && prefs.contains(KEY_SALT)

    /**
     * Sets the initial password. Fails if one is already set — use [changePassword] instead.
     */
    fun setPassword(password: String): Result<Unit> {
        val trimmed = password.trim()
        if (trimmed.length < MIN_PASSWORD_LENGTH) {
            return Result.failure(IllegalArgumentException(ERROR_TOO_SHORT))
        }
        if (isPasswordSet()) {
            return Result.failure(IllegalStateException(ERROR_ALREADY_SET))
        }
        persistPassword(trimmed)
        return Result.success(Unit)
    }

    fun verifyPassword(password: String): Boolean {
        if (!isPasswordSet()) return false
        val storedHash = prefs.getString(KEY_HASH, null) ?: return false
        val saltB64 = prefs.getString(KEY_SALT, null) ?: return false
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val candidate = hashPassword(password.trim(), salt)
        return constantTimeEquals(storedHash, candidate)
    }

    fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        if (!isPasswordSet()) {
            return Result.failure(IllegalStateException(ERROR_NOT_SET))
        }
        if (!verifyPassword(currentPassword)) {
            return Result.failure(IllegalArgumentException(ERROR_WRONG_CURRENT))
        }
        val trimmedNew = newPassword.trim()
        if (trimmedNew.length < MIN_PASSWORD_LENGTH) {
            return Result.failure(IllegalArgumentException(ERROR_TOO_SHORT))
        }
        if (currentPassword.trim() == trimmedNew) {
            return Result.failure(IllegalArgumentException(ERROR_SAME_PASSWORD))
        }
        persistPassword(trimmedNew)
        return Result.success(Unit)
    }

    private fun persistPassword(password: String) {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = hashPassword(password, salt)
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, hash)
            .apply()
    }

    private fun hashPassword(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            KEY_LENGTH_BITS
        )
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val hash = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    /** Avoid early-exit comparison that could leak timing information. */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        val aBytes = a.toByteArray(Charsets.UTF_8)
        val bBytes = b.toByteArray(Charsets.UTF_8)
        if (aBytes.size != bBytes.size) return false
        var result = 0
        for (i in aBytes.indices) {
            result = result or (aBytes[i].toInt() xor bBytes[i].toInt())
        }
        return result == 0
    }

    companion object {
        const val MIN_PASSWORD_LENGTH = 4

        const val ERROR_TOO_SHORT = "too_short"
        const val ERROR_ALREADY_SET = "already_set"
        const val ERROR_NOT_SET = "not_set"
        const val ERROR_WRONG_CURRENT = "wrong_current"
        const val ERROR_SAME_PASSWORD = "same_password"

        private const val PREFS_NAME = "gelengeden_auth"
        private const val KEY_HASH = "password_hash"
        private const val KEY_SALT = "password_salt"

        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val PBKDF2_ITERATIONS = 120_000
        private const val KEY_LENGTH_BITS = 256
        private const val SALT_BYTES = 16
    }
}
