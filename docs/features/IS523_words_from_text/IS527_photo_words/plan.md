# IS527 | План: захват фото и список слов

Что буду делать, что вызывать, какие зависимости. Без кода — код-триады
появятся здесь же отдельным разделом после подтверждения плана. Порядок:
сначала независимое, потом то, что на него опирается. Решения и допущения
— в `brief.md`, точки касания в коде — в `analysis.md`.

## Что умеет ML Kit Text Recognition (чтобы не ждать от него лишнего)

- Своих запросов нет: это распознаватель символов, не нейронка с
  промптом. Вход — картинка, выход — текст с координатами. Словарь,
  список разрешённых слов, подсказку «ищи испанский» передать нельзя.
- Язык задавать не нужно и некуда. Выбирается только письменность через
  класс опций: латиница по умолчанию; китайская, деванагари, японская,
  корейская — отдельные артефакты. Английский и испанский — одна латинская
  модель, диакритику читает сама.
- Отдаёт метаданные, по которым фильтруем мы: у блока, строки и слова —
  `recognizedLanguage` (код языка или «und»); у строки, слова и символа —
  `confidence` 0…1; рамка и угол наклона.
- Область распознавания ограничивается только обрезкой картинки до
  вызова.
- Качество входа важнее фильтров: косой кадр с тенью читается хуже
  ровного. У Google есть отдельный ML Kit Document Scanner — готовый экран
  съёмки с обрезкой по краям листа и выравниванием, без разрешений, через
  Play-сервисы, в бете. Не сейчас; первый кандидат, если качество с
  системной камеры не устроит.

## Этап 1. Модуль распознавания `:modules:library:textrecognition`

Ни от чего не зависит. Образец — `:modules:library:flags`.

1. Регистрация модуля в `settings.gradle.kts`; `build.gradle.kts`:
   Android-библиотека, namespace `me.apomazkin.textrecognition`, minSdk из
   каталога, `testInstrumentationRunner = androidx.test.runner.AndroidJUnitRunner`.
2. Зависимости в каталог `deps`:
   - `com.google.mlkit:text-recognition:16.0.1` — bundled, латиница
     (Д3 брифа: +≈4 МБ на архитектуру, работает без Play-сервисов);
   - `org.jetbrains.kotlinx:kotlinx-coroutines-play-services` той же
     версии, что coroutines (1.10.1) — `Task.await()`;
   - `androidx.exifinterface:exifinterface` — поворот из EXIF;
   - coroutines-core, androidx core-ktx (уже есть в каталоге).
3. Интерфейс `TextRecognizer`: один suspend-метод «Uri → `RecognizedText`».
   `RecognizedText` — строки (текст) и слова (`RecognizedWord`: текст,
   уверенность, язык или null).
4. Реализация `MlKitTextRecognizer(context)`:
   - читаю размеры картинки без декода (`inJustDecodeBounds`), подбираю
     `inSampleSize`, чтобы картинка влезла в бюджет 12 Мп (Д6, уточнено
     ревью);
   - декодирую через `contentResolver.openInputStream`;
   - поворот — из EXIF того же потока (`ExifInterface`), перевожу в
     градусы 0/90/180/270;
   - `InputImage.fromBitmap(bitmap, rotation)` →
     `TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)` →
     `process(image).await()` на `Dispatchers.Default`;
   - результат `Text` разворачиваю: блоки → строки → элементы; собираю
     `RecognizedText`;
   - клиент ML Kit создаю один раз (`lazy`), не закрываю: живёт с
     приложением.
5. AndroidTest `MlKitTextRecognizerTest`:
   - рисую «hello world» на `Bitmap` через `Canvas.drawText`, сохраняю
     JPEG в `cacheDir`, распознаю по `Uri.fromFile`, проверяю, что среди
     слов есть `hello` и `world`;
   - пустой белый `Bitmap` → пустой результат;
   - без бинарных ассетов в репозитории.
6. CI: добавить `:modules:library:textrecognition:connectedDebugAndroidTest`
   в `.github/workflows/on_pull_request.yml:69`.
7. Контрольная точка: `./scripts/cc-build.sh :modules:library:textrecognition:assembleDebug`
   и `…:connectedDebugAndroidTest` на девайсе юзера.

## Этап 2. Извлечение слов — чистый Kotlin в `wordstab`

Зависит только от типа `RecognizedText` (этап 1).

1. Функция `extractWords(words: List<RecognizedWord>, rules): List<String>`
   по Д4 брифа:
   - снять с краёв всё, что не буква, не цифра, не апостроф и не дефис;
   - выбросить токены без букв и короче двух символов;
   - нижний регистр;
   - дубли схлопнуть, порядок — первое появление.
2. Два порога в `rules`, по умолчанию выключены:
   - минимальная уверенность (`minConfidence = 0f`);
   - язык слова должен совпадать с языком словаря (`language = null`).
   Включаем после первых ручников по логам.
3. Unit-тесты `ExtractWordsTest`: по кейсу на каждое правило, плюс
   пороги.

## Этап 3. Состояние, сообщения, эффект, редьюсер в `wordstab`

Зависит от этапа 2 (функция) и типа Uri; UI не трогает.

1. Подсостояние `PhotoWordsState` в `logic/State.kt` с явными флагами:
   `isOpen`, `isChoosingSource`, `isRecognizing`, `words`, `errorRes`.
   Поле в `WordsTabState`. Расширения состояния: открыть выбор, начать
   распознавание, показать слова, показать ошибку, закрыть.
2. Шесть сообщений в `logic/Message.kt` (по важности): открыть выбор
   источника; фото получено (Uri); выбор отменён; слова распознаны;
   распознавание упало; закрыть.
3. Эффект `DatasourceEffect.RecognizeWords(uri)`; ветка в
   `DatasourceEffectHandler`: вызов use case → `extractWords` → полный
   текст по строкам в `logger.d` (сверка с фото на ручниках) → сообщение
   «слова распознаны» или «упало» с ресурсом текста ошибки.
4. Редьюсер: ветки на шесть сообщений. «Фото получено» принимается и при
   закрытом диалоге — на случай, если система убила процесс, пока была
   открыта камера.
5. Тесты: `PhotoWordsExtTest` (расширения состояния, неизменность
   остальных полей) и ветки в `WordsTabReducerKtTest` (эффект
   `RecognizeWords` ровно при «фото получено»).

## Этап 4. Use case

Зависит от этапов 1 и 3.

1. В `deps/WordsTabUseCase` — `suspend fun recognizeText(uri): RecognizedText`.
2. Реализация в `app/.../di/module/wordstab/WordsTabUseCaseImpl.kt`:
   вызывает `TextRecognizer`; если Uri наш (authority FileProvider) —
   удаляет временный файл после распознавания через `Context` из графа.
3. Новый `TextRecognizerModule` (по образцу `CountryProviderModule`):
   провайдер `TextRecognizer` → `MlKitTextRecognizer(context)`, включается
   в `AppModule`. `app` получает зависимость на новый модуль.
4. Сценарный харнес `app/src/test/.../scenario/PilotRegistry.kt` получает
   юзкейс моком извне — правки не нужны.

## Этап 5. Интерфейс

Зависит от этапов 3 и 4.

1. Иконка `ic_photo_camera.xml` в `core/core-resources` (Material Icons).
2. `SmallFabWidget` 40 dp в `modules/core/ui/btn` — тот же стиль, что
   `LexemeFab` (скругление 16), цвета парой из прецедента
   `PrimaryFabWidget`.
3. Контракт вкладок `TabContract.kt`: у `TabSpec` — необязательный
   `secondaryFab: SecondaryFabSpec?` (иконка, подпись TalkBack, клик;
   своей видимости нет). Хост `VocabularyHostScreen`: в слоте FAB —
   колонка из двух кнопок (маленькая сверху, основная снизу) под одной
   `AnimatedVisibility`, прячутся вместе (Д9). `CompositionRootImpl`:
   второй спек из `WordsTabHandle`, пока только в dev-сборке (открытый
   вопрос в триаде 5.6). У GROUPS поле не задаётся.
4. Хелпер `rememberPhotoPicker` в `wordstab/ui/photo`:
   - launcher `TakePicture`: заранее файл `cacheDir/photos/photo_<время>.jpg`
     и Uri через `FileProvider.getUriForFile(context, "${packageName}.fileprovider", file)`;
     Uri — в `rememberSaveable`, чтобы пережить пересоздание;
   - launcher `PickVisualMedia` с фильтром «только картинки»;
   - наличие камеры проверяется через `resolveActivity` intent
     `IMAGE_CAPTURE` (нужен `<queries>`); нет камеры — кнопки «Камера» в
     диалоге нет; запуск обёрнут в try/catch на случай исчезновения камеры;
   - колбэки «получено Uri» / «отменено» → сообщения из этапа 3.
5. Диалог `PhotoWordsDialog` в `wordstab/ui/widget` по образцу
   `LanguagePickerDialog` (голый `Dialog`, `Surface` цвета
   `formBackground`, скругление 16, текст `formTextTertiary`, акцент
   `LexemeColor.primary`), три шага:
   - выбор источника: «Камера», «Галерея», «Отмена»;
   - распознавание: лоадер и «Отмена»;
   - список: заголовок с числом слов, `LazyColumn`, «Закрыть»; пусто —
     «Слов не найдено»; ошибка — текст ошибки.
   Закрытие тапом вне и кнопкой «назад» — через `onDismissRequest`.
6. Подключение диалога в `WordsTabContent` рядом с
   `ConfirmDeleteWordWidget`.
7. Манифест `app`: `<provider>` FileProvider с `${applicationId}.fileprovider`
   и `res/xml/file_paths.xml` (`cache-path photos/`); `<queries>` на
   `android.media.action.IMAGE_CAPTURE`. Разрешений не добавляется.
8. Строки ru и en в `core-resources`: заголовки шагов, «Камера»,
   «Галерея», «Отмена», «Закрыть», «Распознаю…», «Слов не найдено»,
   «Камера недоступна», «Не удалось распознать».

## Этап 6. Проверка и сдача

1. `./scripts/cc-build.sh assembleDebug`, `:app:lintDebug`,
   `testDebugUnitTest` по модулям по одному.
2. `installDebug` + запуск `co.lexeme.app.dev` на девайсе; перед любым
   adb-вводом — проверка, что в фокусе `.dev`.
3. Ручники из брифа по одному в чате: фото страницы с камеры; скриншот
   из галереи; фото без текста; отмена на каждом шаге; альбомный кадр;
   длинный текст с прокруткой; два запуска подряд. После каждого —
   сверка лога распознанного текста с фото.
4. Превью коммита → коммит (явные пути, доки IS527 и IS523 едут в нём)
   → PR в master → четыре зелёные проверки. Релиз — по слову юзера.

## Не в этом плане

Нейронка и бэкенд, значения слов, добавление, пометка «уже есть»,
лемматизация, гейт по письменности, Document Scanner, правка
распознанного текста, Compose UI-тест диалога (по остатку времени).

---

# Триады: код было / код будет / мотивация

Код «было» — из файлов master `3f39b368` (снят 2026-10-07). Разделы в
порядке выполнения. Стиль переносов в «будет» повторяет стиль файла, в
который код попадает (в `State.kt` — авторская лесенка с восемью
пробелами).

Факты, проверенные перед написанием: ML Kit `text-recognition` 16.0.1 —
последняя версия на Google Maven; у `Text.Element` есть `getConfidence()`
(в unbundled-варианте всегда 0 — ещё один довод за bundled) и
`getRecognizedLanguage()` (BCP-47 или «und»); `kotlinx-coroutines-play-services`
1.10.1 есть на Maven Central; `androidx.exifinterface` последняя 1.4.2;
`PickVisualMedia` без разрешений, на устройствах без фотопикера
откатывается на `ACTION_GET_CONTENT`.

