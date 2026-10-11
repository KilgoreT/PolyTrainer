package me.apomazkin.core_network_impl

import me.apomazkin.core_network_impl.dto.LookupRequestDto
import me.apomazkin.core_network_impl.dto.LookupResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/** Адреса сервера слов, как в `backend/api/openapi.yaml`. */
internal interface LexemeService {

    /**
     * `lookupWords` (Ш6). Заголовок `Authorization` ставит
     * [AccessKeyInterceptor]; `X-Run-Id` — номер запуска для склейки логов.
     */
    @POST("v1/words/lookup")
    suspend fun lookup(
            @Header(RUN_ID_HEADER) runId: String,
            @Body body: LookupRequestDto,
    ): Response<LookupResponseDto>

    companion object {
        const val RUN_ID_HEADER = "X-Run-Id"
    }
}
