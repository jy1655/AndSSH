package com.opencode.sshterminal.security

import net.schmizz.sshj.common.Buffer
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.ASN1Sequence
import java.math.BigInteger
import java.util.Base64

internal data class U2fRegisteredKeyMaterial(
    val publicKeyUncompressed: ByteArray,
    val keyHandle: ByteArray,
)

internal data class U2fSignatureMaterial(
    val flags: Int,
    val counter: Long,
    val derSignature: ByteArray,
)

@Suppress("ReturnCount")
internal fun parseU2fRegisterData(registerData: ByteArray): U2fRegisteredKeyMaterial? {
    if (registerData.size < MIN_REGISTER_DATA_SIZE) return null
    if (registerData[0] != U2F_REGISTER_RESERVED_BYTE) return null

    val publicKey = registerData.copyOfRange(1, 1 + U2F_PUBLIC_KEY_BYTES)
    val keyHandleLength = registerData[1 + U2F_PUBLIC_KEY_BYTES].toInt() and 0xFF
    val keyHandleStart = 1 + U2F_PUBLIC_KEY_BYTES + 1
    val keyHandleEnd = keyHandleStart + keyHandleLength
    if (keyHandleLength <= 0 || keyHandleEnd > registerData.size) return null

    return U2fRegisteredKeyMaterial(
        publicKeyUncompressed = publicKey,
        keyHandle = registerData.copyOfRange(keyHandleStart, keyHandleEnd),
    )
}

internal fun parseU2fSignatureData(signatureData: ByteArray): U2fSignatureMaterial? {
    if (signatureData.size <= U2F_SIGNATURE_PREFIX_BYTES) return null
    val flags = signatureData[0].toInt() and 0xFF
    val counter =
        ((signatureData[1].toLong() and 0xFF) shl 24) or
            ((signatureData[2].toLong() and 0xFF) shl 16) or
            ((signatureData[3].toLong() and 0xFF) shl 8) or
            (signatureData[4].toLong() and 0xFF)
    val derSignature = signatureData.copyOfRange(U2F_SIGNATURE_PREFIX_BYTES, signatureData.size)
    return U2fSignatureMaterial(
        flags = flags,
        counter = counter,
        derSignature = derSignature,
    )
}

internal fun buildSshSkEcdsaPublicKeyBlob(
    publicKeyUncompressed: ByteArray,
    application: String,
): ByteArray {
    return Buffer.PlainBuffer()
        .putString(SSH_SK_ECDSA_KEY_TYPE)
        .putString(SSH_ECDSA_CURVE_NISTP256)
        .putString(publicKeyUncompressed)
        .putString(application)
        .compactData
}

internal fun buildSshSkEcdsaAuthorizedKey(
    publicKeyUncompressed: ByteArray,
    application: String,
    comment: String,
): String {
    val blob = buildSshSkEcdsaPublicKeyBlob(publicKeyUncompressed, application)
    val encoded = Base64.getEncoder().encodeToString(blob)
    return "$SSH_SK_ECDSA_KEY_TYPE $encoded $comment"
}

internal fun buildSshSkEcdsaSignatureBlob(signatureMaterial: U2fSignatureMaterial): ByteArray {
    val (r, s) = parseDerEcdsaSignature(signatureMaterial.derSignature)
    val ecdsaRawSignature =
        Buffer.PlainBuffer()
            .putMPInt(r)
            .putMPInt(s)
            .compactData
    val standardEcdsaSignatureBlob =
        Buffer.PlainBuffer()
            .putString(SSH_ECDSA_KEY_TYPE)
            .putString(ecdsaRawSignature)
            .compactData
    return Buffer.PlainBuffer()
        .putByte(signatureMaterial.flags.toByte())
        .putUInt32(signatureMaterial.counter)
        .putString(standardEcdsaSignatureBlob)
        .compactData
}

private fun parseDerEcdsaSignature(der: ByteArray): Pair<BigInteger, BigInteger> {
    ASN1InputStream(der).use { input ->
        val sequence = input.readObject() as? ASN1Sequence ?: error("Invalid DER ECDSA signature")
        val r = (sequence.getObjectAt(0) as ASN1Integer).positiveValue
        val s = (sequence.getObjectAt(1) as ASN1Integer).positiveValue
        return r to s
    }
}

internal const val SSH_SK_ECDSA_KEY_TYPE = "sk-ecdsa-sha2-nistp256@openssh.com"
internal const val SSH_ECDSA_KEY_TYPE = "ecdsa-sha2-nistp256"
internal const val SSH_ECDSA_CURVE_NISTP256 = "nistp256"

private const val U2F_PUBLIC_KEY_BYTES = 65
private const val MIN_REGISTER_DATA_SIZE = 67
private const val U2F_SIGNATURE_PREFIX_BYTES = 5
private const val U2F_REGISTER_RESERVED_BYTE: Byte = 0x05