Отклонения от текста этапов, найденные при чтении кода (этапы выше —
описание намерения, триады — источник истины):

- **Uri в сообщениях и эффекте — строкой.** В unit-тестах `wordstab`
  Android-классы заглушены, `Uri.parse` бросает «not mocked», mockk в
  модуль не подключён. Сообщение `PhotoPicked` и эффект `RecognizeWords`
  несут `uri: String`; в `Uri` строка превращается в исполнителе эффекта,
  он работает только на устройстве. Прецедент `settingstab` с `Uri` в
  сообщении тестами не покрыт — потому и прошёл.
- **Второй FAB — отдельный тип `SecondaryFabSpec` в `TabSpec.secondaryFab`**
  (этап 5.3 говорил про `FabSpec.secondary`): у вторичной кнопки нет
  своей видимости, поле `visible`, которое host не читает, не нужно.
- **Провайдер `TextRecognizer` — отдельный `TextRecognizerModule`**
  (этап 4.3 говорил про `WordsTabModule`): тот — `interface` с `@Binds`,
  провайдер с `Context` по образцу `CountryProviderModule` — класс.
- **Провал распознавания — `RecoverableEffect`, не try/catch** (этап 3.3
  говорил про try/catch): правило проекта — хендлеры ошибки не ловят,
  провал объявлен в типе эффекта, стектрейс пишет `ErrorLoggingObserver`
  (прецеденты: quiztab, groupstab, per_dictionary_components).
- **Уменьшение картинки — бюджетом пикселей (12 Мп), не стороной 2048 px**
  (Д6 брифа, моё допущение, уточнено): сторона 2048 с `inSampleSize`
  степенью двойки пережимала обычный скриншот 1080×2400 до 540×1200 и
  убивала мелкий шрифт; бюджет пикселей оставляет скриншоты и фото до
  12 Мп как есть, фото 48 Мп сжимает вчетверо.

**Ревью плана 2026-10-07, три агента** (API Android/ML Kit; стыковка с
кодом и тестами; бриф и края). Критичных находок нет. Внесено: защита от
устаревшего результата (`requestId`), `RecoverableEffect` + наблюдатель
ошибок в сборке вкладки, бюджет пикселей вместо стороны, обрезка краёв
слова только по «не буква и не цифра», удаление файла камеры в юзкейсе
через `Context`, ошибка вместо «Слов не найдено» при недекодируемой
картинке, `rotationDegrees` вместо ручного `when`, `recycle()` после
завершения задачи, try/catch вокруг запуска камеры, кнопка «Камера»
скрыта без приложения камеры, отмена после смерти процесса не открывает
диалог, подпись TalkBack у малого FAB, `assertEffects` вместо `contains`,
ширина строк ≤ 100, исправления текста. Открыт один вопрос к юзеру —
кнопка в магазинной сборке (5.6). Один из ревьюеров признался, что
дважды нарушил запрет инструментов (`head`, `sed` в пайпе чтения); файлы
не менялись.

## 1. Модуль `:modules:library:textrecognition`

### 1.1. Регистрация модуля

`settings.gradle.kts`

- **Было** (строка 59):

```kotlin
include(":modules:library:flags")
```

- **Будет:**

```kotlin
include(":modules:library:flags")
include(":modules:library:textrecognition")
```

- **Мотивация:** ML Kit живёт в отдельной библиотеке за интерфейсом, как
  `flags`: экран зависит от интерфейса, androidTest модуля гоняется
  отдельно в CI.

### 1.2. Каталоги зависимостей

`deps/other-libs.versions.toml`

- **Было:**

```toml
[versions]
flagLibVersion = "v1.5.4-alpha-1"
mateVersion = "v0.1.6"

[libraries]
flagLib = { group = "com.github.blongho", name = "worldCountryData", version.ref = "flagLibVersion" }
```

- **Будет:**

```toml
[versions]
flagLibVersion = "v1.5.4-alpha-1"
mateVersion = "v0.1.6"
mlkitTextRecognitionVersion = "16.0.1"

[libraries]
flagLib = { group = "com.github.blongho", name = "worldCountryData", version.ref = "flagLibVersion" }
mlkitTextRecognition = { group = "com.google.mlkit", name = "text-recognition", version.ref = "mlkitTextRecognitionVersion" }
```

`deps/android.versions.toml`

- **Было:**

```toml
[versions]
coreKtxVersion = "1.16.0"
materialVersion = "1.12.0"
activityKtxVersion = "1.10.1"
splashscreenVersion = "1.0.1"

[libraries]
coreKtx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtxVersion" }
```

- **Будет:**

```toml
[versions]
coreKtxVersion = "1.16.0"
materialVersion = "1.12.0"
activityKtxVersion = "1.10.1"
splashscreenVersion = "1.0.1"
exifInterfaceVersion = "1.4.2"

[libraries]
coreKtx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtxVersion" }
exifInterface = { group = "androidx.exifinterface", name = "exifinterface", version.ref = "exifInterfaceVersion" }
```

`deps/kotlin.versions.toml`

- **Было:**

```toml
[libraries]
coroutinesCore = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutinesVersion" }
coroutinesTest = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesVersion" }
```

- **Будет:**

```toml
[libraries]
coroutinesCore = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutinesVersion" }
coroutinesTest = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesVersion" }
coroutinesPlayServices = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-play-services", version.ref = "coroutinesVersion" }
```

- **Мотивация:** bundled-модель (Д3 брифа) — работает сразу, без
  Play-сервисов, и только у неё есть `confidence`. `coroutines-play-services`
  даёт `Task.await()` вместо ручного `suspendCancellableCoroutine`.
  ExifInterface — поворот снимка с камеры.

### 1.3. Скрипт сборки модуля — новый файл

`modules/library/textrecognition/build.gradle.kts`

- **Было:** файла нет.
- **Будет** (по образцу `modules/library/flags/build.gradle.kts`):

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "me.apomazkin.textrecognition"
    compileSdk = libs.versions.compileSdk.get().toInt()

    testOptions {
        targetSdk = libs.versions.targetSdk.get().toInt()
    }

    lint {
        targetSdk = libs.versions.targetSdk.get().toInt()
    }

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvm.get())
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvm.get()))
    }
}

dependencies {
    implementation(otherLibs.mlkitTextRecognition)
    implementation(kotlinLibs.coroutinesCore)
    implementation(kotlinLibs.coroutinesPlayServices)
    implementation(androidLibs.exifInterface)

    androidTestImplementation(testLibs.androidxTestExt)
    androidTestImplementation(testLibs.espressoCore)
}
```

- **Мотивация:** те же плагины и блоки, что у `flags`; unit-тестов в
  модуле нет (вся логика — вызовы Android и ML Kit), есть только
  androidTest.

### 1.4. Интерфейс распознавания — новый файл

`modules/library/textrecognition/src/main/java/me/apomazkin/textrecognition/TextRecognizer.kt`

- **Было:** файла нет.
- **Будет:**

```kotlin
package me.apomazkin.textrecognition

import android.net.Uri

/**
 * Слово из распознанного текста: [confidence] 0…1 (в unbundled-варианте
 * ML Kit всегда 0), [language] — код BCP-47 или null, если модель язык не
 * определила.
 */
data class RecognizedWord(
    val text: String,
    val confidence: Float,
    val language: String?,
)

/** Текст с картинки: строки — для сверки с фото в логах, слова — для списка. */
data class RecognizedText(
    val lines: List<String>,
    val words: List<RecognizedWord>,
) {
    companion object {
        val Empty = RecognizedText(lines = emptyList(), words = emptyList())
    }
}

interface TextRecognizer {
    suspend fun recognize(uri: Uri): RecognizedText
}
```

- **Мотивация:** экран и юзкейс зависят от этого интерфейса, не от ML
  Kit; в тестах юзкейса он подменяется моком.

### 1.5. Реализация на ML Kit — новый файл

`modules/library/textrecognition/src/main/java/me/apomazkin/textrecognition/MlKitTextRecognizer.kt`

- **Было:** файла нет.
- **Будет:**

```kotlin
package me.apomazkin.textrecognition

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.google.mlkit.vision.text.TextRecognizer as MlKitClient

private const val DEFAULT_MAX_PIXELS = 12_000_000
private const val UNDETERMINED_LANGUAGE = "und"

/**
 * Латиница моделью ML Kit, встроенной в APK. Картинка уменьшается ещё при
 * декодировании до бюджета [maxPixels]: скриншоты и фото до 12 Мп идут
 * как есть, фото 48 Мп сжимается вчетверо (полноразмерное — сотни
 * мегабайт в памяти). Поворот берётся из EXIF — без него снимок с камеры
 * уходит в модель «боком» и читается мусором. Недекодируемая картинка —
 * исключение: юзер должен увидеть ошибку, а не «Слов не найдено».
 */
class MlKitTextRecognizer(
    private val context: Context,
    private val maxPixels: Int = DEFAULT_MAX_PIXELS,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : TextRecognizer {

    private val client: MlKitClient by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun recognize(uri: Uri): RecognizedText = withContext(dispatcher) {
        val bitmap = decodeScaled(uri) ?: throw IOException("Cannot decode image $uri")
        val image = InputImage.fromBitmap(bitmap, readRotation(uri))
        // recycle() только после завершения задачи: await() при отмене
        // бросает, а задача ML Kit продолжает читать битмап.
        val text = client.process(image).await()
        bitmap.recycle()
        text.toRecognized()
    }

    private fun decodeScaled(uri: Uri): Bitmap? {
        // decodeStream с inJustDecodeBounds всегда возвращает null — размеры
        // читаются из options, поэтому проверка на null только у потока.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = openStream(uri) ?: return null
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val pixels = bounds.outWidth.toLong() * bounds.outHeight
        var sampleSize = 1
        while (pixels / (sampleSize.toLong() * sampleSize) > maxPixels) sampleSize *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val stream = openStream(uri) ?: return null
        return stream.use { BitmapFactory.decodeStream(it, null, options) }
    }

    /** Градусы 0/90/180/270; у PNG без EXIF — 0. Зеркальные варианты учтены библиотекой. */
    private fun readRotation(uri: Uri): Int {
        val stream = openStream(uri) ?: return 0
        return stream.use { ExifInterface(it).rotationDegrees }
    }

    private fun openStream(uri: Uri) = context.contentResolver.openInputStream(uri)

    private fun Text.toRecognized(): RecognizedText = RecognizedText(
        lines = textBlocks.flatMap { block -> block.lines.map { it.text } },
        words = textBlocks.flatMap { block ->
            block.lines.flatMap { line ->
                line.elements.map { element ->
                    val language = element.recognizedLanguage
                    RecognizedWord(
                        text = element.text,
                        confidence = element.confidence,
                        language = language.takeUnless { it == UNDETERMINED_LANGUAGE },
                    )
                }
            }
        },
    )
}
```

(плюс `import java.io.IOException`.)

- **Мотивация:** Д3 (bundled, латиница), Д6 (уменьшение и поворот,
  уточнено ревью: бюджет пикселей вместо стороны). `fromFilePath` из
  документации декодирует полный размер — поэтому свой декод.
  `inSampleSize` — степень двойки, так требует `BitmapFactory`; битмап
  12 Мп в ARGB_8888 — около 48 МБ, терпимо. `rotationDegrees` из
  androidx.exifinterface учитывает и зеркальные ориентации. Исключение
  при недекодируемой картинке (HEIC на API 24–27, пустой файл от
  камеры) доходит до юзера как «Не удалось распознать текст» через
  `RecoverableEffect` (3.3). Клиент один на процесс: его создание
  дорогое, закрывать нечего. Поток открывается трижды (размеры, EXIF,
  декод) — `InputStream` content-провайдера не перематывается.

### 1.6. AndroidTest модуля — новый файл

`modules/library/textrecognition/src/androidTest/java/me/apomazkin/textrecognition/MlKitTextRecognizerTest.kt`

- **Было:** файла нет.
- **Будет:**

```kotlin
package me.apomazkin.textrecognition

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Текст рисуется на Bitmap прямо в тесте — бинарных картинок в репозитории
 * нет. Bundled-модель работает и на эмуляторе CI без Play-сервисов.
 */
@RunWith(AndroidJUnit4::class)
class MlKitTextRecognizerTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val recognizer = MlKitTextRecognizer(context)

    @Test
    fun drawnLatinTextIsRecognizedAsWords() = runBlocking {
        val file = drawToFile(text = "hello world", name = "drawn.jpg")

        val result = recognizer.recognize(Uri.fromFile(file))

        val words = result.words.map { it.text.lowercase() }
        assertTrue("words: $words", "hello" in words)
        assertTrue("words: $words", "world" in words)
        assertTrue("lines: ${result.lines}", result.lines.isNotEmpty())
    }

    @Test
    fun blankImageGivesNoWords() = runBlocking {
        val file = drawToFile(text = null, name = "blank.jpg")

        val result = recognizer.recognize(Uri.fromFile(file))

        assertTrue("words: ${result.words}", result.words.isEmpty())
    }

    private fun drawToFile(text: String?, name: String): File {
        val bitmap = Bitmap.createBitmap(1200, 400, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        if (text != null) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 120f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText(text, 60f, 240f, paint)
        }
        val file = File(context.cacheDir, name)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bitmap.recycle()
        return file
    }
}
```

- **Мотивация:** доказывает, что модель подключена, читает латиницу и не
  выдумывает слова на пустом листе. `Uri.fromFile` открывается через
  `contentResolver` так же, как content-Uri фотопикера.

### 1.7. CI

`.github/workflows/on_pull_request.yml`

- **Было** (строка 69):

```yaml
          script: ./gradlew :core:core-db-impl:connectedDebugAndroidTest :modules:screen:quiz:chat:connectedDebugAndroidTest :modules:library:flags:connectedDebugAndroidTest --continue
