package me.apomazkin.lexeme

import java.text.Normalizer
import java.util.Locale

/**
 * Ключ сравнения «та же лексема» для текста значения (AWP-07): trim →
 * NFD → убрать только знаки ударения U+0301 и U+0300 → NFC → нижний
 * регистр. Остальная диакритика остаётся: «ё» и «й» не сводятся к «е» и
 * «и» (AWP-07.6), а «бежа́ть» совпадает с «бежать» (AWP-07.4).
 */
fun lexemeTextKey(raw: String, locale: Locale = Locale.ROOT): String {
    val decomposed = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
    val withoutStress = decomposed.filterNot { it == ACUTE_ACCENT || it == GRAVE_ACCENT }
    return Normalizer.normalize(withoutStress, Normalizer.Form.NFC).lowercase(locale)
}

private const val ACUTE_ACCENT = '́'
private const val GRAVE_ACCENT = '̀'
