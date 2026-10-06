# IS525 | План: языки словаря

Бриф и решения — `brief.md`.

Формат: каждое изменение — триада **было / будет / мотивация**, где «было»
и «будет» — код. Триады собраны в разделы. Разделы идут в порядке
выполнения: сначала то, что ни от чего не зависит, потом то, что на это
опирается.

Правила подфичи (юзер): оба языка обязательные; изучаемый — основной язык
страны флага, без флага — английский; перевод — язык телефона;
существующие словари заполняет миграция. Код языка — с регионом
(`es-MX`). Сначала механика, интерфейс потом.

Этот план покрывает механику и минимальный интерфейс (разделы 1–8).
Второй заход по интерфейсу (лента флагов, оформление выбора языка и
строки-сводки) получит отдельный план в этом же формате после проверки
механики.

**Осознанное отклонение от В1 в минимальном интерфейсе:** бриф — один
лист с обоими выборами (вкладки); здесь — два нажатия на строке-сводке и
простое окно на каждое. Лист с вкладками — во втором заходе.

**Известное ограничение (не чиним сейчас):** часть кодов библиотеки — не
канонический BCP-47: Сингапур — `cmn` (не `zh`), Филиппины — `tl`,
Норвегия — `no`. Хранится как есть; откат «на общий язык по префиксу» в
будущих подфичах для таких кодов не сработает — учесть там.

Ревью плана 2026-10-05 (три агента): замечания внесены — защита от
падения библиотеки на неизвестный код, рабочий фильтр Ф4, сохранение
флага необитаемой территории при правке, раннер и CI для androidTest
модуля `flags`, пересчёт списка при открытом окне, тесты веток редьюсера,
ширина строк.

| # | Раздел | Опирается на |
|---|---|---|
| 1 | Разбор языков страны и фильтры Ф1–Ф3 (`modules/library/flags`) | — |
| 2 | База данных (`core-db-api`, `core-db-impl`, `core-db`) | — |
| 3 | API слоя данных (`core-db-api`, `core-db-impl`) | 2 |
| 4 | Правила языков и передача их в базу (`app`) | 1, 2 |
| 5 | Юзкейс словаря (`modules/screen/dictionary`, `app`) | 1, 3, 4 |
| 6 | Механика формы (`modules/screen/dictionary/form`) | 5 |
| 7 | Минимальный интерфейс (`…/form/widget`, строки) | 6 |
| 8 | Проверка | 1–7 |

Сборка: `flags` собирается и тестируется после раздела 1; `core-db-api` +
`core-db-impl` — после разделов 2–3 вместе (2.2 пишет в поля сущности из
3.1); `modules/screen/dictionary` и `app` снова собираются после раздела
6 (раздел 5 меняет сигнатуры юзкейса, которые зовёт исполнитель эффектов
формы). Тесты 5.6 гоняются после раздела 6.

---

## 1. Разбор языков страны

### 1.1. Функция разбора — новый файл

`modules/library/flags/src/main/java/me/apomazkin/flags/LanguageTagParser.kt`

- **Было:** файла нет. Код языка из строк библиотеки никто не извлекает.
- **Будет:**

```kotlin
package me.apomazkin.flags

/**
 * Язык страны из строки библиотеки country-data: [tag] — код с
 * региональным вариантом, [englishName] — английское название.
 */
data class CountryLanguage(
    val tag: String,
    val englishName: String,
)

/**
 * Разбор строки библиотеки: код — содержимое последней пары скобок,
 * название — всё до неё. "Spanish (es-MX)" → es-MX / Spanish,
 * "Occitan (post 1500) (oc)" → oc / "Occitan (post 1500)".
 * Строка без кода в скобках (в библиотеке: Антарктида — пустая строка,
 * острова Буве и Херд — "()") → null.
 */
fun parseCountryLanguage(raw: String): CountryLanguage? {
    val trimmed = raw.trim()
    if (!trimmed.endsWith(")")) return null
    val open = trimmed.lastIndexOf('(')
    if (open < 0) return null
    val tag = trimmed
        .substring(open + 1, trimmed.length - 1)
        .trim()
    if (tag.isEmpty()) return null
    val name = trimmed
        .substring(0, open)
        .trim()
        .ifEmpty { tag }
    return CountryLanguage(tag = tag, englishName = name)
}
```

- **Мотивация:** код с регионом нужен для хранения и показа. Английское
  название — запасной вариант показа, если Android не знает код
  (`cau`, `ktu` и другие редкие): лучше «Caucasian languages», чем голый
  код. Разбор — в одном месте, рядом с источником данных. Прогон по всему
  массиву библиотеки (2026-10-05): 732 строки, 729 разбираются, 3 пустые
  (необитаемые территории), все коды в форме `xx`/`xxx` + необязательный
  регион из двух заглавных букв.

### 1.2. Собирательные коды языков — новый файл (фильтр Ф2)

`modules/library/flags/src/main/java/me/apomazkin/flags/CollectiveLanguageCodes.kt`

- **Было:** файла нет.
- **Будет:**

```kotlin
package me.apomazkin.flags

/**
 * Собирательные коды ISO 639-2 — языковые семьи и группы («кавказские
 * языки», «славянские языки»), плюс служебные коды. Словарь такого «языка»
 * быть не может — записи с этими кодами не предлагаются нигде (фильтр Ф2,
 * IS525). В массиве библиотеки country-data v1.5.4: 6 записей в 3 странах
 * (Россия cau, tut; Индия bh, sit, inc; Марокко ber), ни одна не основной
 * язык страны.
 */
object CollectiveLanguageCodes {

    private val codes: Set<String> = setOf(
        "afa", "alg", "apa", "art", "ath", "aus", "bad", "bai", "bat", "ber",
        "bh", "bih", "bnt", "btk", "cai", "cau", "cel", "cmc", "cpe", "cpf",
        "cpp", "crp", "cus", "day", "dra", "fiu", "gem", "ijo", "inc", "ine",
        "ira", "iro", "kar", "khi", "kro", "map", "mkh", "mno", "mun", "myn",
        "nah", "nai", "nic", "nub", "oto", "paa", "phi", "pra", "roa", "sai",
        "sal", "sem", "sgn", "sio", "sit", "sla", "smi", "son", "ssa", "tai",
        "tup", "tut", "wak", "wen", "ypk", "zle", "zls", "zlw", "znd",
        // служебные: неизвестный, несколько языков, не определён, нет языка
        "mis", "mul", "und", "zxx",
    )

    /** Основа кода (до региона): «cau», «sla-RU» → собирательный. */
    fun isCollective(tag: String): Boolean = tag.substringBefore('-') in codes
}
```

- **Мотивация:** критерий «что не язык» — фиксированный список стандарта,
  а не догадка по названию; не зависит от устройства.

### 1.3. Языки страны и страны для словаря в провайдере (фильтры Ф1–Ф3)

`modules/library/flags/src/main/java/me/apomazkin/flags/CountryProviderImpl.kt`

- **Было:**

```kotlin
interface CountryProvider {
    fun getFlagRes(numericCode: Int): Int
    fun getAllCountries(): List<CountryInfo>
    fun getLanguagesForCountry(numericCode: Int): List<String>
}
```

```kotlin
    override fun getLanguagesForCountry(numericCode: Int): List<String> {
        return World.getLanguagesFrom(numericCode)
    }
}
```

- **Будет:**

```kotlin
interface CountryProvider {
    fun getFlagRes(numericCode: Int): Int
    fun getAllCountries(): List<CountryInfo>
    fun getLanguagesForCountry(numericCode: Int): List<String>

    /**
     * IS525: языки страны, пригодные для словаря, в порядке библиотеки —
     * основной первым («es-MX», «en-US»). Отброшены строки без кода (Ф1) и
     * собирательные коды семей (Ф2, [CollectiveLanguageCodes]).
     */
    fun getCountryLanguages(numericCode: Int): List<CountryLanguage>

    /**
     * IS525: страны, пригодные для словаря — у которых после Ф1 и Ф2 есть
     * хотя бы один язык (Ф3). В библиотеке v1.5.4 это 247 стран из 250:
     * без языков Антарктида, Буве, Херд.
     */
    fun getDictionaryCountries(): List<CountryInfo>
}
```

```kotlin
    override fun getLanguagesForCountry(numericCode: Int): List<String> {
        return World.getLanguagesFrom(numericCode)
    }

    override fun getCountryLanguages(numericCode: Int): List<CountryLanguage> {
        // Библиотека на неизвестный код падает с NPE (countryFrom → universe
        // == null в v1.5.4) — граница с библиотекой не должна ронять ни
        // форму, ни миграцию: неизвестная страна = страна без языков.
        val raw = runCatching { World.getLanguagesFrom(numericCode) }.getOrDefault(emptyList())
        return raw
            .mapNotNull(::parseCountryLanguage)
            .filterNot { CollectiveLanguageCodes.isCollective(it.tag) }
    }

    override fun getDictionaryCountries(): List<CountryInfo> {
        return getAllCountries().filter { getCountryLanguages(it.numericCode).isNotEmpty() }
    }
}
```

- **Мотивация:** все три фильтра — в одном месте, у источника данных:
  миграция, форма и будущие подфичи получают уже очищенные данные.
  `getAllCountries` и `getLanguagesForCountry` остаются сырыми — для
  поиска флага по названию языка.

### 1.4. Юнит-тесты разбора и фильтра — новые файлы

`modules/library/flags/src/test/java/me/apomazkin/flags/CollectiveLanguageCodesTest.kt`

- **Было:** файла нет.
- **Будет:**

```kotlin
package me.apomazkin.flags

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Кейсы:
 * 1. семья — cau, tut, sit, inc, ber, bh → собирательный
 * 2. семья с регионом — "sla-RU" → собирательный (основа до дефиса)
 * 3. настоящие языки — ru, es-MX, haw, ktu, ms (макроязык) → не собирательный
 */
class CollectiveLanguageCodesTest {

    @Test
    fun `families are collective`() {
        listOf("cau", "tut", "sit", "inc", "ber", "bh").forEach { tag ->
            assertTrue(tag, CollectiveLanguageCodes.isCollective(tag))
        }
    }

    @Test
    fun `region does not hide a family`() {
        assertTrue(CollectiveLanguageCodes.isCollective("sla-RU"))
    }

    @Test
    fun `real languages and macrolanguages are not collective`() {
        listOf("ru", "es-MX", "haw", "ktu", "ms", "sw").forEach { tag ->
            assertFalse(tag, CollectiveLanguageCodes.isCollective(tag))
        }
    }
}
```

`modules/library/flags/src/test/java/me/apomazkin/flags/LanguageTagParserTest.kt`

- **Было:** в модуле только шаблонный `ExampleUnitTest`.
- **Будет:**

```kotlin
package me.apomazkin.flags

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Кейсы:
 * 1. без региона — "Russian (ru)" → ru / Russian
 * 2. с регионом — "Spanish (es-MX)" → es-MX / Spanish
 * 3. двойные скобки — "Occitan (post 1500) (oc)" → oc / "Occitan (post 1500)"
 * 4. трёхбуквенный код — "Hawaiian (haw)" → haw
 * 5. без скобок — "Klingon" → null
 * 6. пустые скобки — "()" → null (острова Буве и Херд в библиотеке)
 * 7. пустая строка — "" → null (Антарктида в библиотеке)
 */
class LanguageTagParserTest {

    @Test
    fun `plain language`() =
        assertEquals(CountryLanguage("ru", "Russian"), parseCountryLanguage("Russian (ru)"))

    @Test
    fun `regional variant kept`() =
        assertEquals(CountryLanguage("es-MX", "Spanish"), parseCountryLanguage("Spanish (es-MX)"))

    @Test
    fun `last parentheses win`() =
        assertEquals(
            CountryLanguage("oc", "Occitan (post 1500)"),
            parseCountryLanguage("Occitan (post 1500) (oc)"),
        )

    @Test
    fun `three letter code`() = assertEquals("haw", parseCountryLanguage("Hawaiian (haw)")?.tag)

    @Test
    fun `no parentheses`() = assertNull(parseCountryLanguage("Klingon"))

    @Test
    fun `empty parentheses`() = assertNull(parseCountryLanguage("()"))

    @Test
    fun `empty string`() = assertNull(parseCountryLanguage(""))
}
```

- **Мотивация:** формат строк — чужой; обновление библиотеки не должно
  сломать языки молча.

### 1.5. Тест по всему массиву библиотеки — новый файл

`modules/library/flags/src/androidTest/java/me/apomazkin/flags/CountryLanguagesDatasetTest.kt`

- **Было:** файла нет; androidTest-набора в модуле нет (`build.gradle.kts`
  модуля уже подключает `androidx.test.ext:junit` и espresso через
  конвеншн-плагин).
- **Будет:**

