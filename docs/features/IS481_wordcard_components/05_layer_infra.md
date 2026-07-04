# Слой Infra — план изменений

> **Гайды:** `dagger-di.md` (FlowHandler через `@Inject`, mixed `@Assisted`+`@Inject`, `@Binds`), `viewmodel-wiring.md` (`effectHandlerSet`, factory), `theme-and-resources.md` (strings).

Объём малый: один новый FlowHandler регистрируется в `WordCardViewModel`, обновляются строки и `@Inject` граф автоматически разрешает зависимости. **`BlueAssistChip` НЕ мигрируется** (нет общего callsite'а — см. § 1).

---

## §1. BlueAssistChip — NO-OP

`BlueAssistChip` в `:modules:widget:component_widgets` — **read-only**, без onClick. Используется в 4 callsite'ах (`PerDictRowWidget.kt`, `UserDefinedRowWidget.kt`) как **индикатор**, не как actionable chip.

WordCard'у нужен **clickable** chip — реальный API из `SubentityChip` (`me.apomazkin.wordcard.widget.lexeme.SubentityChip`). Этот widget уже clickable, корректно стилизован, локальный для модуля wordcard.

**Решение:** `SubentityChip` остаётся в wordcard, переиспользуется в новом `ComponentChipsRow`. `BlueAssistChip` не трогаем, миграция в `:modules:core:ui` — backlog (низкий приоритет, реальной общности недостаточно).

`SubentityChip` получает overload `(label: String, ...)` — см. § 04 §1.

---

## §2. WordCardViewModel (MODIFY)

Текущая сигнатура (EXISTING):

```kotlin
class WordCardViewModel @AssistedInject constructor(
    @Assisted wordId: Long,
    @Assisted navigator: WordCardNavigator,
    @Assisted uiHost: UiHost,
    datasourceHandler: DatasourceEffectHandler,
    navHandlerFactory: WordCardNavigationEffectHandler.Factory,
    uiEffectHandlerFactory: UiEffectHandler.Factory,
) : ViewModel(), MateStateHolder<WordCardState, Msg> { … }
```

Изменение: добавить `availableTypesFlowHandler` в ctor (Dagger @Inject через регулярный параметр; не Assisted):

```kotlin
class WordCardViewModel @AssistedInject constructor(
    @Assisted wordId: Long,
    @Assisted navigator: WordCardNavigator,
    @Assisted uiHost: UiHost,
    datasourceHandler: DatasourceEffectHandler,
    navHandlerFactory: WordCardNavigationEffectHandler.Factory,
    uiEffectHandlerFactory: UiEffectHandler.Factory,
    availableTypesFlowHandler: AvailableComponentTypesFlowHandler,
) : ViewModel(), MateStateHolder<WordCardState, Msg> {

    private val stateHolder = Mate(
        initState = WordCardState(),
        initEffects = setOf(DatasourceEffect.LoadWord(wordId)),
        coroutineScope = viewModelScope,
        reducer = WordCardReducer(),
        effectHandlerSet = setOf(
            datasourceHandler,
            navHandlerFactory.create(navigator),
            uiEffectHandlerFactory.create(uiHost),
            availableTypesFlowHandler,
        )
    )

    override val state: StateFlow<WordCardState> get() = stateHolder.state
    override fun accept(message: Msg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(
            wordId: Long,
            navigator: WordCardNavigator,
            uiHost: UiHost,
        ): WordCardViewModel
    }
}
```

`Factory` сигнатура — БЕЗ изменений. `AssistedInject` поддерживает mixed @Assisted + auto-@Inject params (regular params вытаскиваются из Dagger-графа автоматически).

---

## §3. WordCardModule (NO-OP)

`app/src/main/java/me/apomazkin/polytrainer/di/module/wordCard/WordCardModule.kt` — Dagger module:

```kotlin
@Module
interface WordCardModule {
    @Binds
    fun bindWordCardUseCase(impl: WordCardUseCaseImpl): WordCardUseCase
}
```

Не меняется. `AvailableComponentTypesFlowHandler` имеет `@Inject constructor(...)` → Dagger найдёт автоматически без явного binding.

---

## §4. build.gradle.kts — NO-OP

`:modules:screen:wordcard/build.gradle.kts` — текущие deps:
- `:modules:core:di`, `:modules:core:mate`, `:modules:core:theme`, `:modules:core:ui`, `:modules:core:tools`, `:modules:core:logger`
- `:modules:domain:lexeme`
- `:core:core-resources`
- `:modules:widget:iconDropDowned`
- dagger / KSP

Все нужные deps уже подключены. **Не нужно добавлять `:modules:widget:component_widgets`** (BlueAssistChip не используется). **Не нужно добавлять `:core:core-db-api`** (UseCase в `app/`-слое).

---

## §5. CompositionRoot / AppComponent

`app/src/main/java/me/apomazkin/polytrainer/di/AppComponent.kt` — без изменений. WordCard subcomponent (если есть) либо `@Singleton` граф уже включает `WordCardModule` и `LexemeApi`. `AvailableComponentTypesFlowHandler` — автоматически.

Verify на этапе imp: grep `AppComponent` на упоминания `WordCardUseCase` — если binding явный, ничего не меняется.

---

## §6. Strings (cross-ref § 04 §6)

`core-resources/src/main/res/values/strings.xml` — добавить ТОЛЬКО новые:
- `word_card_error_load_component_types` («Couldn't load components»)
- `word_card_action_retry` («Retry»)
- `word_card_error_generic` («Couldn't save changes») — H-7: для `OperationFailed` (generic write-error). Без неё — compile-fail (план схлопнул per-op строки в одну).

Уже СУЩЕСТВУЮТ (НЕ добавлять, переиспользовать):
- `word_card_error_restore_lexeme` («Failed to restore value») — A17 retry-снек.
- `word_card_snackbar_lexeme_deleted`, `word_card_snackbar_undo` — cascade/full-delete undo.

---

## §7. Logger / Crashlytics

- `me.apomazkin.wordcard.LogTags.WORDCARD` — переиспользуется в `AvailableComponentTypesFlowHandler` и rewritten `WordCardUseCaseImpl`.
- `LexemeLogger` — без изменений. Errors на handler/usecase path логируются как `logger.e(tag = LogTags.WORDCARD, message = …)`.
- Crashlytics sink уже подключён глобально (`CrashlyticsSink`) — все `logger.e` уходят туда.

---

## §8. Acceptance Tier 4 (Infra)

- `./scripts/cc-build.sh :app:assembleDebug` — зелёный (полная сборка, проверка DI-графа).
- `./scripts/cc-build.sh :app:lintDebug` — зелёный.
- Манyally: WordCard открывается, `WordCardViewModel` создаётся без `IllegalStateException` от Dagger, FlowHandler стартует subscribe (no-op до `LoadAvailableComponentTypes`).

---

## §9. Риски

- **Dagger compile-fail при добавлении ctor-параметра.** Mitigation: `AvailableComponentTypesFlowHandler` — `@Inject constructor(useCase, logger)`; обе зависимости уже в графе. Risk → near-zero.
- **AssistedInject mixed params.** `AssistedInject` поддерживает `@Inject`-параметры между `@Assisted` — verified на existing pattern (`DatasourceEffectHandler` уже так wired).
- **`flowTypesForDictionary` упирается в Room thread.** Reactive Flow от Room — automatically dispatched on dao executor; `collectLatest` в `AvailableComponentTypesFlowHandler` использует `viewModelScope` (наследуется через `Mate`). Risk низкий, parity с другими FlowHandler'ами.
- **Strings ресурсы.** Если `R.string.word_card_error_load_component_types` не сгенерирован → compile-fail. Mitigation: добавляем `<string>` запись ПЕРЕД использованием в reducer.
