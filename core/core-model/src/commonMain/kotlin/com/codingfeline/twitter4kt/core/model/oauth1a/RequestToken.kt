package com.codingfeline.twitter4kt.core.model.oauth1a

public data class RequestToken(
    val token: String,
    val secret: String,
    val callbackConfirmed: Boolean
)
