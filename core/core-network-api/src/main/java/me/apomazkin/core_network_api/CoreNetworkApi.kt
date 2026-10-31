package me.apomazkin.core_network_api

import me.apomazkin.core_network_api.entity.LemmasRequestApiEntity
import me.apomazkin.core_network_api.entity.LemmasResult
import me.apomazkin.core_network_api.entity.LexemesRequestApiEntity
import me.apomazkin.core_network_api.entity.LexemesResult

/**
 * Слой сети приложения: разговор с сервером слов
 * (контракт `backend/api/openapi.yaml`, спеки
 * `backend/docs/handbook/specs/lexemes-from-lemma/spec.md` и
 * `backend/docs/handbook/specs/lemmas-from-text/spec.md`).
 *
 * Устроен как слой базы ([me.apomazkin.core_db_api.CoreDbApi]): этот модуль —
 * публичный язык слоя, реализация в `:core:core-network-impl`, фасад с
 * Dagger-компонентом в `:core:core-network`. Экраны этого модуля не видят,
 * их порты в `app` соединяют распознавание, словарь и сеть.
 *
 * Словарь имён (IS529, IS536, IS537, IS538):
 * - **запрос по лемме** — [LexemesRequestApiEntity]: языки словаря, лемма,
 *   контекст снимка или null и дерево компонентов словаря (LFL-103,
 *   LFL-104, LFL-78);
 * - **запрос лемм** — [LemmasRequestApiEntity]: языки словаря и токены
 *   снимка без дерева (LFT-02);
 * - **токен** — [me.apomazkin.core_network_api.entity.TokenApiEntity]: текст,
 *   уверенность распознавания, язык по мнению распознавания или null;
 * - **исход по лемме** — [LexemesResult]: ровно один из трёх исходов,
 *   исключений наружу слой не выпускает (LFL-23); успех — одно слово ответа
 *   и рекомендации (контракт, раздел 2);
 * - **исход лемм** — [LemmasResult]: так же ровно один из трёх; успех —
 *   текст, перевод и четыре списка строк (LFT-14);
 * - **слово** — [me.apomazkin.core_network_api.entity.WordApiEntity]: слово
 *   ответа со статусом, леммами и замечаниями; у леммы —
 *   лексемы-кандидаты с сопоставлением (LFL-87);
 * - **номер запуска** — `runId`, уходит на сервер заголовком `X-Run-Id` и
 *   связывает строки лога устройства и сервера (термин «Запуск»).
 */
interface CoreNetworkApi {

    /** Адреса сервера слов: `lexemesFromLemma` и `lemmasFromTokens`. */
    interface WordsApi {

        /**
         * Один запрос `lexemesFromLemma` (LFL-103, LFL-104): лемма с контекстом
         * снимка или без него, одно слово в ответе. Ждёт ответ до 180 секунд
         * (LFL-22), ходит только по HTTPS (LFL-21); пустой ключ доступа —
         * [LexemesResult.Denied] с [me.apomazkin.core_network_api.entity.DenyReason.NO_ACCESS_KEY]
         * без обращения к сети (ACC-19).
         */
        suspend fun lexemesFromLemma(request: LexemesRequestApiEntity, runId: Long): LexemesResult

        /**
         * Один запрос `lemmasFromTokens` (LFT-02). Ждёт ответ до 180 секунд,
         * ходит только по HTTPS; пустой ключ доступа — [LemmasResult.Denied]
         * без обращения к сети (ACC-19).
         */
        suspend fun lemmasFromTokens(request: LemmasRequestApiEntity, runId: Long): LemmasResult
    }
}
