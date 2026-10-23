# IS536 | Ручники: лексемы-кандидаты через нейронку

Форма — `backend/docs/handbook/feature-flow/spec-checks.md`, раздел 3:
заголовок кейса несёт адреса строк спек устройства — AWP
(`docs/handbook/specs/words-add-from-photo/spec.md`), WCL
(`docs/handbook/specs/word-card-lexemes/spec.md`), WFP
(`backend/docs/handbook/specs/words-from-photo/spec.md`) — и номер решения
брифа (Д). В чат кейсы выдаются по одному: счётчик, название, шаги-атомы,
последний шаг «Напиши “ок”». Состояние и лог после «ок» проверяет агент.

Вход для всех кейсов: Pixel 7 Pro, dev-сборка `co.lexeme.app.dev` с ключом
в `local.properties`, сервер `api.lexeme.pro` v0.1.10 (контракт 0.3.0:
`lexemes` по компонентам, `remarks`, `recommendations`). Снимать:
`adb logcat -d | grep "###SCAN###\|###VOCAB###\|###WORDCARD###\|###NETWORK###"`,
перед кейсом `adb logcat -c`. Кейсы идут по порядку: поздние опираются на
данные ранних. На время прогона таймаут экрана 30 минут
(`adb shell settings put system screen_off_timeout 1800000`), после —
вернуть прежний (600000).

## Подготовка данных

Прогон идёт с ПУСТОГО словаря (изучаемый `en`, перевод `ru`): слов и
групп нет, компоненты — как заведены по умолчанию (ядро «Перевод»,
«Часть речи» с опциями, «Пример»). Слова в словаре появляются по ходу
кейсов: кейс 4 пишет «run» и «bank» из снимка, кейс 11 заводит руками
«qwrtzk» и «ran». Испанский словарь для «Рода» (кейсы 5 и 6) — отдельный,
если его нет, кейсы пропускаются.

Снимки — любые картинки с английским текстом из галереи, имена файлов
в системном выборе не видны; первая строка прогона — «She ran to the
bank.» (`is532_small.png`, 5 слов). Для M8 — белый лист `blank.png` и
пустой файл `empty.png` (0 байт), оба `adb push` в Pictures и
просканировать.

Сервер: кейс M1 гоняется ДО того, как на сервере появится ключ нейронки
(`LLM_API_KEY` в `~/lexeme/deploy/.env`); после M1 ключ положить
(`task all-pull`), остальные кейсы — с ключом.

Ожидания по лексемам-кандидатам — по форме, не по текстам: отбор делает
нейронка, точные тексты и их число от прогона к прогону могут отличаться.
Сверять: число разделов, имена компонентов у значений, строки «Замечаний»
и «Сопоставления», счёт на кнопке и в снекбаре.

## Словарь логов

### SCAN — экран снимка (`###SCAN###`)

- `Reduce ---message---: <Msg>` — входящее сообщение. Тяжёлые — именем
  класса: `TextRecognized`, `WordsFound`, `FormLoaded`; остальные со
  значениями: `TextEmpty`, `RecognitionFailed(errorRes=…)`, `WordsEmpty`,
  `LookupDenied(errorRes=…)`, `LookupUnavailable(errorRes=…)`,
  `FormFailed(errorRes=…)`, `AddFinished(lexemeCount=…, wordCount=…)`,
  `AddGroupGone(…)`, `AddWordGone(…)`, `AddComponentGone(…)`,
  `AddFailed(…)`, `RetryLookup`, `ToggleCard(lemma=…)`,
  `DeleteCard(lemma=…)`, `DeleteLexeme(lemma=…, key=…)`,
  `DeleteValue(ref=…)`, `SubmitText(ref=…, text=…)`, `ResetText(ref=…)`,
  `SetOption(ref=…, option=…)`, `OpenSheet(sheet=…)`, `CloseSheet`,
  `UpdateDraft(text=…)`, `SetCardTarget(…)`, `SetCardGroup(…)`,
  `SetCommonGroup(groupId=…)`, `ToggleComponentMatches`, `ToggleNotAdded`,
  `RequestAdd`, `ConfirmAdd`, `CancelAdd`, `RetryAdd`, `RequestClose`,
  `ConfirmClose`, `Stay`.
