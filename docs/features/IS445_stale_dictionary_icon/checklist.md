# Checklist

- ✅ Пользователь меняет флаг словаря в форме → appbar picker показывает актуальный флаг без переключения словарей [spec]
  - ✅ Лог: ###DictAppBar### текущий словарь обновлён реактивно после смены флага — Room UPDATE словаря → flowDictionaryList() эмитит обновлённый список → combine пересчитывает DictUiEntity с новым numericCode → Msg.CurrentDict(updated) [implement]
  - ✅ flowCurrentDict() использует combine(prefsFlow, flowDictionaryList()) вместо prefsFlow.map { getDictionaryById } [contract_effect_msg]
  - ✅ Room UPDATE словаря → flowDictionaryList() эмитит → combine пересчитывает → Msg.CurrentDict(updated) [contract_effect_msg]
  - ✅ UseCase интерфейс DictionaryAppBarUseCase сохраняет прежние сигнатуры (3 метода) [usecase]
  - ✅ DictionaryAppBarUseCaseImpl.flowCurrentDict() использует combine вместо map [usecase]
  - ✅ Новых зависимостей и DAO-методов не добавляется [usecase]
- ✅ Пользователь переключает текущий словарь в picker → appbar показывает иконку нового словаря [spec]
  - ✅ Лог: ###DictAppBar### текущий словарь переключён на id={id} — changeDict(id) записывает новый ID в prefs → getLongFlow эмитит → combine пересчитывает с новым ID из list → Msg.CurrentDict(newDict) [implement]
  - ✅ ChangeDict → DatasourceEffect.ChangeDict → prefs записывает ID → getLongFlow эмитит → combine пересчитывает → Msg.CurrentDict(newDict) [contract_effect_msg]

## Ручное тестирование

### Сценарий 1: Обновление флага текущего словаря
1. Открыть приложение, перейти на вкладку со словарями
2. Убедиться что текущий словарь в appbar показывает placeholder (буква на сером фоне)
3. Тап на словарь в списке → открывается форма редактирования
4. Выбрать флаг для словаря → сохранить
5. Вернуться назад на вкладку со словарями
6. Ожидание: appbar picker показывает выбранный флаг вместо placeholder — сразу, без дополнительных действий
7. Логи:
   - `###DictAppBar### текущий словарь обновлён реактивно после смены флага`

### Сценарий 2: Переключение между словарями с разными флагами
1. Иметь минимум два словаря — один с флагом, другой с placeholder
2. Переключить текущий словарь через picker в appbar
3. Ожидание: иконка в appbar сменилась на иконку выбранного словаря
4. Переключить обратно
5. Ожидание: иконка вернулась к предыдущей
6. Логи:
   - `###DictAppBar### текущий словарь переключён на id={id}`

_model: claude-opus-4-6_
