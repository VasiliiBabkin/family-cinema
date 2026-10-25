package ru.familycinema.parental

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinHasher {
    fun salt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    fun hash(pin: String, salt: ByteArray): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(pin.toCharArray(), salt, 120000, 256))
            .encoded

    fun verify(pin: String, salt: ByteArray, expected: ByteArray) =
        MessageDigest.isEqual(hash(pin, salt), expected)
}
