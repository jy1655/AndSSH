package com.opencode.sshterminal.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricBoundKeyManager
    @Inject
    constructor() {
        fun hasKey(): Boolean {
            return runCatching {
                val keyStore = loadKeyStore()
                keyStore.containsAlias(KEY_ALIAS)
            }.getOrDefault(false)
        }

        fun ensureKey(): Boolean {
            return runCatching {
                getOrCreateKey()
                true
            }.getOrDefault(false)
        }

        fun deleteKey() {
            runCatching {
                val keyStore = loadKeyStore()
                if (keyStore.containsAlias(KEY_ALIAS)) {
                    keyStore.deleteEntry(KEY_ALIAS)
                }
            }
        }

        fun createUnlockCipher(): Cipher? {
            return runCatching {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
                cipher
            }.getOrNull()
        }

        fun verifyUnlock(cipher: Cipher?): Boolean {
            if (cipher == null) return false
            val challenge = UNLOCK_CHALLENGE.copyOf()
            return runCatching {
                val encrypted = cipher.doFinal(challenge)
                encrypted.zeroize()
                true
            }.getOrDefault(false)
                .also {
                    challenge.zeroize()
                }
        }

        private fun loadKeyStore(): KeyStore {
            return KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        }

        private fun getOrCreateKey(): SecretKey {
            val keyStore = loadKeyStore()
            keyStore.getEntry(KEY_ALIAS, null)?.let { entry ->
                return (entry as KeyStore.SecretKeyEntry).secretKey
            }

            val keyGenerator =
                KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    KEYSTORE_PROVIDER,
                )
            keyGenerator.init(
                KeyGenParameterSpec
                    .Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_SIZE_BITS)
                    .setUserAuthenticationRequired(true)
                    .setInvalidatedByBiometricEnrollment(true)
                    .apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            setUserAuthenticationParameters(
                                0,
                                KeyProperties.AUTH_BIOMETRIC_STRONG,
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            setUserAuthenticationValidityDurationSeconds(-1)
                        }
                    }.build(),
            )
            return keyGenerator.generateKey()
        }

        companion object {
            private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
            private const val KEY_ALIAS = "biometric_device_bound_unlock_key"
            private const val TRANSFORMATION = "AES/GCM/NoPadding"
            private const val KEY_SIZE_BITS = 256
            private val UNLOCK_CHALLENGE = "andssh-biometric-unlock".toByteArray(Charsets.UTF_8)
        }
    }
