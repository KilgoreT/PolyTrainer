# IS536 | Ручники: леммы из текста и списки (IS537), плашки под словом (IS538)

Форма — `backend/docs/handbook/feature-flow/spec-checks.md`, раздел 3:
заголовок кейса несёт адреса строк спек устройства — AWP
(`docs/handbook/specs/words-add-from-photo/spec.md`), TFP
(`backend/docs/handbook/specs/tokens-from-photo/spec.md`), LFT
(`backend/docs/handbook/specs/lemmas-from-text/spec.md`), LFL
(`backend/docs/handbook/specs/lexemes-from-lemma/spec.md`), WCL
(`docs/handbook/specs/word-card-lexemes/spec.md`). Кейсы 1–10 — этап 1:
окно со списками; кейсы 11–22 — этап 2 по спеке v3: плашки словаря и
плашки нейронки под словом, запись по одной лексеме. Кейсы первой версии
— окно с карточками и диалог карточки слова — заморожены в
`manual_tests-v1.md`, кейсы карточки под словом по спеке v2 с их прогонами
— в `manual_tests-v2.md`. В чат кейсы выдаются по одному: счётчик,
название, шаги-атомы по одному действию, последний шаг «Напиши “ок”»;
длинный кейс режется на несколько. Состояние и лог после «ок» проверяет
агент; шаги с пометкой «агент:» делает агент сам по `adb` или `ssh`.
Основные юзкейсы проходит пользователь сам, гонки и повторные нажатия —
юнит-тесты.

Вход для всех кейсов: Pixel 7 Pro, dev-сборка `co.lexeme.app.dev` с ключом
в `local.properties`, сервер `api.lexeme.pro` v0.1.37 (контракт 0.5.0:
`POST /v1/lemmas/from-tokens`, `POST /v1/lexemes/from-lemma`). Снимать:
`adb logcat -d | grep "###SCAN###\|###VOCAB###\|###NETWORK###"`, перед
кейсом `adb logcat -c`. На время прогона таймаут экрана 30 минут
(`adb shell settings put system screen_off_timeout 1800000`), после —
вернуть прежний (600000).

## Подготовка данных

- Словарь `en` → `ru` на вкладке «Слова» («био» из первой версии
  годится) с группами «Harry Potter 1» и «Finance»; в нём руками заведены:
  слово «study» с лексемой «Перевод: исследование» в группе «Harry Potter
  1» — старое; второе слово «study» с лексемой «Перевод: кабинет» без
  групп, добавлено позже — самое новое; слово «link» без значений. Для
  этапа 1 хватает одного «study» без значений.
- Словарь `es` → `ru` («ёла») — для испанских снимков; групп в нём нет,
  компонента «Род» нет: так приходит рекомендация завести «Род».
- Снимки в галерее: абзац учебника из контрольного набора (тот же, что в
  прогоне 2026-10-23); скриншот плотной страницы биографии
  (`es_dense_screenshot`, 28 строк, Estela Medina); альбомный снимок
  путеводителя с камеры (`en_guide_landscape`, первые строки «CENTRO
  HOTELS», «DOWNTOWN. Isabel la Católica»); скриншот заметки с одной
  строкой «Las nubes corrían por el cielo.»; белый лист `blank.png` и
  пустой файл `empty.png` (0 байт) — `adb push` в Pictures и
  просканировать.
- Ожидания по спискам — по контрольному набору
  `backend/docs/features/IS536_llm_values/control_set.md`, раздел
  «Извлечение словарных форм (v2)». Состав списков нейронка может менять
  в мелочах (открытые промахи — журнал
  `backend/docs/handbook/specs/lexemes-from-lemma/llm-journal.md`):
  сверять число слов, порядок и разделы, одиночные расхождения
  записывать в прогон.
- Снимок должен быть на языке словаря вкладки: «correr» в словаре
  `en` → `ru` сервер ищет в английской базе и отвечает «Не найдено»
  (оповещение о чужом словаре — в бэклоге).

## Словарь логов

### SCAN — экран снимка (`###SCAN###`)

- `Reduce ---message---: <Msg>` — входящее сообщение. Тяжёлые — именем
  класса: `TextRecognized`, `LemmasFound`, `FormLoaded`, `LexemesFound`,
  `LemmasReloaded`; остальные со значениями: `TextEmpty`,
  `RecognitionFailed(errorRes=…)`, `LemmasEmpty`, `LemmasDenied(errorRes=…)`,
  `LemmasUnavailable(errorRes=…)`, `FormFailed(errorRes=…)`, `RetryLemmas`,
  `ToggleTranslation`, `RequestClose`, `OpenSheet(sheet=…)`, `CloseSheet`,
  `SetCommonGroup(groupId=…)`, `NoOperation`.
- `Reduce ---step---: <атом> | k=v` — один шаг состояния; у сообщения
  цепочка шагов. Атомы:
  - запуск: `startAnalyzing | tokens=K`, `showNoWords`, `showError |
    errorRes=…` (без «Повторить»), `showRetryableError | errorRes=…` (с
    «Повторить»), `startFormLoading | dictionaryId=D words=M` (M — слов
    всех списков без повторов);
  - окно: `applyForm | groups=G existing=E`, `markInDictionary |
    inDictionary=N`, `toggleTranslation | expanded=true|false`;
  - группа и листы: `setCommonGroup | groupId=…`, `openSheet |
    sheet=CommonGroup|CardTarget|CardGroup|Option|TextEdit`, `closeSheet`,
    `updateSheetDraft | length=N`;
  - под словом (этап 2): `openSavedCard | word=W saved=S` (S — плашек
    словаря), `startLookup | word=W`, `applyLexemes | word=W lemma=L
    lexemes=N` (L — лемма ответа, карточка остаётся на тексте W),
    `refreshCardValues | word=W active=A`, `refreshCardMarks | word=W
    exists=E lacksCore=C`, `showCardNotFound | word=W res=…`,
    `showCardError | word=W res=…` (без «Повторить»),
    `showCardRetryableError | word=W res=…` (с «Повторить»), `toggleCard
    | word=W expanded=true|false`, `toggleCardMatches | word=W
    expanded=…`, `removeWord | word=W` («Убрать из списка»);
  - плашка нейронки, адрес — слово и ключ лексемы: `dismissLexeme |
    lemma=W key=K` («Не нужно»), `removeEmptyWord | word=W` (ушла
    последняя плашка — слово прочь), `setLexemeTarget | lemma=W key=K
    target=Existing(wordId=…)|NewWord`, `setLexemeGroup | lemma=W key=K
    group=…`, `refreshCardSaved | word=W saved=S`;
  - правки, адрес — слово (лемма карточки): `deleteValue | lemma=W key=K
    componentId=C index=I`, `applyTextEdit | lemma=W … length=N`,
    `dropEmptyTexts | …`, `resetText | …`, `setOption | lemma=W key=K
    componentId=C option=O`, `refreshLemmaValues | lemma=W`,
    `refreshLemmaMarks | lemma=W`, `refreshAllCards | cards=N`;
  - запись плашки и перечитка: `startAddingLexeme | word=W key=K
    target=NewWord|Existing(wordId=…)`, `finishAdding | word=W key=K`,
    `retargetNewWordLexemes | lemma=W wordId=…` (после «Создать»: «Куда»
    остальных плашек — на созданное слово), `startLemmasReload | word=W`,
    `applyReloadedLemmas | word=W lemmas=N`, `showAddFailure | word=W
    key=K failure=…`, `clearAddFailure | word=W key=K`, `resetDeadGroup |
    groupId=…`, `resetDeadTarget | wordId=…`, `dropComponent | typeId=…`;
  - закрытие: `askCloseConfirm | pending=N`, `closeCloseConfirm`, `close`.
- Сообщения этапа 2 со значениями: `LoadLexemes(text=W)`,
  `OpenSavedCard(text=W)`, `ToggleCard(text=W)`, `RemoveWord(text=W)`,
  `AddLexeme(text=W, key=K)` («Создать» или «Перенести»), `RetryAdd(text=W,
  key=K)`, `DismissLexeme(text=W, key=K)`, `ToggleComponentMatches(text=W)`,
  `LexemesNotFound(text=W, textRes=…)`, `LexemesDenied(text=W,
  errorRes=…)`, `LexemesUnavailable(text=W, errorRes=…)`,
  `WordCreated(text=W, key=K, wordId=…)`, `LexemeAdded(text=W, key=K)`,
  `AddGroupGone(…)`, `AddWordGone(…)`, `AddComponentGone(…)`,
  `AddFailed(text=W, key=K, failure=…)`, `LemmasReloadFailed(text=W)`,
  правки `DeleteValue(ref=…)`, `SubmitText(ref=…, text=…)`,
  `ResetText(ref=…)`, `SetOption(ref=…, option=…)`, `UpdateDraft(text=…)`,
  `SetCardTarget(lemma=W, key=K, target=…)`, `SetCardGroup(lemma=W, key=K,
  group=…)`, `ConfirmClose`, `Stay`.
