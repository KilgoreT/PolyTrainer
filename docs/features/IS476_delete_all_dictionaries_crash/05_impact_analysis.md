# Impact Analysis — IS476 / delete_all_dictionaries_crash

## 1. Анализ каждого варианта

### Вариант A — Nullable Flow (`Flow<DictUiEntity?>`)

**Side effects.**
- Контракт `flowCurrentDict()` меняется на nullable в двух UseCase (AppBar, DictionaryTab). Подписчики — два `DatasourceEffectHandler` — получают `null` напрямую и обязаны прокинуть его дальше как `Msg`.
- `Msg.CurrentDict(current: DictUiEntity?)` (AppBar) — параметр становится nullable. State `currentDict` уже nullable — Reducer почти не меняется, только тип параметра в сообщении.
- `Msg.SelectDictionary` (DictionaryTab) — параметр становится nullable; State **сейчас не содержит ссылку на текущий словарь**, придётся либо добавить поле, либо ввести флаг `hasNoDictionary` (см. memory MEMORY.md — «UI flags must be explicit fields in state»).
> 📎 guide: docs/guides/state-and-extensions.md — "Явные поля для каждого UI-элемента ... Вычисление происходит в редьюсере, а не в composable"
- `getCurrentDict(): DictUiEntity?` ломает callsite `LoadTermFlow` (`DatasourceEffectHandler.kt:56`) — `getCurrentDict().id.toInt()` сейчас non-null. Нужна ветка: при `null` либо пропустить эффект (`Msg.NoOperation`), либо эмитнуть пустой Paging-Flow.
- `QuizChatUseCaseImpl.getCurrentDictionaryId(): Long?` — затрагивает `QuizGameImpl.kt:174`. Сейчас id используется в `coEvery { quizChatUseCase.getCurrentDictionaryId() } returns 1L` в тесте — тест не сломается (1L → Long?), но runtime-вызывающие должны обработать null. Реалистично — либо gating на навигации (нельзя открыть chat без словаря), либо эмит пустого quizList.
- `DictionaryUseCaseImpl.deleteDictionary()` начинает чистить orphaned pref (`setLong(null)`). Это меняет состояние pref'а во всех читателях — но все читатели после фикса корректно обработают `null` (см. fallback в `id?.let { ... } ?: list.firstOrNull()`).
> 📎 guide: docs/guides/prefs-datastore.md — "Nullable Flow — ОБЯЗАТЕЛЬНАЯ обработка null ... fallback при null: id?.let { ... } ?: dictionaryApi.getDictionaryList().firstOrNull()"

**Контракты.**
- Интерфейс `DictionaryAppBarUseCase` (модуль `widget/dictionaryAppBar`) — публичный для DI; смена возврата `Flow<DictUiEntity>` → `Flow<DictUiEntity?>` ломает реализацию в `app/.../di/module/widget/DictionaryAppBarUseCaseImpl.kt`. Других реализаций нет.
- `DictionaryTabUseCase` (модуль `screen/dictionaryTab`) — то же: одна реализация в `app`.
- `QuizChatUseCase` — реализация в `app/.../di/module/quizchat/QuizChatUseCaseImpl.kt`. Контракт меняется.
- Класс `DictionaryNotFoundException` (`dictionarytab/deps`) перестаёт использоваться по назначению — кандидат на удаление.

**Тесты.**
- Существующих тестов на `flowCurrentDict()` / `DatasourceEffectHandler` для AppBar и DictionaryTab — нет (`ExampleUnitTest.kt` пустой в AppBar). Падать нечему.
- `QuizGameImplEmptyListTest` уже мокает `getCurrentDictionaryId() returns 1L` — на nullable сигнатуру это `returns 1L as Long?` (mockk прозрачно проглатывает), тест не упадёт.
- Нужно добавить:
  - тест на reducer AppBar: `Msg.CurrentDict(null)` → `state.currentDict == null`;
  - тест на reducer DictionaryTab: `Msg.SelectDictionary(null)` → пустое состояние (или флаг);
  - тест на `DictionaryUseCaseImpl.deleteDictionary()`: при удалении последнего словаря pref чистится (`setLong(null)`);
  - регресс-тест: `flowCurrentDict()` на пустом списке эмитит `null`, а не бросает.
> 📎 guide: docs/guides/testing-reducers.md — "Граничные случаи: пустые списки, NOT_IN_DB id, null значения"

