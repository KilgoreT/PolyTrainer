# IS445 — Implement: Устаревшая иконка словаря в appbar picker

## Изменённые файлы

### `app/src/main/java/me/apomazkin/polytrainer/di/module/widget/DictionaryAppBarUseCaseImpl.kt`

**Что сделано:**
- Заменён `prefsFlow.map { getDictionaryById(id) }` на `combine(prefsFlow, flowDictionaryList()) { id, list -> ... }`
- Добавлен импорт `kotlinx.coroutines.flow.combine`
- Fallback: `list.find { it.id == id }` -> `list.firstOrNull()` -> `DictionaryNotFoundException`
- Удалены suspend-вызовы `getDictionaryById()` и `getDictionaryList()` — вместо них реактивный `flowDictionaryList()`

## Тесты

Все 9 тестов в `DictionaryAppBarUseCaseImplTest` прошли:
- 7 тестов flowCurrentDict (включая ключевой #2 — реактивность при смене флага без смены ID)
- 1 тест flowAvailableDict
- 1 тест changeDict

## log_messages
- Реализация выполнена строго по design_tree: 1 файл, замена map на combine
- Все 9 unit-тестов проходят успешно
- Новых файлов и зависимостей не добавлено

## checklist_items
Существующие пункты чеклиста покрыты реализацией. Новых пунктов не требуется.

_model: claude-opus-4-6_
