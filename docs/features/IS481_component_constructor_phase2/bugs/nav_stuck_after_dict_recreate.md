# Баг: Components-экраны не открываются после удаления + создания словаря (без рестарта app)

## Симптомы

**Сценарий воспроизведения (стабильный):**
1. В приложении удалить все компоненты.
2. Удалить все словари.
3. Создать новый словарь.
4. Тыкнуть на «Компоненты» (или через AppBar в Vocabulary, или Settings → Конструктор компонентов).
5. **Окно не открывается.** Экран остаётся прежним, ничего не происходит.

**Обходной путь:** полностью закрыть приложение и заново запустить → всё работает нормально.

**Затрагивает оба экрана:**
- `PerDictionaryComponentsScreen` (через AppBar в Vocabulary).
- `ComponentsManagerScreen` (через Settings → Конструктор компонентов).

Юзер прямо подтвердил: «и из настроек оно тоже не загружается, т.е. как-то тоже связано с новым айди словаря».

## Что НЕ помогло

| Гипотеза | Что сделано | Результат |
|---|---|---|
| DI/KSP cache не пересобрался — Factory.create() throws | Не правил DI, но юзер сделал clean rebuild | Не помогло |
| `launchSingleTop = true` reuse'ит existing entry | Убран из `goToPerDictionaryComponents`/`goToComponentsManager` | Не помогло |
| BottomBarWidget сохраняет saved state → `restoreState=true` при navigate | Добавлен `restoreState = false` | Не помогло |
| Saved state продолжает restoring stale entries | Добавлен `popUpTo(startDestination) { saveState = false }` + `restoreState = false` | Не помогло |
| `clearBackStack(routeTemplate)` actively очищает saved state | Добавлен `clearBackStack(routeTemplate)` перед navigate | Не проверено ещё после rebuild |

## Что показывают логи (с моими диагностическими `[diag]` метками)

**Цепочка работает до `navigate()` включительно:**

```
06-25 05:24:33.336 ###LEXEME###: AppBar Reduce ---message---: OpenPerDictionaryComponents(dictionaryId=10)
06-25 05:24:33.336 ###LEXEME###: AppBar Reduce --toEffect--: OpenPerDictionaryComponents(dictionaryId=10)
06-25 05:24:33.337 ###MATE###: RunEffect: OpenPerDictionaryComponents(dictionaryId=10)
06-25 05:24:33.337 ###DICT_COMPONENTS###: [diag] DictionaryAppBarNavEffectHandler onScreenEffect: OpenPerDictionaryComponents(dictionaryId=10)
06-25 05:24:33.338 ###DICT_COMPONENTS###: [diag] calling barNavigator.openPerDictionaryComponents(10)
06-25 05:24:33.338 ###DICT_COMPONENTS###: [diag] DictionaryAppBarNavigatorImpl.openPerDictionaryComponents(dictionaryId=10) invoking onOpen lambda
06-25 05:24:33.338 ###DICT_COMPONENTS###: [diag] goToPerDictionaryComponents(dictionaryId=10) BEFORE navigate; currentDestination=settings; backQueue=[null, vocabulary, settings]
06-25 05:24:33.366 ###DICT_COMPONENTS###: [diag] goToPerDictionaryComponents AFTER navigate; currentDestination=per_dict_components/{dictionaryId}; backQueue=[null, vocabulary, per_dict_components/{dictionaryId}]
06-25 05:24:33.366 ###DICT_COMPONENTS###: [diag] DictionaryAppBarNavigatorImpl.openPerDictionaryComponents lambda returned
06-25 05:24:33.366 ###DICT_COMPONENTS###: [diag] barNavigator.openPerDictionaryComponents returned
```

**И ВСЁ.** После этого — тишина. Никаких логов от:
- `composable(per_dict_components/{id}) BLOCK invoked` — внутри `NavGraphBuilder.composable { ... }` lambda.
- `PerDictionaryComponentsScreenDep ENTER composable` — `CompositionRootImpl`.
- `PerDictionaryComponentsViewModel INIT` — ViewModel ctor/init.
- `ComponentsForDictionaryFlowHandler.subscribe()` — flow handler.
- `###DICT_COMPONENTS###: Reduce` — Reducer первого msg.

`NavHost` не рендерит destination.

## Что вижу в backQueue

