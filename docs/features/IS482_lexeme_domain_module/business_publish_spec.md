# publish_spec

## Опубликовано

- `docs/features-spec/lexeme-domain.md` — **создан** (новая фича).
- Имя файла: `lexeme-domain.md` (из META-комментария черновика `spec_filename: lexeme-domain.md`).
- Размер изменений: ~165 строк (вся спека целиком, новая запись в `features-spec/`).

## Корректировки от implement

Без изменений: спека опубликована as-is относительно `business_contract_spec.md`.

Разбор обнаруженных пунктов из `business_implement.md`:

- **`!!.value` smart-cast fix в `QuizGameImpl.kt`** (строки 463, 492, 501, 503) — deviation от DAG (cross-module smart-cast невозможен после переноса `Lexeme` в отдельный модуль), но **не от контракта**: публичный shape `Lexeme.translation: Translation?` и `Lexeme.definition: Definition?` неизменны. В спеке не отражаем — это деталь реализации feature-модуля quiz/chat.
- **Wordcard test literals правка (`category` удалено, `wordId` добавлен)** — factual следствие исключения `category: String?` и включения обязательного `wordId: Long`, которые **уже отражены** в § Domain shape → «Исключённые поля» и § «Состав полей». Дополнительных правок в спеке не требуется.
- **`Lexeme.toUiItem()` extension в `dictionarytab/entity/LexemeUiItem.kt`** — соответствует рекомендации спеки в § UseCase → `DictionaryTabUseCase` («рекомендация: рядом с `LexemeUiItem` в dictionaryTab»). Совпадение, не корректировка.

## PUML

- Скопировано: PUML-схем в feature dir нет, шаг пропущен.
- Целевая директория: N/A.

## Заметки

- META-комментарий `<!-- META: spec_filename: lexeme-domain.md -->` из черновика удалён в финальной спеке.
- Раздел «Тестовые сценарии» из черновика **удалён** в финальной спеке (по `FORGEFLOW_contract_design.md` § «Чего в спеке быть НЕ должно» — это рабочий artefact business_test, не часть публичной спеки).
- Все blockquote-пометки `📎 guide:` из черновика business_contract_spec.md в исходнике **отсутствуют** — они присутствуют только в `business_implement.md` как пост-маркеры. В финальной спеке guide-пометок нет, потому что их не было и в черновике.
- Заголовок: `# Lexeme Domain` (без префикса `IS482`, без `####`).

## log_messages

- IS482 business_publish_spec: финальная спека `docs/features-spec/lexeme-domain.md` опубликована as-is относительно черновика; META и раздел «Тестовые сценарии» убраны.
- Корректировок от implement, затрагивающих публичный контракт `Lexeme` / `Mapper` / `UseCase`, не выявлено: `!!.value` fix — деталь реализации, test-literals — следствие уже зафиксированного shape.
- PUML-схем в feature dir нет, шаг копирования пропущен.

_model: Opus 4.7 (1M context)_
