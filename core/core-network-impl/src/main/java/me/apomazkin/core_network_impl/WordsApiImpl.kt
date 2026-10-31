package me.apomazkin.core_network_impl

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import me.apomazkin.core_network_api.CoreNetworkApi
import me.apomazkin.core_network_api.entity.DenyReason
import me.apomazkin.core_network_api.entity.LemmasRequestApiEntity
import me.apomazkin.core_network_api.entity.LemmasResult
import me.apomazkin.core_network_api.entity.LexemesRequestApiEntity
import me.apomazkin.core_network_api.entity.LexemesResult
import me.apomazkin.core_network_impl.mapper.toApiEntity
import me.apomazkin.core_network_impl.mapper.toNet
import me.apomazkin.core_network_impl.net.ErrorNet
import retrofit2.Response
import javax.inject.Inject

/**
 * `lexemesFromLemma` и `lemmasFromTokens` поверх Retrofit. Единственное место
 * слоя, где исключения сети и разбора превращаются в «сервер недоступен».
 * Обмен с сервером и разбор кодов у операций одинаковы — [call]; операция
 * добавляет только адрес и разбор тела 200.
 */
internal class WordsApiImpl @Inject constructor(
    private val service: LexemeService,
    private val options: NetworkOptions,
) : CoreNetworkApi.WordsApi {

    override suspend fun lexemesFromLemma(
        request: LexemesRequestApiEntity,
        runId: Long,
    ): LexemesResult {
        val outcome = call { service.lexemesFromLemma(runId.toString(), request.toNet()) }
        return when (outcome) {
            is Outcome.Success -> outcome.body.toApiEntity()
            is Outcome.Denied -> LexemesResult.Denied(outcome.reason)
            is Outcome.Unavailable -> LexemesResult.Unavailable(outcome.cause)
        }
    }

    override suspend fun lemmasFromTokens(
        request: LemmasRequestApiEntity,
        runId: Long,
    ): LemmasResult {
        val outcome = call { service.lemmasFromTokens(runId.toString(), request.toNet()) }
        return when (outcome) {
            is Outcome.Success -> outcome.body.toApiEntity()
            is Outcome.Denied -> LemmasResult.Denied(outcome.reason)
            is Outcome.Unavailable -> LemmasResult.Unavailable(outcome.cause)
        }
    }

    /**
     * Обмен с сервером до разбора тела: 200 с телом, отказ с причиной,
     * недоступен с причиной для лога. Общий для обеих операций.
     */
    private sealed interface Outcome<out T> {
        data class Success<T>(
            val body: T,
        ) : Outcome<T>

        data class Denied(
            val reason: DenyReason,
        ) : Outcome<Nothing>

        data class Unavailable(
            val cause: String,
        ) : Outcome<Nothing>
    }

    /**
     * Ключ не задан → отказ без сети (ACC-19); не HTTPS → недоступен (LFL-21);
     * сети нет или ответа нет → недоступен (LFL-22, LFL-23); код отказа →
     * отказ по причине (LFL-63), иной код → недоступен; 200 без тела — не по
     * контракту; 200 с телом → тело. Отмена корутины пролетает.
     */
    private suspend fun <T : Any> call(request: suspend () -> Response<T>): Outcome<T> {
        if (!options.hasAccessKey) return Outcome.Denied(DenyReason.NO_ACCESS_KEY)
        if (!options.isSecureEnough) return Outcome.Unavailable(INSECURE_BASE_URL)
        val response = try {
            request()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            return unavailableOf(failure)
        }
        if (!response.isSuccessful) return denialOf(response)
        val body = response.body() ?: return Outcome.Unavailable(cause = "empty body")
        return Outcome.Success(body)
    }

    /** Сеть, таймаут, тело не по схеме → «сервер не ответил» (LFL-22, LFL-23). */
    private fun unavailableOf(failure: Throwable): Outcome.Unavailable =
        Outcome.Unavailable(cause = "${failure.javaClass.simpleName}: ${failure.message}")

    /**
     * 401, 429, 413 — отказ по коду HTTP; 400 — отказ только с кодом
     * `unsupported_language`, иначе это наша ошибка в запросе; 503 — отказ
     * только с кодом `llm_unavailable` (LFL-63.5); любой другой код —
     * «сервер не ответил» (LFL-23).
     */
    private fun denialOf(response: Response<*>): Outcome<Nothing> = when (response.code()) {
        HTTP_UNAUTHORIZED -> Outcome.Denied(DenyReason.UNAUTHORIZED)
        HTTP_TOO_MANY_REQUESTS -> Outcome.Denied(DenyReason.RATE_LIMITED)
        HTTP_PAYLOAD_TOO_LARGE -> Outcome.Denied(DenyReason.REQUEST_TOO_LARGE)
        HTTP_BAD_REQUEST -> badRequestOf(response)
        HTTP_UNAVAILABLE -> serviceUnavailableOf(response)
        else -> Outcome.Unavailable(cause = "http ${response.code()}")
    }

    private fun badRequestOf(response: Response<*>): Outcome<Nothing> {
        val errorCode = errorCodeOf(response)
        if (errorCode == UNSUPPORTED_LANGUAGE_CODE) {
            return Outcome.Denied(DenyReason.UNSUPPORTED_LANGUAGE)
        }
        return Outcome.Unavailable(cause = "http 400 $errorCode")
    }

    /**
     * 503 с кодом `llm_unavailable` — нейронка не ответила (LFL-99, LFT-15); без
     * кода — как прежде, недоступен.
     */
    private fun serviceUnavailableOf(response: Response<*>): Outcome<Nothing> {
        val errorCode = errorCodeOf(response)
        if (errorCode == LLM_UNAVAILABLE_CODE) return Outcome.Denied(DenyReason.LLM_UNAVAILABLE)
        return Outcome.Unavailable(cause = "http 503 $errorCode")
    }

    /** Код причины из тела ошибки; тело не по схеме — пустая строка. */
    private fun errorCodeOf(response: Response<*>): String {
        val raw = response.errorBody()?.string() ?: return ""
        return try {
            json.decodeFromString(ErrorNet.serializer(), raw).code
        } catch (malformed: SerializationException) {
            ""
        } catch (malformed: IllegalArgumentException) {
            ""
        }
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
