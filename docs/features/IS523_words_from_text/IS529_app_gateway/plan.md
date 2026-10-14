# IS529 | План: шлюз в приложении и список слов из ответа сервера

Бриф — `brief.md` (Д1–Д4). Спека — `backend/spec/words-from-text.md`,
строки WFT-16…23, 56…58, 60…63, 65. Этапы в порядке выполнения,
независимое первым. Каждый пункт — было → будет → мотивация.

Флоу разработки — второй прогон H-06 (`backend/agents/LEDGER.md`):
словарь → каркас с заглушками → красные тесты → ревью каркаса →
реализация → покрытие → ревью этапа. Ревью на этом прогоне — моя
самопроверка по `backend/docs/handbook/speaking-code.md`, без агентов
(юзер: пока без ревью агентами). Метрики прогона —
`backend/agents/runs/dev-flow/<дата>-IS529/`.

Статусы (2026-10-11): Э1–Э9 сделаны, Э10 — коммит PolyTrainer на подходе
(lexeme закоммичен тегами v0.1.3…v0.1.7). Отклонения от плана: OkHttp
5.4.0 вместо 5.5.0 (compileSdk 36); проверка ключа — в слое сети
(`Denied(NO_ACCESS_KEY)`), без флага в стейте; адреса в именах Kotlin —
`WFT-NN_k`; в элементе списка четыре строки (Д4–Д6: лемма, английское
толкование, русское значение); сервер за прогон получил WFT-66 (русские
значения), темп и паузу по политике лимитов Wikimedia (Д25, Д27);
локальный стенд (Docker, adb reverse) был поднят и запрещён юзером,
следы из кода убраны.

## Э1. Выдача ключа одной командой (репозиторий lexeme)

- **Было:** ключ выписывается руками: ssh на сервер, `docker exec
  lexeme-server /app/lexeme key issue <имя> --limit N`, ключ копируется
  из вывода.
- **Будет:** в `Taskfile.yml` lexeme задачи `key:issue` и `key:revoke`:
  ssh по адресу из `deploy/.env` (`DEPLOY_HOST`, `DEPLOY_USER`), внутри
  `docker exec`, ключ печатается один раз в терминал юзера и никуда не
  пишется. Описание в `docs/handbook/deploy.md`.
- **Мотивация:** Д2 — ключи выписывает юзер сам, ему нужна одна команда.

## Э2. Зависимости и каркас модулей (сборка)

- **Было:** сетевых библиотек в проекте нет, каталоги `deps/*.toml` без
  сети, модули `core/core-db-*` — единственный «слой данных».
- **Будет:** каталог `deps/network.versions.toml` → `networkLibs`:
  Retrofit 3.0.0 и `converter-kotlinx-serialization` 3.0.0, OkHttp 5.5.0
  и `mockwebserver3` 5.5.0 (тесты), `kotlinx-serialization-json` 1.11.0,
  плагин `org.jetbrains.kotlin.plugin.serialization` версией Kotlin
  (2.2.0) в `libs.plugins`. Три модуля в `settings.gradle.kts`:
  `:core:core-network-api` (чистый Kotlin/Android library без Compose),
  `:core:core-network-impl` (плагин сериализации, Retrofit, OkHttp,
  Dagger), `:core:core-network` (фасад, `api(core-network-api)` +
  `implementation(core-network-impl)`). `app` зависит на
  `:core:core-network`. `app/build.gradle.kts`: `LEXEME_ACCESS_KEY` из
  `project.findProperty`/`local.properties` в `BuildConfig` обоих типов
  сборки (release — пустая строка по умолчанию), `LEXEME_BASE_URL` =
  `https://api.lexeme.pro`.
- **Мотивация:** Д1, Д3. Версии сверены с Maven Central 2026-10-10;
  Retrofit 3 сам тянет OkHttp 4.12, поэтому OkHttp 5 закрепляется явно.

## Э3. Словарь и api-модуль

- **Было:** имён для сети в проекте нет; экран оперирует `List<String>`
  слов.
- **Будет:** `core-network-api`, пакет `me.apomazkin.core_network_api`:
  `CoreNetworkApi` с вложенным `WordsApi { suspend fun lookup(request:
  LookupRequest, runId: Long): LookupResult }`, `CoreNetworkProvider`
  (как `CoreDbProvider`), сущности `LookupRequest(learningLanguage,
  translationLanguage, tokens)`, `TokenQuery(text, confidence,
  language?)`, `WordResult(text, status, candidates)`, `WordStatus
  {FOUND, NOT_FOUND, ERROR}`, `Candidate(lemma, partOfSpeech, formNote?,
  senses)`, `Sense(gloss, examples)`, sealed `LookupResult { Success
  (attribution, words), Denied(reason: DenyReason {UNAUTHORIZED,
  RATE_LIMITED, REQUEST_TOO_LARGE, UNSUPPORTED_LANGUAGE}), Unavailable
  (cause: String) }`. Файл `doc.go`-аналог — KDoc в `CoreNetworkApi.kt`
  со словарём имён и ссылками на WFT. Без аннотаций сериализации.
