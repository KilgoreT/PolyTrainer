package me.apomazkin.core_network_impl

import me.apomazkin.core_network_impl.net.LemmasRequestNet
import me.apomazkin.core_network_impl.net.LemmasResponseNet
import me.apomazkin.core_network_impl.net.LexemesRequestNet
import me.apomazkin.core_network_impl.net.LexemesResponseNet
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/** Адреса сервера слов, как в `backend/api/openapi.yaml` 0.5.0. */
internal interface LexemeService {

    /**
     * `lexemesFromLemma` (LFL Ш1…Ш8). Заголовок `Authorization` ставит
     * [AccessKeyInterceptor]; `X-Run-Id` — номер запуска для склейки логов.
     */
    @POST("v1/lexemes/from-lemma")
    suspend fun lexemesFromLemma(
        @Header(RUN_ID_HEADER) runId: String,
        @Body body: LexemesRequestNet,
    ): Response<LexemesResponseNet>

    /** `lemmasFromTokens` (LFT Ш1…Ш5): заголовки те же. */
    @POST("v1/lemmas/from-tokens")
    suspend fun lemmasFromTokens(
        @Header(RUN_ID_HEADER) runId: String,
        @Body body: LemmasRequestNet,
    ): Response<LemmasResponseNet>

    companion object {
        const val RUN_ID_HEADER = "X-Run-Id"
    }
}
