package com.opencode.sshterminal.ssh

import com.opencode.sshterminal.security.SSH_SK_ECDSA_KEY_TYPE
import com.opencode.sshterminal.security.U2fSignatureMaterial
import com.opencode.sshterminal.security.buildSshSkEcdsaPublicKeyBlob
import com.opencode.sshterminal.security.buildSshSkEcdsaSignatureBlob
import net.schmizz.sshj.common.Buffer
import net.schmizz.sshj.common.Message
import net.schmizz.sshj.common.SSHPacket
import net.schmizz.sshj.userauth.UserAuthException
import net.schmizz.sshj.userauth.method.AbstractAuthMethod

internal class U2fSecurityKeyAuthMethod(
    private val publicKeyUncompressed: ByteArray,
    private val application: String,
    private val signMessage: (ByteArray) -> U2fSignatureMaterial?,
) : AbstractAuthMethod("publickey") {
    override fun handle(
        cmd: Message,
        buf: SSHPacket,
    ) {
        if (cmd == Message.USERAUTH_60) {
            sendSignedRequest()
            return
        }
        super.handle(cmd, buf)
    }

    override fun buildReq(): SSHPacket {
        return buildReq(includeSignature = false)
    }

    private fun buildReq(includeSignature: Boolean): SSHPacket {
        val keyBlob = buildSshSkEcdsaPublicKeyBlob(publicKeyUncompressed, application)
        return super.buildReq()
            .putBoolean(includeSignature)
            .putString(SSH_SK_ECDSA_KEY_TYPE)
            .putString(keyBlob)
    }

    @Suppress("TooGenericExceptionThrown")
    private fun sendSignedRequest() {
        val requestPacket = buildReq(includeSignature = true)
        val payloadToSign =
            Buffer.PlainBuffer()
                .putString(params.transport.sessionID)
                .putBuffer(requestPacket)
                .compactData
        val signatureMaterial = signMessage(payloadToSign)
        val signatureBlob =
            signatureMaterial
                ?.let(::buildSshSkEcdsaSignatureBlob)
                ?: throw UserAuthException("Hardware security key signature failed")
        requestPacket.putSignature(SSH_SK_ECDSA_KEY_TYPE, signatureBlob)
        params.transport.write(requestPacket)
    }
}