- **Мотивация:** шаг «словарь» флоу; api-модуль — публичный язык слоя,
  как `CoreDbApi`; исключения наружу не выходят, экран получает один из
  трёх исходов (WFT-23, 63).

## Э4. Каркас impl, фасада и проводки с заглушками

- **Было:** нет.
- **Будет:** `core-network-impl`: Retrofit-интерфейс `LexemeService`
  (`@POST("v1/words/lookup")`, `@Header("X-Run-Id")`), DTO с
  `@Serializable` (`LookupRequestDto`, `TokenQueryDto`,
  `LookupResponseDto`, `WordResultDto`, `CandidateDto`, `SenseDto`,
  `ErrorDto`), `AccessKeyInterceptor` (ставит `Authorization: Bearer`),
  фабрика `OkHttpClient` (`callTimeout` 180 с, системные центры по
  умолчанию, проверка схемы `https` при сборке базового адреса,
  логирующий перехватчик без заголовков), `WordsApiImpl` — история
  `lookup`: `response = service.lookup(...)`, `if (!response.isSuccessful)
  return denialOf(response)`, `return successOf(body)`, с хелперами
  `denialOf`, `successOf`, `unavailableOf(throwable)` — пока `TODO()`.
  Dagger `NetworkComponent` с `@BindsInstance accessKey: String, baseUrl:
  String, logger`. `core-network`: `CoreNetworkComponent.init(accessKey,
  baseUrl, logger)` с ленивым синглтоном, как `CoreDbComponent`. `App`
  поднимает компонент и отдаёт `CoreNetworkProvider` в `AppComponent`
  через `dependencies`. `WordsTabUseCase` получает `suspend fun
  lookupWords(tokens: List<RecognizedWord>, runId: Long): LookupResult`,
  реализация в `WordsTabUseCaseImpl` берёт языки текущего словаря из
  `CurrentDictionaryProvider` и зовёт `WordsApi`. Собирается, тестов
  ещё нет.
- **Мотивация:** шаг «каркас» флоу; каркас повторяет слой базы один в
  один (Д3); история `lookup` — говорящий код по правилам handbook.

## Э5. Красные тесты

- **Было:** `ExtractWordsTest` проверяет нормализацию на устройстве,
  тесты редьюсера IS527 без адресов спеки.
- **Будет:** имена тестов несут адреса `WFT-NN.k`:
  - `core-network-impl` на `MockWebServer`: WFT-20.1, 20.2 (тело
    запроса и заголовки), WFT-21.1 (адрес `http` → `Unavailable`),
    WFT-22.1–22.3 (таймаут через подменяемый `callTimeout`),
    WFT-23.1–23.3 (отказ соединения, 500, тело не по схеме), WFT-63.1–63.4
    (401/429/413/400 → `Denied` с причиной по `code`), WFT-05.1 (ключ не в
    логах перехватчика), WFT-56.7, 56.8 (маппинг записанного ответа
    сервера: фикстура `src/test/resources/lookup_corrian_fuera.json`,
    снята с прода без ключа в файле).
  - `wordstab`: `PrepareTokensTest` WFT-17.1–17.5, WFT-18.1, 18.2;
    редьюсер — WFT-16.1, 16.2 (фаза и подпись), WFT-19.1 (пустой ключ →
    запроса нет, «нет доступа»), WFT-60.1, 60.2, WFT-61.1, 61.2,
    WFT-62.1–62.4 (чужой `requestId` отбрасывается), WFT-63.1–63.4
    (тексты по причине), WFT-65.1 (стейт без восстановления запуска).
    WFT-01…03 уже покрыты тестами IS527 — переименовать под адреса.
  - Все красные: заглушки `TODO()` и старый редьюсер.
- **Мотивация:** «красный сначала» (CHECKS); адреса в именах кормят
  `backend/scripts/spec-coverage.sh`.

## Э6. Ревью каркаса

- **Было:** нет.
- **Будет:** самопроверка по `speaking-code.md`: публичные методы
  `WordsApiImpl.lookup`, `WordsTabUseCaseImpl.lookupWords` и ветки
  редьюсера — только история (присвоение из вызова, `if`, `for`,
  `return`), хелперы «глагол-объект-критерий»; граница слоёв: экран не
  импортирует `core_network_*`, api-модуль не импортирует impl и Retrofit.
  Находки правятся до реализации, список — в `agents/runs/dev-flow/…/
  review.md`.
- **Мотивация:** шаг флоу; дешевле поправить имена до того, как на них
  лягут тела.

## Э7. Реализация

- **Было:** `extractWords` нормализует на устройстве, диалог показывает
  `List<String>`, `isRecognizing` — единственная фаза, сети нет.
- **Будет:**
  - `core-network-impl`: тела `denialOf` (код → `DenyReason`, иной код →
    `Unavailable`), `successOf` (DTO → сущности), `unavailableOf`
    (`IOException`, `SerializationException` → `Unavailable`),
    `catch` только в одном месте — границе слоя.
  - `wordstab`: `prepareTokens(words): List<RecognizedWord>` вместо
    `extractWords` (пустые прочь, дубли текста — первый по месту с
    наибольшей уверенностью, первые 2000); `PhotoWordsState`: `phase:
    PhotoPhase {IDLE, RECOGNIZING, LOOKING}` вместо `isRecognizing`,
    `words: List<PhotoWord(text, lemmaLine: String?, markRes: Int?)>`,
    `errorRes`, `requestId`; сообщения `TokensReady(tokens, requestId)`,
    `WordsLookedUp(result, requestId)` вместо `WordsRecognized`; эффект
    `LookupWords(tokens, requestId)` в `DatasourceEffectHandler` →
    `useCase.lookupWords`; пустые токены → «слов не найдено» без эффекта;
    пустой ключ → `errorRes` «нет доступа» без эффекта (ключ приходит в
    стейт из `UseCase` как `hasAccessKey: Boolean` при открытии диалога).
  - UI `PhotoWordsDialog`: подпись индикатора по фазе («Распознаю
    текст», «Ищу слова в словаре»), элемент списка в две строки (слово;
    «лемма, часть речи» либо пометка), число слов в заголовке, подпись
    «Данные: Wiktionary, CC BY-SA 4.0» под списком с `ACTION_VIEW` на
    лицензию, кнопка «Отменить» во время фаз, «Закрыть» после.
  - Строки en/ru в `core-resources`: `photo_words_looking`,
    `photo_words_not_found_mark`, `photo_words_error_mark`,
    `photo_words_attribution`, `photo_words_error_no_access`,
    `photo_words_error_rate_limited`, `photo_words_error_too_many`,
    `photo_words_error_language`, `photo_words_error_server`.
  - Лог устройства: `VOCAB` — «lookup run=N tokens=K», «lookup run=N
    status=… words=M ms=T»; ключ не печатается.
  - Тесты Э5 зелёные, `./scripts/cc-build.sh :core:core-network-impl:
    testDebugUnitTest`, затем `:modules:screen:wordstab:testDebugUnitTest`,
    затем `:app:lintDebug`, затем `assembleDebug`.
- **Мотивация:** закрыть WFT-16…23, 56…58, 60…63, 65 кодом; Д4 про лемму.

## Э8. Покрытие и ручники

- **Было:** `spec-coverage.sh` сканирует только Go-тесты lexeme.
- **Будет:** скрипт принимает дополнительные каталоги (`SPEC_EXTRA_DIRS`)
  и файл ручников; прогон по тестам Kotlin этого репозитория через
  симлинк `backend`. Документ ручников `manual.md` в папке подфичи по
  `backend/spec/CHECKS.md`: кейсы с адресами WFT-16.3, 16.4, 19.1, 56.1,
  56.3–56.5, 56.7, 57.1, 57.2, 58.1, 60.1, 60.3, 61.1, 62.2, 63.1, 63.2,
  65.1; словарь логов `VOCAB`. Прогон на Pixel 7 Pro, dev-сборка
  `co.lexeme.app.dev`, кейсы в чат по одному. Ключ юзер кладёт в
  `local.properties` сам (Э1).
- **Мотивация:** «реализовано» ставится по покрытию, не по ощущению
  (METHOD §3); ручной прогон — первое испытание цепочки на девайсе.

## Э9. Статусы, пропуски, метрики

- **Было:** статусы устройства в спеке «принято»; `misses.md` — два
  пропуска сервера; метрики dev-flow — один прогон.
- **Будет:** статусы покрытых строк → «реализовано», что не покрыто —
  остаётся «принято» с причиной в CHANGELOG; дыры спеки, найденные на
  девайсе, — в `agents/runs/spec-review/2026-10-09-words-from-text/
  misses.md`; метрики второго прогона H-06 (число красных тестов, доля
  правок каркаса после ревью, время этапов) — в `agents/runs/dev-flow/…`,
  исход гипотезы H-06 — в `LEDGER.md`.
- **Мотивация:** реестр агентов и флоу живёт только через гипотезы с
  метриками.

## Э10. Коммиты

- **Было:** PolyTrainer: ветка `IS523_words_from_text` с коммитом IS527,
  доки IS523 частично дублируют lexeme. lexeme: чистое дерево на
  `42ffdbe`.
- **Будет:** один коммит фичи в `IS523_words_from_text` (код, тесты,
  бриф, план, ручники; удаление переехавших доков `brief.md`,
  `phases.md`, `pipeline.md`, `research_surface.md`, `IS528_backend_wiki/`
  с указателем на lexeme в `docs/features/IS523_words_from_text/
  README.md`); push после подтверждения. lexeme: коммит «спека WFT-56,
  CHANGELOG, Taskfile key:issue, скрипт покрытия, метрики прогона», push
  в master; тег не нужен, сервер не менялся.
- **Мотивация:** «доки едут с кодом», коммит фичи один после зелёных
  ручников.