- `Reduce ---step---: no-op | reason=…` — событие пришло и намеренно
  проигнорировано: `markInDictionary: no form`, `closeSheet: no sheet`,
  `noOperation` (действие общих виджетов, которого у экрана нет);
  этап 2: `applyLexemes: no word W`, `refreshCardValues: no card W`,
  `refreshCardMarks: no card W` — ответ пришёл на убранное слово
  (AWP-48.4); `requestClose: adding` — крестик или «назад» во время записи
  (AWP-51.12); `startAddingLexeme: not writable K` — плашка «уже в
  словаре» или без ядра; `retargetNewWordLexemes: nothing to change` —
  после «Перенести» или когда плашек с «новое слово» не осталось;
  `removeEmptyWord: lexemes left W` — плашки ещё есть, слово остаётся;
  `startLemmasReload: no word W` — слово уже ушло, перечитка не нужна;
  `lemmasReloadFailed: W` — перечитка упала, плашки словаря как были;
  `<шаг>: nothing to change` — правка по адресу, которого уже нет.
- `RunEffect: <эффект>` — эффект ушёл в хендлер: `RecognizeText(uri=…)`,
  `ExtractLemmas(tokens=K)`, `LoadForm(dictionaryId=D, lemmas=M)`,
  `LookupLemma(word=W, lemma=L, dictionaryId=D, text=N)` (N — длина
  текста снимка), `CreateWord(word=W, key=K)`, `TransferLexeme(word=W,
  key=K, wordId=…)`, `ReloadLemmas(word=W, dictionaryId=D)`.
- Исходы юзкейса: `recognize outcome=text tokens=K | empty`; `lemmas
  outcome=dict=D lexical=A proper=B function=C unparsed=U | no words |
  denied=<причина> | unavailable`; `form outcome=loaded components=C
  options=O groups=G existing=E | dictionary missing | failed: …`;
  этап 2: `lexemes word=W outcome=found lemmas=1 recommendations=R
  existing=E | not found | no lexemes | denied=<причина> | unavailable |
  word failed`; `create word=W key=K outcome=added words=1 lexemes=1 |
  rejected <причина>`; `transfer word=W key=K outcome=…` так же; `reload
  word=W outcome=loaded … existing=E | dictionary missing | failed: …`.
- Номера запуска у экрана нет: у каждого снимка свой экран, ответ
  закрытого экрана некуда доставить. Номер запуска живёт в сети —
  заголовок `X-Run-Id`, растёт на каждый запрос процесса (TFP-01).

### VOCAB — вкладка «Слова», диалог снимка (`###VOCAB###`)

- `Reduce ---message---: OpenScanDialog | ChooseCamera | ChooseGallery |
  ImagePicked(uri=…) | CameraUnavailable(errorRes=…) | CloseScanDialog`.
- `Reduce ---step---: showScanDialog | hideScanDialog | showScanDialogError
  | errorRes=…`.
- `RunEffect: LaunchCamera | LaunchGallery | OpenScanCandidates(uri=…)`.

### NETWORK — слой сети (`###NETWORK###`)

- `POST /v1/lemmas/from-tokens -> <код> in <мс>ms` — одна строка на
  обмен; ключ не печатается. `503` — нейронка недоступна, `429` — лимит
  аккаунта на сегодня, `400` — язык словаря не поддерживается, `413` —
  слишком много токенов.
- `POST /v1/lexemes/from-lemma -> <код> in <мс>ms` — запрос значений
  слова (этап 2), один на тап или «Проверить по тексту»; коды те же,
  `404` — словарной формы в базе нет («Не найдено»).

### Сервер (`ssh … docker logs lexeme-server`)

- `lemmas from tokens request` — запрос принят: `run_id` (номер запуска
  устройства), число токенов, языки; `llm extract` — вызов нейронки:
  модель, токены, время; `lemmas from tokens` — итог: число форм по
  спискам, «не разобрано».
- Этап 2: строки `lexemes from lemma` — запрос по лемме принят (лемма,
  языки, есть ли контекст) и итог (число лексем, «в тексте»); вызов
  нейронки на отбор — строка `llm …` с моделью и временем.

## Кейсы

## TFP-27.1, AWP-35.1, AWP-35.3, AWP-36.1, AWP-36.2 Кнопка-камера: по ключу, только на «Слова», прячется с панелью ввода

Вход:
- dev-сборка с ключом; словарь `en` → `ru` на вкладке «Слова»

Шаги:
1. Открыть вкладку «Слова».
2. Нажать «+»; закрыть панель ввода.
3. Открыть вкладку «Группы»; вернуться на «Слова».

Ожидания:
- экран: на «Слова» кнопка-камера над «+»
- экран: пока панель ввода открыта — нет ни кнопки-камеры, ни «+»; после
  закрытия обе снова на месте
- экран: на «Группы» кнопки-камеры нет

Анти-проверки:
- лог: нет `POST`, нет `OpenScanDialog`

Прогон: ✅ 2026-10-27 — кнопка на «Слова», с панелью ввода прячется, на
«Группы» нет; лог: `OpenAddWordDialog`/`CloseAddWordDialog`, без `POST`

## AWP-37.1, AWP-38.1, AWP-38.2, AWP-40.1, AWP-40.7, AWP-41.2, AWP-39.1, AWP-14.1, AWP-14.2, AWP-60.1, TFP-28.1, LFT-02.1 Абзац учебника: индикаторы, окно со списками, перевод, «в словаре», закрытие

Вход:
- словарь `en` → `ru` со словом «study»; снимок абзаца учебника в галерее

Шаги:
1. Кнопка-камера → «Галерея» → снимок абзаца; смотреть подписи.
2. Дождаться окна; попробовать нажать вкладку «Группы» и выбор словаря.
3. Тап по «Перевод» — раскрыть; тап ещё раз — свернуть.
4. Найти «study» в «Смысловые»; пролистать до конца.
5. Нажать системную «назад».
6. Повторить шаг 1; дождаться окна; закрыть крестиком.

Ожидания:
- экран: по кнопке-камере — выбор «Камера / Галерея»; подписи «Распознаю
  текст…» (с «Отменить») → «Анализирую текст…» → «Собираю окно…»; затем
  окно на весь экран
- экран: наверху общая группа «Все»; блок «Перевод» свёрнут; раскрытый
  — русский перевод абзаца, после второго тапа снова свёрнут
- экран: «Смысловые» — 24 формы в порядке контрольного набора: study,
  link, early, friendship, high-quality, sibling, relationship, child,
  experience, rewarding, birth, likely, have, good, brother, sister,
  endure, childhood, say, university, publish, journal, family,
  psychology; «Имена собственные» — Laurie, Kramer, Illinois, December;
  «Служебные» — with, who, a, before, the, of, be, to, that, or,
  throughout, their, in; раздела «Не разобрано» нет
- экран: у «study» пометка «в словаре», у остальных слов пометки нет
- экран: вкладки и выбор словаря не нажимаются, окно на месте
- экран: «назад» → окно закрыто сразу, вкладка «Слова»; крестик → то же;
  слов в словаре не прибавилось
- лог: `recognize outcome=text tokens=K`, `startAnalyzing | tokens=K`,
  `ExtractLemmas(tokens=K)`, `POST /v1/lemmas/from-tokens -> 200`,
  `lemmas outcome=dict=D lexical=24 proper=4 function=13 unparsed=0`,
  `startFormLoading | dictionaryId=D words=41`, `form outcome=loaded …
  existing=1`, `applyForm | groups=G existing=1`, `markInDictionary |
  inDictionary=1`, `toggleTranslation | expanded=true`, затем
  `expanded=false`, `RequestClose` → `close`
- сервер: `lemmas from tokens request` с `run_id` из `X-Run-Id`, один
  `llm extract`

Анти-проверки:
- лог: `POST` ровно один на снимок; нет `showError`, нет `FATAL`
- экран: ни «Повторить», ни вопроса при закрытии