```kotlin
package me.apomazkin.flags

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * IS525: контракт данных библиотеки country-data — по ВСЕМ странам.
 *
 * Снимок 2026-10-05 (v1.5.4-alpha-1): 250 стран, 732 строки языков,
 * 729 разбираются; не разбираются ровно 3 пустые строки — Антарктида (AQ),
 * Буве (BV), Херд (HM). Собирательных кодов 6 в 3 странах, ни один не
 * основной. Коды — `xx` или `xxx`, необязательный регион из двух заглавных
 * букв. Тест ловит обновление библиотеки, которое сломает эти допущения.
 */
@RunWith(AndroidJUnit4::class)
class CountryLanguagesDatasetTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val provider = CountryProviderImpl(context)

    private val tagShape = Regex("^[a-z]{2,3}(-[A-Z]{2})?$")

    @Test
    fun everyNonEmptyLanguageStringParses_andTagsAreWellFormed() {
        val unparsed = mutableListOf<String>()
        val badTags = mutableListOf<String>()
        provider.getAllCountries().forEach { country ->
            provider.getLanguagesForCountry(country.numericCode).forEach { raw ->
                val parsed = parseCountryLanguage(raw)
                if (parsed == null) {
                    if (raw.isNotBlank() && raw != "()") unparsed += "${country.alpha2}: $raw"
                } else if (!tagShape.matches(parsed.tag)) {
                    badTags += "${country.alpha2}: $raw"
                }
            }
        }
        assertEquals("непустые строки без кода: $unparsed", emptyList<String>(), unparsed)
        assertEquals("коды неожиданной формы: $badTags", emptyList<String>(), badTags)
    }

    @Test
    fun countryLanguages_containNoCollectiveCodes() {
        val collective = provider.getAllCountries().flatMap { country ->
            provider.getCountryLanguages(country.numericCode)
                .filter { CollectiveLanguageCodes.isCollective(it.tag) }
                .map { "${country.alpha2}: ${it.tag}" }
        }

        assertEquals(emptyList<String>(), collective)
    }

    @Test
    fun dictionaryCountries_areAllButUninhabited() {
        val all = provider.getAllCountries()
            .map { it.alpha2 }
            .toSet()
        val forDictionary = provider.getDictionaryCountries()
            .map { it.alpha2 }
            .toSet()

        assertEquals(247, forDictionary.size)
        assertEquals(setOf("AQ", "BV", "HM"), all - forDictionary)
    }

    @Test
    fun mexicoMainLanguage_isMexicanSpanish() {
        val languages = provider.getCountryLanguages(484)

        assertTrue(languages.isNotEmpty())
        assertEquals("es-MX", languages.first().tag)
    }

    /** Неизвестный библиотеке код (в v1.5.4 `World.getLanguagesFrom` на него бросает NPE). */
    @Test
    fun unknownCountryCode_givesNoLanguages_withoutThrowing() {
        assertEquals(emptyList<CountryLanguage>(), provider.getCountryLanguages(999_999))
    }
}
```

- **Мотивация:** юзер (2026-10-05): «надо на библиотеке проверить всё,
  каждый язык каждой страны». Тест идёт в CI на эмуляторе (после 1.6) и
  поймает обновление библиотеки, которое изменит формат строк, состав
  стран без языков или добавит семью основным языком.

### 1.6. Раннер и CI для androidTest модуля `flags`

`modules/library/flags/build.gradle.kts`

- **Было:** раннер инструментальных тестов не задан — тест 1.5 не
  запустится (в `core/core-db-impl/build.gradle.kts:30` он задан явно).

```kotlin
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
```

- **Будет:**

```kotlin
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
```

`.github/workflows/on_pull_request.yml`, строка 69

- **Было:**

```yaml
          script: ./gradlew :core:core-db-impl:connectedDebugAndroidTest :modules:screen:quiz:chat:connectedDebugAndroidTest --continue
```

- **Будет:**

```yaml
          script: ./gradlew :core:core-db-impl:connectedDebugAndroidTest :modules:screen:quiz:chat:connectedDebugAndroidTest :modules:library:flags:connectedDebugAndroidTest --continue
```

- **Мотивация:** без раннера тест не стартует, без строки в CI он не
  защищает от обновления библиотеки. Зависимости `androidx.test.ext:junit`
  и espresso в модуле уже есть.

---

## 2. База данных

### 2.1. Источник языков по умолчанию — новый файл

`core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/DictionaryLanguageDefaults.kt`

- **Было:** файла нет. Слой базы ничего не знает ни о языках стран, ни о
  языке телефона.
- **Будет:**

```kotlin
package me.apomazkin.core_db_api.entity

/**
 * IS525: языки словаря по умолчанию — для миграции 15→16. Данные (языки
 * стран, язык телефона) живут в app; слой базы получает их этим
 * интерфейсом тем же путём, что [ReservedGroupNames]. Оба метода всегда
 * возвращают код языка.
 */
interface DictionaryLanguageDefaults {

    /** Изучаемый язык для страны флага; [numericCode] == null — словарь без флага. */
    fun learningLanguageFor(numericCode: Int?): String

    /** Язык перевода. */
    fun translationLanguage(): String
}
```

- **Мотивация:** миграция заполняет существующие словари, а данные для
  этого — в `app`. Интерфейс развязывает слои; в тестах подставляется
  заглушка.

### 2.2. Поля словаря

`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/DictionaryDb.kt`

- **Было:**

```kotlin
import androidx.room.Entity
```

```kotlin
        val addDate: Date,
        val changeDate: Date? = null,
)

fun DictionaryDb.toApiEntity() = DictionaryApiEntity(
        id = id ?: throw IllegalArgumentException("DictionaryDb id is null"),
        numericCode = numericCode,
        name = name,
        addDate = addDate,
        changeDate = changeDate,
)
```

- **Будет:**

```kotlin
import androidx.room.ColumnInfo
import androidx.room.Entity
```

```kotlin
        val addDate: Date,
        val changeDate: Date? = null,
        // IS525: языки словаря — код с региональным вариантом («es-MX», «ru»),
        // обязательные. DEFAULT 'en' в схеме — страховка SQLite (NOT NULL
        // без DEFAULT через ALTER TABLE не добавить) и зеркало правила «нет
        // флага — английский»; значение по умолчанию в Kotlin — для тестовых
        // конструкций, код приложения пишет языки явно (DictionaryApi).
        @ColumnInfo(name = "learning_language", defaultValue = "en")
        val learningLanguage: String = "en",
        @ColumnInfo(name = "translation_language", defaultValue = "en")
        val translationLanguage: String = "en",
)

fun DictionaryDb.toApiEntity() = DictionaryApiEntity(
        id = id ?: throw IllegalArgumentException("DictionaryDb id is null"),
        numericCode = numericCode,
        name = name,
        addDate = addDate,
        changeDate = changeDate,
        learningLanguage = learningLanguage,
        translationLanguage = translationLanguage,
)
```

- **Мотивация:** решение юзера — языки обязательные (`String`, не
  `String?`). `defaultValue = "en"` в `@ColumnInfo` — чтобы схема `16.json`
  совпала с колонками, которые создаст миграция. Значение по умолчанию в
  конструкторе оставляет семь конструкций `DictionaryDb(name = …, addDate
  = …)` в androidTest без изменений.

### 2.3. Миграция 15 → 16 — новый файл

`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/migrations/Migration_015_to_016.kt`

- **Было:** файла нет; последняя миграция — `object Migration_014_to_015`.
- **Будет:**

```kotlin
package me.apomazkin.core_db_impl.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import me.apomazkin.core_db_api.entity.DictionaryLanguageDefaults

/**
 * IS525 migration M15 → M16: языки словаря.
 *
 * Шаги:
 *  1. две обязательные колонки `learning_language`, `translation_language`
 *     (TEXT NOT NULL DEFAULT 'en' — иначе ALTER TABLE в SQLite невозможен);
 *  2. изучаемый язык: по каждому коду страны, что есть в таблице, —
 *     [DictionaryLanguageDefaults.learningLanguageFor]; словари без флага —
 *     `learningLanguageFor(null)` (английский);
 *  3. язык перевода всем — [DictionaryLanguageDefaults.translationLanguage].
 *
 * В отличие от прежних миграций — класс, не object: правила языков
 * приходят из app через конструктор (см. RoomModule).
 */
class Migration_015_to_016(
    private val languageDefaults: DictionaryLanguageDefaults,
) : Migration(15, 16) {

    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE `dictionaries` ADD COLUMN `learning_language` TEXT NOT NULL DEFAULT 'en'",
        )
        connection.execSQL(
            "ALTER TABLE `dictionaries` ADD COLUMN `translation_language` TEXT NOT NULL DEFAULT 'en'",
        )
        connection.fillLearningLanguages()
        connection.fillTranslationLanguage()
    }

    private fun SQLiteConnection.fillLearningLanguages() {
        val numericCodes = mutableListOf<Long>()
        prepare("SELECT DISTINCT numericCode FROM dictionaries WHERE numericCode IS NOT NULL").use { stmt ->
            while (stmt.step()) numericCodes += stmt.getLong(0)
        }
        numericCodes.forEach { code ->
            // Правила приходят из app; их ошибка не должна ронять открытие
            // базы (исключение в migrate = крэш на каждом старте): любое
            // исключение = «страна без языка» → английский.
            val language = runCatching { languageDefaults.learningLanguageFor(code.toInt()) }
                .getOrElse { languageDefaults.learningLanguageFor(null) }
            prepare("UPDATE dictionaries SET learning_language = ? WHERE numericCode = ?").use { stmt ->
                stmt.bindText(1, language)
                stmt.bindLong(2, code)
                stmt.step()
            }
        }
        prepare("UPDATE dictionaries SET learning_language = ? WHERE numericCode IS NULL").use { stmt ->
            stmt.bindText(1, languageDefaults.learningLanguageFor(null))
            stmt.step()
        }
    }

    private fun SQLiteConnection.fillTranslationLanguage() {
        prepare("UPDATE dictionaries SET translation_language = ?").use { stmt ->
            stmt.bindText(1, languageDefaults.translationLanguage())
            stmt.step()
        }
    }
}
```

- **Мотивация:** существующие словари получают языки в той же транзакции,
  что и колонки, — отдельного автозаполнения нет, окна «колонки есть,
  языков нет» нет. Импорт старого бэкапа проходит ту же миграцию.

### 2.4. Версия базы

`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/Database.kt`

- **Было:**

```kotlin
    version = 15
)
```

- **Будет:**

```kotlin
    version = 16
)
```

- **Мотивация:** новая схема. `16.json` генерируется сборкой и коммитится.

### 2.5. Передача правил в слой базы и регистрация миграции

`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/di/RoomComponent.kt`

- **Было:**

```kotlin
            @BindsInstance reservedGroupNames: ReservedGroupNames,
        ): RoomComponent
    }

    companion object {

        lateinit var roomComponent: RoomComponent

        fun get(
            context: Context,
            logger: LexemeLogger,
            reservedGroupNames: ReservedGroupNames,
        ): RoomComponent {
            if (!::roomComponent.isInitialized) {
                synchronized(RoomComponent::class) {
                    if (!::roomComponent.isInitialized) {
                        roomComponent = DaggerRoomComponent
                            .factory()
                            .create(context, logger, reservedGroupNames)
                    }
                }
            }
            return roomComponent
        }
    }
```

- **Будет:**

```kotlin
            @BindsInstance reservedGroupNames: ReservedGroupNames,
            // IS525: правила языков словаря по умолчанию — для миграции 15→16.
            @BindsInstance dictionaryLanguageDefaults: DictionaryLanguageDefaults,
        ): RoomComponent
    }

    companion object {

        lateinit var roomComponent: RoomComponent

        fun get(
            context: Context,
            logger: LexemeLogger,
            reservedGroupNames: ReservedGroupNames,
            dictionaryLanguageDefaults: DictionaryLanguageDefaults,
        ): RoomComponent {
            if (!::roomComponent.isInitialized) {
                synchronized(RoomComponent::class) {
                    if (!::roomComponent.isInitialized) {
                        roomComponent = DaggerRoomComponent
                            .factory()
                            .create(context, logger, reservedGroupNames, dictionaryLanguageDefaults)
                    }
                }
            }
            return roomComponent
        }
    }
```

(плюс `import me.apomazkin.core_db_api.entity.DictionaryLanguageDefaults`)

`core/core-db/src/main/java/me/apomazkin/core_db/di/CoreDbComponent.kt`

- **Было:**

```kotlin
        fun init(
            context: Context,
            logger: LexemeLogger,
            reservedGroupNames: ReservedGroupNames,
        ): CoreDbComponent {
            if (!::coreDbComponent.isInitialized) {
                synchronized(CoreDbComponent::class) {
                    if (!::coreDbComponent.isInitialized) {
                        coreDbComponent = DaggerCoreDbComponent
                            .factory()
                            .create(RoomComponent.get(context, logger, reservedGroupNames))
                    }
                }
            }
            return coreDbComponent
        }
```

- **Будет:**

```kotlin
        fun init(
            context: Context,
            logger: LexemeLogger,
            reservedGroupNames: ReservedGroupNames,
            dictionaryLanguageDefaults: DictionaryLanguageDefaults,
        ): CoreDbComponent {
            if (!::coreDbComponent.isInitialized) {
                synchronized(CoreDbComponent::class) {
                    if (!::coreDbComponent.isInitialized) {
                        val roomComponent = RoomComponent.get(
                            context = context,
                            logger = logger,
                            reservedGroupNames = reservedGroupNames,
                            dictionaryLanguageDefaults = dictionaryLanguageDefaults,
                        )
                        coreDbComponent = DaggerCoreDbComponent
                            .factory()
                            .create(roomComponent)
                    }
                }
            }
            return coreDbComponent
        }
```

(плюс импорт `DictionaryLanguageDefaults`)

`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/di/module/RoomModule.kt`

- **Было:**

```kotlin
import me.apomazkin.core_db_impl.room.migrations.Migration_014_to_015
import me.apomazkin.logger.LexemeLogger
```

```kotlin
 * Текущая схема — v15. Миграции:
```

```kotlin
 * - M14→M15 (`Migration_014_to_015.kt`) — IS515: 6 новых builtin-опций
 *   «Части речи» + перестановка позиций; схема не меняется, только данные.
 *   Регистрация ОБЯЗАТЕЛЬНА: без неё fallback ниже молча сотрёт БД.
```

```kotlin
    fun provideDatabase(context: Context, logger: LexemeLogger): Database {
        return Room.databaseBuilder<Database>(
            context = context,
            name = DATABASE_NAME,
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .addMigrations(
                Migration_011_to_012,
                Migration_012_to_013,
                Migration_013_to_014,
                Migration_014_to_015,
            )
```

```kotlin
                                "All tables dropped and recreated from current schema (v15). User data lost. " +
```

- **Будет:**