- `Reduce ---step---: <атом> | k=v` — один шаг состояния; у сообщения
  цепочка шагов. Атомы:
  - запуск: `startLooking | tokens=K`, `showNoWords`, `showError |
    errorRes=…` (без «Повторить»), `showRetryableError | errorRes=…` (с
    «Повторить»), `startFormLoading | dictionaryId=D words=M lemmas=L`;
  - сборка окна: `applyForm | components=C groups=G cards=K notAdded=J`,
    `applyRemarks | remarks=R`, `applyComponentMatchLines | lines=N`;
  - пересчёты: `refreshActiveValues | active=A`, `refreshExistsMarks |
    exists=E`, `refreshCoreMarks | lacksCore=X`, `refreshSummary |
    lexemes=… words=… existing=… lacksCore=… enabled=…`;
  - карточки и значения: `toggleCard | lemma=…`, `deleteCard | lemma=…`,
    `deleteLexeme | lemma=… key=…`, `deleteValue | ref=…`, `applyTextEdit |
    ref=… length=…`, `dropEmptyTexts | ref=…`, `resetText | ref=…`,
    `setOption | ref=… option=…`, `setCardTarget | …`, `setCardGroup | …`,
    `setCommonGroup | groupId=…`, `openSheet | sheet=…`, `closeSheet`,
    `updateSheetDraft | length=…`, `markDirty` — после него закрытие с
    вопросом, `toggleComponentMatchesExpanded | expanded=…`,
    `toggleNotAddedExpanded | expanded=…`;
  - запись: `askAddConfirm | lexemes=… words=…`, `closeAddConfirm`,
    `startAdding | words=… lexemes=…`, `showAddFailure | failure=…`,
    `clearAddFailure`, `resetDeadGroup | groupId=…`, `resetDeadTarget |
    wordId=…`, `dropComponent | typeId=…`, `closeWithResult | lexemes=…
    words=…`;
  - закрытие: `askCloseConfirm`, `closeCloseConfirm`, `close`.
- `Reduce ---step---: no-op | reason=…` — событие пришло и намеренно
  проигнорировано: `…: no form` — окно ещё не собрано, `openSheet: no text
  …`, `clearAddFailure: no failure`, `noOperation`.
- `RunEffect: <эффект>` — эффект ушёл в хендлер: `RecognizeText(uri=…)`,
  `LookupWords(tokens=K)`, `LoadForm(dictionaryId=D, lemmaTexts=…)`,
  `AddWords(words=W, lexemes=X)`.
- Исходы юзкейса: `recognize outcome=text tokens=K | empty`; `lookup
  outcome=dict=D words=M lemmas=L recommendations=R | no words |
  denied=<причина> | unavailable`; `form outcome=loaded components=C
  options=O groups=G existing=E | dictionary missing | failed: …`; `add
  outcome=added words=W lexemes=X | rejected <Rejected…>`.
- Номера запуска у экрана нет (Д51): у каждого снимка свой экран, ответ
  закрытого экрана некуда доставить.

### VOCAB — вкладка «Слова», диалог снимка (`###VOCAB###`)

- `Reduce ---message---: OpenScanDialog | ChooseCamera | ChooseGallery |
  ImagePicked(uri=…) | CameraUnavailable(errorRes=…) | CloseScanDialog |
  ScanCandidatesAdded(lexemeCount=…, wordCount=…)`.
- `Reduce ---step---: showScanDialog | hideScanDialog | showScanDialogError
  | errorRes=…`.
- `RunEffect: LaunchCamera | LaunchGallery | OpenScanCandidates(uri=…)`.

### WORDCARD — диалог карточки слова (`###WORDCARD###`)

- `candidates lookup run=N word=W outcome=found lemmas=L
  recommendations=R | not found | word missing | denied=<причина> |
  unavailable | word failed` — ответ по слову; `candidates form run=N
  word=W outcome=loaded … | dictionary missing | failed: …`; `candidates
  add run=N word=W outcome=added … | rejected …`.
