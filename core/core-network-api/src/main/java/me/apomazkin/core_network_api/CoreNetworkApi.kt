package me.apomazkin.core_network_api

import me.apomazkin.core_network_api.entity.LookupRequest
import me.apomazkin.core_network_api.entity.LookupResult

/**
 * Слой сети приложения: разговор с сервером слов `lexeme`
 * (контракт `backend/api/openapi.yaml`, спека `backend/spec/words-from-text.md`).
 *
 * Устроен как слой базы ([me.apomazkin.core_db_api.CoreDbApi]): этот модуль —
 * публичный язык слоя, реализация в `:core:core-network-impl`, фасад с
 * Dagger-компонентом в `:core:core-network`. Экраны этого модуля не видят,
 * их `UseCase` в `app` соединяет распознавание, словарь и сеть.
 *
 * Словарь имён (IS529, бриф Д3):
 * - **запрос** — [LookupRequest]: изучаемый язык, язык перевода и токены
 *   как распознаны на устройстве (Ш6);
 * - **токен** — [me.apomazkin.core_network_api.entity.TokenQuery]: текст,
 *   уверенность распознавания, язык по мнению распознавания или null;
 * - **результат** — [LookupResult]: ровно один из трёх исходов, исключений
 *   наружу слой не выпускает (WFT-23, WFT-63);
 * - **слово** — [me.apomazkin.core_network_api.entity.WordResult]: слово
 *   ответа со статусом найдено / не найдено / ошибка источника и
 *   кандидатами (Ш10);
 * - **кандидат** — [me.apomazkin.core_network_api.entity.Candidate]: лемма,
 *   часть речи, пометка формы, значения;
 * - **номер запуска** — `runId`, уходит на сервер заголовком `X-Run-Id` и
 *   связывает строки лога устройства и сервера (термин «Запуск»).
 */
interface CoreNetworkApi {

    /** Адреса сервера слов. Пока один — `lookupWords` (Ш6 → Ш10). */
    interface WordsApi {

        /**
         * Один запрос `lookupWords` (WFT-20). Ждёт ответ до 180 секунд
         * (WFT-22), ходит только по HTTPS (WFT-21). Пустой ключ доступа —
         * [LookupResult.Denied] с [me.apomazkin.core_network_api.entity.DenyReason.NO_ACCESS_KEY]
         * без обращения к сети (WFT-19).
         */
        suspend fun lookup(request: LookupRequest, runId: Long): LookupResult
    }
}