```

- **Будет:**

```yaml
          script: ./gradlew :core:core-db-impl:connectedDebugAndroidTest :modules:screen:quiz:chat:connectedDebugAndroidTest :modules:library:flags:connectedDebugAndroidTest :modules:library:textrecognition:connectedDebugAndroidTest --continue
```

- **Мотивация:** androidTest нового модуля — обязательная проверка PR,
  как у `flags`.

## 2. Извлечение слов (`wordstab`, чистый Kotlin)

### 2.1. Зависимость модуля

`modules/screen/wordstab/build.gradle.kts`

- **Было** (строки 48–49):

```kotlin
    implementation(project("path" to ":core:core-resources"))
    implementation(project("path" to ":modules:domain:lexeme"))
```

- **Будет:**

```kotlin
    implementation(project("path" to ":core:core-resources"))
    implementation(project("path" to ":modules:domain:lexeme"))
    implementation(project("path" to ":modules:library:textrecognition"))
```

- **Мотивация:** типы `RecognizedText`/`RecognizedWord` в юзкейсе и
  функции извлечения. `FileProvider` для хелпера фото (5.7) приходит из
  `androidx.core` транзитивно через `activityCompose` (строка 62),
  отдельная зависимость не нужна.

### 2.2. Функция извлечения — новый файл

`modules/screen/wordstab/src/main/java/me/apomazkin/wordstab/logic/photo/ExtractWords.kt`

- **Было:** файла нет.
- **Будет:**

```kotlin
package me.apomazkin.wordstab.logic.photo

import me.apomazkin.textrecognition.RecognizedWord

/**
 * Пороги отсева. По умолчанию выключены: включаются после первых
 * ручников, когда по логам видны реальные значения уверенности.
 * [language] — код языка словаря; слово без определённого языка (null у
 * ML Kit) при включённом фильтре не отсеивается.
 */
data class WordRules(
    val minLength: Int = 2,
    val minConfidence: Float = 0f,
    val language: String? = null,
)

/**
 * Слова ML Kit → список для показа: по краям снято всё, что не буква и
 * не цифра (пунктуация, кавычки, перенос `well-`, притяжательный `dogs'`),
 * апостроф и дефис внутри остаются сами (`don't`, `well-known`), токены
 * без букв и короче [WordRules.minLength] выброшены, регистр нижний,
 * дубли схлопнуты, порядок — первое появление (Д4 брифа, Д10 IS523).
 */
fun extractWords(
    words: List<RecognizedWord>,
    rules: WordRules = WordRules(),
): List<String> = words
    .asSequence()
    .filter { it.confidence >= rules.minConfidence }
    .filter { rules.language == null || it.language == null || it.language == rules.language }
    .map { it.text.trim { char -> !char.isLetterOrDigit() }.lowercase() }
    .filter { it.length >= rules.minLength && it.any(Char::isLetter) }
    .distinct()
    .toList()
```

- **Мотивация:** правила Д4 в одном месте, без Android — unit-тесты
  обычным JUnit. `trim` трогает только края, поэтому внутренние апостроф
  (оба вида) и дефис сохраняются без особых случаев; ревью показало, что
  список «разрешённых на краях» символов ломал `‘Hello,’` → `hello,’`.

### 2.3. Тесты извлечения — новый файл

`modules/screen/wordstab/src/test/java/me/apomazkin/wordstab/logic/photo/ExtractWordsTest.kt`

- **Было:** файла нет.
- **Будет** (стиль — как у `AddWordDialogExtTest`, по кейсу на правило):

```kotlin
package me.apomazkin.wordstab.logic.photo

import me.apomazkin.textrecognition.RecognizedWord
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 1. пунктуация и кавычки по краям снимаются, в т. ч. британские ‘…’
 * 2. апостроф (оба вида) и дефис внутри остаются
 * 3. апостроф и дефис по краям снимаются (перенос, притяжательный, 'tis)
 * 4. точка внутри остаётся (e.g.)
 * 5. диакритика латиницы — буквы (café, niño)
 * 6. числа и токены без букв выбрасываются
 * 7. одиночные буквы выбрасываются
 * 8. регистр приводится к нижнему
 * 9. дубли схлопываются, порядок — первое появление
 * 10. пустой вход — пустой список
 * 11. порог уверенности отсеивает слабые слова
 * 12. фильтр языка пропускает слово без языка
 */
class ExtractWordsTest {

    private fun word(text: String, confidence: Float = 1f, language: String? = "en") =
        RecognizedWord(text = text, confidence = confidence, language = language)

    @Test
    fun `should strip punctuation and quotes from edges`() {
        val input = listOf(word("\"Hello,"), word("world!"), word("(end)."), word("‘Hello,’"))
        assertEquals(listOf("hello", "world", "end"), extractWords(input))
    }

    @Test
    fun `should keep apostrophe and hyphen inside word`() {
        val input = listOf(word("don't"), word("isn’t"), word("well-known"))
        assertEquals(listOf("don't", "isn’t", "well-known"), extractWords(input))
    }

    @Test
    fun `should strip apostrophe and hyphen from edges`() {
        val input = listOf(word("well-"), word("dogs'"), word("'tis"), word("-"))
        assertEquals(listOf("well", "dogs", "tis"), extractWords(input))
    }

    @Test
    fun `should keep dot inside word`() {
        assertEquals(listOf("e.g"), extractWords(listOf(word("e.g."))))
    }

    @Test
    fun `should treat latin diacritics as letters`() {
        val input = listOf(word("Café,"), word("niño"))
        assertEquals(listOf("café", "niño"), extractWords(input))
    }

    @Test
    fun `should drop tokens without letters`() {
        val input = listOf(word("1984"), word("--"), word("42nd"), word("..."))
        assertEquals(listOf("42nd"), extractWords(input))
    }

    @Test
    fun `should drop single letters`() {
        assertEquals(listOf("an"), extractWords(listOf(word("a"), word("I"), word("an"))))
    }

    @Test
    fun `should lowercase words`() {
        assertEquals(listOf("the", "london"), extractWords(listOf(word("The"), word("LONDON"))))
    }

    @Test
    fun `should collapse duplicates keeping first appearance order`() {
        val input = listOf(
            word("the"),
            word("cat"),
            word("The"),
            word("dog"),
            word("cat"),
        )
        assertEquals(listOf("the", "cat", "dog"), extractWords(input))
    }

    @Test
    fun `should return empty list for empty input`() {
        assertEquals(emptyList<String>(), extractWords(emptyList()))
    }

    @Test
    fun `should drop words below confidence threshold when enabled`() {
        val result = extractWords(
            words = listOf(word("sure", confidence = 0.9f), word("blurry", confidence = 0.2f)),
            rules = WordRules(minConfidence = 0.5f),
        )
        assertEquals(listOf("sure"), result)
    }

    @Test
    fun `should keep dictionary language and unknown language when filter enabled`() {
        val result = extractWords(
            words = listOf(
                word("perro", language = "es"),
                word("dog", language = "en"),
                word("gato", language = null),
            ),
            rules = WordRules(language = "es"),
        )
        assertEquals(listOf("perro", "gato"), result)
    }
}
```

- **Мотивация:** каждое правило Д4 закреплено тестом, включая края,
  найденные ревью (британские кавычки, перенос, притяжательный
  апостроф, диакритика); пороги проверены до того, как их включат.

## 3. Состояние, сообщения, эффект, редьюсер (`wordstab`)

### 3.1. Подсостояние диалога

`modules/screen/wordstab/src/main/java/me/apomazkin/wordstab/logic/State.kt`

- **Было** (строки 3, 17–27):

```kotlin
import androidx.compose.runtime.Immutable
```

```kotlin
@Immutable
data class WordsTabState(
        val isLoading: Boolean = true,
        val hasNoDictionary: Boolean = false,
        val topBarState: TopBarState = TopBarState(),
        val termList: TermsSource = TermsSource(pattern = ""),
        val termListMap: Map<String, Flow<PagingData<TermUiItem>>> = emptyMap(),
        val addWordDialogState: AddWordDialogState = AddWordDialogState(),
        val snackbarState: SnackbarState = SnackbarState(),
        val confirmWordDeleteDialogState: ConfirmWordDeleteDialogState = ConfirmWordDeleteDialogState(),
)
```

- **Будет:**

```kotlin
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
```

```kotlin
@Immutable
data class WordsTabState(
        val isLoading: Boolean = true,
        val hasNoDictionary: Boolean = false,
        val topBarState: TopBarState = TopBarState(),
        val termList: TermsSource = TermsSource(pattern = ""),
        val termListMap: Map<String, Flow<PagingData<TermUiItem>>> = emptyMap(),
        val addWordDialogState: AddWordDialogState = AddWordDialogState(),
        val snackbarState: SnackbarState = SnackbarState(),
        val confirmWordDeleteDialogState: ConfirmWordDeleteDialogState = ConfirmWordDeleteDialogState(),
        val photoWordsState: PhotoWordsState = PhotoWordsState(),
)
```

Рядом с `ConfirmWordDeleteDialogState` (после строки 79):

```kotlin
/**
 * IS527: диалог «слова с фото» — три шага явными флагами: выбор источника
 * ([isChoosingSource]) → распознавание ([isRecognizing]) → список
 * ([words]) или ошибка ([errorRes]). Закрыт — флаги в умолчаниях.
 * [requestId] растёт на каждый снимок и переживает закрытие диалога:
 * результат распознавания с чужим id — устаревший, он отбрасывается
 * (эффект mate при закрытии не отменяется).
 */
@Immutable
data class PhotoWordsState(
        val isOpen: Boolean = false,
        val isChoosingSource: Boolean = false,
        val isRecognizing: Boolean = false,
        val words: List<String> = emptyList(),
        @StringRes val errorRes: Int? = null,
        val requestId: Long = 0L,
)
```

В конец файла, перед `choose` (после строки 280):

```kotlin
/**
 * ###### PHOTO WORDS DIALOG (IS527) ######
 */
/** Новый диалог — новый id: результат прежнего снимка на шаг выбора не попадёт. */
fun WordsTabState.showPhotoSourceChooser(): WordsTabState = this.copy(
        photoWordsState = PhotoWordsState(
                isOpen = true,
                isChoosingSource = true,
                requestId = photoWordsState.requestId + 1,
        )
)

/** Новый снимок — новый id: результаты прежних распознаваний устарели. */
fun WordsTabState.startRecognizing(): WordsTabState = this.copy(
        photoWordsState = PhotoWordsState(
                isOpen = true,
                isRecognizing = true,
                requestId = photoWordsState.requestId + 1,
        )
)

fun WordsTabState.showRecognizedWords(
        words: List<String>,
): WordsTabState = this.copy(
        photoWordsState = PhotoWordsState(
                isOpen = true,
                words = words,
                requestId = photoWordsState.requestId,
        )
)

fun WordsTabState.showRecognitionError(
        @StringRes errorRes: Int,
): WordsTabState = this.copy(
        photoWordsState = PhotoWordsState(
                isOpen = true,
                errorRes = errorRes,
                requestId = photoWordsState.requestId,
        )
)

fun WordsTabState.hidePhotoWords(): WordsTabState = this.copy(
        photoWordsState = PhotoWordsState(
                requestId = photoWordsState.requestId,
        )
)

/** Результат относится к текущему открытому диалогу, а не к прошлому снимку. */
fun WordsTabState.isCurrentPhotoRequest(
        requestId: Long,
): Boolean = photoWordsState.isOpen && photoWordsState.requestId == requestId
```

- **Мотивация:** явные флаги состояния вместо вычислений в composable
  (правило проекта); каждая функция строит подсостояние целиком, поэтому
  переход между шагами не оставляет хвостов от предыдущего. `requestId`
  закрывает гонку из ревью: «Отмена» во время распознавания A, новый
  снимок B, результат A приходит в диалог B. Id растёт и при открытии
  выбора источника, иначе результат A попадал бы на шаг выбора нового
  диалога с тем же id (поймано unit-тестом при реализации).

### 3.2. Сообщения

`modules/screen/wordstab/src/main/java/me/apomazkin/wordstab/logic/Message.kt`

- **Было** (строки 3, 110–114):

```kotlin
import androidx.paging.PagingData
```

```kotlin
    /**
     * Message indicating no operation is needed after an effect.
     */
    data object NoOperation : Msg
}
```

- **Будет:**

```kotlin
import androidx.annotation.StringRes
import androidx.paging.PagingData
```

```kotlin
    /** IS527: кнопка-камера — открыть выбор источника фото. */
    data object OpenPhotoSourceChooser : Msg

    /**
     * IS527: камера или галерея вернули снимок. Uri строкой — Android-классы
     * в unit-тестах модуля заглушены; в Uri превращает исполнитель эффекта.
     */
    data class PhotoPicked(val uri: String) : Msg

    /** IS527: камера или галерея закрыты без снимка — назад к выбору источника. */
    data object PhotoPickCancelled : Msg

    /**
     * IS527: распознавание завершено, слова уже отфильтрованы.
     * [requestId] — чей это результат (см. PhotoWordsState.requestId).
     */
    data class WordsRecognized(
            val words: List<String>,
            val requestId: Long,
    ) : Msg

    /** IS527: распознавание упало или камера не запустилась — текст ошибки ресурсом. */
    data class RecognitionFailed(
            @StringRes val errorRes: Int,
            val requestId: Long,
    ) : Msg

    /** IS527: закрыть диалог слов с фото на любом шаге. */
    data object ClosePhotoWords : Msg

    /**
     * Message indicating no operation is needed after an effect.
     */
    data object NoOperation : Msg
}
```

- **Мотивация:** шесть сообщений — по одному на переход диалога; порядок
  в sealed — по ходу сценария; `requestId` в ответных сообщениях — для
  отбраковки устаревших результатов в редьюсере.

### 3.3. Эффект и его исполнение

`modules/screen/wordstab/src/main/java/me/apomazkin/wordstab/logic/DatasourceEffectHandler.kt`

- **Было** (строки 3–15, 17–27, 86–95):

```kotlin
import androidx.paging.cachedIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import me.apomazkin.wordstab.deps.WordsTabUseCase
import me.apomazkin.wordstab.entity.WordInfo
import me.apomazkin.mate.EMPTY_STRING
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.MateEffectHandler
import me.apomazkin.mate.LogTags
import me.apomazkin.logger.LexemeLogger
```

```kotlin
sealed interface DatasourceEffect : Effect {