- `Reduce ---message---: OpenCandidates | CandidatesFound |
  CandidatesNotFound(requestId=N) | CandidatesLookupFailed(errorRes=…,
  requestId=N) | CandidatesLookupUnavailable(…) | CandidatesFormLoaded |
  CandidatesFormFailed(…) | CandidatesAdded(lexemeCount=…, requestId=N) |
  CandidatesTransferred(…) | CandidatesWordGone(…) |
  CandidatesComponentGone(…) | CandidatesDictionaryGone(…) |
  CandidatesAddFailed(…) | Candidates(action=…)`.
- `Reduce ---step---`: `openCandidates | wordId=W requestId=N`,
  `startCandidatesForm | wordId=W lemmas=L formLemmas=F
  recommendations=R`, `showCandidatesError | errorRes=…`,
  `showCandidatesRetryableError | errorRes=…`, `retryCandidatesLookup |
  wordId=W requestId=N`, `applyCandidatesForm | components=C cards=K`,
  `applyCandidatesRemarks | remarks=R`, `applyCandidatesComponentMatchLines
  | lines=N`, `refreshCandidatesActiveValues | inactive=…`,
  `refreshCandidatesExists | exists=…`, `refreshCandidatesLemmaExists |
  existsInLemma=… sameValues=…`, `refreshCandidatesCore | lacksCore=…`,
  `refreshCandidatesSummary | lexemes=… existing=… lacksCore=…
  enabled=…`, действия `toggleCandidateCard | lemma=…`,
  `deleteCandidateLexeme | lemma=… key=…`, `deleteCandidateValue | ref=…`,
  `applyCandidateTextEdit | ref=… length=…`, `resetCandidateText | ref=…`,
  `setCandidateOption | ref=… option=…`, `toggleCandidatesLemmaWord |
  choice=…`, `openCandidatesSheet | sheet=…`, `markCandidatesDirty`,
  запись `startAddingCandidates | lexemes=… choice=…`,
  `showCandidatesAddFailure | failure=…`, `dropCandidatesComponent |
  typeId=…`, закрытие `askCandidatesClose`, `hideCandidates |
  requestId=N`.
- `Reduce ---step---: no-op | reason=<Msg>: foreign run N` — ответ
  закрытого или перезапущенного диалога отброшен (WCL-11.4).

### NETWORK — слой сети (`###NETWORK###`)

- `POST /v1/words/lookup -> <код> in <мс>ms` — одна строка на обмен; ключ
  не печатается. `503` — сервер без нейронки.

### Сервер (`ssh … docker logs lexeme-server`)

- строка вызова нейронки по WFT-101: номер запуска, слова, модель, токены,
  время; без ключа — `llm_unavailable`.

## Кейсы

## WFP-26.3, WCL-11.6, WCL-11.9 Сервер без ключа нейронки: «недоступен» и «Повторить» (Д53)

Вход:
- на сервере нет `LLM_API_KEY`; словарь «био» на вкладке «Слова»

Шаги:
1. Кнопка-камера → «Галерея» → `is532_small.png`; дождаться текста.
2. Нажать «Повторить»; дождаться текста; нажать «Закрыть».
3. Открыть карточку «run» → «Добавить лексемы-кандидаты»; дождаться.
4. Нажать «Повторить»; дождаться; нажать «Закрыть».

Ожидания:
- экран: после «Распознаю текст…» и «Ищу слова в словаре…» — «Сервер
  сейчас недоступен, попробуй позже», под ним «Закрыть» и «Повторить»;
  после «Повторить» снова «Ищу слова…» и тот же текст; после «Закрыть» —
  вкладка «Слова»
- экран, карточка: в диалоге тот же текст, «Закрыть» и «Повторить»;
  «Повторить» → признак загрузки → тот же текст; «Закрыть» → карточка
- лог: `POST /v1/words/lookup -> 503` дважды на экране и дважды в карточке
- лог: `lookup outcome=denied=LLM_UNAVAILABLE`, `showRetryableError |
  errorRes=…`, `RetryLookup`, `startLooking | tokens=K` с тем же K
- лог: `candidates lookup run=N … outcome=denied=LLM_UNAVAILABLE`,
  `showCandidatesRetryableError`, `retryCandidatesLookup | requestId=N+1`

Анти-проверки:
- экран: «Слов не найдено» и «Лексем-кандидатов для «run» нет» не
  показываются