Прогон: ⏳ 2026-10-27 — абзац сканирован дважды (бэкенд v0.1.34 и
v0.1.35), окно собралось, 53 токена, ответ 11–13 с; списки и перевод
оценены в журнале RUN (v2 — 96/40, v3 — 98/60, открыт `better`); не
проверено: «study» не заведён, пометки «в словаре» нет, «назад» и
крестик не нажимались — догнать

## AWP-40.2, AWP-40.8, TFP-12.2 Скриншот плотной страницы: «не разобрано», прокрутка целиком

Вход:
- словарь «ёла» (`es` → `ru`); скриншот `es_dense_screenshot` в галерее

Шаги:
1. Переключиться на «ёла»; кнопка-камера → «Галерея» → скриншот; дождаться
   окна.
2. Прокрутить окно до конца.
3. Закрыть крестиком.

Ожидания:
- экран: четыре раздела; «Не разобрано» — обрывки краёв страницы в порядке
  текста: ожидаем sh, e, ier, destibrg (LFT-13.1); другой состав от
  нейронки — записать в прогон
- экран: первые «Смысловые» — слова первых строк скриншота, сверху вниз
  (скриншот без метки ориентации читается как на экране)
- экран: окно прокручивается целиком до последнего раздела, «ещё N» нет
- лог: `recognize outcome=text tokens=K` (K порядка 200), `lemmas
  outcome=… unparsed=U` (U ≥ 1), `applyForm`, `markInDictionary`

Анти-проверки:
- лог: нет `showError`, нет `FATAL`

Прогон:

## AWP-41.1, TFP-12.1 Перевод предложения; альбомный снимок с меткой ориентации

Вход:
- «ёла»; скриншот «Las nubes corrían por el cielo.»; словарь `en` → `ru`;
  снимок `en_guide_landscape`

Шаги:
1. На «ёла»: кнопка-камера → «Галерея» → скриншот предложения; дождаться
   окна; раскрыть «Перевод».
2. Закрыть; переключиться на `en` → `ru`; кнопка-камера → «Галерея» →
   альбомный снимок путеводителя; дождаться окна.
3. Закрыть.

Ожидания:
- экран, предложение: перевод «Облака бежали по небу.» (или близкий);
  «Смысловые» — nube, correr, cielo; «Служебные» — el, por
- экран, путеводитель: «Смысловые» начинаются словами первых строк —
  centro, hotel, downtown…; мусора от чтения «боком» (обрывки по одной-две
  буквы в смысловых) нет
- лог: оба запуска `… -> 200`, у путеводителя `recognize outcome=text
  tokens=K` (K порядка 100)

Анти-проверки:
- лог: нет `RecognitionFailed`, нет `FATAL`

Прогон:

## TFP-10.3, TFP-11.1, TFP-19.1, AWP-54.1, AWP-54.2 Белый лист и пустой файл: «Слов не найдено», «Не удалось распознать текст»

Вход:
- словарь `en` → `ru`; в галерее `blank.png` и `empty.png`

Шаги:
1. Кнопка-камера → «Галерея» → `blank.png`; дождаться.
2. «Закрыть»; кнопка-камера → «Галерея» → `empty.png`; дождаться.
3. «Закрыть».

Ожидания:
- экран, `blank.png`: индикатор закрылся, окна нет, «Слов не найдено» и
  «Закрыть», без «Повторить»; «Закрыть» → вкладка «Слова»
- экран, `empty.png`: «Не удалось распознать текст» и «Закрыть», без
  «Повторить»
- лог: `recognize outcome=empty` → `TextEmpty` → `showNoWords`;
  `RecognitionFailed(errorRes=…)` → `showError`

Анти-проверки:
- лог: ни одного `POST`; нет `FATAL`

Прогон:

## AWP-53.5, TFP-26.1, TFP-10.2, AWP-38.3, TFP-21.2 Без сети: распознаёт, «Сервер не ответил», «Повторить» с теми же токенами; поворот и «Отменить» на ожидании

Вход:
- словарь `en` → `ru`; снимок абзаца учебника

Шаги:
1. Включить режим полёта.
2. Кнопка-камера → «Галерея» → абзац; дождаться текста.
3. Выключить режим полёта; нажать «Повторить»; пока «Анализирую текст…»,
   повернуть экран и вернуть; дождаться окна; закрыть.
4. Кнопка-камера → «Галерея» → абзац; пока «Анализирую текст…», нажать
   «Отменить».

Ожидания:
- экран: в полёте «Распознаю текст…» проходит, затем «Сервер не
  ответил», под ним «Закрыть» и «Повторить»
- экран: после «Повторить» снова «Анализирую текст…», без «Распознаю
  текст…»; после поворота индикатор и подпись на месте; затем окно со
  списками
- экран: после «Отменить» — вкладка «Слова», окно не открывается
- лог, шаг 2: `recognize outcome=text tokens=K`, `startAnalyzing |
  tokens=K`, `lemmas outcome=unavailable`, `showRetryableError`
- лог, шаг 3: `RetryLemmas` → `startAnalyzing | tokens=K` с тем же K, без
  второго `RecognizeText`; ровно один `POST … -> 200`; после поворота
  второго `POST` нет
- лог, шаг 4: `RequestClose` → `close` до `LemmasFound`; после `close` нет
  `applyForm`

Анти-проверки:
- экран: в полёте нет «Слов не найдено»
- лог: нет `FATAL`

Прогон:

## TFP-21.3, TFP-21.4, TFP-08.3, TFP-08.4, TFP-07.1, TFP-06.3 Камера: закрыта без снимка, отмена во время распознавания, второй снимок; камера отключена

Вход:
- словарь `en` → `ru`; `adb shell run-as co.lexeme.app.dev ls cache/photos`
  пусто

Шаги:
1. Кнопка-камера → «Камера» → закрыть камеру без снимка.
2. Кнопка-камера → «Камера» → снять страницу с текстом; пока «Распознаю
   текст…», нажать «Отменить».
3. Сразу кнопка-камера → «Камера» → снять другую страницу; дождаться
   окна; закрыть.
4. Кнопка-камера (выбор открыт); агент: `adb shell pm disable-user
   --user 0 com.google.android.GoogleCamera`; нажать «Камера»; закрыть
   выбор.
5. Кнопка-камера.
6. Агент: `adb shell pm enable com.google.android.GoogleCamera`.

Ожидания:
- экран, шаг 1: после закрытия камеры — снова выбор способа, без ошибки
- экран, шаги 2–3: после «Отменить» — вкладка; второй снимок распознаётся
  и даёт своё окно, слова — со второй страницы
- экран, шаг 4: «Камера недоступна» в выборе
- экран, шаг 5: в выборе только «Галерея»
- лог: `ImagePicked(uri=…)` дважды; у первого `RequestClose` → `close`;
  окно второго — своим `startAnalyzing`/`applyForm`;
  `CameraUnavailable(errorRes=…)` → `showScanDialogError`
- состояние: после шага 3 папка `cache/photos` пуста

Анти-проверки:
- экран: результат первого снимка не всплывает ни на вкладке, ни во втором
  окне
- лог: после `close` первого экрана нет его `applyForm`

Прогон:

## TFP-22.4, TFP-23.1, AWP-56.2 Гибель процесса: галерея открыта; во время ожидания ответа; новый снимок после возврата

Вход:
- словарь `en` → `ru`; снимок абзаца; `adb` под рукой

Шаги:
1. Кнопка-камера → «Галерея»; не выбирая снимок, агент: `adb shell am
   kill co.lexeme.app.dev`; выбрать абзац.
2. Дождаться окна; закрыть.
3. Кнопка-камера → «Галерея» → абзац; пока «Анализирую текст…», нажать
   Home; агент: `adb shell am kill co.lexeme.app.dev`; вернуться через
   «Недавние».
4. Кнопка-камера → «Галерея» → абзац; дождаться окна; закрыть.

Ожидания:
- экран, шаг 1: приложение ожило, экран снимка открылся и распознал
  абзац, окно собралось
- экран, шаг 3: вкладка «Слова», ни индикатора, ни окна, ни ошибки
- экран, шаг 4: обычный запуск с начала, окно
- лог: после шага 1 `ImagePicked(uri=content://…)`, `recognize
  outcome=text tokens=K`; после шага 3 нет `LemmasFound` и `applyForm`
  убитого запуска; шаг 4 — свои `RecognizeText` и `POST … -> 200`

