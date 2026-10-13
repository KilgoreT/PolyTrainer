package me.apomazkin.lexeme

import java.text.Normalizer
import java.util.Locale

/**
 * Ключ сравнения «то же слово» для текста леммы (AWP-07): trim → NFC →
 * нижний регистр. SQLite понижает регистр только у ASCII, поэтому
 * сравнение идёт в Kotlin; NFC склеивает разложенную диакритику
 * («й» из U+0438 + U+0306 и U+0439 — одно слово).
 */
fun wordTextKey(raw: String, locale: Locale = Locale.ROOT): String =
    Normalizer.normalize(raw.trim(), Normalizer.Form.NFC).lowercase(locale)