- лог: нет `FATAL` в crash-буфере

Прогон: ✅ 2026-10-20 — снимок: 11 токенов, два `503`, повтор с теми же
токенами, закрытие без вопроса; карточка: запуски 1 и 2, оба `503`,
`hideCandidates`

## WFP-24.1, WFP-13.1, AWP-04.10, AWP-29.1, AWP-29.2, AWP-31.1, AWP-31.4, AWP-34.1 Галерея → экран снимка → окно по компонентам (Д51, Д54)

Вход:
- ключ нейронки на сервере; пустой словарь `en` → `ru`

Шаги:
1. Кнопка-камера → «Галерея» → картинка «She ran to the bank.»; смотреть
   подписи.
2. Дождаться окна; попробовать нажать вкладку «Группы».
3. Найти «run»; раскрыть «Замечания» и «Сопоставление».

Ожидания:
- экран: подписи «Распознаю текст…» (с «Отменить») → «Ищу слова в
  словаре…» → «Собираю окно…»; затем окно на весь экран, вкладок и
  выбора словаря нет
- экран: пять карточек «she», «run» (в тексте: ran), «to», «the», «bank»
  в порядке текста, каждая целиком: лемма, «в тексте: …», «Куда» и
  «Группа», лексемы со значениями; между карточками толстая полоса;
  пометок «есть в словаре» нет — словарь пуст
- экран, «run»: у лексемы-кандидата значения с именами компонентов
  словаря — «Перевод», «Часть речи: глагол», «Пример»; лишних полей
  «толкование/пример из ответа» нет; все лексемы — Русского Викисловаря,
  с переводом (Д63), пометки «англ.» нет; примеры у значения — только
  его собственные (Д64)
- экран: «Замечания» без строки «нет перевода» — у значений Русского
  Викисловаря перевод есть всегда (Д63); «Сопоставление» свёрнуто,
  раскрыть — строки «перевод → Перевод», «пример → Пример», «Часть речи:
  глагол», «Часть речи: существительное» и другие опции без повторов
- лог: `recognize outcome=text tokens=5`, `startLooking | tokens=5`,
  `POST /v1/words/lookup -> 200`, `lookup outcome=dict=D words=5 lemmas=5
  recommendations=R`, `startFormLoading | dictionaryId=D words=5
  lemmas=5`, `form outcome=loaded components=C options=O groups=0
  existing=0`, `applyForm | components=C groups=0 cards=5 notAdded=0`,
  `applyRemarks | remarks=R`, `applyComponentMatchLines | lines=N`,
  `refreshActiveValues`, `refreshExistsMarks | exists=0`,
  `refreshCoreMarks | lacksCore=0`, `refreshSummary | … enabled=true`

Анти-проверки:
- экран: строки «Куда пишем» нет; имён «Викисловарь», «от словаря» нет
- лог: `POST` ровно один

Прогон: ❌✅ — ❌ 2026-10-21: путь до окна прошёл (`200` за 9 с, форма,
окно), но из 5 слов у 4 статус `error`: нейронка пишет `option` в
`componentMatches` строкой, сервер ждёт число → «ответ нечитаем»
(WFT-97.2); ✅ 2026-10-23: снимок абзаца учебника, бэкенд v0.1.31 — 42
слова за 26 с, 36 найдено, «не найдено» 6 ожидаемых, значения только
Русского Викисловаря, пар и чужих примеров нет; на экране — как ждали

## AWP-14.3, AWP-15.1, AWP-16.1, AWP-16.6, AWP-17.1, AWP-17.6, AWP-19.2, AWP-25.1 Правки в окне, поворот, закрытие с вопросом (Д56)

Вход:
- окно из предыдущего кейса

Шаги:
1. Удалить карточку «she» крестиком; у «run» удалить вторую лексему.
2. У первой лексемы «run» нажать «Перевод» → дописать слово → «Сохранить»;
   открыть снова → «сбросить».
3. У той же лексемы «Часть речи» → выбрать «существительное».
4. Повернуть экран, вернуть обратно.
5. Нажать «назад» → «Остаться»; крестик → «Закрыть».