**Качество кода.**
- Улучшается: throw как событие реактивного потока — антипаттерн (guide `effect-handlers.md`, `prefs-datastore.md`). Уход на nullable выравнивает источник со State (`state.currentDict: DictUiEntity?` уже есть).
> 📎 guide: docs/guides/prefs-datastore.md — "ЗАПРЕЩЕНО — throw на null ... ← краш при первом запуске"
> 📎 guide: docs/guides/effect-handlers.md — "Flow останавливается в FlowHandler — дальше только Messages"
- Связность: `getCurrentDict()` (suspend) и `flowCurrentDict()` (reactive) становятся согласованными — обе nullable, обе fallback'ятся на пустой список.
- Дублирование: класс `DictionaryNotFoundException` можно удалить — снимает 1 файл из `deps`.

**Сложность реализации.** Средняя. Изменения локализованы в нескольких файлах одного слоя (UseCase + один FlowHandler + один Reducer на модуль), но веток несколько (AppBar / DictionaryTab / QuizChat) и в DictionaryTab требуется расширить State.

**Риск регрессии.** Низкий-средний. Основной риск — ветка `LoadTermFlow` в `DictionaryTab.DatasourceEffectHandler`: если забыть обработать `getCurrentDict() == null`, при удалении всех словарей таб попытается вызвать `.id.toInt()` на null → NPE. Это надо явно покрыть тестом. Гонка между двумя эмиссиями (pref + Room) в combine безопасна — результат всё равно `null`.

---

### Вариант B — Sealed `CurrentDictState { Loading; Selected; Empty }`

**Side effects.**
- Новый sealed-тип нужен в shared-модуле (либо `dictionarypicker.entity`, либо новый shared между AppBar и DictionaryTab) — добавляет вопрос модульной локализации (где живёт `CurrentDictState`, какой модуль его экспортирует, кто импортирует).
- State каждого таба и виджета меняет тип поля `currentDict` с `DictUiEntity?` на `CurrentDictState` — это структурный рефакторинг State, а не точечная правка.
- UI меняется: `DictDropDownWidget(currentDict: DictUiEntity?)` сейчас принимает nullable; нужен либо адаптер `CurrentDictState → DictUiEntity?`, либо переписывание widget'а на when-ветки.
- `Loading` state — новая сущность, которой раньше не было. Combine эмитит только после первой эмиссии обоих upstream — между подпиской и первой эмиссией виджет на Loading. Меняет визуальное поведение: возможны мерцания, placeholder'ы.

**Контракты.**
- Все три UseCase'а (`DictionaryAppBarUseCase`, `DictionaryTabUseCase`, `QuizChatUseCase`) — sealed везде. `getCurrentDict()` suspend? Тоже sealed (`CurrentDictState.Selected | Empty`)? Это размывает простой suspend-контракт.
- `Msg.CurrentDict` → распадается на `CurrentDictSelected(dict)` + `CurrentDictAbsent` + опционально `CurrentDictLoading`. Reducer'ы во всех модулях — when-ветки.

**Тесты.**
- Все тесты на reducer'ах, где упоминается `currentDict`, ломаются — заменить `DictUiEntity?` на sealed. В DictionaryTab сейчас reducer'ов на текущий словарь нет, но добавить новые тесты на каждую ветку sealed обязательно.
- Тесты `QuizGameImplEmptyListTest` — переписывать мок `getCurrentDictionaryId()` под sealed или другой контракт.

**Качество кода.**
- Self-documenting: `Empty` как явное доменное состояние — это плюс. Соответствует effect-handlers guide «два разных Message вместо boolean-флага».
> 📎 guide: docs/guides/effect-handlers.md — "Вместо boolean-флага в State — два разных Message ... Без разделения нужен boolean-флаг в State — грязнее"
- НО — three-way distinction (`Loading | Selected | Empty`) overkill для UI, который и так бинарно реагирует: либо есть словарь, либо нет. `Loading` state в AppBar не имеет UI-обоснования (виджет либо рендерится с данными, либо вообще не рендерится — спека `dictionary-appbar.md`).
- Дублирование: sealed придётся копировать в каждый модуль (или вынести в общий — добавит зависимость).

**Сложность реализации.** Высокая. Поверхность изменений в 2-3 раза больше чем у A. Каждый модуль (AppBar, DictionaryTab, QuizChat, Quiz Game) трогается на уровне State/Reducer/Message + UI.

