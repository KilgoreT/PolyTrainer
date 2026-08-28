package me.apomazkin.group

import java.util.Locale

/**
 * IS493 Э3 (D12.1): результат проверки имени группы.
 * [Valid.normalizedName] — ЕДИНСТВЕННЫЙ источник имени для записи в БД:
 * нормализация рождается в валидаторе, двух независимых точек normalize
 * (сравнение отдельно, запись отдельно) не существует.
 */
sealed interface NameCheck {
    data class Valid(val normalizedName: String) : NameCheck
    data object Empty : NameCheck
    data object Duplicate : NameCheck
    data object Reserved : NameCheck
}

/**
 * trim + NFC (композиция диакритики: разложенное «й» = U+0438+U+0306 →
 * U+0439). Хранится и сравнивается ТОЛЬКО нормализованная форма (А14);
 * та же функция — для импорта публикации.
 */
fun normalizeGroupName(raw: String): String =
    java.text.Normalizer.normalize(raw.trim(), java.text.Normalizer.Form.NFC)

/**
 * Чистая проверка имени (вызывается ИЗ транзакции CoreDbApiImpl, D13.1).
 *
 * - [livingSiblingNames] — имена ЖИВЫХ сиблингов; при rename вызывающий
 *   ОБЯЗАН исключить саму переименуемую группу (self-exclusion, D-1/T-1) —
 *   иначе смена регистра своего имени упрётся в Duplicate;
 * - [reservedNames] — резерв («Все» всех локалей), инжектится снаружи (Р7);
 * - сравнение — case-insensitive по [locale] (единый источник —
 *   Locale.getDefault() на вызывающей стороне, D12.3).
 */
fun validateGroupName(
    raw: String,
    livingSiblingNames: Collection<String>,
    reservedNames: Set<String>,
    locale: Locale,
): NameCheck {
    val normalized = normalizeGroupName(raw)
    if (normalized.isEmpty()) return NameCheck.Empty
    val key = normalized.lowercase(locale)
    // Обе стороны сравнения — через ту же нормализацию (идемпотентна для
    // уже нормализованных имён из БД; резерв из ресурсов тоже прогоняется).
    if (reservedNames.any { normalizeGroupName(it).lowercase(locale) == key }) {
        return NameCheck.Reserved
    }
    if (livingSiblingNames.any { normalizeGroupName(it).lowercase(locale) == key }) {
        return NameCheck.Duplicate
    }
    return NameCheck.Valid(normalizedName = normalized)
}
