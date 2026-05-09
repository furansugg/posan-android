package com.posan.app.util

import java.security.MessageDigest
import java.security.SecureRandom

object Hash {
    private const val ITERATIONS = 4
    private const val SALT_LENGTH = 16

    fun hashPassword(plain: String, saltHex: String? = null): String {
        val salt = saltHex?.let { hexToBytes(it) } ?: SecureRandom().let {
            ByteArray(SALT_LENGTH).also { bytes -> it.nextBytes(bytes) }
        }
        var current = (bytesToHex(salt) + plain).toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        repeat(ITERATIONS) {
            current = md.digest(current)
        }
        return bytesToHex(salt) + ":" + bytesToHex(current)
    }

    fun verify(plain: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 2) return false
        val saltHex = parts[0]
        val expected = stored
        val computed = hashPassword(plain, saltHex)
        return constantTimeEquals(expected, computed)
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) sb.append(String.format("%02x", b))
        return sb.toString()
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val bytes = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            bytes[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return bytes
    }
}