- ДО navigate: `[null, vocabulary, settings]` — юзер физически был на Settings tab.
- ПОСЛЕ navigate: `[null, vocabulary, per_dict_components/{dictionaryId}]` — `popUpTo(startDestination)` отработал (settings popped), destination push'нут.

Backstack корректный. NavController хранит правильный entry. **Но composable lambda не вызывается.**

## Crash check

`adb logcat -d | grep -iE 'fatal|exception|crash|caused'` для PID polytrainer — **никаких exception от приложения нет.** App не падает. Activity жива (`ResumedActivity: MainActivity`).

## База данных (для контекста)

После удаления словарей и создания нового:
- `dictionaries`: 1 запись (новый).
- `component_types`: 4 записи — 1 built-in `translation` (active) + 3 user-defined soft-deleted (`removed_at != null`, все с `dictionary_id=null`).
- `component_values`, `lexemes`, `words`: пусто.

БД целая, schema OK.

## Ключевая улика: **рестарт приложения чинит баг**

Это означает что **bug — это persisted in-memory state**. Что-то живёт в process'е:
- `rememberNavController()` создаёт NavController который остаётся pinned на жизнь Composable MainScreen. Activity destroyed → новый NavController.
- ViewModelStoreOwner per Activity → новый.
- Compose Navigation `savedStates` Map — internal field, persisted между navigate'ами.

## Архитектура навигации

`MainScreen.kt`:
```kotlin
val navController = rememberNavController()
NavHost(navController, startDestination = TabPoint.VOCABULARY.route) {
    vocabulary(navController, compositionRoot, openDictionaryCreate)
    quiz(navController, compositionRoot, openDictionaryCreate)
    statistic(navController, compositionRoot, openDictionaryCreate)
    settings(navController, compositionRoot, openDictionaryList)
}
BottomBarWidget(navController = navController)
```

**Один flat NavGraph** под startDestination=`vocabulary`. Все destinations (включая `per_dict_components/{dictionaryId}` и `components_manager`) — siblings.

**`BottomBarWidget` использует `saveState = true` + `restoreState = true`** при tab-switch (`BottomBarWidget.kt:77-83`):
```kotlin
navController.navigate(tab.route) {
    popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
    }
    launchSingleTop = true
    restoreState = true
}
```

Это и есть исходник в-памяти persistance — saved state накапливается каждое переключение tab'ов и не очищается.

## Open hypothesis (не подтверждена, не опровергнута)

**NavHost не recompose'ится при push нового destination** из-за того что Compose Navigation восстанавливает saved state entry с тем же route template вместо создания нового. При этом `currentBackStackEntryAsState` обновляется (backstack показывает новый entry), но composable lambda для этого destination всё равно не invokes.

Возможно проблема глубже — в `NavBackStackEntry.lifecycle` который не переходит в `RESUMED` для нового entry из-за конфликта с saved state.

## Текущее состояние diagnostic-логов (нужно снять при следующем тесте)

| Точка | Tag | Лог |
|---|---|---|
| `Settings.kt` Reducer | `###LEXEME###` | `Reduce --message--: OpenComponentsManager` |
| `SettingsNavigationEffectHandler` | `###SETTINGS###` | `[diag] onScreenEffect: $effect` |
| `SettingsNavigatorImpl.openComponentsManager()` | `###ALL_COMPONENTS###` | `[diag] invoking onOpen lambda` |
| `Settings.kt:goToComponentsManager()` | `###ALL_COMPONENTS###` | `[diag] BEFORE/AFTER navigate; backQueue=...` |
| `Settings.kt:composable(COMPONENTS_MANAGER_ROUTE)` lambda body | `###ALL_COMPONENTS###` | `[diag] composable(components_manager) BLOCK invoked` |
| `CompositionRootImpl.ComponentsManagerScreenDep` | `###ALL_COMPONENTS###` | `[diag] ENTER composable` |
| `ComponentsManagerViewModel.init` | `###ALL_COMPONENTS###` | `[diag] VM INIT before/after Mate` |
| `AllUserDefinedTypesFlowHandler.subscribe()` | `###ALL_COMPONENTS###` | `[diag] subscribe() called`, `launch{} entered`, `EMIT received` |
| `ComponentsManagerReducer.reduce()` | `###ALL_COMPONENTS###` | `Reduce --prevState/message/newState--` |