Ожидания:
- экран: карточка «she» исчезла, лексема исчезла, счёт на кнопке уменьшился
- экран: после «Сохранить» у значения пометка «изменено»; в листе
  «сбросить» и «было: …»; после «сбросить» пометки нет
- экран: «Часть речи: существительное»; зависимые от прежней опции
  значения погасли (если были)
- экран: после поворота удаления, правка и опция на месте
- экран: «назад» → «Закрыть без добавления?», «Остаться» — всё на месте;
  крестик → «Закрыть» → вкладка «Слова», слов не прибавилось
- лог: `deleteCard | lemma=she`, `markDirty`, `deleteLexeme | lemma=run
  key=…`, `applyTextEdit | ref=… length=…`, `resetText | ref=…`,
  `setOption | ref=… option=…`, `refreshActiveValues`, `askCloseConfirm`,
  `closeCloseConfirm`, `RequestClose`, `ConfirmClose`, `close`

Анти-проверки:
- лог: нет `startAdding`, нет `AddWords`; после поворота нет второго
  `RecognizeText` и `POST`

Прогон:

## AWP-02.1, AWP-19.5, AWP-20.1, AWP-23.1, WFP-24.2 Запись: одно нажатие, «назад» во время записи, снекбар (Д51)

Вход:
- пустой словарь `en` → `ru`; картинка «She ran to the bank.»

Шаги:
1. Кнопка-камера → «Галерея» → та же картинка; дождаться окна.
2. Удалить все карточки, кроме «run» и «bank»; нажать «Добавить N значений
   в 2 слова».
3. В вопросе нажать «Да»; тут же нажать «назад».
4. Дождаться вкладки.

Ожидания:
- экран: вопрос «Добавить N значений в 2 слова?»; после «Да» кнопка
  «Добавляю…», «назад» ничего не делает; затем вкладка «Слова» и снекбар
  «Добавлено N значений в 2 слова»; в словаре появились «run» и «bank» с
  лексемами из окна
- лог: ровно один `startAdding | words=2 lexemes=N`, один `add
  outcome=added words=2 lexemes=N`, `closeWithResult | lexemes=N words=2`
- лог: `ScanCandidatesAdded(lexemeCount=N, wordCount=2)` на вкладке

Анти-проверки:
- лог: второго `startAdding` и `AddWords` нет; `RequestClose` во время
  записи либо нет, либо без `close`
- экран: в словаре нет дублей лексем

Прогон:

## AWP-30.1, AWP-30.5, AWP-30.3, AWP-30.4 «Замечания»: рекомендация компонента и «нет перевода» (Д55)

Вход:
- словарь «ёла» без компонента «Род»; снимок `is532_es.png`

Шаги:
1. Переключиться на «ёла»; кнопка-камера → «Галерея» → `is532_es.png`.
2. Дождаться окна; найти раздел «Замечания».
3. Закрыть окно крестиком.

Ожидания:
- экран: в «Замечаниях» строка «У лексем-кандидатов есть «Род». Заведите
  в конструкторе компонент «Род» (выбор, опции: м.р., ж.р.), и значения
  лягут в него»; кнопок в разделе нет
- экран: у «perro» значения «Перевод», «Часть речи: существительное»;
  значения «Род» нет нигде
- лог: `lookup outcome=… recommendations=1`, `applyRemarks | remarks=R`
  (R ≥ 1)

Анти-проверки:
- экран: раздел «Замечания» без кнопок и ссылок; окно закрылось без
  вопроса (правок не было)

Прогон:

## AWP-32.1, AWP-31.2, AWP-17.6, AWP-32.9 «Род» заведён: значение ложится в компонент, зависимость от опции (Д49)

Вход:
- словарь «ёла»; конструктор компонентов открыт

Шаги:
1. В конструкторе «ёла» завести компонент «Род»: шаблон «выбор», опции
   «м.р.», «ж.р.», зависит от «Часть речи» → «существительное».
2. Кнопка-камера → «Галерея» → `is532_es.png`; дождаться окна.
3. Найти «perro»; раскрыть «Сопоставление».
4. У первой лексемы «perro» сменить «Часть речи» на «глагол», затем назад
   на «существительное».
