package com.opencode.sshterminal.data

internal fun ConnectionProfile.hasUsablePasswordOrPrivateKey(identity: ConnectionIdentity? = null): Boolean {
    val resolvedPassword = identity?.password ?: password
    val resolvedPrivateKeyPath = identity?.privateKeyPath ?: privateKeyPath
    return !resolvedPassword.isNullOrBlank() || !resolvedPrivateKeyPath.isNullOrBlank()
}

internal fun ConnectionProfile.hasBlockingPrivateKeyRelink(identity: ConnectionIdentity? = null): Boolean {
    return !hasUsablePasswordOrPrivateKey(identity) &&
        (requiresPrivateKeyRelink || identity?.requiresPrivateKeyRelink == true)
}

internal fun ConnectionProfile.hasBlockingUnsupportedSecurityKeyAuth(identity: ConnectionIdentity? = null): Boolean {
    return hasUnsupportedSecurityKeyAuth && !hasUsablePasswordOrPrivateKey(identity)
}
