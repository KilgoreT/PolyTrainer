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
