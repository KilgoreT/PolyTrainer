# IS536 | Ручники: леммы из текста и списки (IS537), карточка под словом (IS538)

Форма — `backend/docs/handbook/feature-flow/spec-checks.md`, раздел 3:
заголовок кейса несёт адреса строк спек устройства — AWP
(`docs/handbook/specs/words-add-from-photo/spec.md`), TFP
(`backend/docs/handbook/specs/tokens-from-photo/spec.md`), LFT
(`backend/docs/handbook/specs/lemmas-from-text/spec.md`), LFL
(`backend/docs/handbook/specs/lexemes-from-lemma/spec.md`), WCL
(`docs/handbook/specs/word-card-lexemes/spec.md`). Кейсы 1–10 — этап 1:
окно со списками; кейсы 11–21 — этап 2: карточка под словом и запись.
Кейсы первой версии — окно с карточками и диалог карточки слова —
заморожены в `manual_tests-v1.md`. В чат кейсы выдаются по одному:
счётчик, название, шаги-атомы, последний шаг «Напиши “ок”». Состояние и
лог после «ок» проверяет агент; шаги с пометкой «агент:» делает агент сам
по `adb` или `ssh`.

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

## Словарь логов

### SCAN — экран снимка (`###SCAN###`)

- `Reduce ---message---: <Msg>` — входящее сообщение. Тяжёлые — именем
  класса: `TextRecognized`, `LemmasFound`, `FormLoaded`; остальные со
  значениями: `TextEmpty`, `RecognitionFailed(errorRes=…)`, `LemmasEmpty`,
  `LemmasDenied(errorRes=…)`, `LemmasUnavailable(errorRes=…)`,
  `FormFailed(errorRes=…)`, `RetryLemmas`, `ToggleTranslation`,
  `RequestClose`, `OpenSheet(sheet=…)`, `CloseSheet`,
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
  - карточка под словом (этап 2): `openSavedCard | word=W existing=E`,
    `startLookup | word=W`, `applyLexemes | word=W lemma=L lexemes=N`,
    `refreshCardValues | word=W active=A`, `refreshCardMarks | word=W
    exists=E lacksCore=C canAdd=true|false`, `showCardNotFound | word=W
    res=…`, `showCardError | word=W res=…` (без «Повторить»),
    `showCardRetryableError | word=W res=…` (с «Повторить»), `toggleCard |
    word=W expanded=true|false`, `toggleCardMatches | word=W
    expanded=…`, `deleteWord | word=W`;
  - правки карточки, адрес — лемма: `deleteLexeme | lemma=L key=K`,
    `deleteValue | lemma=L key=K componentId=C index=I`, `applyTextEdit |
    lemma=L … length=N`, `dropEmptyTexts | …`, `resetText | …`,
    `setOption | lemma=L key=K componentId=C option=O`, `setCardTarget |
    lemma=L target=Existing(wordId=…)|NewWord`, `setCardGroup | lemma=L
    group=…`, `refreshLemmaValues | lemma=L`, `refreshLemmaSaved |
    lemma=L`, `refreshLemmaMarks | lemma=L`, `refreshAllCards | cards=N`;
  - запись: `startAddingWord | word=W lexemes=N`, `removeAddedWord |
    word=W`, `showAddFailure | word=W failure=…`, `clearAddFailure |
    word=W`, `resetDeadGroup | groupId=…`, `resetDeadTarget | wordId=…`,
    `dropComponent | typeId=…`;
  - закрытие: `askCloseConfirm | pending=N`, `closeCloseConfirm`, `close`.
- Сообщения этапа 2 со значениями: `LoadLexemes(text=W)`,
  `OpenSavedCard(text=W)`, `ToggleCard(text=W)`, `DeleteWord(text=W)`,
  `AddWord(text=W)`, `RetryAdd(text=W)`, `ToggleComponentMatches(text=W)`,
  `LexemesNotFound(text=W, textRes=…)`, `LexemesDenied(text=W,
  errorRes=…)`, `LexemesUnavailable(text=W, errorRes=…)`,
  `WordAdded(text=W)`, `AddGroupGone(…)`, `AddWordGone(…)`,
  `AddComponentGone(…)`, `AddFailed(text=W, failure=…)`, правки
  `DeleteLexeme(lemma=L, key=K)`, `DeleteValue(ref=…)`, `SubmitText(ref=…,
  text=…)`, `ResetText(ref=…)`, `SetOption(ref=…, option=…)`,
  `UpdateDraft(text=…)`, `SetCardTarget(lemma=L, target=…)`,
  `SetCardGroup(lemma=L, group=…)`, `ConfirmClose`, `Stay`; тяжёлое —
  именем класса: `LexemesFound`.