```kotlin
import me.apomazkin.core_db_impl.room.migrations.Migration_014_to_015
import me.apomazkin.core_db_impl.room.migrations.Migration_015_to_016
import me.apomazkin.core_db_api.entity.DictionaryLanguageDefaults
import me.apomazkin.logger.LexemeLogger
```

```kotlin
 * Текущая схема — v16. Миграции:
```

```kotlin
 * - M14→M15 (`Migration_014_to_015.kt`) — IS515: 6 новых builtin-опций
 *   «Части речи» + перестановка позиций; схема не меняется, только данные.
 * - M15→M16 (`Migration_015_to_016.kt`) — IS525: языки словаря
 *   (`learning_language`, `translation_language`, NOT NULL); существующие
 *   словари заполняются по правилам из app ([DictionaryLanguageDefaults]).
 *   Регистрация ОБЯЗАТЕЛЬНА: без неё fallback ниже молча сотрёт БД.
```

```kotlin
    fun provideDatabase(
        context: Context,
        logger: LexemeLogger,
        dictionaryLanguageDefaults: DictionaryLanguageDefaults,
    ): Database {
        return Room.databaseBuilder<Database>(
            context = context,
            name = DATABASE_NAME,
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .addMigrations(
                Migration_011_to_012,
                Migration_012_to_013,
                Migration_013_to_014,
                Migration_014_to_015,
                Migration_015_to_016(dictionaryLanguageDefaults),
            )
```

```kotlin
                                "All tables dropped and recreated from current schema (v16). User data lost. " +
```

- **Мотивация:** тот же путь, что у имён групп, — новых механизмов нет.
  Без регистрации миграции Room уйдёт в разрушающий откат и сотрёт базу.

### 2.6. Запрос обновления словаря

`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/WordDao.kt`

- **Было:**

```kotlin
    @Query("UPDATE dictionaries SET name = :name, numericCode = :numericCode, changeDate = :changeDate WHERE id = :id")
    suspend fun updateDictionary(id: Long, name: String, numericCode: Int?, changeDate: Long)
```

- **Будет:**

```kotlin
    @Query(
        "UPDATE dictionaries SET name = :name, numericCode = :numericCode, " +
            "learning_language = :learningLanguage, translation_language = :translationLanguage, " +
            "changeDate = :changeDate WHERE id = :id"
    )
    suspend fun updateDictionary(
        id: Long,
        name: String,
        numericCode: Int?,
        learningLanguage: String,
        translationLanguage: String,
        changeDate: Long,
    )
```

- **Мотивация:** сохранение формы пишет оба языка. Вставка не меняется —
  языки едут в самой сущности.

### 2.7. Тест миграции — новый файл

`core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/MigrationFrom15to16.kt`

- **Было:** файла нет; последний тест миграции — `MigrationFrom14to15`.
- **Будет:**

```kotlin
package me.apomazkin.core_db_impl.room

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import me.apomazkin.core_db_api.entity.DictionaryLanguageDefaults
import me.apomazkin.core_db_impl.room.migrations.Migration_011_to_012
import me.apomazkin.core_db_impl.room.migrations.Migration_012_to_013
import me.apomazkin.core_db_impl.room.migrations.Migration_013_to_014
import me.apomazkin.core_db_impl.room.migrations.Migration_014_to_015
import me.apomazkin.core_db_impl.room.migrations.Migration_015_to_016
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * IS525 migration test M15 → M16: языки словаря.
 *
 * Кейсы:
 *  - A словарь с флагом получает язык страны (заглушка: 484 → es-MX);
 *  - B словарь без флага получает английский;
 *  - C язык перевода у всех — из заглушки, данные словарей живы;
 *  - D chained 11→16 — боевой путь старой установки;
 *  - E правила бросают на код страны — миграция не падает, словарь
 *    получает английский.
 */
@RunWith(AndroidJUnit4::class)
class MigrationFrom15to16 {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val dbFile: File = instrumentation.targetContext.getDatabasePath(DB_NAME)

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        instrumentation = instrumentation,
        file = dbFile,
        driver = BundledSQLiteDriver(),
        databaseClass = Database::class,
    )

    @After
    fun cleanUp() {
        listOf("", "-shm", "-wal", "-journal").forEach { suffix ->
            File(dbFile.path + suffix).takeIf { it.exists() }?.delete()
        }
    }

    /** Заглушка правил: Мексика → es-MX, другие страны → xx-<код>, без флага → en; перевод → ru. */
    private val defaults = object : DictionaryLanguageDefaults {
        override fun learningLanguageFor(numericCode: Int?): String = when (numericCode) {
            null -> "en"
            484 -> "es-MX"
            else -> "xx-$numericCode"
        }

        override fun translationLanguage(): String = "ru"
    }

    private fun migrateFrom15(): SQLiteConnection =
        helper.runMigrationsAndValidate(16, listOf(Migration_015_to_016(defaults)))

    private fun migrateFrom11(): SQLiteConnection =
        helper.runMigrationsAndValidate(
            16,
            listOf(
                Migration_011_to_012,
                Migration_012_to_013,
                Migration_013_to_014,
                Migration_014_to_015,
                Migration_015_to_016(defaults),
            ),
        )

    private fun SQLiteConnection.insertDictionary(id: Long, numericCode: Int?, name: String) {
        val code = numericCode?.toString() ?: "NULL"
        execSQL("INSERT INTO dictionaries (id, numericCode, name, addDate) VALUES ($id, $code, '$name', 0)")
    }

    private fun SQLiteConnection.languagesOf(id: Long): Pair<String, String> {
        prepare("SELECT learning_language, translation_language FROM dictionaries WHERE id = $id").use { stmt ->
            stmt.step()
            return stmt.getText(0) to stmt.getText(1)
        }
    }

    // === Case A — флаг → язык страны ===
    @Test
    fun caseA_dictionaryWithFlag_getsCountryLanguage() {
        helper.createDatabase(15).use { v15 ->
            v15.insertDictionary(id = 1, numericCode = 484, name = "MX")
        }
        val v16 = migrateFrom15()

        assertEquals("es-MX" to "ru", v16.languagesOf(1))
        v16.close()
    }

    // === Case B — без флага → английский ===
    @Test
    fun caseB_dictionaryWithoutFlag_getsEnglish() {
        helper.createDatabase(15).use { v15 ->
            v15.insertDictionary(id = 1, numericCode = null, name = "Bio")
        }
        val v16 = migrateFrom15()

        assertEquals("en" to "ru", v16.languagesOf(1))
        v16.close()
    }

    // === Case C — несколько словарей, данные живы ===
    @Test
    fun caseC_severalDictionaries_eachGetsOwnLanguages_dataAlive() {
        helper.createDatabase(15).use { v15 ->
            v15.insertDictionary(id = 1, numericCode = 484, name = "MX")
            v15.insertDictionary(id = 2, numericCode = 826, name = "GB")
            v15.insertDictionary(id = 3, numericCode = null, name = "Bio")
        }
        val v16 = migrateFrom15()

        assertEquals("es-MX" to "ru", v16.languagesOf(1))
        assertEquals("xx-826" to "ru", v16.languagesOf(2))
        assertEquals("en" to "ru", v16.languagesOf(3))
        v16.prepare("SELECT name FROM dictionaries WHERE id = 2").use { stmt ->
            stmt.step()
            assertEquals("GB", stmt.getText(0))
        }
        v16.close()
    }

    // === Case D — chained 11→16 ===
    @Test
    fun caseD_chained11to16_languagesFilled() {
        helper.createDatabase(11).use { v11 ->
            v11.execSQL("INSERT INTO dictionaries (id, numericCode, name, addDate) VALUES (1, 484, 'MX', 0)")
            v11.execSQL("INSERT INTO words (id, dictionary_id, value, add_date) VALUES (1, 1, 'gato', 0)")
        }
        val v16 = migrateFrom11()

        assertEquals("es-MX" to "ru", v16.languagesOf(1))
        v16.prepare("SELECT COUNT(*) FROM words WHERE dictionary_id = 1").use { stmt ->
            stmt.step()
            assertEquals(1L, stmt.getLong(0))
        }
        v16.close()
    }

    // === Case E — правила бросают → английский, миграция жива ===
    @Test
    fun caseE_rulesThrowOnCountry_migrationSurvives_englishFallback() {
        val throwing = object : DictionaryLanguageDefaults {
            override fun learningLanguageFor(numericCode: Int?): String =
                if (numericCode == null) "en" else throw NullPointerException("unknown country $numericCode")

            override fun translationLanguage(): String = "ru"
        }
        helper.createDatabase(15).use { v15 ->
            v15.insertDictionary(id = 1, numericCode = 999_999, name = "Old")
        }
        val v16 = helper.runMigrationsAndValidate(16, listOf(Migration_015_to_016(throwing)))

        assertEquals("en" to "ru", v16.languagesOf(1))
        v16.close()
    }

    companion object {
        private const val DB_NAME = "migration-test-16"
    }
}
```

- **Мотивация:** ошибка миграции — потеря данных юзера;
  `runMigrationsAndValidate` сверяет результат со схемой `16.json`.

---

## 3. API слоя данных

### 3.1. Сущность API

`core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/DictionaryApiEntity.kt`

- **Было:**

```kotlin
    val changeDate: Date? = null,
    val deleteDate: Date? = null,
)
```

- **Будет:**

```kotlin
    val changeDate: Date? = null,
    val deleteDate: Date? = null,
    /**
     * IS525: изучаемый язык — код с региональным вариантом («es-MX»).
     * Обязателен; значение по умолчанию — только для тестовых конструкций,
     * из базы приходит всегда.
     */
    val learningLanguage: String = "en",
    /** IS525: язык перевода. Обязателен; значение по умолчанию — для тестов. */
    val translationLanguage: String = "en",
)
```

- **Мотивация:** языки обязательны на всём пути от базы до формы.
  Значения по умолчанию оставляют девятнадцать конструкций сущности в
  восьми тестовых файлах, не связанных с языками, без изменений.

### 3.2. Контракт `DictionaryApi`

`core/core-db-api/src/main/java/me/apomazkin/core_db_api/CoreDbApi.kt`

- **Было:**

```kotlin
    interface DictionaryApi {
        suspend fun addDictionary(name: String, numericCode: Int? = null): Long
        suspend fun getDictionary(numericCode: Int): DictionaryApiEntity?
        suspend fun getDictionaryById(id: Long): DictionaryApiEntity?
        suspend fun getDictionaryList(): List<DictionaryApiEntity>
        suspend fun updateDictionary(id: Long, name: String, numericCode: Int?)
        suspend fun deleteDictionary(id: Long)
        fun flowDictionaryList(): Flow<List<DictionaryApiEntity>>
    }
```

- **Будет:**

```kotlin
    interface DictionaryApi {
        /** IS525: языки обязательны — словарь без них создать нельзя. */
        suspend fun addDictionary(
            name: String,
            numericCode: Int?,
            learningLanguage: String,
            translationLanguage: String,
        ): Long
        suspend fun getDictionary(numericCode: Int): DictionaryApiEntity?
        suspend fun getDictionaryById(id: Long): DictionaryApiEntity?
        suspend fun getDictionaryList(): List<DictionaryApiEntity>
        suspend fun updateDictionary(
            id: Long,
            name: String,
            numericCode: Int?,
            learningLanguage: String,
            translationLanguage: String,
        )
        suspend fun deleteDictionary(id: Long)
        fun flowDictionaryList(): Flow<List<DictionaryApiEntity>>
    }
```

- **Мотивация:** вызывающий обязан назвать языки; значений по умолчанию
  нет намеренно — иначе правка названия молча перезаписала бы языки.

### 3.3. Реализация

`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/CoreDbApiImpl.kt`

- **Было:**

```kotlin
        override suspend fun addDictionary(name: String, numericCode: Int?): Long {
            val now = Date(System.currentTimeMillis())
            return database.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    val dictId = wordDao.addDictionary(
                        DictionaryDb(
                            numericCode = numericCode,
                            name = name,
                            addDate = now,
                        )
                    )
```

```kotlin
        override suspend fun updateDictionary(id: Long, name: String, numericCode: Int?) {
            wordDao.updateDictionary(id, name, numericCode, System.currentTimeMillis())
        }
```

- **Будет:**

```kotlin
        override suspend fun addDictionary(
            name: String,
            numericCode: Int?,
            learningLanguage: String,
            translationLanguage: String,
        ): Long {
            val now = Date(System.currentTimeMillis())
            return database.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    val dictId = wordDao.addDictionary(
                        DictionaryDb(
                            numericCode = numericCode,
                            name = name,
                            addDate = now,
                            learningLanguage = learningLanguage,
                            translationLanguage = translationLanguage,
                        )
                    )
```

```kotlin
        override suspend fun updateDictionary(
            id: Long,
            name: String,
            numericCode: Int?,
            learningLanguage: String,
            translationLanguage: String,
        ) {
            wordDao.updateDictionary(
                id = id,
                name = name,
                numericCode = numericCode,
                learningLanguage = learningLanguage,
                translationLanguage = translationLanguage,
                changeDate = System.currentTimeMillis(),
            )
        }
```

- **Мотивация:** исполнение контракта; транзакция создания словаря с
  засевом встроенных компонентов не меняется.

### 3.4. Существующие вызовы в androidTest

Файлы `core/core-db-impl/src/androidTest/.../room/`: `Is486DataLayerTest.kt`
(13 вызовов), `CaptionSuggestionsDaoTest.kt` (1), `CascadeExecutorTest.kt`
(1), `Phase3ConstructorDataTest.kt` (1).

- **Было (образец, все 16 одинаковые):**

```kotlin
        val d1 = dictionaryApi.addDictionary("ES", null)
```

- **Будет:**

```kotlin
        val d1 = dictionaryApi.addDictionary("ES", null, "es", "ru")
```

- **Мотивация:** языки в API обязательны (3.2); правка механическая,
  смысл тестов не меняется.

