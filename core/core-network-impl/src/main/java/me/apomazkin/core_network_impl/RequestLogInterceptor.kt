package me.apomazkin.core_network_impl

import me.apomazkin.logger.LexemeLogger
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Строка лога на запрос: метод, путь, код, время. Заголовков в логе нет —
 * ключ доступа не утекает (WFT-05).
 */
internal class RequestLogInterceptor(
        private val logger: LexemeLogger,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startedAt = System.nanoTime()
        val response = chain.proceed(request)
        val tookMs = (System.nanoTime() - startedAt) / NANOS_IN_MILLI
        logger.d(
                tag = LogTags.NETWORK,
                message = "${request.method} ${request.url.encodedPath} -> ${response.code} in ${tookMs}ms",
        )
        return response
    }

    companion object {
        private const val NANOS_IN_MILLI = 1_000_000L
    }
}
