package me.apomazkin.core_network_impl

import kotlinx.serialization.json.Json
import me.apomazkin.logger.LexemeLogger
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Собирает клиент сервера слов. Доверие только системным центрам
 * сертификации — умолчание OkHttp, своих `TrustManager` здесь нет (LFL-21).
 */
internal object LexemeServiceFactory {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = true
    }

    fun create(options: NetworkOptions, logger: LexemeLogger): LexemeService =
        Retrofit.Builder()
            .baseUrl(options.baseUrl)
            .client(httpClient(options, logger))
            .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
            .build()
            .create(LexemeService::class.java)

    /**
     * Ожидание ответа целиком — [NetworkOptions.callTimeout] (LFL-22). Таймаут
     * чтения OkHttp по умолчанию 10 секунд, и он сработал бы раньше общего:
     * сервер думает до 170 секунд (LFL-55), первый прогон на девайсе оборвался
     * ровно на десятой секунде. Чтение и запись ждут столько же, сколько вызов;
     * соединение — 10 секунд, дольше ждать некого.
     */
    fun httpClient(options: NetworkOptions, logger: LexemeLogger): OkHttpClient {
        val timeoutMs = options.callTimeout.inWholeMilliseconds
        return OkHttpClient.Builder()
            .callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .addInterceptor(AccessKeyInterceptor(options.accessKey))
            .addInterceptor(RequestLogInterceptor(logger))
            .build()
    }

    private const val JSON_MEDIA_TYPE = "application/json; charset=UTF-8"
    private const val CONNECT_TIMEOUT_MS = 10_000L
}