**Риск регрессии.** Высокий. Множество touch-point'ов в UI и Reducer'ах; легко пропустить ветку sealed (особенно `Empty` в редком кодпасе). Loading-state — поведенческое изменение, может вылезти в виде мерцания AppBar / placeholder'а на табе при холодной подписке.

---

### Вариант C — Чистка pref + nullable Flow (без рефакторинга suspend-методов)

**Side effects.**
- Реактивные стримы `flowCurrentDict()` (AppBar + DictionaryTab) меняются на nullable — **точно как в A**, та же поверхность.
- Suspend-методы `getCurrentDict()` (DictionaryTab) и `getCurrentDictionaryId()` (QuizChat) **остаются non-null** — throw в них сохраняется как мина.
- Защита для suspend-методов реализуется через **gating на навигации**: нельзя открыть `VocabularyTab` / `QuizChat` без словарей. Реалистично — пересмотреть `MainRouter`, `BottomBarWidget`, `SplashScreen`. Это **отдельный, разбросанный по нескольким файлам кусок работы** (см. список файлов в 04_solutions.md), у которого выше риск пропустить точку входа (deep-link, restoreState навбэка).
> 📎 guide: docs/guides/reducer-patterns.md — "Conditional навигация ... выбор между закрытием приложения и обычным back делается в reducer на основе state, не в composable"
- Orphaned pref чистится — общая часть с A.

**Контракты.**
- `Flow<DictUiEntity?>` — тот же breaking как в A.
- Suspend-контракты остаются — никакого ломания для них.
- `Msg` для AppBar — nullable как в A; для DictionaryTab — то же.

**Тесты.**
- Те же что в A для реактивной части.
- Gating-тесты — новые, на уровне навигации; легко забыть кейс (deep-link, recreate ViewModelStoreOwner).

**Качество кода.**
- Решение «дыра в архитектуре закрыта снаружи, а не внутри». `getCurrentDict()` остаётся с инвариантом «никогда не вызывайся при пустом списке» — этот инвариант проверяется навигацией, что разносит ответственность по разным слоям.
- Концептуально хуже A: бизнес-правило («нет словаря — валидное состояние») признаётся для Flow, но игнорируется для suspend — несогласованный контракт.
- Гонка остаётся: если пользователь успеет триггернуть `LoadTermFlow` ровно в момент CASCADE-delete (например, через быстрый swipe), `getCurrentDict()` может стрельнуть до того, как gating успеет среагировать.

**Сложность реализации.** Реактивная часть — низкая (как A, но меньше файлов). Gating — средняя, размазана по навигации.

**Риск регрессии.**
- Gating-точки легко пропустить → краш остаётся скрытой миной. Это **главный риск** варианта C.
- В отличие от A, не закрывает корень проблемы — закрывает только активные подписки. Любой будущий callsite `getCurrentDict()` без gating'а — рецидив бага.

---

### Вариант D — Stop-on-empty / sentinel в FlowHandler

**Side effects.**
- D.1 (sentinel `DictUiEntity.NONE` с `id = -1`) — sentinel может незаметно протечь в UI/БД. Пример: AppBar отрендерит `FlagPlaceholderWidget` с пустой буквой, а на тапе по dropdown'у `ChangeDict(EMPTY_DICT)` улетит в `useCase.changeDict(-1)` → попытка записать `-1` в pref → orphaned reference другого типа. Защита — дисциплина в каждом callsite'е, компайл-тайм проверок нет.
- D.2 (два Flow: `flowCurrentDict()` nullable + `flowHasAnyDict()` Boolean) — две подписки, два source of truth, гонка между ними. По сути это вариант A в одёжке non-null для одного из Flow + дополнительный Flow.

**Контракты.**
- D.1: non-null контракт сохраняется (формально), но семантика ломается — sentinel это nullable в маскировке. Хуже nullable, потому что компилятор не помогает.
- D.2: два Flow в UseCase, FlowHandler подписывается на оба → две корутины, два сообщения, объединение в Reducer. Дополнительная сложность подписок.

**Тесты.**
- D.1: тестировать sentinel сложнее — нужно проверять `id == -1` везде. Регресс-тестам надо ловить «не протёк ли sentinel в Room/pref».
- D.2: добавить тест на синхронизацию двух Flow.

**Качество кода.**
- D.1 — антипаттерн magic value. Хуже nullable.
- D.2 — концептуально допустимо, но переусложнено для текущей задачи.

