# IS527 | Анализ: где и как ложится захват фото и список слов

Факты из кода сняты 2026-10-07 (master `3f39b368`). Пути — от корня
проекта. Решения и допущения — в `brief.md`; здесь — что именно придётся
трогать, как это стыкуется с существующим, и что рискованно.

## 1. Точки касания

Для каждого слоя: что есть сейчас → что нужно.

1. **Контракт вкладок.**
   - Есть: `modules/screen/vocabulary/.../TabContract.kt:29`
     `data class FabSpec(iconRes, visible: () -> Boolean, onClick)`;
     `TabSpec.fab: FabSpec?` (:64) — один FAB на вкладку.
   - Нужно: второй, маленький FAB над основным — либо
     `FabSpec.secondary: FabSpec?`, либо `TabSpec.fabs: List<FabSpec>`.
     Контракт стабильности (:15-20: TabSpec создаётся один раз,
     изменчивое — через лямбды) сохраняется.
2. **Хост.**
   - Есть: `VocabularyHostScreen.kt:128-142`, слот `floatingActionButton`
     = `AnimatedVisibility(scaleIn/scaleOut)` → `PrimaryFabWidget`.
   - Нужно: `Column` из двух кнопок — сверху маленькая (камера), снизу
     основная; обе под одной `AnimatedVisibility`, прячутся вместе (Д9).
3. **Сборка спеков.**
   - Есть: `app/.../uiDeps/CompositionRootImpl.kt:87-91`,
     `FabSpec(iconRes = words.fabIconRes, visible = { words.isFabVisible.value }, onClick = words::onFabClick)`.
   - Нужно: второй спек из `WordsTabHandle` (`onPhotoFabClick`); у
     GROUPS (:101) второго нет.
4. **Handle вкладки.**
   - Есть: `WordsTabScreen.kt:65` `WordsTabHandle` с `isFabVisible`,
     `fabIconRes`, `onFabClick`.
   - Нужно: `photoFabIconRes`, `onPhotoFabClick()` →
     `Msg.OpenPhotoSourceChooser`.
5. **ui-kit.**
   - Есть: `modules/core/ui/.../btn/base/LexemeFab.kt:31` — Surface
     56 dp; маленького FAB нет.
   - Нужно: `SmallFabWidget` 40 dp в том же стиле (`RoundedCornerShape(16)`),
     цвета парой из прецедента `PrimaryFabWidget`.
6. **Иконки.**
   - Есть: в `core/core-resources/.../drawable` только `ic_add*`.
   - Нужно: `ic_photo_camera.xml` (Material Icons, Apache 2.0).
7. **Манифест.**
   - Есть: `app/src/main/AndroidManifest.xml` без провайдера и без
     `<queries>`.
   - Нужно: `<provider android:name="androidx.core.content.FileProvider" android:authorities="${applicationId}.fileprovider">`
     с `res/xml/file_paths.xml` (`<cache-path name="photos" path="photos/"/>`);
     `<queries><intent><action android:name="android.media.action.IMAGE_CAPTURE"/></intent></queries>`,
     чтобы проверять наличие камеры. `${applicationId}` даёт суффикс
     `.dev` сам.
8. **Зависимости.**
   - Есть: каталоги `deps/*.versions.toml`; activity-compose 1.10.1, в
     нём уже есть `PickVisualMedia` и `TakePicture`.
   - Нужно: `mlkitTextRecognition = "16.0.1"` в каталоге, подключение
     только в новом модуле.
9. **CI.**
   - Есть: `.github/workflows/on_pull_request.yml:69` — список модулей
     для `connectedDebugAndroidTest`.
   - Нужно: добавить `:modules:library:textrecognition:connectedDebugAndroidTest`.

## 2. Новый модуль `:modules:library:textrecognition`

По образцу `:modules:library:flags` (библиотека с интерфейсом, androidTest,
`testInstrumentationRunner`):