### 3.5. Тест API — новый файл

`core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/DictionaryLanguagesApiTest.kt`

- **Было:** файла нет.
- **Будет:**

```kotlin
package me.apomazkin.core_db_impl.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import me.apomazkin.core_db_impl.CoreDbApiImpl
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * IS525: языки словаря через DictionaryApi.
 *
 * Кейсы:
 *  - A создание с языками → читаются обратно;
 *  - B обновление меняет оба языка;
 *  - C обновление названия без смены языков их не трогает (языки
 *    передаются явно те же).
 */
@RunWith(AndroidJUnit4::class)
class DictionaryLanguagesApiTest {

    private lateinit var db: Database
    private lateinit var dictionaryApi: CoreDbApiImpl.DictionaryApiImpl

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder<Database>(context)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        dictionaryApi = CoreDbApiImpl.DictionaryApiImpl(
            database = db,
            wordDao = db.wordDao(),
            componentTypeDao = db.componentTypeDao(),
            componentOptionDao = db.componentOptionDao(),
            logger = RecordingLogger(),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun caseA_addWithLanguages_readBack() = runBlocking {
        val id = dictionaryApi.addDictionary("MX", 484, "es-MX", "ru")

        val entity = dictionaryApi.getDictionaryById(id)!!
        assertEquals("es-MX", entity.learningLanguage)
        assertEquals("ru", entity.translationLanguage)
    }

    @Test
    fun caseB_updateChangesBothLanguages() = runBlocking {
        val id = dictionaryApi.addDictionary("MX", 484, "es-MX", "ru")

        dictionaryApi.updateDictionary(id, "MX", 484, "es", "en")

        val entity = dictionaryApi.getDictionaryById(id)!!
        assertEquals("es", entity.learningLanguage)
        assertEquals("en", entity.translationLanguage)
    }

    @Test
    fun caseC_renameKeepsLanguagesPassedExplicitly() = runBlocking {
        val id = dictionaryApi.addDictionary("MX", 484, "es-MX", "ru")

        dictionaryApi.updateDictionary(id, "Mexico", 484, "es-MX", "ru")

        val entity = dictionaryApi.getDictionaryById(id)!!
        assertEquals("Mexico", entity.name)
        assertEquals("es-MX", entity.learningLanguage)
        assertEquals("ru", entity.translationLanguage)
    }
}
```

(`RecordingLogger` — `internal class` в `Is486DataLayerTest.kt`, тот же
пакет и модуль.)

- **Мотивация:** закрепить контракт на реальной базе, а не на моках.

---

## 4. Правила языков и передача их в базу

### 4.1. Правила — новый файл

`app/src/main/java/me/apomazkin/polytrainer/di/module/dictionary/DictionaryLanguageRules.kt`

- **Было:** файла нет; правил «какой язык у словаря по умолчанию» в коде
  нет.
- **Будет:**

```kotlin
package me.apomazkin.polytrainer.di.module.dictionary

import me.apomazkin.core_db_api.entity.DictionaryLanguageDefaults
import me.apomazkin.flags.CountryProvider
import java.util.Locale

/**
 * IS525: языки словаря по умолчанию — одно правило для миграции 15→16
 * ([DictionaryLanguageDefaults]) и для формы словаря:
 *  - изучаемый: основной язык страны флага; флага нет или у страны нет
 *    разобранного языка — английский;
 *  - перевод: язык телефона без региона («ru-RU» → «ru»).
 *
 * [countryProvider] ленивый: библиотека флагов поднимается, только когда
 * правило действительно спросили про страну (миграция идёт один раз).
 */
class DictionaryLanguageRules(
    private val countryProvider: Lazy<CountryProvider>,
    private val deviceLocale: () -> Locale = Locale::getDefault,
) : DictionaryLanguageDefaults {

    override fun learningLanguageFor(numericCode: Int?): String {
        if (numericCode == null) return NO_FLAG_LANGUAGE
        return countryProvider.value
            .getCountryLanguages(numericCode)
            .firstOrNull()
            ?.tag
            ?: NO_FLAG_LANGUAGE
    }

    override fun translationLanguage(): String {
        val deviceTag = deviceLocale().toLanguageTag()
        val language = deviceTag.substringBefore('-')
        return if (language.isEmpty() || language == "und") NO_FLAG_LANGUAGE else language
    }

    companion object {
        /** Язык словаря без флага (решение юзера, IS525). */
        const val NO_FLAG_LANGUAGE = "en"
    }
}
```

- **Мотивация:** одно правило на миграцию и на форму — старые и новые
  словари получают языки одинаково. `toLanguageTag()` вместо `language`:
  `Locale.language` отдаёт устаревшие коды (`iw` вместо `he`).

### 4.2. Передача при запуске

`app/src/main/java/me/apomazkin/polytrainer/App.kt`

- **Было:**

```kotlin
                coreDbProvider = DaggerAppComponent_CoreDbDependenciesComponent
                    //TODO kilg 13.05.2020 06:39 заменить билдер на фабрику
                    .builder()
                    .coreDbProvider(CoreDbComponent.init(this, logger, reservedGroupNames()))
                    .build(),
```

- **Будет:**

```kotlin
                coreDbProvider = DaggerAppComponent_CoreDbDependenciesComponent
                    //TODO kilg 13.05.2020 06:39 заменить билдер на фабрику
                    .builder()
                    .coreDbProvider(
                        CoreDbComponent.init(
                            context = this,
                            logger = logger,
                            reservedGroupNames = reservedGroupNames(),
                            dictionaryLanguageDefaults = DictionaryLanguageRules(
                                countryProvider = lazy { CountryProviderImpl(this) },
                            ),
                        ),
                    )
                    .build(),
```

(плюс импорты `DictionaryLanguageRules`, `me.apomazkin.flags.CountryProviderImpl`)

- **Мотивация:** миграция получает правила до первого открытия базы.
  Библиотека флагов создаётся лениво — на обычном запуске миграции нет и
  лишней работы нет.

### 4.3. Правила в графе Dagger

`app/src/main/java/me/apomazkin/polytrainer/di/module/flags/CountryProviderModule.kt`

- **Было:**

```kotlin
@Module
class CountryProviderModule {

    @Singleton
    @Provides
    fun provideCountryProvider(context: Context): CountryProvider =
        CountryProviderImpl(context)
}
```

- **Будет:**

```kotlin
@Module
class CountryProviderModule {

    @Singleton
    @Provides
    fun provideCountryProvider(context: Context): CountryProvider =
        CountryProviderImpl(context)

    /** IS525: те же правила языков, что получила миграция, — для юзкейса словаря. */
    @Provides
    fun provideDictionaryLanguageRules(countryProvider: CountryProvider): DictionaryLanguageRules =
        DictionaryLanguageRules(countryProvider = lazyOf(countryProvider))
}
```

(плюс импорт `me.apomazkin.polytrainer.di.module.dictionary.DictionaryLanguageRules`)

- **Мотивация:** юзкейсу словаря нужны те же правила; провайдер страны
  внутри графа — общий.

### 4.4. Тест правил — новый файл

`app/src/test/java/me/apomazkin/polytrainer/di/module/dictionary/DictionaryLanguageRulesTest.kt`

- **Было:** файла нет.
- **Будет:**

```kotlin
package me.apomazkin.polytrainer.di.module.dictionary

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import me.apomazkin.flags.CountryLanguage
import me.apomazkin.flags.CountryProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/**
 * Кейсы:
 * 1. страна с языками → основной (первый) код
 * 2. страны нет (null) → en, провайдер не трогается
 * 3. у страны нет разобранных языков → en
 * 4. язык перевода — язык телефона без региона
 * 5. устаревший код локали (iw) → современный (he)
 */
class DictionaryLanguageRulesTest {

    private val countryProvider: CountryProvider = mockk()
    private var locale = Locale("ru", "RU")
    private val rules = DictionaryLanguageRules(
        countryProvider = lazyOf(countryProvider),
        deviceLocale = { locale },
    )

    @Test
    fun `country main language first`() {
        every { countryProvider.getCountryLanguages(484) } returns listOf(
            CountryLanguage("es-MX", "Spanish"),
            CountryLanguage("yua", "Yucateco"),
        )

        assertEquals("es-MX", rules.learningLanguageFor(484))
    }

    @Test
    fun `no flag gives english without touching provider`() {
        assertEquals("en", rules.learningLanguageFor(null))
        verify(exactly = 0) { countryProvider.getCountryLanguages(any()) }
    }

    @Test
    fun `country without parsed languages gives english`() {
        every { countryProvider.getCountryLanguages(10) } returns emptyList() // Антарктида

        assertEquals("en", rules.learningLanguageFor(10))
    }

    @Test
    fun `translation is device language without region`() {
        assertEquals("ru", rules.translationLanguage())
    }

    @Test
    fun `legacy locale code is modernised`() {
        locale = Locale("iw")

        assertEquals("he", rules.translationLanguage())
    }
}
```

- **Мотивация:** по этим правилам заполняются словари юзера при
  обновлении — ошибку заметят поздно.

---

## 5. Юзкейс словаря

### 5.1. Модель языка — новый файл

`modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/model/LanguageItem.kt`

- **Было:** файла нет.
- **Будет:**

```kotlin
package me.apomazkin.dictionary.model

/**
 * Язык словаря: [tag] — код с региональным вариантом («es-MX», «ru»),
 * [name] — название на языке интерфейса («Испанский (Мексика)»).
 */
data class LanguageItem(
    val tag: String,
    val name: String,
) {
    companion object {
        /**
         * Запасное значение для состояния формы (превью, тесты). В
         * приложении состояние с первого кадра получает языки из use case
         * (см. DictionaryFormAssembly).
         */
        val Fallback = LanguageItem(tag = "en", name = "English")
    }
}

/** Языки новой формы: без флага и перевод (IS525). */
data class LanguageDefaults(
    val noFlag: LanguageItem,
    val translation: LanguageItem,
)
```

- **Мотивация:** форма работает с готовой парой «код + название», без
  разбора строк и без обращения к настройкам телефона.

### 5.2. Словарь для формы

`modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/model/DictionaryItem.kt`

- **Было:**

```kotlin
data class DictionaryItem(
    val id: Long,
    val name: String,
    val numericCode: Int?,
)
```

- **Будет:**

```kotlin
data class DictionaryItem(
    val id: Long,
    val name: String,
    val numericCode: Int?,
    val learningLanguage: LanguageItem,
    val translationLanguage: LanguageItem,
)
```

- **Мотивация:** форма редактирования получает сохранённые языки вместе
  со словарём.

### 5.3. Языки в элементе флага

`modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/model/CountryFlagItem.kt`

- **Было:**

```kotlin
data class CountryFlagItem(
    val numericCode: Int,
    val countryName: String,
    val localizedName: String = "",
    val flagRes: Int,
    val languages: List<String> = listOf(),
)
```

- **Будет:**

```kotlin
data class CountryFlagItem(
    val numericCode: Int,
    val countryName: String,
    val localizedName: String = "",
    val flagRes: Int,
    /** Сырые строки библиотеки — для поиска флага по названию языка. */
    val languages: List<String> = listOf(),
    /** IS525: языки страны, основной первым — для подстановки по флагу. */
    val languageItems: List<LanguageItem> = listOf(),
)
```

- **Мотивация:** редьюсер подставляет основной язык страны прямо из
  выбранного флага чистой функцией — без эффекта и без похода в юзкейс.

### 5.4. Контракт `DictionaryUseCase`

`modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/DictionaryUseCase.kt`

- **Было:**

```kotlin
interface DictionaryUseCase {
    suspend fun getDictionaryList(): List<DictionaryListItem>
    fun flowDictionaryList(): Flow<List<DictionaryListItem>>
    suspend fun addDictionary(name: String, numericCode: Int?): Long
    suspend fun updateDictionary(id: Long, name: String, numericCode: Int?)
    suspend fun deleteDictionary(id: Long)
    suspend fun setCurrentDictionary(id: Long)
    fun updateFilter(query: String)
    fun flagsFlow(): Flow<List<CountryFlagItem>>
    suspend fun getDictionary(id: Long): DictionaryItem
    fun findFlag(numericCode: Int): CountryFlagItem?
}
```

- **Будет:**

```kotlin
interface DictionaryUseCase {
    suspend fun getDictionaryList(): List<DictionaryListItem>
    fun flowDictionaryList(): Flow<List<DictionaryListItem>>
    suspend fun addDictionary(
        name: String,
        numericCode: Int?,
        learningLanguage: String,
        translationLanguage: String,
    ): Long
    suspend fun updateDictionary(
        id: Long,
        name: String,
        numericCode: Int?,
        learningLanguage: String,
        translationLanguage: String,
    )
    suspend fun deleteDictionary(id: Long)
    suspend fun setCurrentDictionary(id: Long)
    fun updateFilter(query: String)
    fun flagsFlow(): Flow<List<CountryFlagItem>>
    suspend fun getDictionary(id: Long): DictionaryItem
    fun findFlag(numericCode: Int): CountryFlagItem?

    /** IS525: языки новой формы — словарь без флага (английский) и перевод (язык телефона). */
    fun languageDefaults(): LanguageDefaults

    /** IS525: полный список языков для выбора, названия на языке интерфейса, по алфавиту. */
    fun allLanguages(): List<LanguageItem>
}
```

- **Мотивация:** форма стартует с готовыми языками, сохраняет их и
  получает список для выбора.

### 5.5. Реализация `DictionaryUseCaseImpl`

`app/src/main/java/me/apomazkin/polytrainer/di/module/dictionary/DictionaryUseCaseImpl.kt`

- **Было:**

```kotlin
class DictionaryUseCaseImpl @Inject constructor(
    private val dictionaryApi: CoreDbApi.DictionaryApi,
    private val countryProvider: CountryProvider,
    private val prefsProvider: PrefsProvider,
) : DictionaryUseCase {
```