    data class LoadTermFlow(
            val pattern: String = EMPTY_STRING,
    ) : DatasourceEffect

    data class CreateWord(val value: String) : DatasourceEffect
    data class UpdateWord(val wordId: Long, val value: String) : DatasourceEffect

    data class RemoveWords(val wordSet: Set<WordInfo>) : DatasourceEffect
}
```

```kotlin
            is DatasourceEffect.RemoveWords -> {
                withContext(io) {
                    eff.wordSet.map { id ->
                        async { wordstabUseCase.deleteWord(id.id) }.await()
                    }
                }
                Msg.NoOperation
            }
        }
        consumer(msg)
```

- **Будет:**

```kotlin
import android.net.Uri
import androidx.paging.cachedIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import me.apomazkin.wordstab.R
import me.apomazkin.wordstab.deps.WordsTabUseCase
import me.apomazkin.wordstab.entity.WordInfo
import me.apomazkin.wordstab.logic.photo.extractWords
import me.apomazkin.mate.EMPTY_STRING
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.MateEffectHandler
import io.github.kilgoret.mate.RecoverableEffect
import me.apomazkin.mate.LogTags
import me.apomazkin.logger.LexemeLogger
```

```kotlin
sealed interface DatasourceEffect : Effect {

    data class LoadTermFlow(
            val pattern: String = EMPTY_STRING,
    ) : DatasourceEffect

    data class CreateWord(val value: String) : DatasourceEffect
    data class UpdateWord(val wordId: Long, val value: String) : DatasourceEffect

    data class RemoveWords(val wordSet: Set<WordInfo>) : DatasourceEffect

    /**
     * IS527: распознать текст на снимке; [uri] строкой, см. Msg.PhotoPicked.
     * Провал (недекодируемая картинка, ML Kit, OOM) объявлен в типе:
     * хендлер ошибки не ловит, стектрейс пишет ErrorLoggingObserver.
     */
    data class RecognizeWords(
            val uri: String,
            val requestId: Long,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.RecognitionFailed(
                errorRes = R.string.photo_words_error_recognition,
                requestId = requestId,
        )
    }
}
```

```kotlin
            is DatasourceEffect.RemoveWords -> {
                withContext(io) {
                    eff.wordSet.map { id ->
                        async { wordstabUseCase.deleteWord(id.id) }.await()
                    }
                }
                Msg.NoOperation
            }

            // IS527: полный текст по строкам — в лог: по нему на ручниках
            // сверяется качество ML Kit с фото (в списке видны только слова).
            // В магазинной сборке DEBUG никуда не уходит (LOG_LEVEL NONE,
            // CrashlyticsSink пишет только WARNING и ERROR).
            is DatasourceEffect.RecognizeWords -> withContext(io) {
                val text = wordstabUseCase.recognizeText(Uri.parse(eff.uri))
                text.lines.forEachIndexed { index, line ->
                    logger.d(
                            tag = me.apomazkin.wordstab.LogTags.VOCAB,
                            message = "OCR line $index: $line",
                    )
                }
                Msg.WordsRecognized(
                        words = extractWords(text.words),
                        requestId = eff.requestId,
                )
            }
        }
        consumer(msg)
```

`modules/screen/wordstab/src/main/java/me/apomazkin/wordstab/ui/WordsTabAssembly.kt`

- **Было** (строки 11, 64):

```kotlin
import me.apomazkin.logger.LexemeLogger
```

```kotlin
        observers = observers,
```

- **Будет:**

```kotlin
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.mate.ErrorLoggingObserver
import me.apomazkin.wordstab.LogTags
```

```kotlin
        // IS527: стектрейсы упавших эффектов (RecognizeWords — RecoverableEffect)
        // в лог, как у quiztab/groupstab; без наблюдателя причина провала
        // видна только как recovery-Msg.
        observers = observers + ErrorLoggingObserver(logger, LogTags.VOCAB),
```

- **Мотивация:** эффект живёт в том же семействе и исполнителе, что
  остальные разовые операции вкладки. Провал объявлен в типе через
  `RecoverableEffect` — правило проекта и прецеденты quiztab, groupstab,
  per_dictionary_components; try/catch в хендлере не поймал бы
  `OutOfMemoryError` и оставил бы диалог на лоадере навсегда. Сборка
  вкладки слов наблюдателя ошибок не имела — добавлен. Тег `VOCAB` в
  хендлере указан полным именем: там уже импортирован `me.apomazkin.mate.LogTags`.

### 3.4. Редьюсер

`modules/screen/wordstab/src/main/java/me/apomazkin/wordstab/logic/WordsTabReducer.kt`

- **Было** (строки 3–6, 139–140):

```kotlin
import me.apomazkin.wordstab.logic.DatasourceEffect.CreateWord
import me.apomazkin.wordstab.logic.DatasourceEffect.LoadTermFlow
import me.apomazkin.wordstab.logic.DatasourceEffect.RemoveWords
import me.apomazkin.wordstab.logic.DatasourceEffect.UpdateWord
```

```kotlin
            is UiMsg -> processUiMessage(state, message)
            Msg.NoOperation -> state to emptySet()
```

- **Будет:**

```kotlin
import me.apomazkin.wordstab.logic.DatasourceEffect.CreateWord
import me.apomazkin.wordstab.logic.DatasourceEffect.LoadTermFlow
import me.apomazkin.wordstab.logic.DatasourceEffect.RecognizeWords
import me.apomazkin.wordstab.logic.DatasourceEffect.RemoveWords
import me.apomazkin.wordstab.logic.DatasourceEffect.UpdateWord
```

```kotlin
            // IS527: диалог «слова с фото» в три шага — выбор источника,
            // распознавание, список (PhotoWordsState).
            Msg.OpenPhotoSourceChooser -> state
                .showPhotoSourceChooser() to setOf()

            // Принимается и при закрытом диалоге: система могла убить
            // процесс, пока была открыта камера, — снимок приходит в свежее
            // состояние и диалог открывается сразу на шаге распознавания.
            // Эффект несёт id нового запроса — он выдан в startRecognizing().
            is Msg.PhotoPicked -> state
                .startRecognizing()
                .let { next ->
                    next to setOf(
                        RecognizeWords(
                            uri = message.uri,
                            requestId = next.photoWordsState.requestId,
                        )
                    )
                }

            // Отмена в камере после смерти процесса не открывает диалог сама.
            Msg.PhotoPickCancelled -> state.choose(
                check = { it.photoWordsState.isOpen },
                yes = { it.showPhotoSourceChooser() to setOf() },
                no = { it to emptySet() },
            )

            // Результат чужого или закрытого запроса (юзер закрыл диалог во
            // время распознавания, снял новое фото) отбрасывается.
            is Msg.WordsRecognized -> state.choose(
                check = { it.isCurrentPhotoRequest(message.requestId) },
                yes = { it.showRecognizedWords(message.words) to setOf() },
                no = { it to emptySet() },
            )

            is Msg.RecognitionFailed -> state.choose(
                check = { it.isCurrentPhotoRequest(message.requestId) },
                yes = { it.showRecognitionError(message.errorRes) to setOf() },
                no = { it to emptySet() },
            )

            Msg.ClosePhotoWords -> state
                .hidePhotoWords() to setOf()

            is UiMsg -> processUiMessage(state, message)
            Msg.NoOperation -> state to emptySet()