```kotlin
// api
data class RecognizedText(val lines: List<String>, val elements: List<String>)

interface TextRecognizer {
    suspend fun recognize(uri: Uri): RecognizedText
}

// impl
class MlKitTextRecognizer(
    private val context: Context,
    private val maxSide: Int = 2048,
) : TextRecognizer {
    private val client by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    override suspend fun recognize(uri: Uri): RecognizedText = withContext(Dispatchers.Default) {
        val image = loadScaled(uri)             // inSampleSize до maxSide + поворот из EXIF
        val text = client.process(image).await() // kotlinx-coroutines-play-services
        RecognizedText(
            lines = text.textBlocks.flatMap { b -> b.lines.map { it.text } },
            elements = text.textBlocks.flatMap { b -> b.lines.flatMap { l -> l.elements.map { it.text } } },
        )
    }
}
```

- `await()` — из `kotlinx-coroutines-play-services` (Task → suspend);
  без него — `suspendCancellableCoroutine` с `addOnSuccessListener`.
- `InputImage.fromFilePath(context, uri)` сам читает EXIF, но декодирует
  полный размер; для фото 48 МП это ~190 МБ ARGB. Поэтому Д6: свой декод
  с `inSampleSize` и `ExifInterface` (`androidx.exifinterface`) → `fromBitmap(bitmap, rotation)`.
- androidTest: `Bitmap.createBitmap(800, 200)` + `Canvas.drawText("hello world", …)`
  → сохранить в `cacheDir` → `recognize(Uri.fromFile)` → `elements` содержит
  `hello` и `world`. Без бинарных ассетов в репозитории. На эмуляторе CI
  (`google_apis`, API 34) bundled-модель работает без Play-сервисов.

## 3. Экран: состояние, сообщения, эффекты (wordstab, mate)

Состояние — явные флаги, отдельное подсостояние по образцу
`AddWordDialogState` (`logic/State.kt:47`):

```kotlin
@Immutable
data class PhotoWordsState(
    val isOpen: Boolean = false,
    val isChoosingSource: Boolean = false,   // шаг 1: «Камера» / «Галерея»
    val isRecognizing: Boolean = false,      // шаг 2: лоадер
    val words: List<String> = emptyList(),   // шаг 3: список
    val errorRes: Int? = null,               // ошибка ML Kit / камеры
)
```

Сообщения (в `logic/Message.kt`, порядок — по важности), для каждого:
кто шлёт → что делает reduce.

- `OpenPhotoSourceChooser` — маленький FAB → `isOpen = true`,
  `isChoosingSource = true`.
- `PhotoPicked(uri)` — UI после контракта, камера или галерея вернули
  Uri → `isChoosingSource = false`, `isRecognizing = true`, эффект
  `RecognizeWords(uri)`.
- `PhotoPickCancelled` — UI, контракт вернул `null` или `false` → назад
  к шагу 1, `isChoosingSource = true`.
- `WordsRecognized(words)` — хендлер эффекта → `isRecognizing = false`,
  `words = …`.
- `RecognitionFailed(errorRes)` — хендлер → `isRecognizing = false`,
  `errorRes = …`.
- `ClosePhotoWords` — «Отмена», «Закрыть», back, тап вне →
  `PhotoWordsState()`.

Эффект `DatasourceEffect.RecognizeWords(uri)` → `DatasourceEffectHandler`
(`logic/DatasourceEffectHandler.kt:41`, `withContext(io)`) → `useCase.recognizeWords(uri)`
→ `Msg.WordsRecognized` / `Msg.RecognitionFailed`. В `WordsTabUseCase`
(`deps/WordsTabUseCase.kt`) — `suspend fun recognizeWords(uri: Uri): List<String>`;
реализация в `app/.../di/module/wordstab/WordsTabUseCaseImpl.kt` вызывает
`TextRecognizer` и `extractWords`.

Извлечение слов — чистая функция в `wordstab` (unit-тесты):

```kotlin
fun extractWords(elements: List<String>): List<String> = elements
    .asSequence()
    .map { it.trim { c -> !c.isLetterOrDigit() && c != '\'' && c != '-' }.lowercase() }
    .filter { token -> token.length >= 2 && token.any(Char::isLetter) }
    .distinct()
    .toList()
```