Симметричные логи для PerDict через `###DICT_COMPONENTS###`.

**Последний лог который реально появляется в logcat** при воспроизведении бага — `goToPerDictionaryComponents AFTER navigate`. Всё что после — тишина.

## Что попробовать ещё

1. **`clearBackStack(routeTemplate)` перед navigate** — ❌ **КРАШИТСЯ с NullPointerException в `NavControllerImpl.restoreStateInternal:1319`**. Этот crash сам по себе ДОКАЗЫВАЕТ что saved state в Compose Navigation `savedStates` Map corrupted после удаления словарей.
2. **`popBackStack(route, inclusive=true, saveState=false)` перед navigate** — В работе сейчас. Принудительно pops entry + не сохраняет state, без NPE.
3. **Убрать `saveState=true` + `restoreState=true` в `BottomBarWidget.kt:77-83`** — root cause накопления state. Side effect: scroll/state не сохраняется между tab-switches.
4. **Force NavController recreation через `key(...)`** — wrapped `navController` в `key(dictionariesVersion)` чтобы пересоздавать при invalidation. Drastic. Side effect: backstack теряется при ВСЕХ изменениях в-памяти state.
5. **🎯 Onboarding redirect (предложение юзера):** **Не показывать «Компоненты» если словарей нет**. После удаления всех словарей UI должен принудительно redirect на onboarding/create-dict экран → юзер не может попасть в bad state. Эта гипотеза побочно фиксит navigation issue потому что после удаления всех словарей backstack нормализуется на onboarding screen. Реализация: в Vocabulary tab при `hasNoDictionary=true` — рендерить full-screen «Создайте первый словарь», блокировать AppBar dropdown (тогда «Компоненты» в AppBar недоступны).
6. **Проверить gradle dependency:** актуальная ли версия `androidx.navigation:navigation-compose`? Версия `2.7.x` имеет known bug в `restoreStateInternal` (наш NPE). Upgrade до `2.8.+` может фиксить.
7. **Минимальный repro в отдельном проекте** — pet-app с двумя destinations + BottomBar `saveState=true` + повторный navigate same route template. Если воспроизводится — framework bug. Если нет — что-то specific к нашему code.
8. **`navController.handleDeepLink`** или **глобальный invalidate**: при удалении последнего словаря явно очищать NavController state через `popBackStack(startDestination, false)` чтобы backstack гарантированно был clean.

## Воспроизводимость

Стабильно при сценарии «удалить всё → создать новый словарь → тыкнуть Компоненты». 100% повторяемость.

Без шага «удалить» — навигация работает (то есть первое открытие после создания словаря работает).

## Затронутые файлы (с моими diagnostic edits)

- `app/src/main/java/me/apomazkin/polytrainer/navigator/SettingsNavigatorImpl.kt` — Log.d
- `app/src/main/java/me/apomazkin/polytrainer/navigator/DictionaryAppBarNavigatorImpl.kt` — Log.d
- `app/src/main/java/me/apomazkin/polytrainer/uiDeps/CompositionRootImpl.kt` — logger.log в ComponentsManager/PerDict ScreenDep
- `modules/screen/main/src/main/java/me/apomazkin/main/Vocabulary.kt` — Log.d, `clearBackStack` + `popUpTo(saveState=false)` + `restoreState=false`
- `modules/screen/main/src/main/java/me/apomazkin/main/Settings.kt` — Log.d, `popUpTo(saveState=false)` + `restoreState=false`
- `modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/SettingsNavigationEffectHandler.kt` — `LexemeLogger` ctor + logger.log
- `modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/DictionaryAppBarNavigationEffectHandler.kt` — Log.d
- `modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/ComponentsManagerViewModel.kt` — logger в init блок
- `modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/PerDictionaryComponentsViewModel.kt` — то же
- `modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/mate/AllUserDefinedTypesFlowHandler.kt` — `subscribe()` + `launch{}` + per-emit логи
- `modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/mate/ComponentsForDictionaryFlowHandler.kt` — то же

## Когда баг будет починен — снести diagnostic-логи

Все `[diag]` логи + `android.util.Log.d` calls в navigator'ах нужно удалить после фикса. `Log.d` в navigator'ах — против гайда `docs/guides/logging.md`, оставлять нельзя.
