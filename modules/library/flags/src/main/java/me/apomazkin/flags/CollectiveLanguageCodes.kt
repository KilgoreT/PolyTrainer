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