Анти-проверки:
- лог: после шага 1 нет `RecognitionFailed`; после шага 3 нет `FATAL`

Прогон:

## AWP-38.4 Сервер молчит две минуты: индикатор, ошибки нет

Вход:
- словарь `en` → `ru`; снимок абзаца; агент на сервере по `ssh`

Шаги:
1. Агент: `docker pause lexeme-server`.
2. Кнопка-камера → «Галерея» → абзац; ждать две минуты на «Анализирую
   текст…».
3. Агент: `docker unpause lexeme-server`; дождаться окна; закрыть.

Ожидания:
- экран: все две минуты индикатор «Анализирую текст…» с «Отменить»,
  ошибки нет; после снятия паузы — окно
- лог: один `POST /v1/lemmas/from-tokens -> 200 in Nms`, N больше 120000

Анти-проверки:
- лог: нет `showRetryableError`, нет второго `POST`

Прогон:

## TFP-27.2, AWP-35.2 Сборка без ключа: кнопки-камеры нет

Вход:
- `local.properties` без `LEXEME_ACCESS_KEY`; свежая dev-сборка

Шаги:
1. Открыть вкладку «Слова».
2. Открыть карточку любого слова.

Ожидания:
- экран: кнопки-камеры на вкладке нет; в карточке нет «Добавить
  лексемы-кандидаты»

Анти-проверки:
- лог: нет `POST`

Прогон:

## AWP-43.1, AWP-43.4, AWP-57.1, AWP-57.3, AWP-18.2, AWP-29.2, AWP-30.2, AWP-30.5, AWP-31.4, AWP-45.1, AWP-45.2, AWP-45.3, AWP-47.1 «correr»: плашки нейронки с «в тексте», без «Куда», «Создать» и «Не нужно» у каждой; «Замечания» без кнопок, «Сопоставление», подпись источника

Вход:
- словарь `es` → `ru` без «Рода» и без слова «correr»; скриншот «Las
  nubes corrían por el cielo.» в галерее

Шаги:
1. Кнопка-камера → «Галерея» → скриншот предложения; дождаться окна.
2. Осмотреть окно до тапов.
3. Тап «correr» в «Смысловые»; смотреть признак загрузки.
4. Осмотреть плашки: пометки, значения, кнопки, разделы.
5. Тап по заголовку «Сопоставление».
6. Тап по заголовку «Сопоставление» ещё раз.
7. Тап по подписи «Данные: Wiktionary, CC BY-SA 4.0»; вернуться.

Ожидания:
- экран: до тапа подписи источника нет; после тапа на «correr» признак
  загрузки, затем подпись «Найдено по тексту» и три плашки в порядке
  ответа — «течь, литься, струиться (о жидкости)» с пометкой «в тексте»,
  затем «бегать; бежать», «бежать, проходить, лететь (о времени)»
  (состав нейронка может менять — сверять по журналу); у второй и
  третьей пометки «в тексте» нет; признака загрузки нет
- экран: в плашке «Перевод: …», «Часть речи: глагол», «Толкование: …»,
  строки «Род» нет; поля «Куда» нет — «correr» в словаре нет; «Группа:
  как у всех» без стрелки — групп в словаре нет; у каждой плашки
  «Создать» (активна) и «Не нужно»
- экран: раздел «Замечания» со строкой «У лексем-кандидатов есть «Род».
  Заведите в конструкторе компонент «Род» (выбор, опции: …), и значения
  лягут в него», кнопок в разделе нет; «Сопоставление» свёрнуто, по тапу
  строки «translations → Перевод», «Часть речи: глагол», …, второй тап
  сворачивает
- экран: под плашками подпись «Данные: Wiktionary, CC BY-SA 4.0», тап
  открывает `https://creativecommons.org/licenses/by-sa/4.0/`
- лог: `LoadLexemes(text=correr)`, `startLookup | word=correr`,
  `RunEffect: LookupLemma(word=correr, lemma=correr, dictionaryId=D,
  text=N)`, `POST /v1/lexemes/from-lemma -> 200`, `lexemes word=correr
  outcome=found lemmas=1 recommendations=1 existing=0`, `LexemesFound`,
  `applyLexemes | word=correr lemma=correr lexemes=3`, `refreshCardValues
  | word=correr active=A`, `refreshCardMarks | word=correr exists=0
  lacksCore=0`, `toggleCardMatches | word=correr expanded=true`, затем
  `expanded=false`
- сервер: `lexemes from lemma` с `run_id` из `X-Run-Id`, один вызов
  нейронки

Анти-проверки:
- лог: ровно один `POST /v1/lexemes/from-lemma`; нет `showCardError`,
  `showCardRetryableError`, `FATAL`
- экран: у окна без плашек нейронки подписи источника нет; «Куда» нет

Прогон: ⏳ 2026-10-30 — словарь «Mexico» (es → ru, 3 компонента, групп
нет), «correr» в нём уже есть, поэтому новым словом взято «nube»: один
`POST /v1/lemmas/from-tokens -> 200 in 3497ms`, смысловые 3, служебные 2,
у «correr» «в словаре» (`markInDictionary | inDictionary=1`); тап «nube»
— один `POST /v1/lexemes/from-lemma -> 200 in 1930ms`, `found lemmas=1
recommendations=0`, `applyLexemes | lexemes=1`; экран: «Найдено по
тексту», плашка «в тексте», «Перевод: облако, туча», «Часть речи:
Существительное ▾», «Группа: Как у всех» без стрелки, «Куда» нет, «Не
нужно» и активная «Создать», «Сопоставление» свёрнуто, подпись источника
под плашками. Не проверено: плашка одна — «в тексте» только у первой
(43.4) не видно; «Замечаний» нет — сервер не дал рекомендаций; ссылка
подписи не нажималась, виджет тот же, что в прогоне v2

## AWP-42.1, AWP-42.2, AWP-42.4, AWP-42.5, AWP-64.1, AWP-64.2, AWP-64.3, AWP-64.4, AWP-62.5 «friendship» и «sibling»: два слова раскрыты, сворачивание, ответ в свёрнутое, «Не нужно» у плашки; у последней — слово уходит, новый снимок возвращает

Вход:
- словарь `en` → `ru`; снимок абзаца учебника

Шаги:
1. Кнопка-камера → «Галерея» → абзац; дождаться окна.
2. Тап «friendship».
3. Тап «sibling».
4. Тап «sibling» ещё раз — свернуть, пока грузится.
5. Дождаться ответов; тап «sibling» — раскрыть.
6. У «friendship» нажать «Не нужно» у второй плашки (если плашка одна —
   пропустить).
7. Тап «friendship» — свернуть.
8. Тап «friendship» — раскрыть.
9. У «friendship» нажать «Не нужно» у оставшейся плашки.
10. Закрыть окно; новый снимок абзаца → окно.

Ожидания:
- экран: под «friendship» и под «sibling» свои плашки; свёрнутый на
  загрузке «sibling» после раскрытия показывает пришедшие плашки
- экран: у «friendship» после «Не нужно» осталась одна плашка; после
  сворачивания и раскрытия — та же плашка, без запроса; после «Не нужно»
  у последней строки «friendship» в списке нет, остальные слова на месте
- экран: в новом окне «friendship» снова в «Смысловые»
- лог: `startLookup | word=friendship`, `startLookup | word=sibling`,
  `ToggleCard(text=sibling)` → `toggleCard | word=sibling expanded=false`,
  `applyLexemes | word=sibling …`, затем `toggleCard | word=sibling
  expanded=true`; `DismissLexeme(text=friendship, key=…)` →
  `dismissLexeme | lemma=friendship key=…`, `refreshLemmaMarks |
  lemma=friendship`, `no-op | reason=removeEmptyWord: lexemes left
  friendship`; `toggleCard | word=friendship expanded=false`, затем
  `true`; на последнее «Не нужно» — `dismissLexeme`, `removeEmptyWord |
  word=friendship`

Анти-проверки:
- лог: ровно два `POST /v1/lexemes/from-lemma` на первое окно; после
  `toggleCard` нет `startLookup`; нет `CreateWord`

