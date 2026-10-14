package me.apomazkin.core_network_impl

import okhttp3.Interceptor
import okhttp3.Response

/** Ставит `Authorization: Bearer <ключ>` на каждый запрос (WFT-20). */
internal class AccessKeyInterceptor(
        private val accessKey: String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$accessKey")
            .build()
        return chain.proceed(request)
    }

    companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "
    }
}