```

- **Мотивация:** по ветке на сообщение, стиль файла; `choose` — уже
  существующий хелпер условных переходов в этом же файле. Проверка по
  `requestId` отсекает и поздний результат после «Отмены», и результат
  прошлого снимка при открытом новом.

### 3.5. Тесты расширений состояния — новый файл

`modules/screen/wordstab/src/test/java/me/apomazkin/wordstab/logic/ext/PhotoWordsExtTest.kt`

- **Было:** файла нет.
- **Будет:**

```kotlin
package me.apomazkin.wordstab.logic.ext

import me.apomazkin.wordstab.logic.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 1. showPhotoSourceChooser открывает диалог на шаге выбора
 * 2. startRecognizing переводит на шаг распознавания и сбрасывает слова и ошибку
 * 3. showRecognizedWords показывает список, флаги шагов сняты
 * 4. showRecognitionError показывает ошибку, слова пусты
 * 5. hidePhotoWords закрывает диалог и сбрасывает всё
 * 6. остальные поля состояния не меняются
 */
class PhotoWordsExtTest {

    private val baseState = WordsTabState(
        isLoading = false,
        snackbarState = SnackbarState(title = "test", show = true),
    )

    @Test
    fun `should open dialog on source chooser step`() {
        val result = baseState.showPhotoSourceChooser()

        assertTrue(result.photoWordsState.isOpen)
        assertTrue(result.photoWordsState.isChoosingSource)
        assertFalse(result.photoWordsState.isRecognizing)
    }

    @Test
    fun `should switch to recognizing step and drop previous result`() {
        val result = baseState
            .showRecognitionError(errorRes = 1)
            .startRecognizing()

        assertTrue(result.photoWordsState.isRecognizing)
        assertFalse(result.photoWordsState.isChoosingSource)
        assertNull(result.photoWordsState.errorRes)
        assertTrue(result.photoWordsState.words.isEmpty())
    }

    @Test
    fun `should show words and clear step flags`() {
        val result = baseState.startRecognizing().showRecognizedWords(listOf("hello", "world"))

        assertEquals(listOf("hello", "world"), result.photoWordsState.words)
        assertTrue(result.photoWordsState.isOpen)
        assertFalse(result.photoWordsState.isRecognizing)
        assertNull(result.photoWordsState.errorRes)
    }

    @Test
    fun `should show error without words`() {
        val result = baseState.startRecognizing().showRecognitionError(errorRes = 7)

        assertEquals(7, result.photoWordsState.errorRes)
        assertTrue(result.photoWordsState.words.isEmpty())
        assertFalse(result.photoWordsState.isRecognizing)
    }

    @Test
    fun `should close dialog and reset everything but request id`() {
        val result = baseState
            .showPhotoSourceChooser()
            .startRecognizing()
            .showRecognizedWords(listOf("hello"))
            .hidePhotoWords()

        assertEquals(PhotoWordsState(requestId = 2L), result.photoWordsState)
    }

    @Test
    fun `should increment request id on chooser and on recognizing start only`() {
        val chooser = baseState.showPhotoSourceChooser()
        val recognizing = chooser.startRecognizing()
        val shown = recognizing.showRecognizedWords(listOf("hello"))
        val reopened = shown.hidePhotoWords().showPhotoSourceChooser()

        assertEquals(1L, chooser.photoWordsState.requestId)
        assertEquals(2L, recognizing.photoWordsState.requestId)
        assertEquals(2L, shown.photoWordsState.requestId)
        assertEquals(3L, reopened.photoWordsState.requestId)
        assertTrue(recognizing.isCurrentPhotoRequest(2L))
        assertFalse(reopened.isCurrentPhotoRequest(2L))
        assertFalse(shown.hidePhotoWords().isCurrentPhotoRequest(2L))
    }

    @Test
    fun `should not touch other fields`() {
        val result = baseState.showPhotoSourceChooser()

        assertEquals(baseState.isLoading, result.isLoading)
        assertEquals(baseState.snackbarState, result.snackbarState)
        assertEquals(baseState.addWordDialogState, result.addWordDialogState)
        assertEquals(baseState.topBarState, result.topBarState)
    }
}
```

- **Мотивация:** по образцу `AddWordDialogExtTest`: переходы и
  неизменность остального состояния.

### 3.6. Тесты редьюсера

`modules/screen/wordstab/src/test/java/me/apomazkin/wordstab/logic/WordsTabReducerKtTest.kt`

- **Было:** веток для сообщений IS527 нет; хелперы `reducer` (строка 47)
  и `createTestState` (строка 102) есть.
- **Будет** (добавляются в конец класса):

```kotlin
    // ===== IS527: слова с фото =====

    @Test
    fun `should open photo source chooser without effects`() {
        val result = reducer.testReduce(createTestState(), Msg.OpenPhotoSourceChooser)

        assertTrue(result.state().photoWordsState.isOpen)
        assertTrue(result.state().photoWordsState.isChoosingSource)
        result.assertNoEffects("Opening chooser must not emit effects")
    }

    @Test
    fun `should start recognizing and emit RecognizeWords with new request id`() {
        val initialState = createTestState().showPhotoSourceChooser()

        val result = reducer.testReduce(initialState, Msg.PhotoPicked(uri = "content://photo/1"))

        assertTrue(result.state().photoWordsState.isRecognizing)
        assertEquals(2L, result.state().photoWordsState.requestId)
        result.assertEffects(
            setOf(DatasourceEffect.RecognizeWords(uri = "content://photo/1", requestId = 2L))
        )
    }

    @Test
    fun `should accept picked photo even when dialog is closed`() {
        val initialState = createTestState()

        val result = reducer.testReduce(initialState, Msg.PhotoPicked(uri = "content://photo/2"))

        assertTrue(result.state().photoWordsState.isOpen)
        assertTrue(result.state().photoWordsState.isRecognizing)
    }

    @Test
    fun `should return to chooser when pick cancelled while dialog is open`() {
        val initialState = createTestState().showPhotoSourceChooser()

        val result = reducer.testReduce(initialState, Msg.PhotoPickCancelled)

        assertTrue(result.state().photoWordsState.isChoosingSource)
        result.assertNoEffects("Cancel must not emit effects")
    }

    @Test
    fun `should ignore pick cancel when dialog is closed`() {
        val initialState = createTestState()

        val result = reducer.testReduce(initialState, Msg.PhotoPickCancelled)

        assertFalse(result.state().photoWordsState.isOpen)
        result.assertNoEffects("Cancel after process death must be silent")
    }

    @Test
    fun `should show recognized words of current request`() {
        val initialState = createTestState().startRecognizing()

        val result = reducer.testReduce(
            initialState,
            Msg.WordsRecognized(words = listOf("hello"), requestId = 1L),
        )

        assertEquals(listOf("hello"), result.state().photoWordsState.words)
        assertFalse(result.state().photoWordsState.isRecognizing)
    }

    @Test
    fun `should ignore recognized words when dialog already closed`() {
        val initialState = createTestState().startRecognizing().hidePhotoWords()

        val result = reducer.testReduce(
            initialState,
            Msg.WordsRecognized(words = listOf("hello"), requestId = 1L),
        )

        assertFalse(result.state().photoWordsState.isOpen)
        result.assertNoEffects("Late result must be ignored silently")
    }

    @Test
    fun `should ignore recognized words of previous request when new dialog is open`() {
        val initialState = createTestState()
            .startRecognizing()
            .hidePhotoWords()
            .showPhotoSourceChooser()

        val result = reducer.testReduce(
            initialState,
            Msg.WordsRecognized(words = listOf("stale"), requestId = 1L),
        )

        assertTrue(result.state().photoWordsState.isChoosingSource)
        assertTrue(result.state().photoWordsState.words.isEmpty())
    }

    @Test
    fun `should show error when recognition of current request failed`() {
        val initialState = createTestState().startRecognizing()

        val result = reducer.testReduce(
            initialState,
            Msg.RecognitionFailed(errorRes = 5, requestId = 1L),
        )

        assertEquals(5, result.state().photoWordsState.errorRes)
        assertFalse(result.state().photoWordsState.isRecognizing)
    }

    @Test
    fun `should show camera error on chooser step with current request id`() {
        val initialState = createTestState().showPhotoSourceChooser()

        val result = reducer.testReduce(
            initialState,
            Msg.RecognitionFailed(errorRes = 6, requestId = 1L),
        )

        assertEquals(6, result.state().photoWordsState.errorRes)
        assertFalse(result.state().photoWordsState.isChoosingSource)
    }

    @Test
    fun `should close photo words dialog`() {
        val initialState = createTestState().startRecognizing().showRecognizedWords(listOf("hello"))

        val result = reducer.testReduce(initialState, Msg.ClosePhotoWords)

        assertEquals(PhotoWordsState(requestId = 1L), result.state().photoWordsState)
        result.assertNoEffects("Closing must not emit effects")
    }
```

- **Мотивация:** эффект ровно один (`assertEffects` из mate-test) и ровно
  при `PhotoPicked`; поздний и чужой результат отбрасываются; снимок после
  смерти процесса принимается, отмена после неё — нет; ошибка камеры с
  шага выбора доходит (id не менялся).

## 4. Юзкейс и DI (`wordstab/deps`, `app`)

### 4.1. Интерфейс юзкейса

`modules/screen/wordstab/src/main/java/me/apomazkin/wordstab/deps/WordsTabUseCase.kt`

- **Было** (строки 3–6, 20–23):

```kotlin
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.wordrow.entity.TermUiItem
```

```kotlin
    suspend fun addWord(value: String): Long
    suspend fun deleteWord(wordId: Long)
    suspend fun updateWord(id: Long, value: String): Boolean
}
```

- **Будет:**

```kotlin
import android.net.Uri
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.textrecognition.RecognizedText
import me.apomazkin.wordrow.entity.TermUiItem
```

```kotlin
    suspend fun addWord(value: String): Long
    suspend fun deleteWord(wordId: Long)
    suspend fun updateWord(id: Long, value: String): Boolean

    /** IS527: текст со снимка (ML Kit на устройстве). */
    suspend fun recognizeText(uri: Uri): RecognizedText
}
```

- **Мотивация:** экран не знает о ML Kit; подмена в тестах — мок.

### 4.2. Реализация юзкейса

`app/src/main/java/me/apomazkin/polytrainer/di/module/wordstab/WordsTabUseCaseImpl.kt`

- **Было** (строки 3–26):

```kotlin
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.wordstab.deps.WordsTabUseCase
import me.apomazkin.wordrow.entity.TermUiItem
import me.apomazkin.wordrow.entity.toUiItem
import me.apomazkin.flags.CountryProvider
import me.apomazkin.polytrainer.di.module.dictionary.CurrentDictionaryProvider
import me.apomazkin.polytrainer.mapper.toDomain
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import javax.inject.Inject

