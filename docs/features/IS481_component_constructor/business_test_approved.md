# Approved findings — business_test.md, iteration 1

6 approved (5 critical + 1 minor). Плюс **F100 → backlog** (out-of-scope перенос).

## F099 [critical] — explicit план миграции existing тестов

50× ссылок на `ComponentValueData` в 6 test файлах через Tier 7 migration. Спека упоминает одной фразой.

**Что исправить в business_test.md:** добавить секцию «Существующие тесты — обновление при миграции» (отдельную в § Тестовые спеки или рядом с § Не покрываем):

```markdown
### Существующие тесты — миграция M12→M13

Tier 7 migration call-sites затрагивают существующие тесты. Они **обязаны** быть обновлены синхронно с domain rewrite.

**Файлы и счётчик `ComponentValueData` references:**

| Файл | Refs |
|---|---|
| `modules/screen/wordcard/src/test/.../WordCardUseCaseImplTest.kt` | 15 |
| `modules/domain/lexeme/src/test/.../LexemeMapperTest.kt` | 14 |
| `modules/screen/wordcard/src/test/.../mate/DatasourceEffectHandlerTest.kt` | 12 |
| `modules/screen/quizchat/src/test/.../QuizGameImplTest.kt` | 3 |
| `modules/screen/quizchat/src/test/.../QuizGameImplFetchDataTest.kt` | 3 |
| `modules/domain/lexeme/src/test/.../LexemeBuiltInExtTest.kt` | 3 |

**Pattern replacement (mechanical):**
- `ComponentValueData.TextValue(s)` → `TemplateValues.TextValues(Primitive.Text(s))`
- `import me.apomazkin.lexeme.ComponentValueData` → `import me.apomazkin.lexeme.TemplateValues; import me.apomazkin.lexeme.Primitive`

После replacement тесты должны компилироваться и проходить без изменения assert'ов (TextValues ≡ старому TextValue по контракту значения).
```

## F100 → backlog (не fix в spec, а перенос)

cardinality downgrade `is_multi=true→false` — в scope_analysis (F-N5a), но business_contract/DT не покрыли. Применяю `feedback_no_scope_expansion`:

1. **В business_test.md** добавить в § «Не покрываем» новый пункт:
   > `cardinality downgrade (`is_multi=true→false`) + edit component` — design (business_contract + business_design_tree) не покрывает edit-операции. **Перенесено в backlog как продолжение фичи (IS481 phase 2)**. Тесты на edit / downgrade будут добавлены в следующей итерации фичи.

2. **В docs/FlowBacklog.md** добавить `IS481cc-F6`:
   > **IS481cc-F6.** scope_analysis включил aspect, business_contract пропустил. Process gap между фазами scope_analysis → business_contract. Идея фикса: на business_contract шаге обязать sub-agent явно сверить с § Aspects из scope_analysis и зафиксировать в `business_contract.md § Не покрываем` каждый aspect который НЕ в контракт.

3. **В docs/Backlog.md** добавить:
   > **IS481 phase 2 — edit component + cardinality downgrade.** Сюда: `EditComponent` Msg/UseCase для existing user-defined типов + `DowngradeCheck` guard для `is_multi=true→false` (заблокировать если есть многосоставные ComponentValue).

## F101 [critical] — race-condition тесты Close-during-flight

**Что исправить в business_test.md:** добавить 3 race-condition сценария в § 3.2 / 3.3 / 3.4:

- 3.2.X: `given state.createDialog=null + state.isCreating=true (dialog closed while operation in flight), when Msg.CreateResult(SameScopeCollision), then state.isCreating=false, state.createDialog stays null, UiEffect.Snackbar(<i18n key>) emitted (snackbar fallback для error display).`
- 3.3.X: аналогично для Rename (renameDialog=null + isRenaming=true).
- 3.4.X: аналогично для Delete.

Контракт уточняется: при closed dialog Result → snackbar instead of dialog inline error.

## F102 [critical] — ConfirmDelete guard на isLoadingImpact

**Что исправить в business_test.md:** добавить в § 3.4:

- 3.4.X: `given deleteConfirm with isLoadingImpact=true, when Msg.ConfirmDelete, then NO DatasourceEffect.SoftDelete emitted (guard); state unchanged.`

Reducer impl invariant: ConfirmDelete пропускается если impact ещё грузится. Тест фиксирует.

## F103 [critical] — orphan prefs reset throws

**Что исправить в business_test.md:** добавить в § 1.5 (UseCase test softDeleteComponent):

- 1.5.6: `given DB soft-delete success + prefsProvider.setStringByRawKey throws, when softDeleteComponent invoked, then either:`
  - `(a) Returns DeleteOutcome.Success (prefs reset wrapped in try/catch best-effort, logged but not propagated), OR`
  - `(b) Returns DeleteOutcome.PartialSuccess (new outcome variant; DB committed, prefs left stale)`.

UseCase impl должен выбрать стратегию. Тест зафиксирует.

Рекомендация для implement: **(a)** — best-effort prefs reset с warning log; не вводить новый outcome variant.

## F106 [minor] — overwrite dialog → reset to empty

**Что исправить в business_test.md:** § 3.x уточнить:

- `given state.createDialog != null (already open), when Msg.OpenCreateDialog, then state.createDialog reset to default empty state: name="", template=Translation, scope=Global, nameError=null, isCreating=false. (overwrite policy — всегда reset).`

Invariant: повторное открытие диалога сбрасывает state.