Прогон: ⏳ 2026-10-30 — вместо «friendship» слово «nube» в «Mexico» с
одной плашкой: «Не нужно» → `DismissLexeme(text=nube,
key=ruwiktionary/1.noun.1)`, `dismissLexeme`, `refreshLemmaMarks`,
`removeEmptyWord | word=nube`, без `POST` и без записи (AWP-64.4,
62.5); крестик → `RequestClose` → `close` без вопроса (AWP-60.1); новый
снимок той же фразы — «nube» снова в «Смысловые» (AWP-64.2). Не
проверено руками: «Не нужно» у одной из нескольких плашек (64.1) — в
кейсе «correr»; два раскрытых слова, сворачивание и повторное «Не нужно»
(42.1–42.5, 64.3) — юнит-тесты

## AWP-62.1, AWP-62.2, AWP-62.4, AWP-63.1, AWP-63.5, AWP-39.5, AWP-18.14, AWP-20.3, AWP-20.9, AWP-47.8, AWP-02.1, AWP-02.3, AWP-47.4, AWP-42.3, AWP-52.1, AWP-52.2, AWP-52.3, AWP-52.4 «Создать» и «Перенести»: «friendship» одной плашкой уходит; у «sister» «Создать» первой — плашка словаря тут же, у остальных «Перенести»; слова на вкладке, в группе и в тренировке

Вход:
- словарь `en` → `ru` с группой «Harry Potter 1» и компонентом «Пример»,
  без слов «friendship» и «sister»; снимок абзаца

Шаги:
1. Кнопка-камера → «Галерея» → абзац; дождаться окна.
2. Общая группа → «Harry Potter 1».
3. Тап «friendship»; дождаться плашки.
4. «Создать» у плашки «friendship».
5. Тап «sister»; дождаться плашек.
6. «Создать» у первой плашки «sister»; смотреть на кнопки остальных
   плашек, пока идёт запись.
7. Осмотреть, что под «sister».
8. «Перенести» у второй плашки «sister».
9. «Не нужно» у остальных плашек «sister», по одной.
10. Закрыть окно крестиком.
11. На вкладке «Слова» открыть карточку «friendship», затем «sister».
12. Запустить тренировку словаря, затем группы «Harry Potter 1».

Ожидания:
- экран: после «Создать» у «friendship» строки «friendship» нет,
  остальные 23 формы в прежнем порядке
- экран: пока идёт запись первой плашки «sister», кнопки всех плашек
  «sister» неактивны (если успели увидеть; второй записи в логе нет —
  AWP-02.3); после записи
  строка «sister» на месте с пометкой «в словаре», под ней «В словаре» —
  плашка «Harry Potter 1 · <сегодня>» с записанной лексемой; у оставшихся
  плашек «Куда: «sister» (…)» и кнопка «Перенести» вместо «Создать»,
  поля «Группа» нет
- экран: после «Перенести» второй плашки её лексема видна в плашке
  словаря «sister»; после «Не нужно» у последней строки «sister» нет, окно
  открыто; крестик закрывает сразу, без вопроса
- экран: на вкладке «Слова» «friendship» и «sister» без обновления
  руками; в карточке «friendship» та же лексема, что была в плашке, — те
  же имена компонентов и значения («Перевод: дружба»); в «sister» две
  лексемы, «Пример» с примерами записи; слова в тренировке словаря и
  группы «Harry Potter 1», кнопки «тренировать» нет
- лог: `AddLexeme(text=friendship, key=K)`, `startAddingLexeme |
  word=friendship key=K target=NewWord`, `RunEffect: CreateWord(word=
  friendship, key=K)`, `create word=friendship key=K outcome=added
  words=1 lexemes=1`, `WordCreated(text=friendship, key=K, wordId=…)`,
  `finishAdding | word=friendship key=K`, `no-op |
  reason=retargetNewWordLexemes: nothing to change`, `removeEmptyWord |
  word=friendship`, `no-op | reason=startLemmasReload: no word
  friendship`
- лог: у «sister» после `WordCreated` — `finishAdding`,
  `retargetNewWordLexemes | lemma=sister wordId=…`, `no-op |
  reason=removeEmptyWord: lexemes left sister`, `startLemmasReload |
  word=sister`, `RunEffect: ReloadLemmas(word=sister, dictionaryId=D)`,
  `reload word=sister outcome=loaded … existing=1`, `LemmasReloaded`,
  `applyReloadedLemmas | word=sister lemmas=1`, `markInDictionary |
  inDictionary=N`, `refreshCardMarks | word=sister …`; второго
  `startAddingLexeme` во время записи нет; на «Перенести» —
  `startAddingLexeme | word=sister key=K2 target=Existing(wordId=…)`,
  `RunEffect: TransferLexeme(word=sister, key=K2, wordId=…)`, `transfer
  word=sister key=K2 outcome=added words=1 lexemes=1`,
  `LexemeAdded(text=sister, key=K2)`, `finishAdding`, снова
  `startLemmasReload` и `applyReloadedLemmas`

Анти-проверки:
- лог: одна строка `create word=friendship`; у «sister» один `create` и
  один `transfer`; нет `showAddFailure`; между `startAddingLexeme` и
  `finishAdding` нет `OpenSheet`, `DismissLexeme`, `SetCardTarget`
- словарь: по одному слову «friendship» и «sister», дублей нет

Прогон: ⏳ 2026-10-30 — «био» пуст, общая группа «Harry Potter 1»
(`setCommonGroup | groupId=16`). «friendship» — две плашки: «Создать» у
первой → `CreateWord`, `create … outcome=added words=1 lexemes=1`,
`WordCreated(… wordId=26)`, `finishAdding`, `retargetNewWordLexemes |
wordId=26`, `no-op | reason=removeEmptyWord: lexemes left friendship`,
`ReloadLemmas`, `applyReloadedLemmas | lemmas=1`, `markInDictionary |
inDictionary=1`; экран: пометка «в словаре», плашка словаря «Harry
Potter 1 · 30.10.2026» с «Перевод: дружба», у второй плашки «Куда:
«friendship» (дружба)» и «Перенести», «Группы» нет (63.1, 63.5, 39.5,
18.14, 20.9, 08.6); «Перенести» → `TransferLexeme(… wordId=26)`,
`LexemeAdded`, `removeEmptyWord | word=friendship`, `no-op |
reason=startLemmasReload: no word friendship` (20.3, 62.1). «sister» —
три плашки: «Создать» у первой → слово 27, плашка словаря с «сестра» и
тремя примерами; «Перенести» у второй → в плашке словаря обе лексемы, у
третьей «Куда» прежнее (63.2, 63.4); «Не нужно» у третьей →
`removeEmptyWord | word=sister`; крестик → `close` без вопроса; карточка
«sister»: «Добавлено 30 октября 2026», группа «Harry Potter 1», две
лексемы, примеры с подписью «Wiktionary» (52.1, 52.4). Не проверено
руками: тренировка группы (52.3) — членство видно в карточке, квиз не
менялся; кнопки во время записи (47.4, 02.x) — запись 20–40 мс,
юнит-тесты

## AWP-46.1, AWP-46.3, AWP-46.4, AWP-46.5, AWP-45.4, AWP-49.1, AWP-49.6, AWP-57.2, AWP-50.1, AWP-50.14, AWP-47.2, AWP-47.6, AWP-18.13 «link» и «study» в словаре: плашки словаря без запроса и без «Куда», «Проверить по тексту», «уже в словаре» по обоим «study»

Вход:
- словарь `en` → `ru` с двумя «study» и «link» без значений (подготовка
  данных); снимок абзаца

Шаги:
1. Кнопка-камера → «Галерея» → абзац; дождаться окна.
2. Тап «link»; осмотреть.
3. Тап «study»; осмотреть.
4. «Проверить по тексту» у «study»; смотреть признак загрузки.
5. Дождаться ответа; осмотреть плашки.

Ожидания:
- экран: под «link» подпись «В словаре» и одна плашка — «Все · <дата
  добавления>» без значений; кнопки «Проверить по тексту» и «Убрать из
  списка»; запроса нет, подписи источника нет
- экран: под «study» две плашки словаря, самое новое первым: «Все ·
  <дата>» с «Перевод: кабинет», затем «Harry Potter 1 · <дата раньше>» с
  «Перевод: исследование»; поля «Куда» под плашками словаря нет; кнопки
  «Проверить по тексту» и «Убрать из списка»
- экран: после «Проверить по тексту» признак загрузки под плашками
  словаря, плашки на месте, кнопки «Проверить по тексту» нет; затем
  «Найдено по тексту» и плашки нейронки: совпавшие с «кабинет» или
  «исследование» — с пометкой «уже в словаре», без «Куда» и без кнопки
  записи, только «Не нужно»; у новых — «Куда: новое слово ▾», «Создать»
  и «Не нужно»; второй кнопки «Проверить по тексту» нет
