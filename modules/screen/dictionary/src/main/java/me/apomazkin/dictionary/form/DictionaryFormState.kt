package me.apomazkin.dictionary.form

import androidx.compose.runtime.Immutable
import me.apomazkin.dictionary.model.CountryFlagItem
import me.apomazkin.dictionary.model.LanguageItem

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
