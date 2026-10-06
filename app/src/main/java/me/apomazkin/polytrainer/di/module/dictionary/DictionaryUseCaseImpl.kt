package me.apomazkin.polytrainer.di.module.dictionary

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.dictionary.DictionaryUseCase
import me.apomazkin.dictionary.model.CountryFlagItem
import me.apomazkin.dictionary.model.DictionaryItem
import me.apomazkin.dictionary.model.DictionaryListItem
import me.apomazkin.dictionary.model.LanguageDefaults
import me.apomazkin.dictionary.model.LanguageItem
import me.apomazkin.flags.CountryInfo
import me.apomazkin.flags.CountryProvider
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import java.text.Collator
import java.util.Locale
import javax.inject.Inject

class DictionaryUseCaseImpl @Inject constructor(
    private val dictionaryApi: CoreDbApi.DictionaryApi,
    private val countryProvider: CountryProvider,
    private val prefsProvider: PrefsProvider,
    private val languageRules: DictionaryLanguageRules,
) : DictionaryUseCase {

    private val allFlags: List<CountryFlagItem> by lazy { loadAllFlags() }
    private val filterQuery = MutableStateFlow("")

    override suspend fun getDictionaryList(): List<DictionaryListItem> {
        return dictionaryApi.getDictionaryList().map { entity ->
            DictionaryListItem(
                id = entity.id,
                name = entity.name,
                flagRes = entity.numericCode?.let { countryProvider.getFlagRes(it) },
            )
        }
    }

    override fun flowDictionaryList(): Flow<List<DictionaryListItem>> {
        return dictionaryApi.flowDictionaryList().map { list ->
            list.map { entity ->
                DictionaryListItem(
                    id = entity.id,
                    name = entity.name,
                    flagRes = entity.numericCode?.let { countryProvider.getFlagRes(it) },
                )
            }
        }
    }

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

    override suspend fun deleteDictionary(id: Long) {
        dictionaryApi.deleteDictionary(id)
        val currentId = prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG)
        if (currentId == id) {
            val remaining = dictionaryApi.getDictionaryList().firstOrNull()
            if (remaining != null) {
                setCurrentDictionary(remaining.id)
            } else {
                // IS476: нет оставшихся словарей — чистим orphaned pref,
                // иначе следующие читатели CURRENT_DICTIONARY_ID_LONG получат
                // ссылку на удалённый id и упадут на fallback'ах.
                prefsProvider.setLong(PrefKey.CURRENT_DICTIONARY_ID_LONG, null)
            }
        }
    }

    override suspend fun setCurrentDictionary(id: Long) {
        prefsProvider.setLong(PrefKey.CURRENT_DICTIONARY_ID_LONG, id)
    }

    override fun updateFilter(query: String) {
        filterQuery.value = query
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun flagsFlow(): Flow<List<CountryFlagItem>> {
        return filterQuery
            .transformLatest { query ->
                if (query.isBlank()) emit(query) else { delay(300L); emit(query) }
            }
            .map { query -> filterFlags(allFlags, query) }
    }

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

    private fun filterFlags(
        allFlags: List<CountryFlagItem>,
        query: String,
    ): List<CountryFlagItem> {
        if (query.isBlank()) return allFlags
        val q = query.trim().lowercase()
        return allFlags.filter { flag ->
            flag.countryName.lowercase().contains(q) ||
                flag.localizedName.lowercase().contains(q) ||
                flag.languages.any { lang -> lang.lowercase().contains(q) }
        }
    }

}
