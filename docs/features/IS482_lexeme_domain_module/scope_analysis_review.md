# Review: scope_analysis

## Итерация 1 (2026-05-30T15:13:57-0600)

### F001 [architect] critical

**Description:** Аспекты `module_kotlin_only` (pure-Kotlin domain) и `mapper_location` (Variant A — mapper в domain/lexeme зависит от core-db-api) конфликтуют: `core-db-api/build.gradle.kts` использует `com.android.library`, а pure-Kotlin (`java-library`) модуль не может потреблять aar-артефакт. Решения двух sub-flow будут несовместимы — нужно зафиксировать constraint в scope.

**Status:** approved

**Verdict:** Реальный конфликт — `core-db-api` это `com.android.library`, pure-Kotlin модуль не может потреблять aar, scope зафиксировал оба варианта без указания на блокер.

### F002 [architect] critical

**Description:** Маппер `LexemeApiEntity → Lexeme` переезжает в `modules/domain/lexeme`, что нарушает документированную конвенцию data-layer.md § «Маппинг сущностей» «API → Domain (в UseCase модуле)» и инвертирует Dependency Rule (domain → data вместо data → domain). Артефакт фиксирует решение в § Аспекты без явного drift_rule и без сравнения с альтернативой «оставить маппер в app/ как общую top-level extension».

**Status:** approved

**Verdict:** Перенос маппера в `modules/domain/lexeme` противоречит data-layer.md «API → Domain в UseCase модуле», и хотя аспект `mapper_location` упомянут, явного drift_rule и сравнения с альтернативой «оставить в app/» нет.

### F003 [architect] minor

**Description:** Пакет нового модуля заявлен как `me.apomazkin.lexeme`, тогда как `naming.md` § «Пакеты» предписывает `me.apomazkin.<module>.entity` для domain-моделей. Для модуля-домена это сознательное отклонение — нужно зафиксировать как convention extension либо использовать `me.apomazkin.lexeme.entity`.

**Status:** rejected

**Verdict:** Конкретный package path — design-решение sub-flow, а не классификация слоёв; scope_analysis не обязан фиксировать convention extension.

### F004 [architect] minor

**Description:** `LexemeUiItem` содержит `wordId: Long`, отсутствующее в `wordcard.Lexeme`. Если business sub-flow выберет union-shape без `wordId`, dictionaryTab потребуется новая UI-обёртка — этого файла нет в секции «Создание». Скоп списка файлов условен от решения business sub-flow и не отражён.

**Status:** rejected

**Verdict:** Артефакт явно упоминает `wordId` и условную UI-обёртку в § Удаление (строка 86), отдавая решение business sub-flow — finding утверждает обратное.

### F005 [qa_engineer] minor

**Description:** Тестовый план для маппера упоминает «null-cases для translation/definition», но не уточняет ожидаемое поведение (маппер падает / возвращает null Lexeme / создаёт Lexeme с null-полями) — критерий проверки недетерминирован.

**Status:** rejected

**Verdict:** Ожидаемое поведение маппера для null-cases — design-решение business sub-flow, scope лишь фиксирует что тесты нужны.

### F006 [qa_engineer] minor

**Description:** Claim «category всегда null (строка 221 WordCardUseCaseImpl)» не сопровождается Verify через Read/Grep — невалидный claim о коде; то же касается ссылок на строки 216-225, 117-123, 108-121, 140-153 в трёх UseCaseImpl и строк 105-107 / 80-141 в IS481-документах.

**Status:** rejected

**Verdict:** Проверил по коду — `WordCardUseCaseImpl.kt:221` действительно `category = null`, диапазоны 216-225 / 117-123 точно описывают функции маппинга, claim ревьюера ложный.

### F007 [qa_engineer] minor

**Description:** Список тестовых файлов для правки импортов в wordcard указан как «4-5 файлов» с «и т.д.» — нечёткое количество и неполный перечень делает scope невоспроизводимым (другой разработчик не сможет проверить что все консьюмеры мигрированы).

