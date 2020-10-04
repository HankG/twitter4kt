package com.codingfeline.twitter4kt.core.oauth1a

import com.codingfeline.twitter4kt.core.ConsumerKeys
import com.codingfeline.twitter4kt.core.apiUrl
import com.codingfeline.twitter4kt.core.model.ApiResult
import com.codingfeline.twitter4kt.core.model.error.TwitterOAuthException
import com.codingfeline.twitter4kt.core.model.oauth1a.AccessToken
import com.codingfeline.twitter4kt.core.model.oauth1a.RequestToken
import com.codingfeline.twitter4kt.core.util.Twitter4ktInternalAPI
import com.codingfeline.twitter4kt.core.util.appendNotNulls
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.http.Parameters
import io.ktor.http.parseQueryString
import io.ktor.utils.io.readUTF8Line
import kotlinx.datetime.Clock

public class OAuth1aFlow(
    private val consumerKeys: ConsumerKeys,
    private val oAuthConfig: OAuthConfig,
    private val httpClientConfig: HttpClientConfig<*>.() -> Unit = {}
) {
    private val httpClient by lazy {
        HttpClient {
            install(OAuthFlowHeaders.Feature) {
                this.consumerKeys = this@OAuth1aFlow.consumerKeys
                this.oAuthConfig = this@OAuth1aFlow.oAuthConfig
                this.clock = Clock.System
            }

            // TODO: move this to httpClientConfig


            this.apply(httpClientConfig)
        }
    }

    /**
     * Allows a Consumer application to obtain an OAuth Request Token to request user authorization.
     * This method fulfills [Section 6.1](https://oauth.net/core/1.0/#auth_step1) of the [OAuth 1.0 authentication flow](http://oauth.net/core/1.0/#anchor9).
     *
     * [Twitter API reference](https://developer.twitter.com/en/docs/authentication/api-reference/request_token)
     */
    public suspend fun fetchRequestToken(): ApiResult<RequestToken> {
        val url = apiUrl("oauth/request_token").build()

        val result = kotlin.runCatching {
            httpClient.post<String>(url = url) {
                body = FormDataContent(Parameters.Empty)
            }
        }

        return result.fold(
            onSuccess = { res ->
                val results = parseQueryString(res)
                val token = requireNotNull(results["oauth_token"]) { "oauth_token is missing" }
                val secret = requireNotNull(results["oauth_token_secret"]) { "oauth_token_secret is missing" }
                val callbackConfirmed =
                    requireNotNull(results["oauth_callback_confirmed"]) { "oauth_callback_confirmed is missing" }.toBoolean()

                ApiResult.success(RequestToken(token, secret, callbackConfirmed))
            },
            onFailure = { e ->
                val message = e.getMessageOrDefault("Something went wrong during oauth/request_token request")
                ApiResult.failure(TwitterOAuthException(message, e))
            }
        )
    }

    /**
     * Allows a Consumer application to exchange the OAuth Request Token for an OAuth Access Token.
     * This method fulfills [Section 6.3](http://oauth.net/core/1.0/#auth_step3) of the [OAuth 1.0 authentication flow](http://oauth.net/core/1.0/#anchor9).
     *
     * [Twitter API reference](https://developer.twitter.com/en/docs/authentication/api-reference/access_token)
     *
     * @param oAuthToken The oauth_token here must be the same as the oauth_token returned in the request_token step.
     * @param oAuthVerifier If using the OAuth web-flow, set this parameter to the value of the oauth_verifier returned in the callback URL.
     * If you are using out-of-band OAuth, set this value to the pin-code. For OAuth 1.0a compliance this parameter is required.
     * OAuth 1.0a is strictly enforced and applications not using the oauth_verifier will fail to complete the OAuth flow.
     */
    @OptIn(Twitter4ktInternalAPI::class)
    public suspend fun fetchAccessToken(oAuthToken: String, oAuthVerifier: String): ApiResult<AccessToken> {
        val url = apiUrl("oauth/access_token").build()

        val result = kotlin.runCatching {
            httpClient.post<String>(url = url) {
                body = FormDataContent(
                    Parameters.build {
                        appendNotNulls(
                            "oauth_token" to oAuthToken,
                            "oauth_verifier" to oAuthVerifier
                        )
                    }
                )
            }
        }

        return result.fold(
            onSuccess = { res ->
                val results = parseQueryString(res)

                val token = requireNotNull(results["oauth_token"]) { "oauth_token is missing" }
                val secret = requireNotNull(results["oauth_token_secret"]) { "oauth_token_secret is missing" }
                val userId = requireNotNull(results["user_id"]) { "user_id is missing" }
                val screenName = requireNotNull(results["screen_name"]) { "screen_name is missing" }
                ApiResult.success(AccessToken(token, secret, userId, screenName))
            },
            onFailure = { e ->
                val message = e.getMessageOrDefault("Something went wrong during oauth/access_token request")
                ApiResult.failure(TwitterOAuthException(message, e))
            }
        )
    }

    private suspend fun Throwable.getMessageOrDefault(defaultMessage: String): String {
        return if (this is ClientRequestException) {
            response.content.readUTF8Line()
        } else {
            message
        } ?: defaultMessage
    }
}