- `Reduce ---step---: no-op | reason=…` — событие пришло и намеренно
  проигнорировано: `markInDictionary: no form`, `closeSheet: no sheet`,
  `noOperation` (действие общих виджетов, которого у экрана нет);
  этап 2: `applyLexemes: no word W`, `refreshCardValues: no card W`,
  `refreshCardMarks: no card W` — ответ пришёл на удалённое слово
  (AWP-48.4); `requestClose: adding` — крестик или «назад» во время записи
  (AWP-51.12); `startAddingWord: nothing to write W` — добавлять нечего;
  `<шаг>: nothing to change` — правка по адресу, которого уже нет.
- `RunEffect: <эффект>` — эффект ушёл в хендлер: `RecognizeText(uri=…)`,
  `ExtractLemmas(tokens=K)`, `LoadForm(dictionaryId=D, lemmas=M)`,
  `LookupLemma(word=W, lemma=L, dictionaryId=D, text=N)` (N — длина
  текста снимка), `AddWord(word=W, lexemes=N)`.
- Исходы юзкейса: `recognize outcome=text tokens=K | empty`; `lemmas
  outcome=dict=D lexical=A proper=B function=C unparsed=U | no words |
  denied=<причина> | unavailable`; `form outcome=loaded components=C
  options=O groups=G existing=E | dictionary missing | failed: …`;
  этап 2: `lexemes word=W outcome=found lemmas=1 recommendations=R
  existing=E | not found | no lexemes | denied=<причина> | unavailable |
  word failed`; `add word=W outcome=added words=1 lexemes=N | rejected
  <причина>`.
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

## AWP-43.1, AWP-43.4, AWP-57.1, AWP-57.3, AWP-29.2, AWP-30.2, AWP-30.5, AWP-31.4, AWP-45.1, AWP-45.2, AWP-45.3, AWP-47.1 «corrían»: карточка «correr» с «в тексте», «Замечания» без кнопок, «Сопоставление», подпись источника

Вход:
- словарь `es` → `ru` без «Рода»; скриншот «Las nubes corrían por el
  cielo.» в галерее

Шаги:
1. Кнопка-камера → «Галерея» → скриншот предложения; дождаться окна.
2. Осмотреть окно до тапов: подписи источника нет.
3. Тап «correr» в «Смысловые»; смотреть признак загрузки под словом.
4. Осмотреть карточку: лексемы, пометки, разделы; тап по заголовку
   «Сопоставление»; тап ещё раз.
5. Тап по подписи «Данные: Wiktionary, CC BY-SA 4.0»; вернуться в
   приложение.

Ожидания:
- экран: до тапа подписи источника нет; после тапа под «correr» признак
  загрузки, затем карточка: лемма «correr» с пометкой формы, три
  лексемы в порядке ответа — «течь, литься, струиться (о жидкости)» с
  пометкой «в тексте», затем «бегать; бежать», «бежать, проходить, лететь
  (о времени)» (состав нейронка может менять — сверять по журналу); у
  второй и третьей пометки «в тексте» нет; признака загрузки нет
- экран: у лексемы «Перевод: …», «Часть речи: глагол», «Толкование: …»,
  строки «Род» нет
- экран: раздел «Замечания» со строкой «У лексем-кандидатов есть «Род».
  Заведите в конструкторе компонент «Род» (выбор, опции: …), и значения
  лягут в него», кнопок в разделе нет; «Сопоставление» свёрнуто, по тапу
  строки «translations → Перевод», «Часть речи: глагол», …, второй тап
  сворачивает
- экран: под карточкой подпись «Данные: Wiktionary, CC BY-SA 4.0», тап
  открывает `https://creativecommons.org/licenses/by-sa/4.0/`; кнопки
  «Удалить» и «Добавить», «Добавить» активна