- лог: `OpenSavedCard(text=link)` → `openSavedCard | word=link saved=1`;
  `OpenSavedCard(text=study)` → `openSavedCard | word=study saved=2`;
  `LoadLexemes(text=study)` → `startLookup | word=study`, `POST
  /v1/lexemes/from-lemma -> 200`, `applyLexemes | word=study lemma=study
  lexemes=N`, `refreshCardMarks | word=study exists=E lacksCore=0`

Анти-проверки:
- лог: при тапе «study» и «link» нет `POST`; `POST /v1/lexemes/from-lemma`
  один — на «Проверить по тексту»
- экран: под плашками словаря нет «Куда» и подписи источника

Прогон: ⏳ 2026-10-30 — вместо «study» и «link» слово «correr» в
«Mexico» с одной лексемой: тап → `openSavedCard | word=correr saved=1`
без `POST`; экран: «В словаре», плашка «Все · 29.10.2026», «Перевод:
бегать; бежать», «Часть речи: Глагол», «Куда» под плашкой словаря нет
(46.1, 46.5); «Проверить по тексту» → `startLookup | word=correr`, один
`POST /v1/lexemes/from-lemma -> 200 in 2199ms`, `found lemmas=1`,
`applyLexemes | lexemes=1`, `refreshCardMarks | exists=1`; экран:
плашка словаря на месте, «Найдено по тексту», плашка с пометками «в
тексте» и «уже в словаре», без «Куда» и «Группы», только «Не нужно»,
кнопок «Проверить по тексту» и «Убрать из списка» больше нет (49.1,
49.6, 50.1, 18.13, 47.6). Не проверено: две плашки словаря (46.3) и
пустая (46.4) — в «Mexico» одно слово; признак загрузки под плашками
(57.2) — ответ за 2 с; подпись до проверки (45.4) не снималась; «уже в
словаре» по двум словам (50.14) — юнит-тест. Нейронка для «Las nubes
corrían» снова выбрала «бегать» — открытый вопрос LFL-83.1

## AWP-18.1, AWP-18.3, AWP-18.4, AWP-18.8, AWP-18.9, AWP-18.10, AWP-18.12, AWP-20.10, AWP-20.11, AWP-63.2, AWP-63.3, AWP-63.4, AWP-62.6, AWP-64.5 «study»: «Куда» с листом по словам, «Перенести» в старое «study», «Создать» третье, явный выбор остаётся, «Не нужно» у «уже в словаре»

Вход:
- словарь `en` → `ru` с двумя «study» и группой «Harry Potter 1»; снимок
  абзаца; у «study» после проверки хотя бы две плашки без «уже в словаре»
  (иначе — одна, шаги с третьим словом пропустить)

Шаги:
1. Кнопка-камера → «Галерея» → абзац; дождаться окна; общая группа →
   «Harry Potter 1».
2. Тап «study»; «Проверить по тексту»; дождаться плашек.
3. «Куда: … ▾» у первой плашки без «уже в словаре»; осмотреть лист.
4. Выбрать «study» с «исследование».
5. «Перенести» у этой плашки; осмотреть плашки словаря.
6. У второй плашки без «уже в словаре» оставить «новое слово»; «Создать».
7. Осмотреть плашки словаря и «Куда» у оставшихся плашек.
8. «Не нужно» у плашки «уже в словаре».
9. Остальные плашки — «Не нужно»; на вкладке «Слова» найти все «study».

Ожидания:
- экран: лист «Куда» — «новое слово «study»» выбрано, затем строки
  «кабинет · 1 значение · Все · <дата добавления>», «исследование · 1
  значение · Harry Potter 1 · <дата раньше>», даты — дни добавления слов,
  не правок; после выбора «исследование» у этой плашки «Куда: «study»
  (исследование) ▾», кнопка «Перенести», поля «Группа» нет; у соседней
  плашки по-прежнему «новое слово»
- экран: после «Перенести» плашка словаря «исследование» показывает
  новую лексему, группа «Harry Potter 1» у неё как была; «Куда» соседней
  плашки не сдвинулось
- экран: после «Создать» третья плашка словаря «study» — «Harry Potter
  1 · <сегодня>» с созданной лексемой; у плашки, где «Куда» было выбрано
  руками, выбор остался; у плашек с «новое слово» — «Куда» стало третьим
  «study»
- экран: «Не нужно» у плашки «уже в словаре» убирает её; когда плашек не
  осталось — строки «study» нет; на вкладке «Слова» три «study»
- лог: `openSheet | sheet=CardTarget`, `SetCardTarget(lemma=study, key=K,
  target=Existing(wordId=…))` → `setLexemeTarget | lemma=study key=K
  target=Existing(wordId=…)`, `closeSheet`; `startAddingLexeme |
  word=study key=K target=Existing(wordId=…)`, `TransferLexeme(…)`,
  `transfer … outcome=added`, `LexemeAdded`, `finishAdding`, `no-op |
  reason=retargetNewWordLexemes: nothing to change`, `startLemmasReload`,
  `applyReloadedLemmas | word=study lemmas=2`; затем `startAddingLexeme |
  … target=NewWord`, `CreateWord(…)`, `WordCreated(…)`,
  `retargetNewWordLexemes | lemma=study wordId=…`, `applyReloadedLemmas |
  word=study lemmas=3`

Анти-проверки:
- лог: один `POST /v1/lexemes/from-lemma`; нет `showAddFailure`; у плашки
  «уже в словаре» нет `startAddingLexeme`
- словарь: у «study» (исследование) группа «Harry Potter 1» одна, лишних
  групп не появилось; у «study» (кабинет) ничего не изменилось

Прогон: ⏳ 2026-10-30 — «Не нужно» у единственной плашки «correr» в
«Mexico» с пометкой «уже в словаре»: `DismissLexeme(text=correr,
key=ruwiktionary/1.verb.1)`, `dismissLexeme`, `removeEmptyWord |
word=correr` — плашка «уже в словаре» уходит по «Не нужно», с ней ушло
слово (64.5; 62.6 без второй плашки); крестик → `close` без вопроса.
«sister» в «био» (одно слово, 2 лексемы, «Harry Potter 1»): новый снимок
— `existing=2`, `inDictionary=2`, тап → `openSavedCard | saved=1` без
`POST`; «Проверить по тексту» → три плашки, `exists=2`: две «уже в
словаре» только с «Не нужно», у третьей «Куда: новое слово ▾», «Группа:
Как у всех ▾», «Создать» (18.3, 47.6, 50.1); «Куда» → лист «Куда добавить
значения «sister»»: строка «сестра, (старшая) медсестра, сестра · 2
значения · Harry Potter 1 · 30.10.2026», последней выбранное «новое
слово «sister»» (18.1, 18.9, 18.10); выбор строки → `setLexemeTarget |
target=Existing(wordId=27)`, `closeSheet`; «Перенести» →
`TransferLexeme(… wordId=27)`, `added`, `LexemeAdded`, `no-op |
reason=removeEmptyWord: lexemes left sister`, `ReloadLemmas`,
`refreshCardMarks | exists=2` (18.12, 20.10, 63.2). Пример 18.1 в спеке
поправлен под лист: «новое слово» последним и без приписки «третье в
словаре» — так было и в v2. «brother» в «био», три плашки: своя группа
«Finance» у первой → `setLexemeGroup | key=ruwiktionary/1.noun.1
group=Group(groupId=17)`; «Создать» → слово 28, плашка словаря
«Finance · 31.10.2026» с «брат» и частью речи «Глагол», у двух других
«Куда: «brother» (брат)», «Перенести», «Группы» нет (08.1, 08.6, 20.7);
«Куда» второй плашки → «новое слово «brother»» → «Создать» → слово 29,
`no-op | reason=retargetNewWordLexemes: nothing to change`, `applyReloadedLemmas
| lemmas=2`; под словом две плашки словаря, новое первым: «Все ·
31.10.2026» с «собрат, друг…», затем «Finance · 31.10.2026»; у третьей
плашки «Куда» осталось на первом слове (18.4, 20.11, 46.3, 63.3)

## AWP-15.2, AWP-16.1, AWP-16.3, AWP-16.6, AWP-17.1, AWP-08.1, AWP-08.3, AWP-08.5, AWP-08.6, AWP-08.8 Правки плашки: пример, перевод и сброс, часть речи; своя группа только у «новое слово» и у каждой плашки своя; словарь без групп