Запуск контрактов — в UI, как в `settingstab/.../ImportDataWidget.kt:15`
(launcher → колбэк → Msg). Для камеры Uri нужен до запуска: Composable
создаёт файл `cacheDir/photos/<uuid>.jpg` и Uri через
`FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)`;
`TakePicture` возвращает `Boolean`, Uri уже известен. Хелпер
`rememberPhotoPicker(onPicked: (Uri) -> Unit, onCancelled: () -> Unit)`
в `wordstab/ui/photo/` держит оба launcher'а.

Диалог `PhotoWordsDialog` (`wordstab/ui/widget/`), по образцу
`LanguagePickerDialog` (голый `Dialog` → `Surface(formBackground, RoundedCornerShape(16))`,
`fillMaxHeight(0.8f)` для списка), цвета явные парами из прецедента:
`formBackground` / `formTextTertiary`, кнопки — как в `AlarmDialogWidget`,
лоадер — `CircularProgressIndicator(color = LexemeColor.primary)`.

Подключение в `WordsTabContent` (`WordsTabScreen.kt:149`, `Box`) рядом с
`ConfirmDeleteWordWidget` (:194): `if (state.photoWordsState.isOpen) PhotoWordsDialog(...)`.

## 4. Тесты

- **unit, `extractWords`** — пунктуация по краям, апостроф и дефис
  внутри, числа, одиночные буквы, регистр, дубли, порядок. Файл
  `wordstab/src/test/.../logic/ExtractWordsTest.kt`.
- **unit, редьюсер** — все шесть Msg, неизменность остальных полей,
  эффект `RecognizeWords` ровно при `PhotoPicked`. Файлы
  `WordsTabReducerKtTest.kt` (образец :115-150) и `PhotoWordsExtTest.kt`.
- **androidTest** — ML Kit читает нарисованный текст; пустая картинка →
  пустой результат. Папка `textrecognition/src/androidTest/`.
- **ручные** — кейсы из брифа на девайсе `.dev`, в чате.

Compose UI-тест диалога — по образцу `quiz/chat` (`createComposeRule`),
если останется время; не обязателен.

## 5. Риски и что проверить

- **Память.** Полноразмерный декод фото с современной камеры — OOM на
  слабых устройствах. Закрыто Д6 (`inSampleSize`). Проверить на реальном
  фото с девайса юзера.
- **Поворот.** Фото с камеры часто хранится повёрнутым с EXIF-меткой; без
  учёта EXIF ML Kit читает «боком» и отдаёт мусор. Закрыто
  `ExifInterface`; кейс «альбомный кадр» в ручниках.
- **Пересоздание Activity.** Системная камера может убить наш процесс
  (мало памяти); тогда результат контракта приходит в новую Activity, а
  state mate — свежий, `PhotoPicked` придёт в закрытый диалог. Первая
  версия: редьюсер принимает `PhotoPicked` и при `isOpen = false`
  (открывает диалог сразу на шаге 2). Uri камеры хранить в
  `rememberSaveable`.
- **Нет камеры** (эмулятор без камеры, планшет): `resolveActivity` с
  `<queries>` → показать ошибку в диалоге, не крэш.
- **Размер APK.** +≈4 МБ на архитектуру (bundled). Если неприемлемо —
  unbundled с `meta-data DEPENDENCIES=ocr` (Д3 брифа, отменяется словом).
- **Кнопка видна при любом словаре**, включая нелатинский (ограничение
  Д3). Гейт по письменности — `ULocale.addLikelySubtags(…).script` —
  отдельным шагом позже.
- **Контракт `TabSpec` меняется** — затрагивает `GROUPS` (второго FAB нет,
  `secondary = null`) и сценарный харнес `PilotRegistry` только если он
  собирает `TabSpec` (проверить при плане).

## 6. Порядок реализации (для плана триадами)

1. Библиотека `textrecognition` (интерфейс, ML Kit, androidTest, CI) — ни
   от чего не зависит.
2. `extractWords` + unit-тесты — чистый Kotlin.
3. Состояние/Msg/Effect/редьюсер + тесты редьюсера.
4. Use case (интерфейс в `wordstab/deps`, реализация в `app`).
5. UI: иконка, `SmallFabWidget`, второй FAB в контракте и хосте,
   `rememberPhotoPicker`, `PhotoWordsDialog`, манифест (`FileProvider`,
   `<queries>`), строки ru/en.
6. Сборка, установка `.dev`, ручники.
