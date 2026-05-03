# Checklist

- ✅ Пользователь открывает AppBar dropdown для словаря без флага → видит серый круг с первой буквой названия вместо пустоты
  - ✅ [contract_state] State не затрагивается: title уже доступен через currentDict
  - ✅ [design_tree] DictDropDownWidget: else-ветка с FlagPlaceholderWidget, letter = currentDict?.title?.firstOrNull()
- ✅ Пользователь видит пункт dropdown-меню для словаря без флага → видит серый круг с первой буквой названия вместо пустоты
  - ✅ [contract_state] State не затрагивается: title передаётся параметром
  - ✅ [design_tree] ItemDictMenuWidget: else-ветка с FlagPlaceholderWidget, letter = title.firstOrNull()
- ✅ Пользователь видит список словарей для словаря без флага → видит серый круг с первой буквой названия вместо generic иконки
  - ✅ [contract_state] State не затрагивается: name доступен через item.name
  - ✅ [design_tree] DictionaryListItemWidget: замена Icon(ic_tab_vocabulary) на FlagPlaceholderWidget, letter = item.name.firstOrNull()
- ✅ Пользователь открывает форму словаря без флага → placeholder отображается как раньше (регрессия после переноса)
  - ✅ [contract_state] State не затрагивается: меняется только import
  - ✅ [design_tree] DictionaryFormWidget: import → me.apomazkin.ui, добавлен modifier = Modifier.size(48.dp)

## Ручное тестирование

### Placeholder в AppBar dropdown
1. Создать словарь без выбора флага
2. Перейти на главный экран
3. Посмотреть на иконку текущего словаря в AppBar
4. Ожидание: серый круг с первой буквой названия словаря (uppercase)

### Placeholder в пункте dropdown-меню
1. Создать два словаря: один с флагом, один без
2. Тапнуть на dropdown в AppBar
3. Посмотреть на пункт словаря без флага
4. Ожидание: серый круг с первой буквой названия

### Placeholder в списке словарей
1. Перейти в раздел управления словарями
2. Посмотреть на элемент списка для словаря без флага
3. Ожидание: серый круг с первой буквой названия вместо generic иконки

### Регрессия формы словаря
1. Открыть форму существующего словаря без флага
2. Посмотреть на секцию с иконкой флага
3. Ожидание: серый круг с первой буквой названия (48dp), такой же как был до переноса