5. Удалить все карточки, кроме «perro»; добавить; в карточке «perro»
   удалить новую лексему и нажать «Отменить» в снекбаре.

Ожидания:
- экран: у лексем «perro» значение «Род: м.р.»; в «Сопоставлении» строка
  «Род: м.р.»; замечания про «Род» нет
- экран: при «глагол» значение «Род» гаснет, при «существительное» —
  снова активно
- экран: в карточке «perro» у лексемы «Часть речи: существительное» и
  «Род: м.р.»; после удаления и «Отменить» лексема вернулась с частью речи
  и родом
- лог: `applyComponentMatchLines | lines=N` с строкой «Род», `setOption |
  … option=…`, `refreshActiveValues | active=…` меняется, `add
  outcome=added words=1 lexemes=…`

Анти-проверки:
- экран: «Замечаний» про «Род» нет; у лексемы-глагола «Род» не пишется

Прогон:

## WFP-21.3, WFP-21.4, WFP-08.3, WFP-08.4, WFP-07.1 Отмена во время распознавания и камера без снимка (Д52)

Вход:
- словарь «био»; `adb shell run-as co.lexeme.app.dev ls cache/photos`
  пусто

Шаги:
1. Кнопка-камера → «Камера» → закрыть камеру без снимка.
2. Кнопка-камера → «Камера» → снять страницу с текстом; пока «Распознаю
   текст…», нажать «Отменить».
3. Сразу кнопка-камера → «Камера» → снять другую страницу; дождаться окна.
4. Закрыть окно.

Ожидания:
- экран: после закрытия камеры без снимка — вкладка «Слова» без диалога и
  без ошибки
- экран: после «Отменить» — вкладка; второй снимок распознаётся и даёт
  своё окно, слова — со второй страницы
- лог: `ImagePicked(uri=…)` дважды; после первого `RequestClose` → `close`;
  окно второго — своим `startLooking`/`applyForm`
- состояние: после шага 4 папка `cache/photos` пуста

Анти-проверки:
- экран: результат первого снимка не всплывает ни на вкладке, ни во втором
  окне
- лог: после `close` первого экрана нет его `applyForm`

Прогон:

## WFP-10.3, WFP-11.1, WFP-19.1, WFP-19.2 Пустой лист, пустой файл, текст без слов словаря

Вход:
- словарь «био»; в галерее `blank.png`, `empty.png`, `is532_junk.png`

Шаги:
1. Кнопка-камера → «Галерея» → `blank.png`; дождаться.
2. «Закрыть»; кнопка-камера → «Галерея» → `empty.png`; дождаться.
3. «Закрыть»; кнопка-камера → «Галерея» → `is532_junk.png`; дождаться.
4. «Закрыть».

Ожидания:
- экран, `blank.png`: «Слов не найдено» и «Закрыть», без «Повторить»
- экран, `empty.png`: «Не удалось распознать текст» и «Закрыть»
- экран, `is532_junk.png`: «Слов не найдено» и «Закрыть»
- лог: `recognize outcome=empty` → `TextEmpty` → `showNoWords` без `POST`;
  `RecognitionFailed(errorRes=…)` → `showError`; `recognize outcome=text
  tokens=2` → `POST … -> 200` → `lookup outcome=no words` → `showNoWords`

Анти-проверки:
- лог: для `blank.png` и `empty.png` нет `POST`
- лог: нет `FATAL`

Прогон:

## WFP-22.4, AWP-27.1 Гибель процесса: галерея открыта; окно открыто (Д52)

Вход:
- словарь «био»; `adb` под рукой

Шаги:
1. Кнопка-камера → «Галерея»; не выбирая снимок, выполнить
   `adb shell am kill co.lexeme.app.dev`; выбрать `is532_small.png`.
2. Дождаться окна; нажать Home; выполнить
   `adb shell am kill co.lexeme.app.dev`.
3. Вернуться в приложение через «Недавние».

Ожидания:
- экран, шаг 1: приложение ожило, экран снимка открылся и распознал
  `is532_small.png`, окно собралось
- экран, шаг 3: вкладка «Слова», окна нет, ошибки нет
- лог: после шага 1 `ImagePicked(uri=content://…)`, `recognize
  outcome=text tokens=K`; после шага 3 нет `RecognizeText`

