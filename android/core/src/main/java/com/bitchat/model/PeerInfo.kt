//
// PeerInfo.kt
// bitchat Android
//
// This is free and unencumbered software released into the public domain.
// For more information, see <https://unlicense.org>
//

package com.bitchat.model

/** Basic peer representation */
data class PeerInfo(
    val id: String,
    val nickname: String? = null,
    val rssi: Int? = null
)
