package com.gelengeden.app.data

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Local app-lock credentials stored as salted PBKDF2 hashes in SharedPreferences.
 * The password remains available as a recovery method when an optional pattern is enabled.
 */
class AuthManager(context: Context) {

    enum class LoginMethod { PASSWORD, PATTERN }

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isPasswordSet(): Boolean =
        prefs.contains(KEY_PASSWORD_HASH) && prefs.contains(KEY_PASSWORD_SALT)

    fun isPatternSet(): Boolean =
        prefs.contains(KEY_PATTERN_HASH) && prefs.contains(KEY_PATTERN_SALT)

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)

    fun setBiometricEnabled(enabled: Boolean): Result<Unit> {
        if (enabled && !isPasswordSet()) {
            return Result.failure(IllegalStateException(ERROR_NOT_SET))
        }
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        return Result.success(Unit)
    }

    fun loginMethod(): LoginMethod = runCatching {
        LoginMethod.valueOf(prefs.getString(KEY_LOGIN_METHOD, LoginMethod.PASSWORD.name).orEmpty())
    }.getOrDefault(LoginMethod.PASSWORD).let { method ->
        if (method == LoginMethod.PATTERN && !isPatternSet()) LoginMethod.PASSWORD else method
    }

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
        persistCredential(trimmed, KEY_PASSWORD_SALT, KEY_PASSWORD_HASH)
        return Result.success(Unit)
    }

    fun verifyPassword(password: String): Boolean =
        verifyCredential(password.trim(), KEY_PASSWORD_SALT, KEY_PASSWORD_HASH)

    /**
     * Creates or replaces the optional 3×3 pattern. The raw node sequence is never stored.
     */
    fun setPattern(nodes: List<Int>): Result<Unit> {
        val canonical = PatternCredential.canonicalize(nodes)
            ?: return Result.failure(IllegalArgumentException(ERROR_PATTERN_TOO_SHORT))
        persistCredential(canonical, KEY_PATTERN_SALT, KEY_PATTERN_HASH)
        return Result.success(Unit)
    }

    fun verifyPattern(nodes: List<Int>): Boolean {
        val canonical = PatternCredential.canonicalize(nodes) ?: return false
        return verifyCredential(canonical, KEY_PATTERN_SALT, KEY_PATTERN_HASH)
    }

    /** Generates a one-time recovery code; only its PBKDF2 hash is persisted. */
    fun generateRecoveryCode(): Result<String> {
        if (!isPatternSet()) {
            return Result.failure(IllegalStateException(ERROR_PATTERN_NOT_SET))
        }
        val code = buildString(12) {
            repeat(12) { append(RECOVERY_ALPHABET[SecureRandom().nextInt(RECOVERY_ALPHABET.length)]) }
        }
        persistCredential(code, KEY_RECOVERY_SALT, KEY_RECOVERY_HASH)
        return Result.success(code)
    }

    /** Verifies and consumes the recovery code so it cannot be reused. */
    fun consumeRecoveryCode(code: String): Boolean {
        val normalized = code.filterNot(Char::isWhitespace).uppercase()
        if (normalized.isBlank()) return false
        val valid = verifyCredential(normalized, KEY_RECOVERY_SALT, KEY_RECOVERY_HASH)
        if (valid) {
            prefs.edit()
                .remove(KEY_RECOVERY_SALT)
                .remove(KEY_RECOVERY_HASH)
                .apply()
        }
        return valid
    }

    fun selectLoginMethod(method: LoginMethod): Result<Unit> {
        if (method == LoginMethod.PATTERN && !isPatternSet()) {
            return Result.failure(IllegalStateException(ERROR_PATTERN_NOT_SET))
        }
        prefs.edit().putString(KEY_LOGIN_METHOD, method.name).apply()
        return Result.success(Unit)
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
        persistCredential(trimmedNew, KEY_PASSWORD_SALT, KEY_PASSWORD_HASH)
        return Result.success(Unit)
    }

    private fun persistCredential(secret: String, saltKey: String, hashKey: String) {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = hashSecret(secret, salt)
        prefs.edit()
            .putString(saltKey, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(hashKey, hash)
            .apply()
    }

    private fun verifyCredential(secret: String, saltKey: String, hashKey: String): Boolean {
        val storedHash = prefs.getString(hashKey, null) ?: return false
        val saltB64 = prefs.getString(saltKey, null) ?: return false
        val salt = runCatching { Base64.decode(saltB64, Base64.NO_WRAP) }.getOrNull() ?: return false
        return constantTimeEquals(storedHash, hashSecret(secret, salt))
    }

    private fun hashSecret(secret: String, salt: ByteArray): String {
        val spec = PBEKeySpec(
            secret.toCharArray(),
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
        const val ERROR_PATTERN_TOO_SHORT = "pattern_too_short"
        const val ERROR_PATTERN_NOT_SET = "pattern_not_set"
        const val ERROR_WRONG_RECOVERY_CODE = "wrong_recovery_code"

        private const val PREFS_NAME = "gelengeden_auth"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_PASSWORD_SALT = "password_salt"
        private const val KEY_PATTERN_HASH = "pattern_hash"
        private const val KEY_PATTERN_SALT = "pattern_salt"
        private const val KEY_LOGIN_METHOD = "login_method"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_RECOVERY_HASH = "recovery_code_hash"
        private const val KEY_RECOVERY_SALT = "recovery_code_salt"
        private const val RECOVERY_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val PBKDF2_ITERATIONS = 120_000
        private const val KEY_LENGTH_BITS = 256
        private const val SALT_BYTES = 16
    }
}