Анти-проверки:
- лог: после шага 1 нет `RecognitionFailed`; после шага 3 нет `FATAL`

Прогон:

## WCL-08.5, WCL-07.3, WCL-07.5, WCL-07.6, WCL-16.1, WCL-17.1, WCL-10.1, WCL-10.3, WCL-12.4 Диалог карточки: состав, поворот, запись (Д50)

Вход:
- «bank» записан кейсом 4, у него две-три лексемы из окна

Шаги:
1. Карточка «bank» → «Добавить лексемы-кандидаты»; пока признак загрузки,
   повернуть экран и вернуть.
2. Дождаться диалога; прокрутить до конца; удалить одну
   лексему-кандидата; повернуть экран и вернуть.
3. Нажать «Добавить N значений» два раза подряд быстро; тут же «назад».
4. Дождаться закрытия.

Ожидания:
- экран: после поворота признак загрузки на месте, второго запроса нет
- экран: заголовок «bank» с крестиком, раздел прямой леммы «bank»,
  значения с именами компонентов; у лексем, которые уже есть в слове, —
  пометка «есть»; разделы «Замечания» (если есть) и «Сопоставление»
  свёрнут; «Данные: Wiktionary, CC BY-SA 4.0»; кнопка «Добавить N
  значений» всегда видна при прокрутке; строки «Куда пишем» нет
- экран: после удаления и поворота лексемы нет, счёт без неё
- экран: во время записи «Добавляю…», «назад» и тап мимо не закрывают;
  затем диалог закрыт, внизу карточки «Добавлено N значений», новые
  лексемы в карточке
- лог: один `candidates lookup run=N`, `startCandidatesForm | …`,
  `applyCandidatesForm | components=4 cards=1`,
  `applyCandidatesComponentMatchLines | lines=…`, `refreshCandidatesExists
  | exists=E` (E ≥ 1); один `startAddingCandidates | lexemes=N
  choice=null`, `candidates add run=N … outcome=added`

Анти-проверки:
- лог: после поворота нет второго `openCandidates` и `POST`; второго
  `startAddingCandidates` нет; `hideCandidates` только после `CandidatesAdded`

Прогон:

## WCL-11.1, WCL-15.1, WCL-15.2 Нет лексем-кандидатов; лемма-форма: переименовать слово

Вход:
- словарь «био»; слова «qwrtzk» и «ran» заведены руками на вкладке
  «Слова», без значений

Шаги:
1. Карточка «qwrtzk» → «Добавить лексемы-кандидаты»; дождаться; «Закрыть».
2. Карточка «ran» → «Добавить лексемы-кандидаты»; дождаться.
3. В разделе «форма слова «run»» выбрать «переименовать слово в «run»»;
   оставить одну лексему; «Добавить 1 значение».

Ожидания:
- экран, «qwrtzk»: «Лексем-кандидатов для «qwrtzk» нет» и только «Закрыть»
- экран, «ran»: раздел «форма слова «run»» с пометкой сервера и выбором
  «переименовать слово в «run»» / «перенести в «run» (…) и удалить «ran»»
  (второй — потому что «run» в словаре есть)
- экран: после записи карточка называется «run»… либо слово «ran»
  переименовано и лексема в нём; «Добавлено 1 значение»
- лог: `candidates lookup … outcome=not found`, `showCandidatesError`;
  `startCandidatesForm | … formLemmas=1`, `toggleCandidatesLemmaWord |
  choice=Rename(…)`, `startAddingCandidates | lexemes=1 choice=Rename(…)`

Анти-проверки:
- экран, «qwrtzk»: «Повторить» нет
- лог: нет `FATAL`

Прогон:

## WFP-04.2 Сборка без ключа: кнопки-камеры нет (если есть время)

Вход:
- `local.properties` без `LEXEME_ACCESS_KEY`; свежая dev-сборка

Шаги:
1. Открыть вкладку «Слова».
2. Открыть карточку любого слова.

Ожидания:
- экран: кнопки-камеры на вкладке нет; в карточке нет «Добавить
  лексемы-кандидаты»

Анти-проверки:
- лог: нет `POST /v1/words/lookup`

Прогон:
