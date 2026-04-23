# Тема и ресурсы

## Система темы

Тема построена на **Material3 dark color scheme**. Точка входа — `AppTheme`:

```kotlin
// modules/core/theme/Theme.kt
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = LexemeColor.primary,
            onPrimary = LexemeColor.onPrimary,
            secondary = LexemeColor.secondary,
            // ...
        ),
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
```

## Цвета

Определены в `modules/core/theme/Color.kt`:

```kotlin
object LexemeColor {
    val primary = Color(0xFF4A49BC)        // Основной фиолетовый
    val onPrimary = Color(0xFFFFFFFF)      // Белый на primary
    val secondary = Color(0xFF19191B)      // Тёмно-серый
    val onSecondary = Color(0xFFF2F2F3)    // Светло-серый на secondary
    val tertiary = Color(0xFFF1E9FA)       // Светло-фиолетовый (позитив)
    val error = Color(0xFFFEE2E2)          // Фон ошибки
    val onError = Color(0xFFDE2424)        // Текст ошибки
    val background = Color(0xFFFFFFFF)     // Белый фон
    val surface = Color(0xFFFFFFFF)        // Белая поверхность
}
```

Дополнительные цвета вне Material3:

```kotlin
val whiteColor = Color(0xFFFFFFFF)
val blackColor = Color(0xFF000000)
val grayTextColor = Color(0xFF9E9E9E)
val disableButtonTitleColor = Color(0xFFBDBDBD)

// Статистика
val statLearnedFg = Color(...)
val statInProcessFg = Color(...)
val statNotStartedFg = Color(...)

// Градиенты
val gradientPrimary = Brush.horizontalGradient(listOf(Color(0xFF3170D7), Color(0xFF242792)))
```

### Как добавить новый цвет

1. Material3 цвет → добавить в `LexemeColor` + обновить `darkColorScheme()` в `Theme.kt`
2. Кастомный цвет → добавить как `val` в `Color.kt` вне объекта
3. Использовать: `import me.apomazkin.theme.myColor`

## Типографика

Определена в `modules/core/theme/LexemeStyle.kt`:

```kotlin
object LexemeStyle {
    val H1 = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold)
    val H2 = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)
    // ...
    val BodyXL = TextStyle(fontSize = 20.sp)
    val BodyL = TextStyle(fontSize = 16.sp)
    val BodyM = TextStyle(fontSize = 14.sp)
    val BodyS = TextStyle(fontSize = 12.sp)
    val BodyMBold = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
}
```

Использование:

```kotlin
Text(text = "Заголовок", style = LexemeStyle.H2)
Text(text = "Обычный текст", style = LexemeStyle.BodyM)
// Или через Material3:
Text(text = "Текст", style = MaterialTheme.typography.bodyMedium)
```

## ResourceManager

Обёртка над Android-ресурсами для DI и тестирования:

```kotlin
// modules/core/ui/resource/ResourceManager.kt
interface ResourceManager {
    fun stringByResId(@StringRes id: Int): String
    fun stringByResId(@StringRes id: Int, value: String): String  // %s подстановка
    fun stringByArrayId(@ArrayRes id: Int): String                // случайный из массива
}
```

Реализация в app-модуле:

```kotlin
class ResourceManagerImpl @Inject constructor(
    private val ctx: Context,
) : ResourceManager {
    override fun stringByResId(id: Int) = ctx.resources.getString(id)
    override fun stringByResId(id: Int, value: String) = ctx.resources.getString(id, value)
    override fun stringByArrayId(id: Int) = ctx.resources.getStringArray(id).random()
}
```

Используется в Reducer для получения строк:

```kotlin
class ChatReducer(
    private val resourceManager: ResourceManager,
) : MateReducer<...> {
    private fun welcomeMessage(): String {
        return resourceManager.stringByResId(R.string.chat_quiz_msg_system_welcome)
    }
}
```

## Организация строковых ресурсов

- Общие строки → `core/core-resources/src/main/res/values/strings.xml`
- Специфичные для фичи → `strings.xml` в модуле фичи
- Массивы (случайные ответы квиза) → `<string-array>` в `strings.xml`
- Локализация: `values-ru-rRU/` для русского

Именование: `<раздел>_<описание>`:
```xml
<string name="vocabulary_empty_title">No words yet</string>
<string name="chat_quiz_msg_system_welcome">Welcome to quiz!</string>
<string name="word_card_lexeme_order_title">Lexeme %s</string>
```

## Иконки

Все SVG-иконки как XML vectors в `core/core-resources/src/main/res/drawable/`:

```
ic_add.xml, ic_delete.xml, ic_edit.xml, ic_close.xml
ic_tab_vocabulary.xml, ic_tab_training.xml, ic_tab_stats.xml
ic_quiz_write.xml, ic_send.xml, ic_back.xml
```

Использование:

```kotlin
Icon(
    painter = painterResource(id = R.drawable.ic_add),
    contentDescription = "Add",
    tint = MaterialTheme.colorScheme.primary,
)
```

## Размеры (dimens.xml)

```xml
<dimen name="d0">0dp</dimen>
<dimen name="d4">4dp</dimen>
<dimen name="d8">8dp</dimen>
<dimen name="d16">16dp</dimen>
<dimen name="d24">24dp</dimen>
<dimen name="d32">32dp</dimen>
<dimen name="d64">64dp</dimen>
```

## Preview аннотации

```kotlin
// Для виджетов: два языка, с фоном
@PreviewWidget
@Composable
private fun Preview() {
    AppTheme { MyWidget() }
}

// Для экранов: system UI, Pixel 3, два языка
@PreviewScreen
@Composable
private fun Preview() {
    AppTheme { MyScreen(state = MyState()) {} }
}

// Для параметризованных превью
@PreviewWidget
@Composable
private fun Preview(@PreviewParameter(BoolParam::class) enabled: Boolean) {
    AppTheme { MyButton(enabled = enabled) }
}
```
