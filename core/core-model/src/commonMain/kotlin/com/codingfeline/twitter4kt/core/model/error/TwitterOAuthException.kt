package com.codingfeline.twitter4kt.core.model.error

class TwitterOAuthException(message: String?, cause: Throwable) : RuntimeException(message, cause)