Вход:
- словарь `en` → `ru` с группами и «Примером», в нём слово «sibling» с
  одной лексемой; словарь `es` → `ru` без групп; снимок абзаца; скриншот
  предложения

Шаги:
1. Абзац → окно; общая группа → «Harry Potter 1»; тап «sister».
2. У плашки с несколькими «Пример» — крестик у первого примера.
3. Тап по «Перевод: …» → ввести «  сестра  » → «Сохранить».
4. Снова тап по «Перевод» → «сбросить».
5. Тап по «Часть речи: …» → выбрать «глагол».
6. «Группа: как у всех ▾» у первой плашки → «Finance»; осмотреть вторую
   плашку.
7. «Группа» у первой плашки → «Как у всех»; «Создать» у первой плашки.
8. Тап «sibling»; «Проверить по тексту»; у плашки без «уже в словаре»
   «Куда» → «sibling»; осмотреть.
9. Сменить словарь на `es` → `ru`; скриншот предложения → окно; тап
   «nube»; осмотреть общую группу и «Группа» в плашке.

Ожидания:
- экран: пример удалён, остальные примеры и плашка на месте; после
  «Сохранить» — «Перевод: сестра» с пометкой «изменено», края обрезаны;
  в листе правки «было: …» и «сбросить»; после «сбросить» — текст ответа,
  пометки «изменено» нет; «Часть речи: глагол» только у этой плашки
- экран: «Группа: Finance» у первой плашки, у второй — «как у всех»; после
  «Как у всех» — у первой снова «как у всех»; слово записано в «Harry
  Potter 1»
- экран: у плашки с «Куда: «sibling» (…)» поля «Группа» нет
- экран (es): наверху общая группа только «Все», у плашки «Группа: как у
  всех» без стрелки, подсказка «Новые группы создаются на вкладке
  «Группы»» в листе общей группы
- лог: `DeleteValue(ref=…)` → `deleteValue | lemma=sister key=…
  componentId=… index=0`, `refreshLemmaValues`, `refreshLemmaMarks`,
  `no-op | reason=removeEmptyWord: lexemes left sister`;
  `OpenSheet(sheet=TextEdit(…))` → `openSheet | sheet=TextEdit`,
  `UpdateDraft(text=…)` → `updateSheetDraft | length=…`,
  `SubmitText(…)` → `applyTextEdit | lemma=sister … length=6`, `no-op |
  reason=dropEmptyTexts: nothing to change`, `closeSheet`; `ResetText(…)`
  → `resetText`; `SetOption(ref=…, option=…)` → `setOption | lemma=sister
  … option=…`; `SetCardGroup(lemma=sister, key=K, group=Group(groupId=…))`
  → `setLexemeGroup | lemma=sister key=K group=…`, затем `group=AsCommon`

Анти-проверки:
- лог: на правках нет `POST`; у «sister» одна плашка с «Часть речи:
  глагол», остальные как были
- экран (es): кнопки «создать группу» нет

Прогон: ⏳ 2026-10-30/31 — «sister» в «био» после проверки, плашки «уже
в словаре»: × у примера «Emily is Sophie's younger sister.» →
`deleteValue | lemma=sister … componentId=19 index=0`, два других
примера и плашка на месте (15.2); «Перевод» плашки «(старшая)
медсестра, сестра» → «  сиделка  » → «Перевод: сиделка» без пробелов,
пометка «изменено», «уже в словаре» снята, появились «Куда: новое
слово», «Группа» и «Создать» — пометки пересчитаны после правки (16.1,
16.3, 50.1). «brother» — три плашки, слова в словаре нет: «Часть речи»
первой → «Глагол», у второй «Существительное» (17.1); ввод «братан» в
листе правки → «сбросить» → `resetText`, `closeSheet`, «Перевод: брат»
без «изменено» (16.6, на несохранённом вводе). Своя группа — следующий
кейс

## AWP-48.1, AWP-48.3, AWP-48.4, AWP-48.5, AWP-51.5, AWP-57.4, AWP-47.9 «Убрать из списка»: у «Не найдено», во время загрузки, у слова «в словаре»; новый снимок возвращает, ответ после уборки отброшен

Вход:
- словарь `es` → `ru` со словом «nube»; скриншот плотной страницы;
  скриншот предложения

Шаги:
1. Скриншот плотной страницы → окно; найти «sh» в «Не разобрано».
2. Тап «sh»; дождаться «Не найдено»; «Убрать из списка».
3. Тап «gran»; сразу «Убрать из списка», пока виден признак загрузки;
   подождать ответ.
4. Крестик.
5. Скриншот предложения → окно; тап «nube»; «Убрать из списка».
6. Новый снимок плотной страницы → окно.

Ожидания:
- экран: под «sh» после ответа — «Не найдено» и «Убрать из списка»;
  строки «sh» нет; под «gran» на загрузке признак и кнопка «Убрать из
  списка»; «gran» ушло и после ответа не вернулось; окно закрылось сразу,
  без вопроса
- экран: под «nube» плашка словаря и кнопки; после «Убрать из списка»
  строки «nube» нет, слово «nube» в словаре как было
- экран: в новом окне «sh» и «gran» снова в списках
- лог: `RemoveWord(text=sh)` → `removeWord | word=sh`; `startLookup |
  word=gran`, `removeWord | word=gran`, затем `LexemesFound` → `no-op |
  reason=applyLexemes: no word gran`, `no-op | reason=refreshCardValues:
  no card gran`, `no-op | reason=refreshCardMarks: no card gran`;
  `RequestClose` → `close` без `askCloseConfirm`; `openSavedCard |
  word=nube saved=1`, `removeWord | word=nube`

Анти-проверки:
- лог: после уборки «gran» нет `applyLexemes | word=gran`; нет
  `askCloseConfirm`; на «nube» нет `POST`
- словарь: «nube» на месте

Прогон: ⏳ 2026-10-31 — «friendship» в «био», в словаре, без проверки:
тап → `openSavedCard | word=friendship saved=1`, «Убрать из списка» →
`RemoveWord(text=friendship)`, `removeWord | word=friendship`, строки
нет, эффектов записи нет (48.1, 48.5). Новый снимок возвращает слово
(48.3) — проверено на «nube» после «Не нужно». Не проверено руками:
уборка во время загрузки (48.4, 57.4, 47.9) — гонка, юнит-тесты

## AWP-55.1, AWP-55.10, AWP-49.5, AWP-61.2, AWP-61.7 «sh» → «Не найдено»; «Проверить по тексту» → «Не найдено» с плашками словаря на месте; без сети: «Сервер не ответил», «Повторить» запрашивает снова

Вход:
- словарь `es` → `ru` со словом «sh» без значений; скриншот плотной
  страницы

Шаги:
1. Скриншот плотной страницы → окно; тап «sh» в «Не разобрано».
2. «Проверить по тексту» у «sh».
3. Включить авиарежим; тап «maestra».
4. Выключить авиарежим; у «maestra» «Повторить».

Ожидания:
- экран: под «sh» плашка словаря без значений, затем после проверки — «Не
  найдено» и «Убрать из списка», плашка словаря на месте, кнопок записи и
  «Повторить» нет; под «maestra» — «Сервер не ответил», «Повторить» и
  «Убрать из списка»; после «Повторить» — признак загрузки, затем плашки
  нейронки
- лог: `POST /v1/lexemes/from-lemma -> 404`, `lexemes word=sh outcome=not
  found`, `LexemesNotFound(text=sh, textRes=…)` → `showCardNotFound |
  word=sh res=…`; для «maestra» — `LexemesUnavailable(text=maestra,
  errorRes=…)` → `showCardRetryableError | word=maestra res=…`; после
  «Повторить» — `LoadLexemes(text=maestra)`, `startLookup | word=maestra`,
  `POST /v1/lexemes/from-lemma -> 200`, `applyLexemes | word=maestra …`

Анти-проверки:
- лог: на «sh» нет `showCardRetryableError`; у «maestra» ровно два
  `startLookup`; нет `FATAL`

Прогон: ⏳ 2026-10-31 — картинка «Qwrtzk zxqvb.» в «био»: `lexical=0
proper=1 function=0 unparsed=1` — «Qwrtzk» в именах собственных,
«zxqvb.» в «Не разобрано» с точкой; тап «Qwrtzk» → `POST
/v1/lexemes/from-lemma -> 200 in 154ms`, `outcome=not found`,
`showCardNotFound`; экран: «Не найдено» и «Убрать из списка», без
«Создать» и «Повторить» (55.1). «Не найдено» после проверки слова «в
словаре» (55.10, 49.5) — юнит-тесты; без сети (61.2, 61.7) пропущено по
решению юзера 2026-10-31

## AWP-51.1, AWP-51.3, AWP-51.4, AWP-51.9, AWP-51.12, AWP-51.13, AWP-60.2, AWP-14.2, AWP-64.6 Закрытие с плашками: вопрос с числом слов, «Остаться», «Закрыть»; во время записи крестик и «Не нужно» не действуют; слово с частью записанных плашек считается одним

Вход:
- словарь `en` → `ru` без «friendship», «sibling», «sister»; снимок абзаца

Шаги:
1. Абзац → окно; тап «friendship»; тап «sibling»; дождаться плашек.
2. «Создать» у плашки «friendship»; смотреть на крестик, пока идёт
   запись.
3. Крестик; осмотреть вопрос; «Остаться».
4. Тап «relationship»; крестик, пока виден признак загрузки; «Остаться».
5. Тап «sister»; дождаться; «Создать» у первой плашки; смотреть на «Не
   нужно» у второй, пока идёт запись.
6. Дождаться записи; системная «назад»; «Закрыть».
7. Новый снимок; тап «friendship»… (если «friendship» уже в словаре —
   «sibling»); «Создать» у единственной плашки; крестик.

Ожидания:
- экран: крестик и «Не нужно» во время записи серые (если успели
  увидеть); вопрос «Закрыть без добавления?» с «Не добавлено слов: 1» —
  «sibling» с плашкой; «Остаться» — окно как было; с загружающимся
  «relationship» число 2; после записи первой плашки «sister» и остатка
  плашек — «назад» показывает вопрос с числом 3: «sister» считается одним
  словом; «Закрыть» — окно закрыто, в словаре «friendship» и «sister» с
  одной лексемой; в новом окне после записи единственной плашки крестик
  закрывает сразу
- лог: `RequestClose` → `askCloseConfirm | pending=1`; `Stay` →
  `closeCloseConfirm`; `askCloseConfirm | pending=2`; `askCloseConfirm |
  pending=3`; `ConfirmClose` → `close`; во время записи `RequestClose` →
  `no-op | reason=requestClose: adding` (если успели); в новом окне
  `RequestClose` → `close`

Анти-проверки:
- лог: после `ConfirmClose` нет `startAddingLexeme`; в словаре нет
  «sibling» и «relationship»

Прогон: ⏳ 2026-10-31 — абзац в «био», у «brother» три плашки, две
записаны: крестик → `askCloseConfirm | pending=1`, вопрос «Закрыть без
добавления?», «Не добавлено слов: 1», «Остаться» и «Закрыть» (51.1,
51.13); «Остаться» → `closeCloseConfirm`; системная «назад» →
`askCloseConfirm | pending=1` (14.2); «Закрыть» → `ConfirmClose`, `close`;
вкладка «Слова»: два «brother», «sister», «friendship» без обновления
руками, «friendship» после «Убрать из списка» на месте (51.4, 52.1,
48.5). Закрытие без вопроса (60.1, 60.2) — в кейсах «nube» и «sister».
Не проверено руками: загружающееся слово в счёте (51.9), крестик и «Не
нужно» во время записи (51.12, 64.6) — гонка, юнит-тесты

## AWP-14.3, AWP-43.6, AWP-42.6, AWP-42.7 Поворот экрана: с ошибкой на слове, во время загрузки, после правок — стейт на месте, второго запроса нет; слово «в словаре» после проверки сворачивается и раскрывается целиком

Вход:
- словарь `en` → `ru` со словом «study»; снимок абзаца; автоповорот
  включён

Шаги:
1. Абзац → окно; авиарежим; тап «link» → ошибка; тап «link» — свернуть.
2. Повернуть телефон; тап «link» — раскрыть.
3. Авиарежим выключить; тап «sibling»; пока виден признак загрузки —
   повернуть телефон; дождаться плашек.
4. У «sibling» «Не нужно» у одной плашки, править перевод у другой;
   повернуть телефон.
5. Тап «study»; «Проверить по тексту»; дождаться; тап «study» —
   свернуть; тап «study» — раскрыть.

Ожидания:
- экран: после поворота у «link» та же ошибка и «Повторить», запрос сам
  не ушёл; на «sibling» признак загрузки переживает поворот, ответ ложится
  под слово; «Не нужно» и правка после поворота на месте
- экран: у «study» после сворачивания и раскрытия плашки словаря и
  плашки нейронки на месте, как были, запроса нет
- лог: на «sibling» один `startLookup` и один `POST
  /v1/lexemes/from-lemma`; после поворотов нет `startLookup`, нет
  `RecognizeText`; у «study» `toggleCard | word=study expanded=false`,
  затем `true`, без `startLookup`

Анти-проверки:
- лог: нет `showRetryableError`, нет повторного `ExtractLemmas`

Прогон: ⏳ 2026-10-31 — не проверить: главный экран в манифесте
приложения прибит к портрету (`android:screenOrientation="portrait"`),
телефон с автоповоротом окно не поворачивает. Стейт окна при
пересоздании закрыт сценарными юнит-тестами (43.6, 14.3); сворачивание
и раскрытие (42.6, 42.7) — юнит-тесты. Строки спеки про поворот — вопрос
юзеру

## AWP-27.1, AWP-27.2, AWP-27.3 Гибель процесса при открытом окне: после возврата окна нет, добавленное в словаре, остальное потеряно

Вход:
- словарь `en` → `ru` без «friendship» и «sibling»; снимок абзаца; агент
  с `adb`

Шаги:
1. Абзац → окно; тап «friendship» → «Создать»; тап «sibling» → «Не нужно»
   у одной плашки.
2. Кнопка «Домой»; агент: `adb shell am kill co.lexeme.app.dev`; вернуться
   в приложение.
3. Новый снимок абзаца → окно; тап «sibling» → «Создать» и сразу агент:
   `adb shell am kill co.lexeme.app.dev`; вернуться; осмотреть словарь.

Ожидания:
- экран: после возврата вкладка «Слова» без окна; «friendship» в словаре,
  «sibling» нет; после второго возврата окна нет, «sibling» либо с
  лексемой, либо отсутствует — половины нет
- лог: после возврата строк `###SCAN###` нет до нового снимка

Анти-проверки:
- словарь: нет «sibling» без лексем и нет лексем без слова

Прогон: ⏳ 2026-10-31 — само собой: окно с правками на плашках «sister»
осталось открытым на ночь, система перезапустила процесс (pid 19872 →
25848); после возврата вкладка «Слова» без окна, `###SCAN###` до нового
снимка нет, «friendship» и «sister» в словаре со всеми записанными
лексемами, правки плашек потеряны (27.1, 27.3). Убийство во время записи
(27.2) не гонялось — атомарность записи закрыта инструментальными тестами
AWP-20

## WCL-01.1, WCL-04.4, LFL-104.1 Карточка слова: значения по лемме через новую операцию

Вход:
- словарь `es` → `ru` со словом «corrían» без значений

Шаги:
1. Открыть карточку слова «corrían» → «Добавить лексемы-кандидаты».
2. Дождаться диалога; осмотреть разделы; «Добавить».

Ожидания:
- экран: диалог с разделом «correr» и пометкой формы, лексемы по лемме;
  после «Добавить» лексемы в карточке слова
- лог: `POST /v1/lexemes/from-lemma -> 200`, `candidates lookup …
  outcome=found lemmas=1 …`

Анти-проверки:
- лог: нет `POST /v1/lemmas/from-tokens`; нет `FATAL`

Прогон: ⏳ 2026-10-31 — вместо «corrían» в «ёла» слово «friendship» в
«био» с двумя лексемами: «Добавить лексемы-кандидаты» →
`openCandidates | wordId=26`, один `POST /v1/lexemes/from-lemma -> 200
in 2726ms`, `found lemmas=1`, `applyCandidatesForm | cards=1`,
`refreshCandidatesExists | exists=2`; экран: раздел «friendship», две
лексемы с пометкой «есть» и крестиками, «Сопоставление», подпись
источника, «2 уже есть — не добавятся», «Добавлять нечего», «Добавить»
неактивна (WCL-01.1, LFL-104.1). Раздел леммы-формы с пометкой
(WCL-04.4) не проверен: слово — сама лемма
