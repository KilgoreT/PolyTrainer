package me.apomazkin.prefs

/**
 * IS481 picker. Per-dictionary pref key для quiz component picker selection.
 *
 * Single source of truth — используется в `QuizChatUseCaseImpl` (encode/decode)
 * и `QuizPickerFlowHandler` (subscribe). Изоляция выборов между словарями
 * через `dictionaryId` в ключе.
 */
fun quizPickerPrefKey(dictionaryId: Long): String = "quiz_picker_dict_$dictionaryId"
