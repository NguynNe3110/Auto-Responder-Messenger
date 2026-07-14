package com.autoreply.messenger

data class ReplyHistory(
    val id: Long,
    val sender: String,
    val receivedMessage: String,
    val replyMessage: String,
    val timestamp: Long
)