**Сложность реализации.** Средняя для D.1, средняя-высокая для D.2.

**Риск регрессии.** Высокий для D.1 (sentinel через UI и БД), средний для D.2 (две подписки, гонка).

---

### Вариант E — try/catch в FlowHandler

**Side effects.**
- `.catch { }` глотает исключение, после чего **upstream Flow завершается**. AppBar после удаления всех словарей не реагирует на создание нового словаря, пока ViewModel не пересоздана. Это незаметная регрессия — пользователь создаёт словарь, ничего не происходит, путается.
- Источник дефекта (`throw DictionaryNotFoundException`) не убирается — мина живёт.
- Orphaned pref не чистится — мина в pref'е тоже живёт.
- `getCurrentDict()` suspend и `getCurrentDictionaryId()` остаются без защиты — рецидив через любой будущий callsite.

**Контракты.** Не меняются.

**Тесты.** Один тест на `.catch` — но он не покроет регрессию «после первой эмиссии Flow мёртв».

**Качество кода.** Антипаттерн по 3 guide'ам сразу. Прямое нарушение `docs/guides/effect-handlers.md` («ошибки реактивного источника обязан обрабатывать UseCase/FlowHandler, не пропуская исключения наружу» — guide подразумевает обработку через nullable/sealed, а не через try/catch). Прямое нарушение `docs/guides/prefs-datastore.md`.
> 📎 guide: docs/guides/effect-handlers.md — "Без сложной логики в хендлерах — они выполняют эффект и конвертируют результат в сообщение"
> 📎 guide: docs/guides/prefs-datastore.md — "ЗАПРЕЩЕНО — throw на null ... ← краш при первом запуске"

**Сложность реализации.** Низкая.

**Риск регрессии.** Высокий — Flow умирает после первой ошибки, виджет/таб становятся неотзывчивыми.

---

## 2. Таблица сравнения

| Критерий | A | B | C | D | E |
|---|:---:|:---:|:---:|:---:|:---:|
| Закрывает корень (`throw` в Flow) | да | да | да | да (sentinel/sealed) | **нет** (только перехват) |
| Закрывает корень (`throw` в suspend) | да | да | **нет** (gating) | **нет** | **нет** |
| Соответствие guide'ам | да | да | частично | **нет** (D.1) / да (D.2) | **нет** |
| Поверхность изменений | средняя | **высокая** | низкая-средняя | средняя | минимальная |
| Изменение публичных API | да | да (sealed) | да (только Flow) | D.1 нет / D.2 да | нет |
| Изменение State (структурное) | плюс поле в Tab | везде sealed | плюс поле в Tab | плюс поле/Boolean | нет |
| Качество кода (читаемость/связность) | улучшает | улучшает (overkill) | приемлемо | **ухудшает** (D.1) | **ухудшает** |
| Сложность реализации | средняя | **высокая** | низкая+средняя | средняя | низкая |
| Риск регрессии | низкий-средний | **высокий** | средний (gating) | **высокий** (D.1) | **высокий** (мёртвый Flow) |
| Покрывает все 3 точки дефекта | да | да | частично | частично | **нет** |
| Чистит orphaned pref | да | да | да | независимо | нет |
| Новые тесты (примерно) | 3-5 | 10+ | 2-4 + gating | 4-6 | 1 |

---

## 3. Рекомендация

```
**Рекомендуется: вариант A**

Обоснование (2-3 предложения): Вариант A прямой и согласованный — nullable Flow убирает throw как событие реактивного потока (анти-паттерн по двум guide'ам), выравнивает источник с уже-nullable State (`state.currentDict: DictUiEntity?`) и закрывает все три точки дефекта одним подходом. Поверхность изменений умеренная, тестов мало, downstream-код минимально затронут — Reducer AppBar почти готов, в DictionaryTab нужно явно расширить State (что согласуется с правилом из memory о flags-as-explicit-fields), QuizChat решается nullable Long?. Это «убрать мину», а не «обойти её».

Главный риск: ветка `LoadTermFlow` в `DictionaryTab.DatasourceEffectHandler` — `getCurrentDict().id.toInt()` сейчас non-null, на nullable превратится в NPE, если забыть обработать. Закрывается явным тестом и обязательным расширением State `DictionaryTab` (поле для «нет словаря» или редирект на DICTIONARY_SETUP).
```

_model: claude-opus-4-7[1m]_
