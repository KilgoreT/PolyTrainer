package me.apomazkin.core_network_api

import me.apomazkin.core_network_api.entity.LookupRequestApiEntity
import me.apomazkin.core_network_api.entity.LookupResult

/**
 * Слой сети приложения: разговор с сервером слов
 * (контракт `backend/api/openapi.yaml`, спека
 * `backend/docs/handbook/specs/words-from-text/spec.md`).
 *
 * Устроен как слой базы ([me.apomazkin.core_db_api.CoreDbApi]): этот модуль —
 * публичный язык слоя, реализация в `:core:core-network-impl`, фасад с
 * Dagger-компонентом в `:core:core-network`. Экраны этого модуля не видят,
 * их порты в `app` соединяют распознавание, словарь и сеть.
 *
 * Словарь имён (IS529, IS536):
 * - **запрос** — [LookupRequestApiEntity]: языки словаря, токены как
 *   распознаны на устройстве и дерево компонентов словаря (WFT-78);
 * - **токен** — [me.apomazkin.core_network_api.entity.TokenApiEntity]: текст,
 *   уверенность распознавания, язык по мнению распознавания или null;
 * - **результат** — [LookupResult]: ровно один из трёх исходов, исключений
 *   наружу слой не выпускает (WFT-23, WFT-63);
 * - **слово** — [me.apomazkin.core_network_api.entity.WordApiEntity]: слово
 *   ответа со статусом, леммами и замечаниями; у леммы —
 *   лексемы-кандидаты с сопоставлением (WFT-87);
 * - **номер запуска** — `runId`, уходит на сервер заголовком `X-Run-Id` и
 *   связывает строки лога устройства и сервера (термин «Запуск»).
 */
interface CoreNetworkApi {

    /** Адреса сервера слов. Пока один — `lookupWords`. */
    interface WordsApi {

        /**
         * Один запрос `lookupWords` (WFT-20). Ждёт ответ до 180 секунд
         * (WFT-22), ходит только по HTTPS (WFT-21). Пустой ключ доступа —
         * [LookupResult.Denied] с [me.apomazkin.core_network_api.entity.DenyReason.NO_ACCESS_KEY]
         * без обращения к сети (WFT-19).
         */
        suspend fun lookup(request: LookupRequestApiEntity, runId: Long): LookupResult
    }
}
