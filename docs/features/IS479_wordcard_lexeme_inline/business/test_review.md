## Итерация 1 (2026-05-19T22:50:00-0600)

architect: 0 critical + 5 minor. qa_engineer: 0 critical + 12 minor.

Все findings — расширение покрытия и точечные ремарки. Conductor decision: accept ит.1 как done без ит.2. Tech debt — закрыть в `implement` по факту проявления.

## Tech debt (closed as accepted)

### Handler-зеркало (architect F1-F2)

- DatasourceEffectHandler покрывает только Translation/RemoveWord/LoadWord.
- Не покрыто: UpdateLexemeDefinition success/failure/null, RemoveDefinition cascade/non-cascade/null, UpdateWord success/failure, RemoveLexeme success/null.
- **Рекомендация для `implement`:** добавить симметричные handler-тесты при работе над `DatasourceEffectHandler.kt` (узел #3).

### closeAllEditModes coverage (architect F3-F5)

- `OpenLexemeMenu(isShow=true)`, `CreateLexeme`, `CreateTranslation`, `CreateDefinition` — entry-points для closeAllEditModes по контракту, но тесты проверяют только основной эффект ветки, не закрытие конкурирующих edit.
- **Рекомендация:** добавить эти проверки в TranslationManagementTest/DefinitionManagementTest/LexemeManagementTest.

### Точечные QA-замечания (qa_engineer)

- `RemoveLexeme for real id` — добавить assertion что лексема ещё в списке до handler-ответа (pessimistic-pattern).
- `branch 1 pessimistic remove` — явный комментарий что origin не nullify, ждёт RefreshTranslation(null).
- `OpenTopBarMenu idempotent` — false-positive устойчив, нужна `assertEquals(initial, result.state())`.
- `RefreshTranslation NOT_IN_DB` — добавить кейс `translation = null` для NOT_IN_DB replacement (defensive).
- `branch 1 pessimistic remove CommitTranslationEdit` — добавить тест с `edited = "   "` (whitespace, не empty) — проверка `isBlank()` vs `isEmpty()`.
- `EnterWordEditMode closes active lexeme edit` — добавить `assertNoEffects()`.
- `WordLoadedTest` multiple lexemes — `assertEquals(1L, list[0].id)`.
- DatasourceEffectHandler null-result tests — assert конкретный текст snackbar.
- `CancelDefinitionEdit on existing definition` — отсутствует (есть только cancel на freshly-created chip).
- `Refresh*` для не-найденного lexemeId — defensive regression тест отсутствует.
- `EnterTranslationEditMode` — добавить closure активной translation-edit на ДРУГОЙ лексеме (только conflict definition покрыт).

### Не покрыто (objectively out-of-scope тестового модуля)

- UseCase contract сценарии 16-18 (`addLexemeTranslation` atomicity, cascade/non-cascade в impl) — требуют DI-mock'ов (MockK/Mockito), не подключённых в `:modules:screen:wordcard`. Покрытие — через integration-тест на `:app` модуле либо вручную на implement-этапе.

---

## Conductor decision

ит.1 закрыт. Все findings — minor, накопительные. К `implement` идём с текущим покрытием (16 сценариев из 18). Tech debt list — checklist для `implement` фазы (если test упадёт по этой причине → добавить).