- лог: `LoadLexemes(text=correr)`, `startLookup | word=correr`,
  `RunEffect: LookupLemma(word=correr, lemma=correr, dictionaryId=D,
  text=N)`, `POST /v1/lexemes/from-lemma -> 200`, `lexemes word=correr
  outcome=found lemmas=1 recommendations=1 existing=0`, `LexemesFound`,
  `applyLexemes | word=correr lemma=correr lexemes=3`, `refreshCardValues
  | word=correr active=A`, `refreshCardMarks | word=correr exists=0
  lacksCore=0 canAdd=true`, `toggleCardMatches | word=correr
  expanded=true`, затем `expanded=false`
- сервер: `lexemes from lemma` с `run_id` из `X-Run-Id`, один вызов
  нейронки

Анти-проверки:
- лог: ровно один `POST /v1/lexemes/from-lemma`; нет `showCardError`,
  `showCardRetryableError`, `FATAL`
- экран: у окна без карточек подписи источника нет

Прогон: ❌✅ 2026-10-29 — первая попытка со словарём en → ru: сервер искал
«correr» в английской базе, «Не найдено» (запись в бэклог про чужой
словарь); со словарём es → ru (3 компонента, без групп): один
`POST /v1/lexemes/from-lemma -> 200 in 4665ms`, `found lemmas=1
recommendations=0`, `applyLexemes | lexemes=3`, `canAdd=true`,
`toggleCardMatches | expanded=true`; лексемы `ruwiktionary/1.verb.1`
«бегать; бежать», `1.verb.2`, `1.verb.3` — первая по тапу «бегать», не
«течь» (открытый вопрос LFL-83.1); «Замечаний» нет: рекомендаций сервер
не дал; подпись источника и ссылка — по «ок» юзера

## AWP-42.1, AWP-42.2, AWP-42.4, AWP-42.5, AWP-15.1, AWP-15.8 «friendship» и «sibling»: две карточки, сворачивание, ответ в свёрнутую, раскрытие с удалением без запроса

Вход:
- словарь `en` → `ru`; снимок абзаца учебника

Шаги:
1. Кнопка-камера → «Галерея» → абзац; дождаться окна.
2. Тап «friendship»; сразу тап «sibling» и ещё раз тап «sibling» — свернуть,
   пока грузится.
3. Дождаться ответов; тап «sibling» — раскрыть.
4. У «friendship» крестик у второй лексемы; быстро нажать крестик у первой
   трижды.
5. Тап «friendship» — свернуть; тап ещё раз — раскрыть.

Ожидания:
- экран: раскрыты карточки под «friendship» и под «sibling», каждая под
  своим словом; свёрнутая на загрузке «sibling» после раскрытия показывает
  пришедшие значения
- экран: у «friendship» после крестиков остались лексемы без удалённых —
  тройной тап снял одну; после сворачивания и раскрытия карточка та же,
  без удалённых, признака загрузки нет
- лог: `startLookup | word=friendship`, `startLookup | word=sibling`,
  `ToggleCard(text=sibling)` → `toggleCard | word=sibling expanded=false`,
  `applyLexemes | word=sibling …`, затем `toggleCard | word=sibling
  expanded=true`; `DeleteLexeme(lemma=friendship, key=…)` → `deleteLexeme
  | lemma=friendship key=…`, `refreshLemmaMarks | lemma=friendship`; на
  второй и третий крестик — `no-op | reason=deleteLexeme: nothing to
  change`; `toggleCard | word=friendship expanded=false`, затем `true`

Анти-проверки:
- лог: ровно два `POST /v1/lexemes/from-lemma`; после `toggleCard` нет
  `startLookup`

Прогон: ✅ 2026-10-29 — абзац, 53 токена, 24/4/13; два `POST
/v1/lexemes/from-lemma -> 200` (3117 и 2510 мс), «friendship» 2
лексемы, «sibling» 1; «sibling» свёрнута и раскрыта во время загрузки,
ответ лёг (`toggleCard … expanded=false|true` до `applyLexemes`); у
«friendship» удалены обе лексемы (`deleteLexeme … noun.2`, `noun.1`,
`refreshLemmaMarks`), повторный крестик не делался — лексемы не осталось;
сворачивание и раскрытие «friendship» через `adb input tap` —
`toggleCard | word=friendship expanded=false`, затем `true`, без
`startLookup`; скриншот: пустая карточка с «Сопоставлением», подписью
источника, «Удалить» и серой «Добавить». Шаги с жестами с этого кейса
делает агент через adb (решение юзера 2026-10-29)