class WordsTabUseCaseImpl @Inject constructor(
    private val dictionaryApi: CoreDbApi.DictionaryApi,
    private val wordApi: CoreDbApi.WordApi,
    private val termApi: CoreDbApi.TermApi,
    private val prefsProvider: PrefsProvider,
    private val countryProvider: CountryProvider,
    private val currentDictionaryProvider: CurrentDictionaryProvider,
) : WordsTabUseCase {
```

- **Будет:**

```kotlin
import android.content.Context
import android.net.Uri
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.textrecognition.RecognizedText
import me.apomazkin.textrecognition.TextRecognizer
import me.apomazkin.wordstab.deps.WordsTabUseCase
import me.apomazkin.wordrow.entity.TermUiItem
import me.apomazkin.wordrow.entity.toUiItem
import me.apomazkin.flags.CountryProvider
import me.apomazkin.polytrainer.di.module.dictionary.CurrentDictionaryProvider
import me.apomazkin.polytrainer.mapper.toDomain
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import javax.inject.Inject

private const val FILE_PROVIDER_SUFFIX = ".fileprovider"

class WordsTabUseCaseImpl @Inject constructor(
    private val dictionaryApi: CoreDbApi.DictionaryApi,
    private val wordApi: CoreDbApi.WordApi,
    private val termApi: CoreDbApi.TermApi,
    private val prefsProvider: PrefsProvider,
    private val countryProvider: CountryProvider,
    private val currentDictionaryProvider: CurrentDictionaryProvider,
    private val textRecognizer: TextRecognizer,
    private val appContext: Context,
) : WordsTabUseCase {

    // IS527: распознавание целиком в библиотеке. Снимок камеры (наш
    // FileProvider) после распознавания не нужен — удаляется в любом исходе;
    // картинка из галереи не наша, её не трогаем.
    override suspend fun recognizeText(uri: Uri): RecognizedText = try {
        textRecognizer.recognize(uri)
    } finally {
        if (uri.authority == appContext.packageName + FILE_PROVIDER_SUFFIX) {
            appContext.contentResolver.delete(uri, null, null)
        }
    }
```

- **Мотивация:** юзкейс — единственная точка, где экран получает данные
  с устройства. `Context` в графе уже есть (`@BindsInstance appContext`),
  поэтому чистить снимок здесь дешевле и надёжнее, чем при следующем
  снимке (ревью: очистка папки гонялась бы с ещё идущим распознаванием).
  `FileProvider.delete` удаляет файл по content-Uri.

### 4.3. Тест реализации юзкейса

`app/src/test/java/me/apomazkin/polytrainer/di/module/wordstab/WordsTabUseCaseImplTest.kt`

- **Было** (строки 65–77):

```kotlin
        useCase = WordsTabUseCaseImpl(
            dictionaryApi = dictionaryApi,
            wordApi = wordApi,
            termApi = termApi,
            prefsProvider = prefsProvider,
            countryProvider = countryProvider,
            // IS500: живой провайдер на тех же моках — семантика
            // flowCurrentDict проверяется сквозь него.
            currentDictionaryProvider = CurrentDictionaryProvider(
                dictionaryApi = dictionaryApi,
                prefsProvider = prefsProvider,
            ),
        )
```

- **Будет:**

```kotlin
        useCase = WordsTabUseCaseImpl(
            dictionaryApi = dictionaryApi,
            wordApi = wordApi,
            termApi = termApi,
            prefsProvider = prefsProvider,
            countryProvider = countryProvider,
            // IS500: живой провайдер на тех же моках — семантика
            // flowCurrentDict проверяется сквозь него.
            currentDictionaryProvider = CurrentDictionaryProvider(
                dictionaryApi = dictionaryApi,
                prefsProvider = prefsProvider,
            ),
            // IS527: распознавание в этих тестах не трогается.
            textRecognizer = mockk(),
            appContext = mockk(),
        )
```

- **Мотивация:** конструктор получил два параметра; `mockk` в тесте уже
  используется для остальных зависимостей, ни один существующий кейс
  распознавание не зовёт.

### 4.4. Dagger-модуль распознавания — новый файл

`app/src/main/java/me/apomazkin/polytrainer/di/module/textrecognition/TextRecognizerModule.kt`

- **Было:** файла нет.
- **Будет** (по образцу `CountryProviderModule`):

```kotlin
package me.apomazkin.polytrainer.di.module.textrecognition

import android.content.Context
import dagger.Module
import dagger.Provides
import me.apomazkin.textrecognition.MlKitTextRecognizer
import me.apomazkin.textrecognition.TextRecognizer
import javax.inject.Singleton

@Module
class TextRecognizerModule {

    /** IS527: один клиент ML Kit на процесс — его создание дорогое. */
    @Singleton
    @Provides
    fun provideTextRecognizer(context: Context): TextRecognizer = MlKitTextRecognizer(context)
}
```

`app/src/main/java/me/apomazkin/polytrainer/di/AppComponent.kt`

- **Было** (строки 24, 103–104):

```kotlin
import me.apomazkin.polytrainer.di.module.flags.CountryProviderModule
```

```kotlin
        CountryProviderModule::class,
        NavigationModule::class,
```

- **Будет:**

```kotlin
import me.apomazkin.polytrainer.di.module.flags.CountryProviderModule
import me.apomazkin.polytrainer.di.module.textrecognition.TextRecognizerModule
```

```kotlin
        CountryProviderModule::class,
        TextRecognizerModule::class,
        NavigationModule::class,
```

`app/build.gradle.kts`

- **Было** (строка 144):

```kotlin
    implementation(project("path" to ":modules:library:flags"))
```

- **Будет:**

```kotlin
    implementation(project("path" to ":modules:library:flags"))
    implementation(project("path" to ":modules:library:textrecognition"))
```

- **Мотивация:** `Context` уже есть в графе (`@BindsInstance appContext`),
  модуль-провайдер по готовому образцу; `PilotRegistry` юзкейс получает
  извне моком и правки не требует.

## 5. Интерфейс

### 5.1. Иконка камеры — новый файл

`core/core-resources/src/main/res/drawable/ic_photo_camera.xml`

- **Было:** файла нет; иконки камеры среди ~40 drawable папки нет.
- **Будет** (Material Icons `photo_camera`, Apache 2.0, белая заливка —
  как у `ic_add.xml`):

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#ffffff"
        android:pathData="M12,15.2c1.77,0 3.2,-1.43 3.2,-3.2s-1.43,-3.2 -3.2,-3.2 -3.2,1.43 -3.2,3.2 1.43,3.2 3.2,3.2z" />
    <path
        android:fillColor="#ffffff"
        android:pathData="M9,2L7.17,4H4c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V6c0,-1.1 -0.9,-2 -2,-2h-3.17L15,2H9zm3,15c-2.76,0 -5,-2.24 -5,-5s2.24,-5 5,-5 5,2.24 5,5 -2.24,5 -5,5z" />
</vector>
```

- **Мотивация:** drawable живёт в `core-resources`, как `ic_add` для FAB
  групп; `nonTransitiveRClass=false` — доступен как `R.drawable.ic_photo_camera`
  из `wordstab`.

### 5.2. Размер базового FAB

`modules/core/ui/src/main/java/me/apomazkin/ui/btn/base/LexemeFab.kt`

- **Было** (строки 19, 25, 30–46):

```kotlin
import androidx.compose.ui.unit.dp
```

```kotlin
private const val DEFAULT_CORNER_RADIUS = 16
```

```kotlin
@Composable
fun LexemeFab(
    @DrawableRes iconRes: Int,
    color: Color,
    enabled: Boolean = true,
    iconColor: Color = whiteColor,
    cornerRadius: Int = DEFAULT_CORNER_RADIUS,
    contentDescription: String = "Floating action button",
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(56.dp)
            .background(
                color = color,
                shape = RoundedCornerShape(cornerRadius.dp)
            ),
```

- **Будет:**

```kotlin
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
```

```kotlin
private const val DEFAULT_CORNER_RADIUS = 16
private val DEFAULT_FAB_SIZE = 56.dp
```

```kotlin
@Composable
fun LexemeFab(
    @DrawableRes iconRes: Int,
    color: Color,
    enabled: Boolean = true,
    iconColor: Color = whiteColor,
    cornerRadius: Int = DEFAULT_CORNER_RADIUS,
    size: Dp = DEFAULT_FAB_SIZE,
    contentDescription: String = "Floating action button",
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(size)
            .background(
                color = color,
                shape = RoundedCornerShape(cornerRadius.dp)
            ),
```

- **Мотивация:** параметр с умолчанием — существующие вызовы не меняются;
  маленький FAB получает тот же стиль, а не копию виджета.

### 5.3. Маленький FAB — новый файл

`modules/core/ui/src/main/java/me/apomazkin/ui/btn/SmallFabWidget.kt`

- **Было:** файла нет.
- **Будет** (по образцу `PrimaryFabWidget.kt`):

```kotlin
package me.apomazkin.ui.btn

import androidx.annotation.DrawableRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.R
import me.apomazkin.ui.btn.base.LexemeFab
import me.apomazkin.ui.preview.PreviewWidget

private val SMALL_FAB_SIZE = 40.dp

/**
 * IS527: вторичный FAB над основным (камера над «+»). Цвет — тот же
 * primary, что у [PrimaryFabWidget], размер 40 dp. [contentDescription]
 * обязателен: иначе TalkBack читает обе кнопки одинаково.
 */
@Composable
fun SmallFabWidget(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    LexemeFab(
        iconRes = iconRes,
        enabled = enabled,
        color = MaterialTheme.colorScheme.primary,
        size = SMALL_FAB_SIZE,
        contentDescription = contentDescription,
        onClick = onClick,
    )
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        SmallFabWidget(
            iconRes = R.drawable.ic_send,
            contentDescription = "Send",
        ) {}
    }
}
```

- **Мотивация:** явные цвета парой из прецедента (`primary` + белая
  иконка из `LexemeFab`), никаких M3-умолчаний; подпись для TalkBack —
  находка ревью.

### 5.4. Контракт вкладок

`modules/screen/vocabulary/src/main/java/me/apomazkin/vocabulary/TabContract.kt`

- **Было** (строки 24–33, 59–66):

```kotlin
/**
 * FAB активной вкладки; null в [TabSpec.fab] — FAB отсутствует.
 * [visible] — читалка State (host оборачивает в AnimatedVisibility scaleIn/Out).
 */
@Stable
data class FabSpec(
    @DrawableRes val iconRes: Int,
    val visible: () -> Boolean,
    val onClick: () -> Unit,
)
```

```kotlin
@Stable
data class TabSpec(
    @StringRes val titleRes: Int,
    val isTopBarOverridden: () -> Boolean = { false },
    val topBarOverride: @Composable () -> Unit = {},
    val fab: FabSpec? = null,
    val content: @Composable (SnackbarHostState, DictionarySlot) -> Unit,
)
```

- **Будет:**

```kotlin
/**
 * FAB активной вкладки; null в [TabSpec.fab] — FAB отсутствует.
 * [visible] — читалка State (host оборачивает в AnimatedVisibility scaleIn/Out).
 */
@Stable
data class FabSpec(
    @DrawableRes val iconRes: Int,
    val visible: () -> Boolean,
    val onClick: () -> Unit,
)

/**
 * IS527: вторичный маленький FAB над основным (камера над «+»). Своей
 * видимости нет: показывается и прячется вместе с [TabSpec.fab] (Д9 IS523),
 * без основного не рисуется. [contentDescriptionRes] — подпись для TalkBack.
 */
@Stable
data class SecondaryFabSpec(
    @DrawableRes val iconRes: Int,
    @StringRes val contentDescriptionRes: Int,
    val onClick: () -> Unit,
)
```

```kotlin
@Stable
data class TabSpec(
    @StringRes val titleRes: Int,
    val isTopBarOverridden: () -> Boolean = { false },
    val topBarOverride: @Composable () -> Unit = {},
    val fab: FabSpec? = null,
    val secondaryFab: SecondaryFabSpec? = null,
    val content: @Composable (SnackbarHostState, DictionarySlot) -> Unit,
)
```

- **Мотивация:** отдельный тип без `visible` — чтобы не было поля,
  которое host не читает; умолчание `null` — GROUPS не меняется, контракт
  стабильности (спек собирается один раз) сохранён.

### 5.5. Хост: колонка из двух кнопок

`modules/screen/vocabulary/src/main/java/me/apomazkin/vocabulary/ui/VocabularyHostScreen.kt`

- **Было** (строки 7–14, 26, 35, 128–142):

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
```

```kotlin
import androidx.compose.ui.Modifier
```

```kotlin
import me.apomazkin.ui.btn.PrimaryFabWidget
```

```kotlin
        floatingActionButton = {
            activeSpec.fab?.let { fab ->
                val fabVisible = fab.visible()
                AnimatedVisibility(
                    visible = fabVisible,
                    enter = scaleIn(),
                    exit = scaleOut(),
                ) {
                    PrimaryFabWidget(
                        iconRes = fab.iconRes,
                        enabled = fabVisible,
                    ) { fab.onClick() }
                }
            }
        },
```

- **Будет:**

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
```

```kotlin
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
```

```kotlin
import me.apomazkin.ui.btn.PrimaryFabWidget
import me.apomazkin.ui.btn.SmallFabWidget
```

```kotlin
        floatingActionButton = {
            activeSpec.fab?.let { fab ->
                val fabVisible = fab.visible()
                AnimatedVisibility(
                    visible = fabVisible,
                    enter = scaleIn(),
                    exit = scaleOut(),
                ) {
                    // IS527: вторичный FAB (камера) над основным, одна
                    // анимация на обе кнопки — прячутся вместе (Д9).
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        activeSpec.secondaryFab?.let { secondary ->
                            SmallFabWidget(
                                iconRes = secondary.iconRes,
                                contentDescription = stringResource(secondary.contentDescriptionRes),
                                enabled = fabVisible,
                            ) { secondary.onClick() }
                        }
                        PrimaryFabWidget(
                            iconRes = fab.iconRes,
                            enabled = fabVisible,
                        ) { fab.onClick() }
                    }
                }
            }
        },
```

- **Мотивация:** `Column` внутри той же `AnimatedVisibility` — без
  второй анимации и без второго слота `Scaffold`; выравнивание по правому
  краю ставит маленькую кнопку ровно над большой. `stringResource` в
  файле уже импортирован (строка 28).

### 5.6. Сборка спеков и handle вкладки

`app/src/main/java/me/apomazkin/polytrainer/uiDeps/CompositionRootImpl.kt`

- **Было** (строки 87–93):

```kotlin
                    fab = FabSpec(
                        iconRes = words.fabIconRes,
                        visible = { words.isFabVisible.value },
                        onClick = words::onFabClick,
                    ),
                    // words словарь-слот игнорирует — резолвит сам (В3/D9.2).
                    content = { snackbarHostState, _ -> words.Content(snackbarHostState) },
```

- **Будет:**

```kotlin
                    fab = FabSpec(
                        iconRes = words.fabIconRes,
                        visible = { words.isFabVisible.value },
                        onClick = words::onFabClick,
                    ),
                    // IS527: камера над «+» — слова с фото. Только в dev-сборке,
                    // пока не готов сквозной путь до добавления слов (Д12 IS523).
                    secondaryFab = if (BuildConfig.DEBUG) {
                        SecondaryFabSpec(
                            iconRes = words.photoFabIconRes,
                            contentDescriptionRes = words.photoFabDescriptionRes,
                            onClick = words::onPhotoFabClick,
                        )
                    } else {
                        null
                    },
                    // words словарь-слот игнорирует — резолвит сам (В3/D9.2).
                    content = { snackbarHostState, _ -> words.Content(snackbarHostState) },
```

(плюс импорты `me.apomazkin.vocabulary.SecondaryFabSpec` рядом с `FabSpec`
и `me.apomazkin.polytrainer.BuildConfig`.)

**Решено юзером 2026-10-07: гейт `BuildConfig.DEBUG`** (вариант 1). Родительский
бриф (Д12): «кнопка-камера скрыта, пока не готов сквозной путь 1–6»; в
магазинной сборке кнопки нет до появления добавления слов. Снимается
отдельным коммитом в подфиче 6.

`modules/screen/wordstab/src/main/java/me/apomazkin/wordstab/ui/WordsTabScreen.kt`

- **Было** (строки 77–82):

```kotlin
    @DrawableRes
    val fabIconRes: Int = R.drawable.ic_add

    fun onFabClick() {
        viewModel.accept(Msg.OpenAddWordDialog())
    }
```

- **Будет:**

```kotlin
    @DrawableRes
    val fabIconRes: Int = R.drawable.ic_add

    fun onFabClick() {
        viewModel.accept(Msg.OpenAddWordDialog())
    }

    /** IS527: кнопка-камера над «+» — слова с фото. */
    @DrawableRes
    val photoFabIconRes: Int = R.drawable.ic_photo_camera

    @StringRes
    val photoFabDescriptionRes: Int = R.string.photo_words_title

    fun onPhotoFabClick() {
        viewModel.accept(Msg.OpenPhotoSourceChooser)
    }
```

(плюс `import androidx.annotation.StringRes` рядом с `DrawableRes`.)

- **Мотивация:** мост остаётся чистой склейкой: иконка, подпись и клик
  приходят из handle, как у основного FAB.

### 5.7. Хелпер выбора фото — новый файл

`modules/screen/wordstab/src/main/java/me/apomazkin/wordstab/ui/photo/PhotoPicker.kt`

- **Было:** файла нет; прецедент launcher'а — `settingstab/.../ImportDataWidget.kt`.
- **Будет:**

```kotlin
package me.apomazkin.wordstab.ui.photo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

private const val AUTHORITY_SUFFIX = ".fileprovider"
private const val PHOTOS_DIR = "photos"

/**
 * IS527: два системных пути к снимку — приложение камеры (`TakePicture`,
 * файл в кеше через FileProvider) и фотопикер (`PickVisualMedia`, без
 * разрешений). Результат — колбэками наружу, дальше — сообщения mate.
 */
@Stable
class PhotoPicker internal constructor(
    private val context: Context,
    private val takePicture: ManagedActivityResultLauncher<Uri, Boolean>,
    private val pickImage: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>,
    private val pendingCameraUri: MutableState<Uri?>,
) {

    /** Нужен `<queries>` на IMAGE_CAPTURE в манифесте, иначе на API 30+ всегда false. */
    fun isCameraAvailable(): Boolean =
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).resolveActivity(context.packageManager) != null

    fun launchCamera() {
        val uri = newPhotoUri(context)
        pendingCameraUri.value = uri
        takePicture.launch(uri)
    }

    fun launchGallery() {
        pickImage.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }
}

@Composable
internal fun rememberPhotoPicker(
    onPicked: (Uri) -> Unit,
    onCancelled: () -> Unit,
): PhotoPicker {
    val context = LocalContext.current
    // Uri снимка переживает пересоздание Activity: камера — отдельный
    // процесс, наш могут убить, пока она открыта.
    val pendingCameraUri = rememberSaveable { mutableStateOf<Uri?>(null) }
    val takePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { saved ->
        val uri = pendingCameraUri.value
        pendingCameraUri.value = null
        if (saved && uri != null) onPicked(uri) else onCancelled()
    }
    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) onPicked(uri) else onCancelled()
    }
    return remember(context, takePicture, pickImage) {
        PhotoPicker(
            context = context,
            takePicture = takePicture,
            pickImage = pickImage,
            pendingCameraUri = pendingCameraUri,
        )
    }
}

/** Новый файл под снимок; удаляет его юзкейс после распознавания (4.2). */
private fun newPhotoUri(context: Context): Uri {
    val dir = File(context.cacheDir, PHOTOS_DIR).apply { mkdirs() }
    val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)
}
```

- **Мотивация:** Д1/Д2 брифа. `rememberSaveable` с `Uri` — `Uri` Parcelable,
  штатный saver кладёт его в Bundle. `context.packageName` даёт
  `co.lexeme.app.dev` в dev-сборке — authority совпадает с манифестом
  (`${applicationId}.fileprovider`). Launcher'ы регистрируются там, где
  хелпер вызван, — он вызывается безусловно в `WordsTabContent` (5.9),
  чтобы результат после смерти процесса не потерялся. `launchCamera`
  может бросить `ActivityNotFoundException` (камеру отключили между
  проверкой и запуском) или `SecurityException` (если какая-то библиотека
  объявит `CAMERA` в объединённом манифесте) — ловит вызывающий (5.9).

### 5.8. Диалог — новый файл

`modules/screen/wordstab/src/main/java/me/apomazkin/wordstab/ui/widget/photo/PhotoWordsDialog.kt`

- **Было:** файла нет; образец — `LanguagePickerDialog` (IS525) и
  `AlarmDialogWidget`.
- **Будет:**

```kotlin
package me.apomazkin.wordstab.ui.widget.photo

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.formBackground
import me.apomazkin.theme.formTextTertiary
import me.apomazkin.ui.btn.CancelButtonWidget
import me.apomazkin.ui.btn.PrimaryFullButtonWidget
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.wordstab.R
import me.apomazkin.wordstab.logic.PhotoWordsState

/**
 * IS527: один диалог на три шага — выбор источника, распознавание, список
 * (или ошибка). Цвета явные, парой из LanguagePickerDialog (IS525):
 * фон formBackground, текст formTextTertiary, акцент LexemeColor.primary.
 * [isCameraAvailable] false — кнопки «Камера» нет, остаётся галерея.
 */
@Composable
internal fun PhotoWordsDialog(
    state: PhotoWordsState,
    isCameraAvailable: Boolean,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onDismiss: () -> Unit,
) {
    val hasList = state.words.isNotEmpty()
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = formBackground,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (hasList) Modifier.fillMaxHeight(0.8f) else Modifier),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title(state),
                    style = LexemeStyle.BodyM,
                    color = formTextTertiary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                when {
                    state.isChoosingSource -> SourceChooser(
                        isCameraAvailable = isCameraAvailable,
                        onCamera = onCamera,
                        onGallery = onGallery,
                    )
                    state.isRecognizing -> Recognizing()
                    state.errorRes != null -> Message(textRes = state.errorRes)
                    !hasList -> Message(textRes = R.string.photo_words_empty)
                    else -> WordList(words = state.words, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(16.dp))
                CancelButtonWidget(
                    modifier = Modifier.fillMaxWidth(),
                    titleRes = if (state.isChoosingSource || state.isRecognizing) {
                        R.string.button_cancel
                    } else {
                        R.string.photo_words_close
                    },
                ) { onDismiss() }
            }
        }
    }
}

@Composable
private fun title(state: PhotoWordsState): String = when {
    state.isRecognizing -> stringResource(id = R.string.photo_words_recognizing)
    state.words.isNotEmpty() -> stringResource(id = R.string.photo_words_found, state.words.size)
    else -> stringResource(id = R.string.photo_words_title)
}

@Composable
private fun SourceChooser(
    isCameraAvailable: Boolean,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
) {
    if (isCameraAvailable) {
        PrimaryFullButtonWidget(
            modifier = Modifier.fillMaxWidth(),
            titleRes = R.string.photo_words_camera,
            onClick = onCamera,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
    PrimaryFullButtonWidget(
        modifier = Modifier.fillMaxWidth(),
        titleRes = R.string.photo_words_gallery,
        onClick = onGallery,
    )
}

@Composable
private fun Recognizing() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = LexemeColor.primary)
    }
}

@Composable
private fun Message(@StringRes textRes: Int) {
    Text(
        text = stringResource(id = textRes),
        style = LexemeStyle.BodyM,
        color = formTextTertiary,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun WordList(words: List<String>, modifier: Modifier) {
    LazyColumn(modifier = modifier) {
        items(words, key = { it }) { word ->
            Text(
                text = word,
                style = LexemeStyle.BodyM,
                color = formTextTertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            )
        }
    }
}

@PreviewWidget
@Composable
private fun PreviewList() {
    AppTheme {
        PhotoWordsDialog(
            state = PhotoWordsState(isOpen = true, words = listOf("hello", "world")),
            isCameraAvailable = true,
            onCamera = {},
            onGallery = {},
            onDismiss = {},
        )
    }
}
```

- **Мотивация:** Д5 брифа. Высота 80 % только у списка — короткие шаги
  не растягиваются. `key = { it }` корректен: слова уникальны после
  `distinct()`. Закрытие тапом вне и «назад» — через `onDismissRequest`
  самого `Dialog`. Без приложения камеры кнопки «Камера» нет вовсе —
  ревью: иначе после ошибки «Камера недоступна» до галереи не добраться.

### 5.9. Подключение в контент вкладки

`modules/screen/wordstab/src/main/java/me/apomazkin/wordstab/ui/WordsTabScreen.kt`

- **Было** (строки 3, 34–38, 194–201):

```kotlin
import androidx.activity.compose.BackHandler
```

```kotlin
import me.apomazkin.wordstab.ui.widget.ConfirmDeleteWordWidget
import me.apomazkin.wordstab.ui.widget.EmptyWidget
import me.apomazkin.wordstab.ui.widget.WordListWidget
import me.apomazkin.wordstab.ui.widget.addWordBottom.AddWordPanelWidget
import me.apomazkin.wordstab.ui.widget.topBar.ActionTopBarWidget
```

```kotlin
        if (state.confirmWordDeleteDialogState.isOpen) {
            ConfirmDeleteWordWidget(
                state = state.confirmWordDeleteDialogState,
                sendMessage = sendMessage,
            )
        }
    }
}
```

- **Будет:**

```kotlin
import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
```

```kotlin
import me.apomazkin.wordstab.ui.photo.rememberPhotoPicker
import me.apomazkin.wordstab.ui.widget.ConfirmDeleteWordWidget
import me.apomazkin.wordstab.ui.widget.EmptyWidget
import me.apomazkin.wordstab.ui.widget.WordListWidget
import me.apomazkin.wordstab.ui.widget.addWordBottom.AddWordPanelWidget
import me.apomazkin.wordstab.ui.widget.photo.PhotoWordsDialog
import me.apomazkin.wordstab.ui.widget.topBar.ActionTopBarWidget
```

```kotlin
        if (state.confirmWordDeleteDialogState.isOpen) {
            ConfirmDeleteWordWidget(
                state = state.confirmWordDeleteDialogState,
                sendMessage = sendMessage,
            )
        }
        // IS527: launcher'ы регистрируются всегда, не под `if`: после смерти
        // процесса в камере диалог закрыт, а результат должен дойти.
        val photoPicker = rememberPhotoPicker(
            onPicked = { uri -> sendMessage(Msg.PhotoPicked(uri = uri.toString())) },
            onCancelled = { sendMessage(Msg.PhotoPickCancelled) },
        )
        if (state.photoWordsState.isOpen) {
            // Наличие камеры — на открытие диалога, не на каждую рекомпозицию.
            val isCameraAvailable = remember { photoPicker.isCameraAvailable() }
            val cameraError = Msg.RecognitionFailed(
                errorRes = R.string.photo_words_error_camera,
                requestId = state.photoWordsState.requestId,
            )
            PhotoWordsDialog(
                state = state.photoWordsState,
                isCameraAvailable = isCameraAvailable,
                onCamera = {
                    // Камеру могли отключить между проверкой и запуском;
                    // SecurityException — если CAMERA объявит чужая библиотека.
                    try {
                        photoPicker.launchCamera()
                    } catch (e: ActivityNotFoundException) {
                        sendMessage(cameraError)
                    } catch (e: SecurityException) {
                        sendMessage(cameraError)
                    }
                },
                onGallery = photoPicker::launchGallery,
                onDismiss = { sendMessage(Msg.ClosePhotoWords) },
            )
        }
    }
}
```

- **Мотивация:** диалог — рядом с другими диалогами вкладки; хелпер вне
  условия по причине из 5.7; ошибка запуска камеры — через то же
  сообщение, что ошибка распознавания, с другим текстом и текущим
  `requestId` (на шаге выбора он не менялся, редьюсер её примет).
  `remember` в файле уже импортирован (строка 16).

### 5.10. Манифест и пути FileProvider

`app/src/main/AndroidManifest.xml`

- **Было** (строки 5–11, 18–22):

```xml
    <uses-permission android:name="com.google.android.gms.permission.AD_ID" tools:node="remove"/>

    <application
        android:name=".App"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.Lexeme.Starting">
```

```xml
        <meta-data
            android:name="firebase_analytics_collection_enabled"
            android:value="false" />

        <activity
```

- **Будет:**

```xml
    <uses-permission android:name="com.google.android.gms.permission.AD_ID" tools:node="remove"/>

    <!-- IS527: проверка наличия приложения камеры (resolveActivity на API 30+
         без queries всегда null). Разрешение CAMERA не объявляется: для
         ACTION_IMAGE_CAPTURE оно не нужно. -->
    <queries>
        <intent>
            <action android:name="android.media.action.IMAGE_CAPTURE" />
        </intent>
    </queries>

    <application
        android:name=".App"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.Lexeme.Starting">
```

```xml
        <meta-data
            android:name="firebase_analytics_collection_enabled"
            android:value="false" />

        <!-- IS527: файл снимка для приложения камеры; ${applicationId} даёт
             суффикс .dev в dev-сборке. -->
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>

        <activity
```

`app/src/main/res/xml/file_paths.xml` — новый файл (в папке уже лежит
`backup_descriptor.xml`):

- **Было:** файла нет.
- **Будет:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="photos" path="photos/" />
</paths>
```

- **Мотивация:** системной камере нужен content-Uri с правом записи;
  `cache-path` совпадает с папкой из 5.7.

### 5.11. Строки

`core/core-resources/src/main/res/values/strings.xml`

- **Было** (строки 126–127):

```xml
    <string name="vocabulary_delete_word">Delete word?</string>
    <string name="vocabulary_delete_words">Delete words?</string>
```

- **Будет:**

```xml
    <string name="vocabulary_delete_word">Delete word?</string>
    <string name="vocabulary_delete_words">Delete words?</string>

    <!-- IS527: words from photo -->
    <string name="photo_words_title">Words from photo</string>
    <string name="photo_words_camera">Camera</string>
    <string name="photo_words_gallery">Gallery</string>
    <string name="photo_words_recognizing">Recognizing…</string>
    <string name="photo_words_found">Words: %1$d</string>
    <string name="photo_words_empty">No words found</string>
    <string name="photo_words_close">Close</string>
    <string name="photo_words_error_camera">Camera is not available</string>
    <string name="photo_words_error_recognition">Could not recognize text</string>
```

`core/core-resources/src/main/res/values-ru-rRU/strings.xml`

- **Было** (строки 114–115):

```xml
    <string name="vocabulary_delete_word">Удалить слово?</string>
    <string name="vocabulary_delete_words">Удалить слова?</string>
```

- **Будет:**

```xml
    <string name="vocabulary_delete_word">Удалить слово?</string>
    <string name="vocabulary_delete_words">Удалить слова?</string>

    <!-- IS527: слова с фото -->
    <string name="photo_words_title">Слова с фото</string>
    <string name="photo_words_camera">Камера</string>
    <string name="photo_words_gallery">Галерея</string>
    <string name="photo_words_recognizing">Распознаю…</string>
    <string name="photo_words_found">Слов: %1$d</string>
    <string name="photo_words_empty">Слов не найдено</string>
    <string name="photo_words_close">Закрыть</string>
    <string name="photo_words_error_camera">Камера недоступна</string>
    <string name="photo_words_error_recognition">Не удалось распознать текст</string>
```

- **Мотивация:** «Слов: N» без существительного в склонении — plurals
  не нужны; «Отменить» — существующий `button_cancel`.

## 6. Контрольные точки сборки и проверки

1. После раздела 1: `./scripts/cc-build.sh :modules:library:textrecognition:assembleDebug`,
   затем `…:connectedDebugAndroidTest` на девайсе (`.dev` в фокусе не
   требуется — тест ставит свой пакет `me.apomazkin.textrecognition.test`).
2. После разделов 2–3: `./scripts/cc-build.sh :modules:screen:wordstab:testDebugUnitTest`.
3. После раздела 4: `./scripts/cc-build.sh :app:testDebugUnitTest --tests "*WordsTabUseCaseImplTest"`.
4. После раздела 5: `./scripts/cc-build.sh assembleDebug`, `:app:lintDebug`,
   `installDebug`, запуск `co.lexeme.app.dev` через `monkey`. Перед
   ручниками — проверка объединённого манифеста
   (`app/build/intermediates/merged_manifests/debug/…/AndroidManifest.xml`):
   `android.permission.CAMERA` в нём нет, `<queries>` и `<provider>` есть.
5. Ручники по одному в чате, после каждого — лог `###VOCAB###` с `OCR line`
   и сверка с фото:
   - фото страницы книги с камеры; альбомный кадр;
   - обычный скриншот из галереи; длинный скриншот с прокруткой;
   - текст с британскими кавычками, переносами по дефису, испанской
     диакритикой; двухколоночная страница — порядок слов;
   - фото без текста — «Слов не найдено»;
   - отмена на каждом шаге: тап вне на выборе, «назад» в камере,
     «Отмена» во время распознавания и сразу новый снимок (устаревший
     результат не должен всплыть);
   - два запуска подряд; длинный список — прокрутка;
   - смерть процесса при открытой камере (`adb shell am kill co.lexeme.app.dev`,
     пока камера на экране): снимок доходит в диалог; «назад» в камере
     ничего не открывает;
   - камера отключена (`pm disable-user` приложения камеры или эмулятор
     без камеры) — кнопки «Камера» нет, галерея работает;
   - поворот экрана на каждом шаге диалога;
   - на «Группах» камеры нет; при открытой панели добавления слова камеры
     нет.
6. Коммит одним пакетом с доками IS523 и IS527 (превью перед `git
   commit`), PR, четыре зелёные проверки.

## 7. Прогон 2026-10-07

Девайс: Pixel 7 Pro, Android 17, dev-сборка `co.lexeme.app.dev`.

- **Сборка и проверки:** `assembleDebug` и `:app:lintDebug` чистые.
  Unit-тесты wordstab: извлечение 12, расширения состояния 7, редьюсер
  33 (11 новых). App: `WordsTabUseCaseImplTest` 6, `PilotScenarioTest` 5.
  AndroidTest `textrecognition` на девайсе: 2 из 2 за 1,6 с.
  Объединённый манифест: `CAMERA` нет, `<queries>` и провайдер
  `co.lexeme.app.dev.fileprovider` есть.
- **Найдено при реализации:** unit-тест поймал дыру в защите от
  устаревших результатов — `requestId` рос только на снимке, результат
  старого снимка принимался на шаге выбора нового диалога с тем же id.
  Теперь id растёт и при открытии выбора; тесты и триады обновлены.
- **Ручники, 13 из 13 пройдены:** фото страницы с камеры (испанский,
  диакритика на месте: «agradeció», «ñandú», 67 слов); скриншот из
  галереи через фотопикер (`content://media/picker/…`); фото без текста
  → «Слов не найдено», файл снимка удалён; тап мимо диалога; «назад» в
  камере → выбор источника; «Отменить» на лоадере → поздний результат
  отброшен; отмена + новый диалог → результат старого снимка не попал на
  шаг выбора (id 7 против 8); альбомный кадр — поворот из EXIF верный;
  длинный список 163 слова без пропусков кадров; поворот экрана на
  списке; «Закрыть»; камера прячется вместе с «+», на «Группах» её нет;
  смерть процесса при открытой камере (`am kill`) → снимок пришёл в
  свежее состояние, список построен. Крэшей ноль.
- **Для ручников временно** ставилась задержка 4 с в dev-сборке перед
  распознаванием (иначе лоадер не успеть отменить); перед коммитом
  убрана.
- **Качество ML Kit (к Д2 IS523):** книжная страница и альбомный кадр —
  чисто; плотный мелкий текст — обрывки с краёв кадра и соседней
  колонки, пара кривых символов («Naciỏ», «đe»), переносы слов режутся
  («pri-», «mei»). Порядок блоков внутри абзаца ML Kit не гарантирует
  (строки «fiesta.», «observarlo.» пришли не на месте) — учесть при
  сборке контекста слова в подфиче 4.
- **Не проверено:** телефон без приложения камеры (кнопку «Камера»
  скрывает `resolveActivity`, на Pixel камера есть); тёмная тема диалога
  — девайс был в тёмной теме весь прогон, замечаний нет.
