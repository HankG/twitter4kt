package com.codingfeline.twitter4kt.v1.api.friendships

import com.codingfeline.twitter4kt.TEST_ACCESS_TOKEN
import com.codingfeline.twitter4kt.TEST_ACCESS_TOKEN_SECRET
import com.codingfeline.twitter4kt.TEST_CONSUMER_KEY
import com.codingfeline.twitter4kt.TEST_CONSUMER_SECRET
import com.codingfeline.twitter4kt.TEST_USER_ID
import com.codingfeline.twitter4kt.core.ConsumerKeys
import com.codingfeline.twitter4kt.core.Twitter
import com.codingfeline.twitter4kt.core.model.ApiResult
import com.codingfeline.twitter4kt.core.model.oauth1a.AccessToken
import com.codingfeline.twitter4kt.core.startSession
import com.codingfeline.twitter4kt.v1.api.runTest
import io.ktor.client.features.ClientRequestException
import io.ktor.utils.io.readUTF8Line
import kotlin.test.Ignore
import kotlin.test.Test

class CreateTest {
    private val consumerKeys = ConsumerKeys(
        key = TEST_CONSUMER_KEY,
        secret = TEST_CONSUMER_SECRET,
    )

    private val accessToken = AccessToken(
        token = TEST_ACCESS_TOKEN,
        secret = TEST_ACCESS_TOKEN_SECRET,
        userId = TEST_USER_ID,
        screenName = TEST_USER_ID
    )

    @Ignore
    @Test
    fun test() = runTest {
        val twitter = Twitter {
            this.consumerKeys = this@CreateTest.consumerKeys
        }

        val apiClient = twitter.startSession(accessToken)
        val result = apiClient.friendships.createByScreenName("yslibnet")

        println("result: $result")
        if (result.isFailure) {
            val error = (result as ApiResult.Failure).error
            if (error is ClientRequestException) {
                val res = error.response.content.readUTF8Line()
                println("res: $res")
            }
        }
    }
}