```kotlin
    override suspend fun addDictionary(name: String, numericCode: Int?): Long {
        val id = dictionaryApi.addDictionary(name, numericCode)
        setCurrentDictionary(id)
        return id
    }

    override suspend fun updateDictionary(id: Long, name: String, numericCode: Int?) {
        dictionaryApi.updateDictionary(id, name, numericCode)
    }
```

```kotlin
    override suspend fun getDictionary(id: Long): DictionaryItem {
        val entity = dictionaryApi.getDictionaryById(id)
            ?: error("Dictionary with id=$id not found")
        return DictionaryItem(
            id = entity.id,
            name = entity.name,
            numericCode = entity.numericCode,
        )
    }

    override fun findFlag(numericCode: Int): CountryFlagItem? {
        return allFlags.firstOrNull { it.numericCode == numericCode }
    }

    private fun loadAllFlags(): List<CountryFlagItem> {
        val deviceLocale = java.util.Locale.getDefault()
        return countryProvider.getAllCountries().map { country ->
            val localized = java.util.Locale("", country.alpha2)
                .getDisplayCountry(deviceLocale)
            CountryFlagItem(
                numericCode = country.numericCode,
                countryName = country.name,
                localizedName = localized,
                flagRes = countryProvider.getFlagRes(country.numericCode),
                languages = countryProvider.getLanguagesForCountry(country.numericCode),
            )
        }
    }
```

- **Будет:**

```kotlin
class DictionaryUseCaseImpl @Inject constructor(
    private val dictionaryApi: CoreDbApi.DictionaryApi,
    private val countryProvider: CountryProvider,
    private val prefsProvider: PrefsProvider,
    private val languageRules: DictionaryLanguageRules,
) : DictionaryUseCase {
```

```kotlin
    override suspend fun addDictionary(
        name: String,
        numericCode: Int?,
        learningLanguage: String,
        translationLanguage: String,
    ): Long {
        val id = dictionaryApi.addDictionary(
            name = name,
            numericCode = numericCode,
            learningLanguage = learningLanguage,
            translationLanguage = translationLanguage,
        )
        setCurrentDictionary(id)
        return id
    }

    override suspend fun updateDictionary(
        id: Long,
        name: String,
        numericCode: Int?,
        learningLanguage: String,
        translationLanguage: String,
    ) {
        dictionaryApi.updateDictionary(
            id = id,
            name = name,
            numericCode = numericCode,
            learningLanguage = learningLanguage,
            translationLanguage = translationLanguage,
        )
    }
```

```kotlin
    override suspend fun getDictionary(id: Long): DictionaryItem {
        val entity = dictionaryApi.getDictionaryById(id)
            ?: error("Dictionary with id=$id not found")
        return DictionaryItem(
            id = entity.id,
            name = entity.name,
            numericCode = entity.numericCode,
            learningLanguage = storedLanguageItem(entity.learningLanguage, entity.numericCode),
            translationLanguage = storedLanguageItem(entity.translationLanguage, null),
        )
    }

    /**
     * Флаг по коду страны — и для стран вне списка словаря (Ф3): у юзера
     * может быть словарь с флагом необитаемой территории, при правке он
     * должен остаться.
     */
    override fun findFlag(numericCode: Int): CountryFlagItem? {
        return allFlags.firstOrNull { it.numericCode == numericCode }
            ?: countryProvider.getAllCountries()
                .firstOrNull { it.numericCode == numericCode }
                ?.let(::flagItem)
    }

    override fun languageDefaults(): LanguageDefaults = LanguageDefaults(
        noFlag = languageItem(languageRules.learningLanguageFor(null)),
        translation = languageItem(languageRules.translationLanguage()),
    )

    /**
     * Полный список для выбора. Ф4: коды без названия на устройстве
     * отбрасываются; устаревшие коды (`iw`, `in`, `ji`), которые
     * `getISOLanguages` ещё отдаёт, — тоже, иначе иврит в списке дважды.
     */
    override fun allLanguages(): List<LanguageItem> {
        val deviceLocale = Locale.getDefault()
        val collator = Collator.getInstance(deviceLocale)
        return Locale.getISOLanguages()
            .filter { tag -> Locale.forLanguageTag(tag).toLanguageTag() == tag }
            .mapNotNull { tag ->
                androidLanguageName(tag, deviceLocale)?.let { name -> LanguageItem(tag, name) }
            }
            .sortedWith(compareBy(collator) { it.name })
    }

    /** Название языка на языке телефона, с заглавной буквы; null — Android код не знает. */
    private fun androidLanguageName(tag: String, deviceLocale: Locale): String? {
        val name = Locale.forLanguageTag(tag).getDisplayName(deviceLocale)
        if (name.isEmpty() || name.equals(tag, ignoreCase = true)) return null
        return name.replaceFirstChar { it.titlecase(deviceLocale) }
    }

    /** Код → элемент: название от Android, иначе [fallbackName] (английское из библиотеки). */
    private fun languageItem(tag: String, fallbackName: String = tag): LanguageItem {
        val name = androidLanguageName(tag, Locale.getDefault()) ?: fallbackName
        return LanguageItem(tag = tag, name = name)
    }

    /**
     * Сохранённый код → элемент. Для кода, которого Android не знает
     * (`cmn`, `tet`), запасное название ищется среди языков страны флага —
     * то же, что показывалось при подстановке по флагу.
     */
    private fun storedLanguageItem(tag: String, numericCode: Int?): LanguageItem {
        val fallbackName = numericCode
            ?.let { countryProvider.getCountryLanguages(it) }
            ?.firstOrNull { it.tag == tag }
            ?.englishName
            ?: tag
        return languageItem(tag = tag, fallbackName = fallbackName)
    }

    /** Флаги для словаря — страны с языком (Ф3), языки страны уже без семей (Ф1, Ф2). */
    private fun loadAllFlags(): List<CountryFlagItem> {
        return countryProvider.getDictionaryCountries().map(::flagItem)
    }

    private fun flagItem(country: CountryInfo): CountryFlagItem {
        val deviceLocale = Locale.getDefault()
        val localized = Locale("", country.alpha2)
            .getDisplayCountry(deviceLocale)
        return CountryFlagItem(
            numericCode = country.numericCode,
            countryName = country.name,
            localizedName = localized,
            flagRes = countryProvider.getFlagRes(country.numericCode),
            languages = countryProvider.getLanguagesForCountry(country.numericCode),
            languageItems = countryProvider
                .getCountryLanguages(country.numericCode)
                .map { languageItem(tag = it.tag, fallbackName = it.englishName) },
        )
    }
```

(плюс импорты `java.text.Collator`, `java.util.Locale`,
`me.apomazkin.flags.CountryInfo`, `LanguageDefaults`, `LanguageItem`;
`java.util.Locale` в теле заменён на импорт)

- **Мотивация:** вся работа с настройками телефона и библиотекой флагов —
  в одном месте. Список флагов — из `getDictionaryCountries` (Ф3), но
  `findFlag` смотрит и в сырой список: словарь с флагом Антарктиды при
  правке флаг не теряет. Сравнение названия с кодом — до заглавной буквы,
  иначе фильтр Ф4 не работал бы («Ktu» ≠ «ktu»). Для кодов, которых
  Android не знает, и при подстановке, и после сохранения показывается одно
  и то же английское название из библиотеки. `Collator` — алфавит языка
  интерфейса, а не порядок байтов.

### 5.6. Тесты юзкейса

`app/src/test/java/me/apomazkin/polytrainer/di/module/dictionary/DictionaryUseCaseImplTest.kt`

- **Было:**

```kotlin
    @Before
    fun setUp() {
        dictionaryApi = mockk(relaxed = true)
        countryProvider = mockk(relaxed = true)
        prefsProvider = mockk(relaxed = true)
        useCase = DictionaryUseCaseImpl(dictionaryApi, countryProvider, prefsProvider)
    }
```

```kotlin
    @Test
    fun `should delegate to API and return id`() = runTest {
        // Test case 5
        coEvery { dictionaryApi.addDictionary("English", 826) } returns 42L

        val result = useCase.addDictionary("English", 826)

        assertEquals(42L, result)
    }

    @Test
    fun `should set current dictionary on add`() = runTest {
        // Test case 6
        coEvery { dictionaryApi.addDictionary("English", 826) } returns 42L

        useCase.addDictionary("English", 826)

        coVerify { prefsProvider.setLong(PrefKey.CURRENT_DICTIONARY_ID_LONG, 42L) }
    }

    @Test
    fun `should pass null numericCode when no flag`() = runTest {
        // Test case 7
        coEvery { dictionaryApi.addDictionary("Bio", null) } returns 10L

        val result = useCase.addDictionary("Bio", null)

        assertEquals(10L, result)
        coVerify { dictionaryApi.addDictionary("Bio", null) }
    }

    // === updateDictionary ===

    @Test
    fun `should delegate update to API`() = runTest {
        // Test case 8
        useCase.updateDictionary(5L, "Updated", 724)

        coVerify { dictionaryApi.updateDictionary(5L, "Updated", 724) }
    }
```

- **Будет:**

```kotlin
    @Before
    fun setUp() {
        dictionaryApi = mockk(relaxed = true)
        countryProvider = mockk(relaxed = true)
        prefsProvider = mockk(relaxed = true)
        val rules = DictionaryLanguageRules(
            countryProvider = lazyOf(countryProvider),
            deviceLocale = { Locale("ru", "RU") },
        )
        useCase = DictionaryUseCaseImpl(dictionaryApi, countryProvider, prefsProvider, rules)
    }
```

```kotlin
    @Test
    fun `should delegate to API and return id`() = runTest {
        // Test case 5
        coEvery { dictionaryApi.addDictionary("English", 826, "en-GB", "ru") } returns 42L

        val result = useCase.addDictionary("English", 826, "en-GB", "ru")

        assertEquals(42L, result)
    }

    @Test
    fun `should set current dictionary on add`() = runTest {
        // Test case 6
        coEvery { dictionaryApi.addDictionary("English", 826, "en-GB", "ru") } returns 42L

        useCase.addDictionary("English", 826, "en-GB", "ru")

        coVerify { prefsProvider.setLong(PrefKey.CURRENT_DICTIONARY_ID_LONG, 42L) }
    }

    @Test
    fun `should pass null numericCode when no flag`() = runTest {
        // Test case 7
        coEvery { dictionaryApi.addDictionary("Bio", null, "en", "ru") } returns 10L

        val result = useCase.addDictionary("Bio", null, "en", "ru")

        assertEquals(10L, result)
        coVerify { dictionaryApi.addDictionary("Bio", null, "en", "ru") }
    }

    // === updateDictionary ===

    @Test
    fun `should delegate update to API`() = runTest {
        // Test case 8
        useCase.updateDictionary(5L, "Updated", 724, "es-ES", "ru")

        coVerify { dictionaryApi.updateDictionary(5L, "Updated", 724, "es-ES", "ru") }
    }

    // === IS525: языки ===

    @Test
    fun `getDictionary maps language tags`() = runTest {
        // Test case 14
        coEvery { dictionaryApi.getDictionaryById(7L) } returns DictionaryApiEntity(
            id = 7, numericCode = 484, name = "MX", addDate = now,
            learningLanguage = "es-MX", translationLanguage = "ru",
        )

        val item = useCase.getDictionary(7L)

        assertEquals("es-MX", item.learningLanguage.tag)
        assertEquals("ru", item.translationLanguage.tag)
    }

    @Test
    fun `languageDefaults are english and device language`() {
        // Test case 15
        val defaults = useCase.languageDefaults()

        assertEquals("en", defaults.noFlag.tag)
        assertEquals("ru", defaults.translation.tag)
    }

    @Test
    fun `flags carry country languages with main first`() = runTest {
        // Test case 16
        every { countryProvider.getDictionaryCountries() } returns listOf(mexico)
        every { countryProvider.getCountryLanguages(484) } returns listOf(
            CountryLanguage("es-MX", "Spanish"),
            CountryLanguage("yua", "Yucateco"),
        )

        val flags = useCase.flagsFlow().first()
        val flag = flags.single()

        assertEquals(listOf("es-MX", "yua"), flag.languageItems.map { it.tag })
    }

    @Test
    fun `allLanguages is not empty, canonical and sorted by name`() {
        // Test case 17
        val all = useCase.allLanguages()
        val collator = Collator.getInstance(Locale.getDefault())
        val names = all.map { it.name }

        assertTrue(all.size > 100)
        assertEquals(names, names.sortedWith(collator))
        assertTrue(all.all { Locale.forLanguageTag(it.tag).toLanguageTag() == it.tag })
        assertEquals(all.size, all.map { it.tag }.toSet().size)
    }

    @Test
    fun `findFlag returns flag of a country excluded from dictionary list`() {
        // Test case 18: Антарктида (010) вне getDictionaryCountries, но в getAllCountries
        every { countryProvider.getDictionaryCountries() } returns listOf(mexico)
        every { countryProvider.getAllCountries() } returns listOf(mexico, antarctica)
        every { countryProvider.getCountryLanguages(10) } returns emptyList()

        val flag = useCase.findFlag(10)

        assertEquals(10, flag?.numericCode)
        assertTrue(flag?.languageItems.isNullOrEmpty())
    }

    @Test
    fun `getDictionary uses library english name for a code unknown to Android`() = runTest {
        // Test case 19: Сингапур — основной язык cmn; Android имени может не знать
        coEvery { dictionaryApi.getDictionaryById(9L) } returns DictionaryApiEntity(
            id = 9, numericCode = 702, name = "SG", addDate = now,
            learningLanguage = "cmn", translationLanguage = "ru",
        )
        every { countryProvider.getCountryLanguages(702) } returns listOf(
            CountryLanguage("cmn", "Mandarin Chinese"),
        )

        val item = useCase.getDictionary(9L)

        assertEquals("cmn", item.learningLanguage.tag)
        // Название либо от Android (если знает), либо английское из библиотеки — но не голый код.
        assertNotEquals("cmn", item.learningLanguage.name)
    }
```

