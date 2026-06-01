# Approved findings (scope_analysis, iter 2)

Эти findings прошли inquisitor iter2 и обязаны быть закрыты на следующей итерации шага.

Все три — minor. По правилу review_module: первая minor-only итерация → repeat. Если iter3 опять только minor → принимается с tech debt.

## F010 [architect] minor

**Description:** Пакет `me.apomazkin.lexeme` (строки 76-77, 135) расходится с `naming.md` § «Пакеты» «`me.apomazkin.<module>.entity` — Domain модели» — должен быть `me.apomazkin.lexeme.entity.Lexeme.kt`, либо явный drift_rule «для категории `modules/domain/` модуль сам является доменом, suffix `.entity` избыточен».

**Inquisitor verdict:** scope ссылается на naming.md § «Пакеты» и одновременно предлагает путь без `.entity`-сегмента — нужен либо явный drift_rule, либо коррекция пути.

**Что нужно:** В аспекте про package добавить явный выбор — либо `me.apomazkin.lexeme` с **drift_rule** ("для категории `modules/domain/` где модуль сам является доменом, suffix `.entity` избыточен — Lexeme.kt лежит в корне пакета модуля, не в подпакете `.entity`"), либо `me.apomazkin.lexeme.entity` (соответствует naming.md без drift). Решение зафиксировать в scope, не делегировать sub-flow.

## F011 [qa_engineer] minor

**Description:** Файл `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/entity/Lexeme.kt` (содержит `LexemeLabel` enum, `toLexemeLabel`, `lexicalCategory`, `toChipPicker`) не упомянут ни в § «Удаление», ни в § «Правка».

**Inquisitor verdict:** `dictionarytab/entity/Lexeme.kt` (LexemeLabel + helpers) сосуществует в одной `entity/` папке с мигрируемым `LexemeUiItem.kt` и не классифицирован в scope — реальный пробел в классификации файлов.

**Что нужно:** Через Read проверить содержимое `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/entity/Lexeme.kt`. Файл содержит `LexemeLabel` enum — это категория слова (лексическая часть речи), НЕ доменная сущность Lexeme. Имя файла случайно конфликтует с фичей IS482. Добавить в scope (можно в новую секцию «§ Не трогаем» или в § Аспекты `name_collision_with_lexemelabel`): этот файл не относится к фиче IS482, имя совпадает по случайности (Lexeme как название файла vs LexemeLabel как содержимое). Решение по дальнейшему — оставить как есть либо переименовать файл (опционально, не часть IS482).

## F012 [qa_engineer] minor

**Description:** В § «Правка» указан `WordCardReducer.kt` как требующий правки import / package-qualified-доступа к `Lexeme`, но Reducer не использует тип `Lexeme` напрямую — только `LexemeState`, `lexemeId: Long`.

**Inquisitor verdict:** WordCardReducer.kt не использует доменный тип `Lexeme` (только `LexemeState`/`lexemeId: Long`) — файл ошибочно включён в § Правка.

**Что нужно:** Через Grep проверить — `WordCardReducer.kt` действительно не содержит ссылок на тип `Lexeme`. Если так — удалить эту строку из § Правка. (Технически закомментированная строка `// ===== Lexeme =====` это комментарий, не использование типа.)
