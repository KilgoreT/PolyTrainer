package me.apomazkin.core_network_impl

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Настройки клиента сервера слов. [baseUrl] с завершающим `/`; [accessKey]
 * пустой — запросы не уходят (ACC-19); [callTimeout] — потолок ожидания
 * ответа целиком (LFL-22); [requireHttps] — не-`https` адрес отвергается без
 * соединения (LFL-21). Тесты с поддельным сервером на `http://localhost`
 * выключают проверку и укорачивают таймаут; приложение умолчания не меняет.
 */
data class NetworkOptions(
        val baseUrl: String,
        val accessKey: String,
        val callTimeout: Duration = 180.seconds,
        val requireHttps: Boolean = true,
) {
    val isSecureEnough: Boolean
        get() = !requireHttps || baseUrl.startsWith("https://")

    val hasAccessKey: Boolean
        get() = accessKey.isNotBlank()
}