Фикстуры класса:

```kotlin
    private val mexico = CountryInfo(numericCode = 484, name = "Mexico", alpha2 = "MX")
    private val antarctica = CountryInfo(numericCode = 10, name = "Antarctica", alpha2 = "AQ")
```

(плюс импорты `kotlinx.coroutines.flow.first`, `java.text.Collator`,
`java.util.Locale`, `me.apomazkin.flags.CountryInfo`,
`me.apomazkin.flags.CountryLanguage`, `org.junit.Assert.assertNotEquals`;
в шапке класса — пункты 14–19 в списке кейсов)

- **Мотивация:** закрепить подстановку и преобразование кодов; в тестах
  сверяются коды, не названия — названия зависят от локали машины.

---

## 6. Механика формы

### 6.1. Состояние

`modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/DictionaryFormState.kt`

- **Было:**

```kotlin
@Immutable
data class DictionaryFormScreenState(
    val editingDictionaryId: Long? = null,
    val name: String = "",
    val flagFilter: String = "",
    val flags: List<CountryFlagItem> = emptyList(),
    val selectedFlag: CountryFlagItem? = null,
    val saveButtonEnabled: Boolean = false,
)

// === Extension Functions ===

fun DictionaryFormScreenState.updateName(value: String) = copy(
    name = value,
    saveButtonEnabled = value.isNotBlank(),
)

fun DictionaryFormScreenState.selectFlag(flag: CountryFlagItem) = copy(
    selectedFlag = flag,
)

fun DictionaryFormScreenState.deselectFlag() = copy(
    selectedFlag = null,
)

fun DictionaryFormScreenState.updateFlagFilter(query: String) = copy(
    flagFilter = query,
)

fun DictionaryFormScreenState.updateFlags(list: List<CountryFlagItem>) = copy(
    flags = list,
)

fun DictionaryFormScreenState.prefillForEdit(
    name: String,
    flag: CountryFlagItem?,
) = copy(
    name = name,
    selectedFlag = flag,
    saveButtonEnabled = name.isNotBlank(),
)
```

- **Будет:**

```kotlin
@Immutable
data class DictionaryFormScreenState(
    val editingDictionaryId: Long? = null,
    val name: String = "",
    val flagFilter: String = "",
    val flags: List<CountryFlagItem> = emptyList(),
    val selectedFlag: CountryFlagItem? = null,
    val saveButtonEnabled: Boolean = false,
    // IS525: языки заданы всегда; настоящие значения ставит сборка формы.
    val learningLanguage: LanguageItem = LanguageItem.Fallback,
    val translationLanguage: LanguageItem = LanguageItem.Fallback,
    /** Язык словаря без флага (английский) — подставляется при снятии флага. */
    val noFlagLanguage: LanguageItem = LanguageItem.Fallback,
    /** Изучаемый язык выбран вручную — смена флага его не трогает. */
    val isLearningLanguageManual: Boolean = false,
    val languagePicker: LanguagePickerState = LanguagePickerState(),
)

/** Какой язык выбирается в окне выбора. */
enum class LanguageTarget { LEARNING, TRANSLATION }

@Immutable
data class LanguagePickerState(
    val isOpen: Boolean = false,
    val target: LanguageTarget = LanguageTarget.LEARNING,
    val query: String = "",
    /** Код текущего выбора для отметки в списке. */
    val selectedTag: String = "",
    val allLanguages: List<LanguageItem> = emptyList(),
    val visibleLanguages: List<LanguageItem> = emptyList(),
)

// === Extension Functions ===

fun DictionaryFormScreenState.updateName(value: String) = copy(
    name = value,
    saveButtonEnabled = value.isNotBlank(),
)

fun DictionaryFormScreenState.selectFlag(flag: CountryFlagItem): DictionaryFormScreenState {
    val byFlag = flag.mainLanguageOr(noFlagLanguage)
    return copy(
        selectedFlag = flag,
        learningLanguage = if (isLearningLanguageManual) learningLanguage else byFlag,
    )
}

fun DictionaryFormScreenState.deselectFlag() = copy(
    selectedFlag = null,
    learningLanguage = if (isLearningLanguageManual) learningLanguage else noFlagLanguage,
)

fun DictionaryFormScreenState.updateFlagFilter(query: String) = copy(
    flagFilter = query,
)

fun DictionaryFormScreenState.updateFlags(list: List<CountryFlagItem>) = copy(
    flags = list,
)

/**
 * Подстановка сохранённого словаря. Изучаемый язык считается выбранным
 * вручную, если отличается от того, что дал бы флаг (или от языка без
 * флага) — тогда смена флага его не затрёт.
 */
fun DictionaryFormScreenState.prefillForEdit(
    name: String,
    flag: CountryFlagItem?,
    learningLanguage: LanguageItem,
    translationLanguage: LanguageItem,
): DictionaryFormScreenState {
    val withFlag = copy(selectedFlag = flag)
    return withFlag.copy(
        name = name,
        saveButtonEnabled = name.isNotBlank(),
        learningLanguage = learningLanguage,
        translationLanguage = translationLanguage,
        isLearningLanguageManual = learningLanguage.tag != withFlag.languageByFlag().tag,
    )
}

/**
 * Полный список пришёл. Если окно уже открыто (ответ `LoadLanguages`
 * пришёл после `OpenLanguagePicker`) — видимый список пересчитывается.
 */
fun DictionaryFormScreenState.applyAllLanguages(all: List<LanguageItem>): DictionaryFormScreenState {
    val withAll = copy(languagePicker = languagePicker.copy(allLanguages = all))
    if (!languagePicker.isOpen) return withAll
    val visible = withAll.languagesFor(languagePicker.target, languagePicker.query)
    return withAll.copy(languagePicker = withAll.languagePicker.copy(visibleLanguages = visible))
}

fun DictionaryFormScreenState.openLanguagePicker(
    target: LanguageTarget,
): DictionaryFormScreenState {
    val selected = when (target) {
        LanguageTarget.LEARNING -> learningLanguage
        LanguageTarget.TRANSLATION -> translationLanguage
    }
    return copy(
        languagePicker = languagePicker.copy(
            isOpen = true,
            target = target,
            query = "",
            selectedTag = selected.tag,
            visibleLanguages = languagesFor(target, query = ""),
        ),
    )
}

fun DictionaryFormScreenState.closeLanguagePicker() = copy(
    languagePicker = languagePicker.copy(isOpen = false, query = ""),
)

fun DictionaryFormScreenState.updateLanguageQuery(query: String) = copy(
    languagePicker = languagePicker.copy(
        query = query,
        visibleLanguages = languagesFor(languagePicker.target, query),
    ),
)

/**
 * Выбор языка в окне. Изучаемый считается ручным, только если отличается
 * от того, что дал бы флаг, — то же правило, что в [prefillForEdit]:
 * поведение в сессии и после переоткрытия формы совпадает.
 */
fun DictionaryFormScreenState.chooseLanguage(item: LanguageItem): DictionaryFormScreenState {
    val chosen = when (languagePicker.target) {
        LanguageTarget.LEARNING -> copy(
            learningLanguage = item,
            isLearningLanguageManual = item.tag != languageByFlag().tag,
        )
        LanguageTarget.TRANSLATION -> copy(translationLanguage = item)
    }
    return chosen.closeLanguagePicker()
}

/** Язык, который даёт текущий флаг; без флага — язык словаря без флага. */
private fun DictionaryFormScreenState.languageByFlag(): LanguageItem =
    selectedFlag?.mainLanguageOr(noFlagLanguage) ?: noFlagLanguage

private fun CountryFlagItem.mainLanguageOr(fallback: LanguageItem): LanguageItem =
    languageItems.firstOrNull() ?: fallback

/**
 * Список окна выбора: для изучаемого языка сверху языки выбранной
 * страны, ниже все остальные без повторов; поиск — по названию и коду.
 */
private fun DictionaryFormScreenState.languagesFor(
    target: LanguageTarget,
    query: String,
): List<LanguageItem> {
    val countryLanguages = when (target) {
        LanguageTarget.LEARNING -> selectedFlag?.languageItems.orEmpty()
        LanguageTarget.TRANSLATION -> emptyList()
    }
    val countryTags = countryLanguages.map { it.tag }.toSet()
    val all = countryLanguages + languagePicker.allLanguages.filterNot { it.tag in countryTags }
    val needle = query.trim().lowercase()
    if (needle.isEmpty()) return all
    return all.filter { item ->
        item.name.lowercase().contains(needle) || item.tag.lowercase().contains(needle)
    }
}
```

(плюс импорт `me.apomazkin.dictionary.model.LanguageItem`)

- **Мотивация:** всё, что показывает форма, — явные поля состояния.
  Значения по умолчанию `Fallback` оставляют `DictionaryFormScreenState()`
  в превью и тестах рабочим; в приложении их сразу замещает сборка (6.2).
  Правила «флаг подставляет, ручной выбор не затирается» живут в
  `selectFlag` / `deselectFlag` / `chooseLanguage` и проверяются тестами
  расширений.

### 6.2. Сборка формы

`modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/DictionaryFormAssembly.kt`

- **Было:**

```kotlin
    ): Mate<DictionaryFormScreenState, DictionaryFormMsg, Effect> = Mate(
        initState = DictionaryFormScreenState(
            editingDictionaryId = editingDictionaryId,
        ),
        initEffects = if (editingDictionaryId != null) {
            setOf(DictionaryFormEffect.LoadDictionary(editingDictionaryId))
        } else {
            emptySet()
        },
        coroutineScope = coroutineScope,
```

- **Будет:**

```kotlin
    ): Mate<DictionaryFormScreenState, DictionaryFormMsg, Effect> {
        // IS525: языки есть с первого кадра — словарь без флага английский,
        // перевод — язык телефона; полный список языков догружается эффектом.
        val defaults = useCase.languageDefaults()
        return Mate(
            initState = DictionaryFormScreenState(
                editingDictionaryId = editingDictionaryId,
                learningLanguage = defaults.noFlag,
                translationLanguage = defaults.translation,
                noFlagLanguage = defaults.noFlag,
            ),
            initEffects = buildSet {
                add(DictionaryFormEffect.LoadLanguages)
                if (editingDictionaryId != null) {
                    add(DictionaryFormEffect.LoadDictionary(editingDictionaryId))
                }
            },
            coroutineScope = coroutineScope,
```

(и закрывающая скобка `}` после `Mate(...)` — функция становится блочной)

- **Мотивация:** форма с первого кадра в законном состоянии — языки есть
  до всякой загрузки. `languageDefaults()` синхронный и дешёвый
  (две локали), библиотеку флагов не трогает.

### 6.3. Сообщения

`modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/DictionaryFormMsg.kt`

- **Было:**

```kotlin
    data class SelectFlag(val item: CountryFlagItem) : DictionaryFormMsg
    data object Save : DictionaryFormMsg
    data object Back : DictionaryFormMsg

    // Datasource messages
    data class FlagsUpdated(val list: List<CountryFlagItem>) : DictionaryFormMsg
    data class DictionaryLoaded(
        val name: String,
        val flag: CountryFlagItem?,
    ) : DictionaryFormMsg
```

- **Будет:**

```kotlin
    data class SelectFlag(val item: CountryFlagItem) : DictionaryFormMsg
    data object Save : DictionaryFormMsg
    data object Back : DictionaryFormMsg
    // IS525: выбор языков
    data class OpenLanguagePicker(val target: LanguageTarget) : DictionaryFormMsg
    data object CloseLanguagePicker : DictionaryFormMsg
    data class LanguageQueryChanged(val query: String) : DictionaryFormMsg
    data class SelectLanguage(val item: LanguageItem) : DictionaryFormMsg

    // Datasource messages
    data class FlagsUpdated(val list: List<CountryFlagItem>) : DictionaryFormMsg
    data class LanguagesLoaded(val all: List<LanguageItem>) : DictionaryFormMsg
    data class DictionaryLoaded(
        val name: String,
        val flag: CountryFlagItem?,
        val learningLanguage: LanguageItem,
        val translationLanguage: LanguageItem,
    ) : DictionaryFormMsg
```

(плюс импорт `LanguageItem`)

- **Мотивация:** каждому действию юзера и каждому ответу данных — своё
  сообщение.

### 6.4. Эффекты и их исполнитель

`modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/DictionaryFormEffectHandler.kt`

- **Было:**

```kotlin
sealed interface DictionaryFormEffect : Effect {
    data class LoadDictionary(val id: Long) : DictionaryFormEffect
    data class SaveDictionary(val name: String, val numericCode: Int?) : DictionaryFormEffect
    data class UpdateDictionary(
        val id: Long,
        val name: String,
        val numericCode: Int?,
    ) : DictionaryFormEffect
}
```

```kotlin
        val msg = when (effect) {
            is DictionaryFormEffect.LoadDictionary -> {
                val item = withContext(io) {
                    dictionaryUseCase.getDictionary(effect.id)
                }
                val flag = item.numericCode?.let { dictionaryUseCase.findFlag(it) }
                DictionaryFormMsg.DictionaryLoaded(item.name, flag)
            }

            is DictionaryFormEffect.SaveDictionary -> {
                withContext(io) {
                    dictionaryUseCase.addDictionary(effect.name, effect.numericCode)
                }
                DictionaryFormMsg.DictionarySaved
            }

            is DictionaryFormEffect.UpdateDictionary -> {
                withContext(io) {
                    dictionaryUseCase.updateDictionary(effect.id, effect.name, effect.numericCode)
                }
                DictionaryFormMsg.DictionarySaved
            }
        }
```

- **Будет:**

