package com.opencode.sshterminal.security

class TodoKeyRepository : KeyRepository {
    override suspend fun saveEncryptedPrivateKey(alias: String, privateKeyPem: ByteArray) {
        error("Not implemented: back with Android Keystore + AEAD (Tink or platform crypto)")
    }

    override suspend fun loadEncryptedPrivateKey(alias: String): ByteArray? {
        error("Not implemented: back with Android Keystore + AEAD (Tink or platform crypto)")
    }

    override suspend fun delete(alias: String) {
        error("Not implemented: back with Android Keystore + AEAD (Tink or platform crypto)")
    }
}