## AWP-62.1, AWP-62.2, AWP-52.1, AWP-52.2, AWP-52.3, AWP-52.4, AWP-42.3, AWP-20.9, AWP-02.1, AWP-47.4, AWP-47.8 «Добавить» у «friendship», «sibling» и «sister»: строки уходят, слова на вкладке с лексемами, в группе и в тренировке

Вход:
- словарь `en` → `ru` с группой «Harry Potter 1» и компонентом «Пример»;
  снимок абзаца

Шаги:
1. Кнопка-камера → «Галерея» → абзац; дождаться окна; общая группа →
   «Harry Potter 1».
2. Тап «friendship»; дождаться карточки; «Добавить» два раза быстро.
3. Тап «sibling» → «Добавить»; тап «sister» → «Добавить».
4. Закрыть окно крестиком.
5. На вкладке «Слова» открыть карточку «friendship», затем «sister»;
   запустить тренировку словаря и группы «Harry Potter 1».

Ожидания:
- экран: после первого «Добавить» строки «friendship» нет, остальные 23
  формы в прежнем порядке; после «sibling» — 22, после «sister» — 21,
  окно открыто
- экран: пока идёт запись — кнопки карточки неактивны, тап по значению
  лист не открывает; окно закрылось сразу, без вопроса
- экран: на вкладке «Слова» «friendship», «sibling», «sister» без
  обновления руками; в карточке «friendship» та лексема, что была под
  словом, — те же имена компонентов и значения («Перевод: дружба»);
  в карточке «sister» «Пример» с тремя примерами записи; кнопки
  «тренировать» нет, слова в тренировке словаря и группы «Harry Potter 1»
- лог: на слово — `AddWord(text=W)`, `startAddingWord | word=W
  lexemes=N`, `RunEffect: AddWord(word=W, lexemes=N)`, `add word=W
  outcome=added words=1 lexemes=N`, `WordAdded(text=W)`,
  `removeAddedWord | word=W`; на второе быстрое нажатие второго
  `startAddingWord` нет

Анти-проверки:
- лог: одна строка `add word=friendship` на снимок; между
  `startAddingWord` и `removeAddedWord` нет `OpenSheet`, `DeleteLexeme`,
  `SetCardTarget`; нет `showAddFailure`
- словарь: по одной лемме «friendship», «sibling», «sister», дублей нет

Прогон: ⏳ 2026-10-29 — абзац, три слова по очереди: `friendship`
1 лексема, `sibling` 1, `sister` 3 (`active=9`), на каждое один `POST
/v1/lexemes/from-lemma -> 200` (2329, 2977, 5077 мс), `startAddingWord`
→ `add word=… outcome=added words=1 lexemes=N` → `removeAddedWord`;
крестик после трёх записей — `RequestClose` → `close` без вопроса
(AWP-60.2); `showAddFailure` нет. Не проверено: общая группа не
ставилась (`SetCommonGroup` в логе нет) — AWP-20.9 и AWP-52.3 по группе
открыты, двойное нажатие (AWP-02.1) и правки во время записи (AWP-47.8)
руками не делались — закрыты юнит-тестами; карточка «sister» — по «ок»
юзера; тренировка словаря — дерево экрана: квиз-чат с подсказкой
«Перевод: сущ. (старшая) медсестра, сестра» — лексема «sister» из окна в
тренировке (AWP-52.3 по словарю, по группе открыто)

## AWP-46.1, AWP-46.4, AWP-18.1, AWP-18.3, AWP-18.5, AWP-18.8, AWP-18.9, AWP-18.10, AWP-49.1, AWP-57.2, AWP-50.1, AWP-47.6, AWP-20.10 «study» в словаре: сохранённые значения, лист «куда класть» с датами и группами, «Проверить по тексту» → «уже в словаре», запись в выбранное слово

Вход:
- словарь `en` → `ru` с двумя «study» и «link» без значений (подготовка
  данных); снимок абзаца

Шаги:
1. Кнопка-камера → «Галерея» → абзац; дождаться окна.
2. Тап «study»; осмотреть карточку.
3. «Куда: … ▾» → осмотреть лист; выбрать «study» с «исследование».
4. Общая группа → «Harry Potter 1»; снова «Куда: … ▾», осмотреть подсказку;
   закрыть лист.
5. «Проверить по тексту»; смотреть признак загрузки; дождаться ответа.
6. «Добавить»; на вкладке «Слова» открыть «study» с «исследование».
7. Новый снимок абзаца; тап «link».

