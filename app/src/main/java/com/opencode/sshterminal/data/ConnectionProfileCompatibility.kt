package com.opencode.sshterminal.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

internal data class ParsedConnectionBackupPayload(
    val exportedAtEpochMillis: Long,
    val profiles: List<ConnectionProfile>,
    val identities: List<ConnectionIdentity>,
    val unsupportedSecurityKeyProfileCount: Int,
)

internal object ConnectionProfileCompatibility {
    fun decodeProfileOrNull(
        json: Json,
        rawProfileJson: String,
    ): ConnectionProfile? {
        return runCatching {
            decodeProfileOrNull(
                json = json,
                profileJson = json.parseToJsonElement(rawProfileJson),
            )
        }.getOrNull()
    }

    fun decodeProfileOrNull(
        json: Json,
        profileJson: JsonElement,
    ): ConnectionProfile? {
        return runCatching {
            val profileObject = profileJson.jsonObject
            val decoded = json.decodeFromJsonElement<ConnectionProfile>(profileObject)
            if (hasConfiguredLegacySecurityKeyAuth(profileObject)) {
                decoded.copy(hasUnsupportedSecurityKeyAuth = true)
            } else {
                decoded
            }
        }.getOrNull()
    }

    fun parseBackupPayload(
        json: Json,
        rawPayloadJson: String,
    ): ParsedConnectionBackupPayload {
        val payloadObject = json.parseToJsonElement(rawPayloadJson).jsonObject
        val profiles =
            payloadObject.getValue("profiles").jsonArray.map { profileJson ->
                requireNotNull(
                    decodeProfileOrNull(
                        json = json,
                        profileJson = profileJson,
                    ),
                ) { "Invalid backup profile" }
            }
        return ParsedConnectionBackupPayload(
            exportedAtEpochMillis = payloadObject.getValue("exportedAtEpochMillis").jsonPrimitive.long,
            profiles = profiles,
            identities = json.decodeFromJsonElement(payloadObject.getValue("identities")),
            unsupportedSecurityKeyProfileCount = profiles.count { it.hasUnsupportedSecurityKeyAuth },
        )
    }

    private fun hasConfiguredLegacySecurityKeyAuth(profileObject: JsonObject): Boolean {
        val application = profileObject.nonBlankString("securityKeyApplication")
        val keyHandle = profileObject.nonBlankString("securityKeyHandleBase64")
        val publicKey = profileObject.nonBlankString("securityKeyPublicKeyBase64")
        return application != null && keyHandle != null && publicKey != null
    }

    private fun JsonObject.nonBlankString(fieldName: String): String? {
        return get(fieldName)
            ?.jsonPrimitive
            ?.contentOrNull
            ?.trim()
            ?.takeIf(String::isNotEmpty)
    }
}
