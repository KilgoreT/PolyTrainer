# Checklist

- [ ] Пользователь открывает AppBar dropdown для словаря без флага → видит серый круг с первой буквой названия вместо пустоты [spec]
  - [ ] Лог: ###DictDropDown### placeholder shown for dictionary without flag [spec]
  - [ ] State не затрагивается: title уже доступен через currentDict [contract_state]
- [ ] Пользователь видит пункт dropdown-меню для словаря без флага → видит серый круг с первой буквой названия вместо пустоты [spec]
  - [ ] Лог: ###ItemDictMenu### placeholder shown for dictionary without flag [spec]
  - [ ] State не затрагивается: title передаётся параметром [contract_state]
- [ ] Пользователь видит список словарей для словаря без флага → видит серый круг с первой буквой названия вместо generic иконки [spec]
  - [ ] Лог: ###DictionaryList### placeholder shown instead of generic icon [spec]
  - [ ] State не затрагивается: name доступен через item.name [contract_state]
- [ ] Пользователь открывает форму словаря без флага → placeholder отображается как раньше (регрессия после переноса) [spec]
  - [ ] Лог: ###DictForm### placeholder displayed after module relocation [spec]
  - [ ] State не затрагивается: меняется только import [contract_state]
  - [ ] UseCase не требуется: нет Effects, нет обращений к данным [usecase]

## Ручное тестирование

### Placeholder в AppBar dropdown
1. Создать словарь без выбора флага (numericCode = null)
2. Перейти на главный экран
3. Посмотреть на иконку текущего словаря в AppBar
4. Ожидание: серый круг с первой буквой названия словаря (uppercase)
5. Логи:
   - `###DictDropDown### placeholder shown for dictionary without flag`

### Placeholder в пункте dropdown-меню
1. Создать два словаря: один с флагом, один без
2. Тапнуть на dropdown в AppBar для раскрытия меню
3. Посмотреть на пункт словаря без флага
4. Ожидание: серый круг с первой буквой названия, рядом текст названия словаря
5. Логи:
   - `###ItemDictMenu### placeholder shown for dictionary without flag`

### Placeholder в списке словарей
1. Перейти в раздел управления словарями
2. Посмотреть на элемент списка для словаря без флага
3. Ожидание: серый круг с первой буквой названия вместо generic иконки словаря
4. Логи:
   - `###DictionaryList### placeholder shown instead of generic icon`

### Регрессия формы словаря
1. Открыть форму существующего словаря без флага
2. Посмотреть на секцию с иконкой флага
3. Ожидание: серый круг с первой буквой названия (48dp), такой же как был до переноса
4. Логи:
   - `###DictForm### placeholder displayed after module relocation`

## log_messages
- Создан чеклист: 4 корневых бизнес-сценария (AppBar, dropdown item, список, форма) с черновиками логов
- Ручное тестирование: 4 сценария покрывают все точки применения placeholder

_model: claude-opus-4-6-20250502_