Ожидания:
- экран: карточка «study» без запроса: «В словаре» — «Перевод: кабинет»
  (самое новое «study»), «Куда: «study» (кабинет) ▾», кнопки «Проверить по
  тексту» и «Удалить»
- экран: лист — две строки: «кабинет · 1 значение · Все · <дата
  добавления>», «исследование · 1 значение · Harry Potter 1 · <дата
  раньше>» и «новое слово «study»»; после выбора «исследование» в
  карточке «Перевод: исследование»; при общей «Harry Potter 1» подсказка
  «Слово войдёт в группу «Harry Potter 1» вместе со своими 1 старыми
  значениями»
- экран: после «Проверить по тексту» признак загрузки на слове, затем
  лексемы-кандидаты: совпавшая с «исследование» — «уже в словаре», новые
  без пометки, кнопки «Добавить» и «Удалить», «Проверить по тексту» нет
- экран: после «Добавить» строки «study» нет; у «study» (исследование) на
  вкладке новые лексемы, старая на месте, группа «Harry Potter 1» на
  месте; второе «study» (кабинет) не изменилось
- экран: карточка «link» — без лексем, кнопки «Проверить по тексту» и
  «Удалить»
- лог: `OpenSavedCard(text=study)`, `openSavedCard | word=study
  existing=2`, `openSheet | sheet=CardTarget`, `SetCardTarget(lemma=study,
  target=Existing(wordId=…))` → `setCardTarget`, `refreshLemmaSaved |
  lemma=study`, `refreshLemmaMarks | lemma=study`, `closeSheet`;
  `LoadLexemes(text=study)` → `startLookup | word=study`, `POST
  /v1/lexemes/from-lemma -> 200`, `applyLexemes | word=study lemma=study
  lexemes=N`, `refreshCardMarks | word=study exists=1 … canAdd=true`;
  `startAddingWord | word=study lexemes=N-1`, `add word=study
  outcome=added words=1 lexemes=N-1`, `removeAddedWord`

Анти-проверки:
- лог: при тапе «study» и «link» нет `POST`; `POST /v1/lexemes/from-lemma`
  один — на «Проверить по тексту»; нет `showAddFailure`
- словарь: третьего «study» нет

Прогон: ⏳ 2026-10-29 — вместо двух «study» второе слово сделано из
«sister» (добавлено кейсом 13): форма `existing=4`, `inDictionary=4`;
«link» без значений — `openSavedCard | word=link existing=1` без `POST`
(AWP-46.4); «sister» — `openSavedCard … existing=1` без `POST` (AWP-46.1),
«Проверить по тексту» → `startLookup`, `POST … -> 200 in 4592ms`,
`applyLexemes | lexemes=3`, `refreshCardMarks | exists=3 … canAdd=false`
(AWP-49.1, 50.1, 50.12); `SetCardTarget(… NewWord)` → `refreshLemmaSaved`,
`refreshLemmaMarks`, `closeSheet` (AWP-18.4); удалена `1.noun.3`,
`startAddingWord | lexemes=2` → `added words=1 lexemes=2` →
`removeAddedWord`; вкладка «Слова»: два «sister» — 2 и 3 лексемы, «link»,
«sibling», «friendship». Лист «куда класть» с двумя леммами, выбор старой
и запись в выбранное слово — следующий заход (кейс в чате 5 и 6).
❌ 2026-10-29 на следующем заходе: у карточки «в словаре» не было «Куда»
и «Группы» — окно рисовало для фазы «сохранённые» только строки
сохранённых и кнопки, без заголовка леммы (AWP-46.3, AWP-18.8 руками не
проходились); починено: `LemmaCardWidget` рисуется и у «сохранённых»,
сохранённые строки — под ним в любой фазе; пересобрано и поставлено

## AWP-15.2, AWP-16.1, AWP-16.3, AWP-16.6, AWP-17.1, AWP-08.1, AWP-08.3, AWP-08.5, AWP-18.6 Правки карточки: пример, перевод и сброс, часть речи, своя группа; словарь без групп

Вход:
- словарь `en` → `ru` с группами и «Примером»; словарь `es` → `ru` без
  групп; снимок абзаца; скриншот предложения

Шаги:
1. Абзац → окно; общая группа → «Harry Potter 1»; тап «sister».
2. У лексемы с несколькими «Пример» — крестик у первого примера.
3. Тап по «Перевод: …» → ввести «  сестра  » → «Сохранить»; снова тап по
   «Перевод» → «сбросить».
