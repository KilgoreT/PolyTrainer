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
