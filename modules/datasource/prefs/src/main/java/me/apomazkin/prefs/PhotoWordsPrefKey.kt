package me.apomazkin.prefs

/**
 * IS532: pref-ключ соответствий окна добавления слов со снимка по словарю
 * (AWP-06). Значение — вид данных → id компонента или `none`, через
 * запятую; отсутствие ключа — умолчания. Общая группа не хранится
 * (AWP-06.8).
 */
fun photoWordsMappingPrefKey(dictionaryId: Long): String = "photo_words_mapping_dict_$dictionaryId"