```kotlin
sealed interface DictionaryFormEffect : Effect {
    data class LoadDictionary(val id: Long) : DictionaryFormEffect
    /** IS525: полный список языков для окна выбора. */
    data object LoadLanguages : DictionaryFormEffect
    data class SaveDictionary(
        val name: String,
        val numericCode: Int?,
        val learningLanguage: String,
        val translationLanguage: String,
    ) : DictionaryFormEffect
    data class UpdateDictionary(
        val id: Long,
        val name: String,
        val numericCode: Int?,
        val learningLanguage: String,
        val translationLanguage: String,
    ) : DictionaryFormEffect
}
```

```kotlin
        val msg = when (effect) {
            is DictionaryFormEffect.LoadDictionary -> {
                val item = withContext(io) {
                    dictionaryUseCase.getDictionary(effect.id)
                }
                val flag = item.numericCode?.let { dictionaryUseCase.findFlag(it) }
                DictionaryFormMsg.DictionaryLoaded(
                    name = item.name,
                    flag = flag,
                    learningLanguage = item.learningLanguage,
                    translationLanguage = item.translationLanguage,
                )
            }

            is DictionaryFormEffect.LoadLanguages -> {
                val all = withContext(io) { dictionaryUseCase.allLanguages() }
                DictionaryFormMsg.LanguagesLoaded(all)
            }

            is DictionaryFormEffect.SaveDictionary -> {
                withContext(io) {
                    dictionaryUseCase.addDictionary(
                        name = effect.name,
                        numericCode = effect.numericCode,
                        learningLanguage = effect.learningLanguage,
                        translationLanguage = effect.translationLanguage,
                    )
                }
                DictionaryFormMsg.DictionarySaved
            }

            is DictionaryFormEffect.UpdateDictionary -> {
                withContext(io) {
                    dictionaryUseCase.updateDictionary(
                        id = effect.id,
                        name = effect.name,
                        numericCode = effect.numericCode,
                        learningLanguage = effect.learningLanguage,
                        translationLanguage = effect.translationLanguage,
                    )
                }
                DictionaryFormMsg.DictionarySaved
            }
        }
```

- **Мотивация:** полный список языков приходит из юзкейса (190 названий —
  в `io`), сохранение несёт языки до базы.

### 6.5. Редьюсер

`modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/DictionaryFormReducer.kt`

- **Было:**

```kotlin
            is DictionaryFormMsg.Save -> {
                val numericCode = state.selectedFlag?.numericCode
                if (state.editingDictionaryId != null) {
                    state to setOf(
                        DictionaryFormEffect.UpdateDictionary(
                            id = state.editingDictionaryId,
                            name = state.name,
                            numericCode = numericCode,
                        )
                    )
                } else {
                    state to setOf(
                        DictionaryFormEffect.SaveDictionary(
                            name = state.name,
                            numericCode = numericCode,
                        )
                    )
                }
            }

            is DictionaryFormMsg.Back -> state to setOf(NavigationEffect.Back)

            is DictionaryFormMsg.FlagsUpdated -> state
                .updateFlags(message.list) to emptySet()

            is DictionaryFormMsg.DictionaryLoaded -> state
                .prefillForEdit(message.name, message.flag) to emptySet()
```

- **Будет:**

```kotlin
            is DictionaryFormMsg.Save -> {
                val numericCode = state.selectedFlag?.numericCode
                if (state.editingDictionaryId != null) {
                    state to setOf(
                        DictionaryFormEffect.UpdateDictionary(
                            id = state.editingDictionaryId,
                            name = state.name,
                            numericCode = numericCode,
                            learningLanguage = state.learningLanguage.tag,
                            translationLanguage = state.translationLanguage.tag,
                        )
                    )
                } else {
                    state to setOf(
                        DictionaryFormEffect.SaveDictionary(
                            name = state.name,
                            numericCode = numericCode,
                            learningLanguage = state.learningLanguage.tag,
                            translationLanguage = state.translationLanguage.tag,
                        )
                    )
                }
            }

            is DictionaryFormMsg.Back -> state to setOf(NavigationEffect.Back)

            is DictionaryFormMsg.OpenLanguagePicker -> state
                .openLanguagePicker(message.target) to emptySet()

            is DictionaryFormMsg.CloseLanguagePicker -> state
                .closeLanguagePicker() to emptySet()

            is DictionaryFormMsg.LanguageQueryChanged -> state
                .updateLanguageQuery(message.query) to emptySet()

            is DictionaryFormMsg.SelectLanguage -> state
                .chooseLanguage(message.item) to emptySet()

            is DictionaryFormMsg.FlagsUpdated -> state
                .updateFlags(message.list) to emptySet()

            is DictionaryFormMsg.LanguagesLoaded -> state
                .applyAllLanguages(message.all) to emptySet()

            is DictionaryFormMsg.DictionaryLoaded -> state
                .prefillForEdit(
                    name = message.name,
                    flag = message.flag,
                    learningLanguage = message.learningLanguage,
                    translationLanguage = message.translationLanguage,
                ) to emptySet()
```

(ветка `SelectFlag` не меняется — `selectFlag` / `deselectFlag` теперь
сами ведут язык)

- **Мотивация:** редьюсер остаётся тонким — вся логика в переходах
  состояния 6.1.

### 6.6. Тесты формы

`modules/screen/dictionary/src/test/java/me/apomazkin/dictionary/form/ext/FormFieldsExtTest.kt`,
`…/reducer/FormActionsTest.kt`, `…/reducer/FormDataLoadingTest.kt`

- **Было (фикстуры, одинаковые в трёх файлах):**

```kotlin
    private val spainFlag = CountryFlagItem(
        numericCode = 724, countryName = "Spain", flagRes = 100,
        languages = listOf("Spanish", "Catalan"),
    )
    private val mexicoFlag = CountryFlagItem(
        numericCode = 484, countryName = "Mexico", flagRes = 101,
        languages = listOf("Spanish"),
    )
```

- **Будет:**

```kotlin
    private val spanish = LanguageItem(tag = "es-ES", name = "Spanish (Spain)")
    private val mexican = LanguageItem(tag = "es-MX", name = "Spanish (Mexico)")
    private val english = LanguageItem(tag = "en", name = "English")
    private val russian = LanguageItem(tag = "ru", name = "Russian")
    private val spainFlag = CountryFlagItem(
        numericCode = 724, countryName = "Spain", flagRes = 100,
        languages = listOf("Spanish", "Catalan"),
        languageItems = listOf(spanish, LanguageItem(tag = "ca", name = "Catalan")),
    )
    private val mexicoFlag = CountryFlagItem(
        numericCode = 484, countryName = "Mexico", flagRes = 101,
        languages = listOf("Spanish"),
        languageItems = listOf(mexican),
    )
```

- **Было (вызовы `prefillForEdit` в `FormFieldsExtTest`, строки 250, 260,
  270, 284):**

```kotlin
        val result = initial.prefillForEdit("English", spainFlag)
        val result = initial.prefillForEdit("English", null)
        val result = initial.prefillForEdit("", spainFlag)
        val result = initial.prefillForEdit("English", spainFlag)
```

- **Будет (те же строки, по порядку):**

```kotlin
        val result = initial.prefillForEdit("English", spainFlag, spanish, russian)
        val result = initial.prefillForEdit("English", null, english, russian)
        val result = initial.prefillForEdit("", spainFlag, spanish, russian)
        val result = initial.prefillForEdit("English", spainFlag, spanish, russian)
```

- **Было (`DictionaryLoaded` в `FormDataLoadingTest`, строки 89, 103, 117,
  133):**

```kotlin
            DictionaryFormMsg.DictionaryLoaded("English", spainFlag),
            DictionaryFormMsg.DictionaryLoaded("English", null),
            DictionaryFormMsg.DictionaryLoaded("", null),
            DictionaryFormMsg.DictionaryLoaded("English", spainFlag),
```

- **Будет (те же строки, по порядку):**

```kotlin
            DictionaryFormMsg.DictionaryLoaded("English", spainFlag, spanish, russian),
            DictionaryFormMsg.DictionaryLoaded("English", null, english, russian),
            DictionaryFormMsg.DictionaryLoaded("", null, english, russian),
            DictionaryFormMsg.DictionaryLoaded("English", spainFlag, spanish, russian),
```

Импорты, которые добавляются в тестовые файлы (пакеты тестов
`…form.ext` и `…form.reducer` отличаются от `…form`):
`me.apomazkin.dictionary.model.LanguageItem`,
`me.apomazkin.dictionary.form.LanguageTarget`; в `FormFieldsExtTest` ещё
`…form.applyAllLanguages`, `…form.openLanguagePicker`,
`…form.chooseLanguage`, `…form.updateLanguageQuery`. Шапки «Test cases»
трёх файлов дополняются новыми пунктами.

- **Новые кейсы (`FormFieldsExtTest`, после блока `prefillForEdit`):**

```kotlin
    // === IS525: языки ===

    @Test
    fun `selectFlag substitutes main country language`() {
        val result = DictionaryFormScreenState(noFlagLanguage = english).selectFlag(mexicoFlag)

        assertEquals(mexican, result.learningLanguage)
        assertFalse(result.isLearningLanguageManual)
    }

    @Test
    fun `selectFlag keeps manual language`() {
        val initial = DictionaryFormScreenState(
            learningLanguage = russian,
            isLearningLanguageManual = true,
        )
        val result = initial.selectFlag(mexicoFlag)

        assertEquals(russian, result.learningLanguage)
    }

    @Test
    fun `deselectFlag returns no-flag language`() {
        val initial = DictionaryFormScreenState(
            noFlagLanguage = english,
            selectedFlag = mexicoFlag,
            learningLanguage = mexican,
        )
        val result = initial.deselectFlag()

        assertEquals(english, result.learningLanguage)
    }

    @Test
    fun `deselectFlag keeps manual language`() {
        val initial = DictionaryFormScreenState(
            noFlagLanguage = english, selectedFlag = mexicoFlag,
            learningLanguage = russian, isLearningLanguageManual = true,
        )
        val result = initial.deselectFlag()

        assertEquals(russian, result.learningLanguage)
    }

    @Test
    fun `prefillForEdit marks language manual when it differs from flag`() {
        val initial = DictionaryFormScreenState(noFlagLanguage = english)

        val byFlag = initial.prefillForEdit("MX", mexicoFlag, mexican, russian)
        val manual = initial.prefillForEdit("MX", mexicoFlag, russian, russian)
        val noFlagEnglish = initial.prefillForEdit("Bio", null, english, russian)
        val noFlagOther = initial.prefillForEdit("Bio", null, spanish, russian)

        assertFalse(byFlag.isLearningLanguageManual)
        assertTrue(manual.isLearningLanguageManual)
        assertFalse(noFlagEnglish.isLearningLanguageManual)
        assertTrue(noFlagOther.isLearningLanguageManual)
    }

    @Test
    fun `chooseLanguage for learning sets manual and closes picker`() {
        val initial = DictionaryFormScreenState().openLanguagePicker(LanguageTarget.LEARNING)
        val result = initial.chooseLanguage(russian)

        assertEquals(russian, result.learningLanguage)
        assertTrue(result.isLearningLanguageManual)
        assertFalse(result.languagePicker.isOpen)
    }

    @Test
    fun `chooseLanguage of the flag language is not manual`() {
        val initial = DictionaryFormScreenState(selectedFlag = mexicoFlag, learningLanguage = russian)
            .openLanguagePicker(LanguageTarget.LEARNING)
        val result = initial.chooseLanguage(mexican)

        assertEquals(mexican, result.learningLanguage)
        assertFalse(result.isLearningLanguageManual)
    }

    @Test
    fun `chooseLanguage for translation does not touch manual flag`() {
        val initial = DictionaryFormScreenState().openLanguagePicker(LanguageTarget.TRANSLATION)
        val result = initial.chooseLanguage(russian)

        assertEquals(russian, result.translationLanguage)
        assertFalse(result.isLearningLanguageManual)
    }

    @Test
    fun `applyAllLanguages recomputes visible list when picker is open`() {
        val opened = DictionaryFormScreenState().openLanguagePicker(LanguageTarget.TRANSLATION)
        val result = opened.applyAllLanguages(listOf(english, russian))

        assertEquals(listOf(english, russian), result.languagePicker.visibleLanguages)
    }

    @Test
    fun `openLanguagePicker lists country languages first without duplicates`() {
        val initial = DictionaryFormScreenState(selectedFlag = spainFlag)
            .applyAllLanguages(listOf(english, russian, spanish))
        val result = initial.openLanguagePicker(LanguageTarget.LEARNING)

        assertEquals(
            listOf("es-ES", "ca", "en", "ru"),
            result.languagePicker.visibleLanguages.map { it.tag },
        )
        assertEquals("en", result.languagePicker.selectedTag)
    }

    @Test
    fun `updateLanguageQuery filters by name and tag`() {
        val initial = DictionaryFormScreenState()
            .applyAllLanguages(listOf(english, russian, spanish))
            .openLanguagePicker(LanguageTarget.TRANSLATION)

        val byName = initial.updateLanguageQuery("rus")
        val byTag = initial.updateLanguageQuery("es-")

        assertEquals(listOf(russian), byName.languagePicker.visibleLanguages)
        assertEquals(listOf(spanish), byTag.languagePicker.visibleLanguages)
    }
```

- **Новые кейсы (`FormActionsTest`): `Save` в обоих режимах, `SelectFlag`
  и по одному на каждую новую ветку редьюсера:**

