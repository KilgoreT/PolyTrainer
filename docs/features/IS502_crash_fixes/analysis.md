# IS502 | Аналитика крашей и фиксы

Вход: два стектрейса Crashlytics и скрин консоли (файлы рядом), код
master `8aae8c15`, релизные теги 0.1.4–0.1.7.

---

## Крэш A: nav-гонка `DICTIONARY_CREATE`

### Механика

```
IllegalStateException: State must be at least 'CREATED' to be moved to 'DESTROYED'
  in component NavBackStackEntry ... route=DICTIONARY_CREATE?editId={editId}
  at NavControllerImpl.updateBackStackLifecycle
  at NavControllerImpl.navigate
  at ListNavigatorImpl.openCreate            ← тап «Новый словарь»
```

После `navigate` `NavController` пересчитывает lifecycle всех entry.
Падает entry самой формы: он ещё INITIALIZED, а пересчёт требует
DESTROYED — переход запрещён `LifecycleRegistry`. Состояние возникает,
когда вторая навигация стартует, пока entry первой не прогрелся:
двойной тап / два эффекта подряд.

Версия 0.1.7 — код до внедрения внешнего mate (`ListNavigatorImpl`). В
master навигацию исполняет `AppNavigationExecutor` с `launchSingleTop`,
но `singleTop` сравнивает только `currentDestination` и класс гонки не
закрывает.

### Фикс

`AppNavigationExecutor`, одно место на все экраны. Mate не трогается:
гейту нужен `NavController` (lifecycle entry), а mate от навигационной
библиотеки не зависит.

1. **Гейт `navigateGated`:** верхний entry — тот же экран (по первому
   сегменту route) и ещё не RESUMED → переход в полёте, повтор
   пропускается с логом `push skipped | route=… reason=in-flight`.
   Цена: быстрый тап по двум разным словам в окне перехода откроет
   первое.
2. **Последний рубеж:** `execute` ловит только `IllegalStateException`
   → `logger.e(NAV, …)` → Crashlytics non-fatal через `CrashlyticsSink`.

**Проверка:** смоук двойных тапов на девайсе 27.09 — дубль
`Push(WordCard 2867)` через 250 мс отброшен гейтом; `FATAL` и
`execute failed` — ноль.

---

## Крэш B: миграция на локальной release-сборке

### Механика

```
MateException: Mate runtime error: EffectFailed(effect=CheckInit,
  cause=IllegalStateException: Migration didn't properly handle: component_types)
  at MateFailPolicy.Strict
```

База на девайсе — версия 11, но с `component_types` промежуточной
схемы, оставленной непрод-сборкой. `Migration_011_to_012` создаёт
таблицу `CREATE TABLE IF NOT EXISTS` — видит существующую, пропускает,
и пост-миграционная проверка Room падает.

### Почему это не баг прода

| Релиз | Версия БД | `component_types` |
|---|---|---|
| 0.1.4, 0.1.5 | 11 | нет (entities без `ComponentTypeDb`) |
| 0.1.6, 0.1.7 | 13 | есть, финальная схема |
| master | 13 | без изменений |

Каждый релиз поднимает версию ровно одной миграцией на все вошедшие
фичи — промежуточных схем у юзеров из магазина не бывает. Миграции не
меняем.

### Откуда ложная тревога и фикс

Версия `Debug (1)` — это не debug-сборка (у неё Crashlytics был выключен
и версия `Debug-dev`), а **локальная release-сборка** без версии из CI.
Сбор включался по `!BuildConfig.DEBUG`, поэтому любая release —
магазинная и локальная — слала отчёты в прод-корзину.

Фикс — единый выключатель отправки (спека `logger`, «Отправка в
Firebase»):

- `app/build.gradle.kts`: значение `REMOTE_LOG_LEVEL` по умолчанию для
  release зависит от `BuildSource` — `CI_PROD` → `WARNING`, иначе
  `NONE`;
- `App.initRemoteReporting`: отправка ⇔ `REMOTE_LOG_LEVEL` не `NONE` —
  `setCrashlyticsCollectionEnabled` + `setAnalyticsCollectionEnabled`,
  строка `remote reporting: on|off | level=…` в лог;
- манифест: автостарт Crashlytics и Analytics выключен.

**Проверка (27.09):**

| Вариант | Как проверено | Результат |
|---|---|---|
| debug без флага | девайс, лог | `remote reporting: off \| level=NONE` |
| debug с `-PREMOTE_LOG_LEVEL=WARNING` | девайс, лог | `remote reporting: on \| level=WARNING` |
| локальный release без флага | сгенерированный `BuildConfig` | `NONE` |
| локальный release с флагом | сгенерированный `BuildConfig` | `WARNING` |
| магазинная (`CI_PROD`) | `BuildConfig` при имитации `CI_PROD` | `WARNING` |

Не проверено локально: фактическая доставка в консоль Firebase и запуск
магазинной сборки на девайсе — проявится на первом релизе (строка
`remote reporting: on` и события в прод-корзине).

Release-политика `MateFailPolicy` остаётся в Backlog.
