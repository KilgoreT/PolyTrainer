package me.apomazkin.core_network_impl

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import me.apomazkin.core_network_api.CoreNetworkApi
import me.apomazkin.core_network_api.entity.DenyReason
import me.apomazkin.core_network_api.entity.LookupRequestApiEntity
import me.apomazkin.core_network_api.entity.LookupResult
import me.apomazkin.core_network_impl.mapper.toApiEntity
import me.apomazkin.core_network_impl.mapper.toNet
import me.apomazkin.core_network_impl.net.ErrorNet
import me.apomazkin.core_network_impl.net.LookupResponseNet
import retrofit2.Response
import javax.inject.Inject

/**
 * `lookupWords` поверх Retrofit. Единственное место слоя, где исключения
 * сети и разбора превращаются в [LookupResult.Unavailable].
 */
internal class WordsApiImpl @Inject constructor(
    private val service: LexemeService,
    private val options: NetworkOptions,
) : CoreNetworkApi.WordsApi {

    // Ключ не задан → отказ без сети (WFT-19); не HTTPS → недоступен (WFT-21);
    // сети нет или ответа нет → недоступен (WFT-22, WFT-23); код отказа →
    // отказ по причине (WFT-63), иной код → недоступен; 200 → зеркала.
    override suspend fun lookup(request: LookupRequestApiEntity, runId: Long): LookupResult {
        if (!options.hasAccessKey) return deniedWithoutKey()
        if (!options.isSecureEnough) return unavailableOverInsecureUrl()
        val exchange = exchange(request, runId)
        if (exchange.failed()) return unavailableOf(exchange.failure)
        if (exchange.rejected()) return denialOf(exchange.response)
        return successOf(exchange.response)
    }

    /** Один обмен с сервером: ответ любого кода или причина, по которой его нет. */
    private class Exchange(
        val response: Response<LookupResponseNet>?,
        val failure: Throwable?,
    ) {
        fun failed(): Boolean = response == null

        fun rejected(): Boolean = response != null && !response.isSuccessful
    }

    /** Вызов сервиса; отмена корутины пролетает, остальное — в [Exchange.failure]. */
    private suspend fun exchange(request: LookupRequestApiEntity, runId: Long): Exchange = try {
        Exchange(response = service.lookup(runId.toString(), request.toNet()), failure = null)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Exchange(response = null, failure = failure)
    }

    /** Ключ доступа не задан — запрос не отправляется (WFT-19). */
    private fun deniedWithoutKey(): LookupResult = LookupResult.Denied(DenyReason.NO_ACCESS_KEY)

    /** Адрес не `https` — соединение не устанавливается (WFT-21). */
    private fun unavailableOverInsecureUrl(): LookupResult = LookupResult.Unavailable(INSECURE_BASE_URL)

    /** Сеть, таймаут, тело не по схеме → «сервер не ответил» (WFT-22, WFT-23). */
    private fun unavailableOf(failure: Throwable?): LookupResult {
        val cause = failure?.let { "${it.javaClass.simpleName}: ${it.message}" } ?: "no response"
        return LookupResult.Unavailable(cause = cause)
    }

    /**
     * 401, 429, 413 — отказ по коду HTTP; 400 — отказ только с кодом
     * `unsupported_language`, иначе это наша ошибка в запросе; 503 — отказ
     * только с кодом `llm_unavailable` (WFT-63.5); любой другой код —
     * «сервер не ответил» (WFT-23).
     */
    private fun denialOf(response: Response<LookupResponseNet>?): LookupResult {
        val code = response?.code() ?: return unavailableOf(null)
        return when (code) {
            HTTP_UNAUTHORIZED -> LookupResult.Denied(DenyReason.UNAUTHORIZED)
            HTTP_TOO_MANY_REQUESTS -> LookupResult.Denied(DenyReason.RATE_LIMITED)
            HTTP_PAYLOAD_TOO_LARGE -> LookupResult.Denied(DenyReason.REQUEST_TOO_LARGE)
            HTTP_BAD_REQUEST -> badRequestOf(response)
            HTTP_UNAVAILABLE -> serviceUnavailableOf(response)
            else -> LookupResult.Unavailable(cause = "http $code")
        }
    }

    private fun badRequestOf(response: Response<LookupResponseNet>): LookupResult {
        val errorCode = errorCodeOf(response)
        if (errorCode == UNSUPPORTED_LANGUAGE_CODE) return LookupResult.Denied(DenyReason.UNSUPPORTED_LANGUAGE)
        return LookupResult.Unavailable(cause = "http 400 $errorCode")
    }

    /** 503 с кодом `llm_unavailable` — нейронка не ответила ни разу (WFT-99); без кода — как прежде, недоступен. */
    private fun serviceUnavailableOf(response: Response<LookupResponseNet>): LookupResult {
        val errorCode = errorCodeOf(response)
        if (errorCode == LLM_UNAVAILABLE_CODE) return LookupResult.Denied(DenyReason.LLM_UNAVAILABLE)
        return LookupResult.Unavailable(cause = "http 503 $errorCode")
    }

    /** Код причины из тела ошибки; тело не по схеме — пустая строка. */
    private fun errorCodeOf(response: Response<LookupResponseNet>): String {
        val raw = response.errorBody()?.string() ?: return ""
        return try {
            json.decodeFromString(ErrorNet.serializer(), raw).code
        } catch (malformed: SerializationException) {
            ""
        } catch (malformed: IllegalArgumentException) {
            ""
        }
    }

    /** 200 → зеркала api-модуля (WFT-56); пустое тело — не по контракту. */
    private fun successOf(response: Response<LookupResponseNet>?): LookupResult {
        val body = response?.body() ?: return LookupResult.Unavailable(cause = "empty body")
        return body.toApiEntity()
    }

    companion object {
        internal const val INSECURE_BASE_URL = "base url is not https"
        private const val HTTP_BAD_REQUEST = 400
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_PAYLOAD_TOO_LARGE = 413
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private const val HTTP_UNAVAILABLE = 503
        private const val UNSUPPORTED_LANGUAGE_CODE = "unsupported_language"
        private const val LLM_UNAVAILABLE_CODE = "llm_unavailable"
        private val json = Json { ignoreUnknownKeys = true }
    }
}
