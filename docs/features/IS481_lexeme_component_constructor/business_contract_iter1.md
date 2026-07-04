# Business contract: IS481 Lexeme Component Constructor

Дельта к существующему wordcard / quiz-chat контракту. Mate refactor (Msg / Effect /
Reducer для translation+definition) — backlog (B4/C2).

## State

### `WordCardState` (`modules/screen/wordcard/.../mate/State.kt`)

Дельта — один плоский флаг:

```kotlin
data class WordCardState(
    /* ...existing fields... */
    val lexemeIdPendingDelete: Long? = null,
    val hasDefinitionComponent: Boolean = false,   // ← IS481 AGG-6
)
```

- **`hasDefinitionComponent`** — per-dictionary флаг наличия user-defined типа
  `name="Definition", system_key=NULL`. Управляет видимостью chip «Определение». Explicit
  field, заполняется один раз на load. Composable AND'ит с per-lexeme `canAddDefinition`.
- Инвариант: `true` ⇔ `term.dictionary.componentTypes` содержит запись `name="Definition",
  systemKey=null`. Проверяется в маппере.

### Domain `Lexeme` (`modules/domain/lexeme/.../Lexeme.kt`)

Расширение data class — `components` + shim (B4/C2):

```kotlin
data class Lexeme(
    val lexemeId: LexemeId,
    val components: List<ComponentValue>,                                 // ← IS481
    @Deprecated("Use components") val translation: Translation? = null,   // shim
    @Deprecated("Use components") val definition: Definition? = null,     // shim
    val addDate: Date,
    val changeDate: Date? = null,
)
```

[см. полный iter1 текст в git history — архивная копия для отслеживания эволюции после review iteration 1]

_model: claude-opus-4-7[1m]_
_archived from iter1, full text in git history_