4. Тап по «Часть речи: …» → выбрать «глагол».
5. «Группа: как у всех ▾» → «Finance»; снова → «Как у всех»; «Добавить».
6. Сменить словарь на `es` → `ru`; скриншот предложения → окно; тап
   «nube»; осмотреть общую группу и «Группа» в карточке.

Ожидания:
- экран: пример удалён, остальные примеры и лексема на месте; после
  «Сохранить» — «Перевод: сестра» с пометкой «изменено», края обрезаны;
  в листе правки «было: …» и «сбросить»; после «сбросить» — текст ответа,
  пометки «изменено» нет; «Часть речи: глагол» только у этой лексемы
- экран: у карточки «⌂ Finance», после «Как у всех» пометки нет; слово
  записано в «Harry Potter 1»
- экран (es): наверху общая группа только «Все», у «Группа» выбора нет,
  подсказка «Новые группы создаются на вкладке «Группы»»
- лог: `DeleteValue(ref=…)` → `deleteValue | lemma=sister key=…
  componentId=… index=0`, `refreshLemmaValues`, `refreshLemmaMarks`;
  `OpenSheet(sheet=TextEdit(…))` → `openSheet | sheet=TextEdit`,
  `UpdateDraft(text=…)` → `updateSheetDraft | length=…`,
  `SubmitText(…)` → `applyTextEdit | lemma=sister … length=6`, `no-op |
  reason=dropEmptyTexts: nothing to change`, `closeSheet`; `ResetText(…)`
  → `resetText`; `SetOption(ref=…, option=…)` → `setOption | lemma=sister
  … option=…`; `SetCardGroup(lemma=sister, group=Group(groupId=…))` →
  `setCardGroup`, затем `group=AsCommon`

Анти-проверки:
- лог: на правках нет `POST`; у «sister» одна лексема с «Часть речи:
  глагол», остальные как были
- экран (es): кнопки «создать группу» нет

Прогон:

## AWP-48.1, AWP-48.3, AWP-48.4, AWP-51.5 «Удалить» у слова: уходит из списка, новый снимок возвращает, ответ после удаления отброшен

Вход:
- словарь `es` → `ru`; скриншот плотной страницы

Шаги:
1. Скриншот плотной страницы → окно; найти «sh» в «Не разобрано».
2. Тап «sh» → в карточке «Удалить».
3. Тап «gran» и сразу «Удалить», пока виден признак загрузки; подождать
   ответ.
4. Крестик.
5. Новый снимок той же страницы → окно.

Ожидания:
- экран: строки «sh» нет; «gran» ушло и после ответа не вернулось; окно
  закрылось сразу, без вопроса; в новом окне «sh» и «gran» снова в
  списках
- лог: `DeleteWord(text=sh)` → `deleteWord | word=sh`; `startLookup |
  word=gran`, `deleteWord | word=gran`, затем `LexemesFound` → `no-op |
  reason=applyLexemes: no word gran`, `no-op | reason=refreshCardValues:
  no card gran`, `no-op | reason=refreshCardMarks: no card gran`;
  `RequestClose` → `close` без `askCloseConfirm`

Анти-проверки:
- лог: после удаления «gran» нет `applyLexemes | word=gran`; нет
  `askCloseConfirm`

Прогон:

## AWP-55.1, AWP-61.2, AWP-61.7 «sh» → «Не найдено»; без сети: «Сервер не ответил», «Повторить» запрашивает снова

Вход:
- словарь `es` → `ru`; скриншот плотной страницы

Шаги:
1. Скриншот плотной страницы → окно; тап «sh» в «Не разобрано».
2. Включить авиарежим; тап «maestra».
3. Выключить авиарежим; у «maestra» «Повторить».

Ожидания:
- экран: под «sh» — «Не найдено» и «Удалить», без «Добавить» и
  «Повторить»; под «maestra» — «Сервер не ответил», «Повторить» и
  «Удалить»; после «Повторить» — признак загрузки, затем карточка с
  лексемами
- лог: `POST /v1/lexemes/from-lemma -> 404`, `lexemes word=sh outcome=not
  found`, `LexemesNotFound(text=sh, textRes=…)` → `showCardNotFound |
  word=sh res=…`; для «maestra» — `LexemesUnavailable(text=maestra,
  errorRes=…)` → `showCardRetryableError | word=maestra res=…`; после
  «Повторить» — `LoadLexemes(text=maestra)`, `startLookup | word=maestra`,
  `POST /v1/lexemes/from-lemma -> 200`, `applyLexemes | word=maestra …`