**Status:** approved

**Verdict:** «4-5 файлов … и т.д.» в шаге scope_analysis — нечёткое перечисление, scope должен быть воспроизводим, а не приблизителен.

### F008 [qa_engineer] minor

**Description:** Не описан негативный сценарий частичной миграции — что считается признаком успешного завершения (ноль импортов `wordcard.entity.Lexeme` / `quiz.chat.entity.Lexeme` / `dictionarytab.entity.LexemeUiItem` в коде); без этого критерия покрытие миграции непроверяемо.

**Status:** rejected

**Verdict:** Критерий успешности миграции — acceptance-criteria sub-flow, а не классификация слоёв scope_analysis.

### F009 [qa_engineer] minor

**Description:** «Возможно нужно» для `app/build.gradle.kts` (строка 29, 94) оставлено как открытый вопрос без критерия проверки — нужен явный шаг «собрать `app` после правок и убедиться что транзитивные зависимости работают», иначе scope недетерминирован.

**Status:** rejected

**Verdict:** «Возможно нужно для `app/build.gradle.kts`» явно делегировано infra sub-flow («Проверить — задача infra sub-flow») — это правильная передача, а не недетерминизм scope.

## Итерация 2 (2026-05-30T15:20:39-0600)

Iter2 закрыла approved iter1 (F001, F002, F007). Найдены новые findings.

### F010 [architect] minor

**Description:** Пакет `me.apomazkin.lexeme` (строки 76-77, 135) расходится с `naming.md` § «Пакеты» «`me.apomazkin.<module>.entity` — Domain модели» — должен быть `me.apomazkin.lexeme.entity.Lexeme.kt`, либо явный drift_rule «для категории `modules/domain/` модуль сам является доменом, suffix `.entity` избыточен».

**Status:** approved

**Verdict:** Scope ссылается на naming.md § «Пакеты» и одновременно предлагает путь без `.entity`-сегмента — нужен либо явный drift_rule, либо коррекция пути; это область scope (классификация и фиксация отклонений от гайдов).

### F011 [qa_engineer] minor

**Description:** Файл `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/entity/Lexeme.kt` (содержит `LexemeLabel` enum, `toLexemeLabel`, `lexicalCategory`, `toChipPicker`) не упомянут ни в § «Удаление», ни в § «Правка». Этот файл сосуществует с `LexemeUiItem.kt` в той же `entity/` папке; после миграции его судьба не определена.

**Status:** approved

**Verdict:** `dictionarytab/entity/Lexeme.kt` (LexemeLabel + helpers) сосуществует в одной `entity/` папке с мигрируемым `LexemeUiItem.kt` и не классифицирован в scope — реальный пробел в классификации файлов.

### F012 [qa_engineer] minor

**Description:** В § «Правка» указан `WordCardReducer.kt` как требующий правки import / package-qualified-доступа к `Lexeme`, но Reducer не использует тип `Lexeme` напрямую — только `LexemeState`, `lexemeId: Long`. Лишний файл в scope.

**Status:** approved

**Verdict:** WordCardReducer.kt не использует доменный тип `Lexeme` (только `LexemeState`/`lexemeId: Long`) — файл ошибочно включён в § Правка, это неверная классификация scope.

## Итерация 3 (2026-05-30T17:01:10-0600)

Iter3 закрыла approved iter2 (F010, F011, F012) точечными правками.

### PASS [architect]

Все iter2 findings закрыты. Architectural correctness: чистота слоёв соблюдена (domain pure-Kotlin), Dependency Rule не нарушен (mapper в `app/`), границы конечны, циклов нет.

### PASS [qa_engineer]

Все iter2 findings закрыты. Scope consistent, main-source consumers перечислены, test файлы точно идентифицированы (3 wordcard, 0 в quiz/chat и dictionaryTab tests).

**Решение:** оба ревьювера PASS → `raw_findings = []` → петля закрывается **без inquisitor**. `review_passed = true`.
