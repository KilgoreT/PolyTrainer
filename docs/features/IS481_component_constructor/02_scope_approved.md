# Approved findings — 02_scope.md, iteration 8

1 finding (F050 minor). Шаг повторяется (iter 9 для minor-only streak check).

## F050 [minor] — `DictionaryApi.getAllDictIds()` не существует

В Open question про prefs cleanup strategy B используется метод `DictionaryApi.getAllDictIds()` — но в реальном API его нет. Существуют `getDictionaryList(): List<DictionaryApiEntity>` (suspend) и `flowDictionaryList(): Flow<List<DictionaryApiEntity>>`.

**Что исправить:** в Open question и в строке файла `PrefsProvider.kt` (вариант B) заменить `DictionaryApi.getAllDictIds()` на `DictionaryApi.getDictionaryList()` (или `.flowDictionaryList().first()` если нужно reactive). Уточнить что метод возвращает `List<DictionaryApiEntity>` из которого UseCase извлекает `id` поле.