```kotlin
    @Test
    fun `Save carries both language tags`() {
        val initial = DictionaryFormScreenState(
            name = "MX",
            selectedFlag = mexicoFlag,
            learningLanguage = mexican,
            translationLanguage = russian,
        )
        val result = reducer.testReduce(initial, DictionaryFormMsg.Save)

        val effect = result.effects().first() as DictionaryFormEffect.SaveDictionary
        assertEquals("es-MX", effect.learningLanguage)
        assertEquals("ru", effect.translationLanguage)
    }

    @Test
    fun `Save in edit mode carries both language tags`() {
        val initial = DictionaryFormScreenState(
            editingDictionaryId = 5,
            name = "MX",
            learningLanguage = mexican,
            translationLanguage = russian,
        )
        val result = reducer.testReduce(initial, DictionaryFormMsg.Save)

        val effect = result.effects().first() as DictionaryFormEffect.UpdateDictionary
        assertEquals("es-MX", effect.learningLanguage)
        assertEquals("ru", effect.translationLanguage)
    }

    @Test
    fun `SelectFlag substitutes language via reducer`() {
        val initial = DictionaryFormScreenState(noFlagLanguage = english)
        val result = reducer.testReduce(initial, DictionaryFormMsg.SelectFlag(mexicoFlag))

        assertEquals(mexican, result.state().learningLanguage)
        result.assertNoEffects()
    }

    @Test
    fun `OpenLanguagePicker opens picker for target`() {
        val result = reducer.testReduce(
            DictionaryFormScreenState(),
            DictionaryFormMsg.OpenLanguagePicker(LanguageTarget.TRANSLATION),
        )

        assertTrue(result.state().languagePicker.isOpen)
        assertEquals(LanguageTarget.TRANSLATION, result.state().languagePicker.target)
        result.assertNoEffects()
    }

    @Test
    fun `CloseLanguagePicker closes picker`() {
        val opened = DictionaryFormScreenState().openLanguagePicker(LanguageTarget.LEARNING)
        val result = reducer.testReduce(opened, DictionaryFormMsg.CloseLanguagePicker)

        assertFalse(result.state().languagePicker.isOpen)
        result.assertNoEffects()
    }

    @Test
    fun `LanguageQueryChanged filters visible languages`() {
        val opened = DictionaryFormScreenState()
            .applyAllLanguages(listOf(english, russian))
            .openLanguagePicker(LanguageTarget.TRANSLATION)
        val result = reducer.testReduce(opened, DictionaryFormMsg.LanguageQueryChanged("rus"))

        assertEquals(listOf(russian), result.state().languagePicker.visibleLanguages)
        result.assertNoEffects()
    }

    @Test
    fun `SelectLanguage applies choice and closes picker`() {
        val opened = DictionaryFormScreenState().openLanguagePicker(LanguageTarget.TRANSLATION)
        val result = reducer.testReduce(opened, DictionaryFormMsg.SelectLanguage(russian))

        assertEquals(russian, result.state().translationLanguage)
        assertFalse(result.state().languagePicker.isOpen)
        result.assertNoEffects()
    }
```

- **Новые кейсы (`FormDataLoadingTest`):**

```kotlin
    @Test
    fun `LanguagesLoaded fills picker list`() {
        val result = reducer.testReduce(
            DictionaryFormScreenState(),
            DictionaryFormMsg.LanguagesLoaded(listOf(spanish, mexican)),
        )

        assertEquals(2, result.state().languagePicker.allLanguages.size)
        result.assertNoEffects()
    }

    @Test
    fun `DictionaryLoaded prefills languages and manual flag`() {
        val initial = DictionaryFormScreenState(editingDictionaryId = 5, noFlagLanguage = english)
        val result = reducer.testReduce(
            initial,
            DictionaryFormMsg.DictionaryLoaded("MX", mexicoFlag, russian, russian),
        )

        assertEquals(russian, result.state().learningLanguage)
        assertEquals(russian, result.state().translationLanguage)
        assertTrue(result.state().isLearningLanguageManual)
        result.assertNoEffects()
    }
```

- **Мотивация:** правила 6.1 — суть подфичи; каждое закреплено тестом.
  Существующие кейсы меняются только в сигнатурах.

---

## 7. Минимальный интерфейс

### 7.1. Капсула поиска с настраиваемой подсказкой

`modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/widget/SearchPillWidget.kt`

- **Было:**

```kotlin
internal fun SearchPillWidget(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
```

```kotlin
                    Text(
                        text = stringResource(id = R.string.dictionary_filter_flags_hint),
```

- **Будет:**

```kotlin
internal fun SearchPillWidget(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    @StringRes hintRes: Int = R.string.dictionary_filter_flags_hint,
) {
```

```kotlin
                    Text(
                        text = stringResource(id = hintRes),
```

(плюс импорт `androidx.annotation.StringRes`)

- **Мотивация:** окно выбора языка берёт готовую капсулу поиска формы, а не
  M3-поле с цветами по умолчанию.

### 7.2. Строка-сводка языков — новый файл

`modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/widget/LanguageSummaryWidget.kt`

- **Было:** файла нет.
- **Будет:**

```kotlin
package me.apomazkin.dictionary.form.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.formTextSecondary
import me.apomazkin.ui.preview.PreviewWidget

/**
 * IS525, минимальный вид: «<изучаемый> → <перевод>». Нажатие на язык
 * открывает его выбор. Оформление — вторым заходом.
 */
@Composable
internal fun LanguageSummaryWidget(
    learningName: String,
    translationName: String,
    onLearningClick: () -> Unit,
    onTranslationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = learningName,
            style = LexemeStyle.BodyM,
            color = LexemeColor.primary,
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onLearningClick)
                .padding(vertical = 12.dp),
        )
        Text(
            text = "→",
            style = LexemeStyle.BodyM,
            color = formTextSecondary,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Text(
            text = translationName,
            style = LexemeStyle.BodyM,
            color = LexemeColor.primary,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onTranslationClick)
                .padding(vertical = 12.dp),
        )
    }
}

@Composable
@PreviewWidget
private fun Preview() {
    AppTheme {
        LanguageSummaryWidget(
            learningName = "Испанский (Мексика)",
            translationName = "Русский",
            onLearningClick = {},
            onTranslationClick = {},
        )
    }
}
```

- **Мотивация:** решение В1 (одна строка после флагов) в минимальном виде;
  цвета явные, из палитры формы.

### 7.3. Окно выбора языка — новый файл

`modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/widget/LanguagePickerDialog.kt`

- **Было:** файла нет.
- **Будет:**

```kotlin
package me.apomazkin.dictionary.form.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import me.apomazkin.dictionary.R
import me.apomazkin.dictionary.form.LanguagePickerState
import me.apomazkin.dictionary.form.LanguageTarget
import me.apomazkin.dictionary.model.LanguageItem
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.formBackground
import me.apomazkin.theme.formTextTertiary

/**
 * IS525, минимальный вид: заголовок, поиск, список; текущий выбор —
 * акцентным цветом. Оформление (лист с вкладками) — вторым заходом.
 */
@Composable
internal fun LanguagePickerDialog(
    state: LanguagePickerState,
    onQueryChange: (String) -> Unit,
    onSelect: (LanguageItem) -> Unit,
    onDismiss: () -> Unit,
) {
    val titleRes = when (state.target) {
        LanguageTarget.LEARNING -> R.string.dictionary_language_learning_title
        LanguageTarget.TRANSLATION -> R.string.dictionary_language_translation_title
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = formBackground,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = titleRes),
                    style = LexemeStyle.BodyM,
                    color = formTextTertiary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                SearchPillWidget(
                    value = state.query,
                    onValueChange = onQueryChange,
                    hintRes = R.string.dictionary_language_search_hint,
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    items(state.visibleLanguages, key = { it.tag }) { item ->
                        val isSelected = item.tag == state.selectedTag
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(item) }
                                .padding(vertical = 12.dp),
                        ) {
                            Text(
                                text = item.name,
                                style = LexemeStyle.BodyM,
                                color = if (isSelected) LexemeColor.primary else formTextTertiary,
                            )
                        }
                    }
                }
            }
        }
    }
}
```

- **Мотивация:** минимальное средство пройти ручные кейсы механики;
  `Dialog`, а не нижний лист — поле поиска с клавиатурой в листе давало
  проблемы (IS496).

### 7.4. Форма

`modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/widget/DictionaryFormWidget.kt`

- **Было:**

```kotlin
        FlagGridWidget(
            flags = formState.flags,
            selectedFlag = formState.selectedFlag,
            isFilterActive = formState.flagFilter.isNotBlank(),
            onFlagClick = { sendMsg(DictionaryFormMsg.SelectFlag(it)) },
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.height(8.dp))

        val buttonTextRes = if (formState.editingDictionaryId != null) {
```

```kotlin
        SubmitButtonWidget(
            titleRes = buttonTextRes,
            enabled = formState.saveButtonEnabled,
            onClick = { sendMsg(DictionaryFormMsg.Save) },
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
```

- **Будет:**

```kotlin
        FlagGridWidget(
            flags = formState.flags,
            selectedFlag = formState.selectedFlag,
            isFilterActive = formState.flagFilter.isNotBlank(),
            onFlagClick = { sendMsg(DictionaryFormMsg.SelectFlag(it)) },
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.height(8.dp))

        LanguageSummaryWidget(
            learningName = formState.learningLanguage.name,
            translationName = formState.translationLanguage.name,
            onLearningClick = {
                sendMsg(DictionaryFormMsg.OpenLanguagePicker(LanguageTarget.LEARNING))
            },
            onTranslationClick = {
                sendMsg(DictionaryFormMsg.OpenLanguagePicker(LanguageTarget.TRANSLATION))
            },
        )

        Spacer(modifier = Modifier.height(8.dp))

        val buttonTextRes = if (formState.editingDictionaryId != null) {
```

```kotlin
        SubmitButtonWidget(
            titleRes = buttonTextRes,
            enabled = formState.saveButtonEnabled,
            onClick = { sendMsg(DictionaryFormMsg.Save) },
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (formState.languagePicker.isOpen) {
        LanguagePickerDialog(
            state = formState.languagePicker,
            onQueryChange = { sendMsg(DictionaryFormMsg.LanguageQueryChanged(it)) },
            onSelect = { sendMsg(DictionaryFormMsg.SelectLanguage(it)) },
            onDismiss = { sendMsg(DictionaryFormMsg.CloseLanguagePicker) },
        )
    }
}
```

(плюс импорт `me.apomazkin.dictionary.form.LanguageTarget`)

- **Мотивация:** строка после флагов (В1); окно показывается строго по
  полю состояния.

### 7.5. Строки

`core/core-resources/src/main/res/values-ru-rRU/strings.xml`

- **Было:**

```xml
    <string name="dictionary_edit_title">Редактирование словаря</string>
```

- **Будет:**

```xml
    <string name="dictionary_edit_title">Редактирование словаря</string>
    <string name="dictionary_language_learning_title">Изучаемый язык</string>
    <string name="dictionary_language_translation_title">Язык перевода</string>
    <string name="dictionary_language_search_hint">Поиск языка…</string>
```

`core/core-resources/src/main/res/values/strings.xml`

- **Было:**

```xml
    <string name="dictionary_edit_title">Edit dictionary</string>
```

- **Будет:**

```xml
    <string name="dictionary_edit_title">Edit dictionary</string>
    <string name="dictionary_language_learning_title">Language you learn</string>
    <string name="dictionary_language_translation_title">Translation language</string>
    <string name="dictionary_language_search_hint">Search language…</string>
```

- **Мотивация:** тексты не зашиваются в код.

---

## 8. Проверка

- Unit, по одному модулю: `:modules:library:flags:testDebugUnitTest`,
  `:modules:screen:dictionary:testDebugUnitTest`, `:app:testDebugUnitTest`.
  Lint: `:app:lintDebug`.
- AndroidTest на девайсе, по одному модулю:
  `:core:core-db-impl:connectedDebugAndroidTest` (миграция 15→16 по
  `16.json`, API языков, все прежние),
  `:modules:library:flags:connectedDebugAndroidTest` (весь массив
  библиотеки).
- `installDebug`, запуск `.dev`, FATAL = 0.
- Ручные кейсы на `.dev`:
  1. Новый словарь: до выбора флага — «English → Русский»; флаг Мексики —
     «Испанский (Мексика)»; сохранить; открыть редактирование — языки на
     месте.
  2. Выбрать изучаемый язык вручную, потом сменить флаг — язык не
     затёрся.
  3. Существующие словари после обновления: с флагом — язык страны; без
     флага — английский; исправить оба языка, сохранить, проверить.
  4. Снять флаг у нового словаря — изучаемый вернулся к английскому.
  5. Первичная настройка (первый запуск) — форма с языками работает.
  6. Импорт старого бэкапа (`docs/LexemeDb11.sqlite`, схема v11) через
     «Импорт данных» без перезапуска приложения — словари получили языки,
     форма редактирования их показывает.
  7. Словарь с флагом необитаемой территории (создать до обновления на
     старой сборке или подготовить в бэкапе): после обновления — английский;
     в редактировании флаг виден, после сохранения не потерян.
- Коммит — один, по команде юзера, после зелёных проверок и кейсов.

**Прогон 2026-10-06:** unit `flags` 10, `dictionary` 68, `app` 24 — зелёные;
lint зелёный; androidTest на девайсе: `core-db-impl` 163/0 (миграция
15→16, случаи A–E; API языков), `flags` 5/0 (весь массив библиотеки).
Ручные кейсы 1–5 — ✅; 6 (импорт бэкапа) — пропущен по решению юзера
(переоткрытие Room после импорта — старое поведение, не из IS525, на
девайсе не проверялось); 7 — на девайсе не воспроизвести без импорта,
закрыт unit-тестом 18 (`findFlag` вне списка словаря).

**Попутно найден и исправлен баг IS519 (в релизе 0.1.14):** ячейка сетки
флагов после фильтра показывала чужой флаг («Мексика» с флагом
Афганистана) — `produceState` держал старый растр при смене ключей, а
`items` без ключа переиспользовал ячейку. Теперь состояние растра —
`remember(flagRes, heightPx)` + `LaunchedEffect`, элементы сетки с
`key = numericCode` (`FlagGridWidget.kt`).