Анти-проверки:
- лог: на «sh» нет `showCardRetryableError`; у «maestra» ровно два
  `startLookup`; нет `FATAL`

Прогон:

## AWP-51.1, AWP-51.3, AWP-51.4, AWP-51.9, AWP-51.12, AWP-60.2, AWP-14.2 Закрытие с загруженными словами: вопрос с числом, «Остаться», «Закрыть»; во время записи крестик не действует

Вход:
- словарь `en` → `ru`; снимок абзаца

Шаги:
1. Абзац → окно; тап «friendship», тап «sibling»; дождаться карточек.
2. «Добавить» у «friendship» и в тот же миг крестик.
3. Крестик → осмотреть вопрос → «Остаться».
4. Тап «relationship» и сразу крестик, пока грузится → «Остаться».
5. Системная «назад» → «Закрыть».
6. Новый снимок; тап «friendship» → «Добавить»; крестик.

Ожидания:
- экран: крестик во время записи «friendship» не срабатывает (если
  успели); вопрос «Закрыть без добавления?» с «Не добавлено слов: 1»,
  кнопки «Остаться» и «Закрыть»; «Остаться» — окно как было; с
  загружающимся «relationship» число 2; «назад» показывает тот же вопрос;
  «Закрыть» — окно закрыто, в словаре только «friendship»; в новом окне
  после записи единственного загруженного слова крестик закрывает сразу
- лог: `RequestClose` → `askCloseConfirm | pending=1`; `Stay` →
  `closeCloseConfirm`; `askCloseConfirm | pending=2`; `ConfirmClose` →
  `close`; во время записи `RequestClose` → `no-op | reason=requestClose:
  adding` (если успели); в новом окне `RequestClose` → `close`

Анти-проверки:
- лог: после `ConfirmClose` нет `startAddingWord`; в словаре нет
  «sibling» и «relationship»

Прогон:

## AWP-14.3, AWP-43.6, AWP-42.6 Поворот экрана: с ошибкой на слове, во время загрузки, после правок — стейт на месте, второго запроса нет

Вход:
- словарь `en` → `ru`; снимок абзаца; автоповорот включён

Шаги:
1. Абзац → окно; авиарежим; тап «link» → ошибка; тап «link» — свернуть.
2. Повернуть телефон; тап «link» — раскрыть.
3. Авиарежим выключить; тап «sibling»; пока виден признак загрузки —
   повернуть телефон; дождаться карточки.
4. Удалить лексему, править перевод у «sibling»; повернуть телефон.

Ожидания:
- экран: после поворота у «link» та же ошибка и «Повторить», запрос сам
  не ушёл; на «sibling» признак загрузки переживает поворот, ответ ложится
  в карточку; удаление и правка после поворота на месте
- лог: на «sibling» один `startLookup` и один `POST
  /v1/lexemes/from-lemma`; после поворотов нет `startLookup`, нет
  `RecognizeText`

Анти-проверки:
- лог: нет `showRetryableError`, нет повторного `ExtractLemmas`

Прогон:

## AWP-27.1, AWP-27.2, AWP-27.3 Гибель процесса при открытом окне: после возврата окна нет, добавленное в словаре, остальное потеряно

Вход:
- словарь `en` → `ru`; снимок абзаца; агент с `adb`

Шаги:
1. Абзац → окно; тап «friendship» → «Добавить»; тап «sibling» → удалить
   лексему.
2. Кнопка «Домой»; агент: `adb shell am kill co.lexeme.app.dev`; вернуться
   в приложение.
3. Новый снимок абзаца → окно; тап «sibling» → «Добавить» и сразу агент:
   `adb shell am kill co.lexeme.app.dev`; вернуться; осмотреть словарь.

Ожидания:
- экран: после возврата вкладка «Слова» без окна; «friendship» в словаре,
  «sibling» нет; после второго возврата окна нет, «sibling» либо целиком с
  лексемами, либо отсутствует — половины нет
- лог: после возврата строк `###SCAN###` нет до нового снимка

Анти-проверки:
- словарь: нет «sibling» без лексем и нет лексем без слова

Прогон:

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

Прогон:
