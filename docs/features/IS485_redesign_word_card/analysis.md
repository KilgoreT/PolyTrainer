# IS485 word_card — Анализ: виджеты, Mate, объём работ

По коду `modules/screen/wordcard/` (2026-07-15). Источник требований: [brief.md](brief.md) (макеты, токены, решения №1-6).

## Виджеты `modules/screen/wordcard/` — что и как менять

| Файл | Правка |
|---|---|
| `WordCardScreen.kt` | фон `colorScheme.tertiary` (#F1E9FA) → новый токен `#EDE9F6` (строка :143); system bars туда же; loader-оверлей остаётся |
| `widget/TopBarWidget.kt` | прозрачный контейнер TopAppBar (на фоне экрана); состав (back + ⋮) не меняется |
| `widget/WordFieldWidget.kt` | карточка: r12/elevation4 → r18 + мягкая тень `0 2 5 rgba(50,40,90,0.06)`; дата: лейбл 13.5 `#9A9AA2` + дата bold (сейчас близко — BodyM gray + BodyMBold); **флаг 38dp из стейта** вместо захардкоженного `example_ic_flag_gb` 24dp; инлайн-редактирование слова остаётся |
| `widget/lexeme/LexemeCard.kt` | r12/elevation4 → r18 + мягкая тень, паддинг 16→20; **+вариант черновика**: бордер `#DCD6F2` + бейдж «Черновик» (isDraft уже выводится из `id == NOT_IN_DB`) |
| `widget/lexeme/SubentityChip.kt` | уже filled primary — правки формы: r6 → r8, h26, текст 13 (сейчас labelLarge), иконка 16 → 11-13. Оба использования (label-чип «×» и добавление «+») выглядят в макете одинаково — виджет остаётся один |
| `widget/lexeme/ComponentChipsRow.kt` | +лейбл секции «ДОБАВИТЬ КОМПОНЕНТ» (12 bold `#9A9AA2` tracking, right-aligned) над чипами; чипы выравниваются вправо |
| `widget/lexeme/ComponentValueField.kt` | view: label-чип + значение 17; edit: label-чип + поле с курсором primary и underline 1px `#C2C1E8` (тонкий, не как у формы словаря) |
| `widget/lexeme/ComponentLabel.kt` | под канон чипа h26/r8/13 |
| `widget/lexeme/DeleteLexemeButton.kt` | красный `#D64545` + корзина 18dp + divider `#F0EEF7` над кнопкой; текст «Удалить» (решение №3) |
| `widget/lexeme/LexemeComponentsBlock.kt` | сборка: подсказка «Выберите компонент, чтобы добавить значение» (14.5 `#8A8A90`) для пустого черновика; проброс isDraft |
| `widget/AddLexemeWidget.kt` (FAB) | новый вид: 60dp r19, `#C9C3EA` + тень `0 10 22 -6 rgba(100,90,160,0.5)`; disabled (`isCreatingLexeme` — уже в стейте): `#D8D3EC` opacity 0.75 БЕЗ тени |
| `ConfirmDeleteLexemeWidget` / `ConfirmDeleteWordWidget` | лёгкая стилизация по токенам (фреймов нет — по системе) |

## Mate-правка (одна, узаконена решением №5 — флаг словаря)

Данных о флаге в стейте нет (`WordState.Loaded` несёт только `dictionaryId`; `Term` — тоже). Наименее шумный путь — расширить существующую цепочку загрузки, без новых Msg/Effect:

1. `entity/Term.kt` — +поле `dictionaryFlagRes: Int? = null`.
2. `app/.../WordCardUseCaseImpl.getTermById` — резолвит флаг словаря: `numericCode` словаря из БД → `flagRes` через blongho (тот же паттерн, что `findFlag` у формы словаря).
3. `mate/State.kt` — `WordState.Loaded` +`dictionaryFlagRes: Int? = null`; `mate/WordCardReducer.kt` — ветка `WordLoaded` маппит поле (1 строка).
4. `WordFieldWidget` — `ImageFlagWidget(flagRes ?: placeholder)`.

Существующие тесты не ломаются (поля с дефолтами); новые: 1 reducer-тест «WordLoaded прокидывает dictionaryFlagRes» (full-state assert, R-TR-003) + дополнение `WordCardUseCaseImplTest` на резолв флага.

## Прочее

- **Строки** (обе локали): +«Черновик», +«Выберите компонент, чтобы добавить значение», +«ДОБАВИТЬ КОМПОНЕНТ»; «Удалить» — существующий ключ.
- **Токены** `theme/Color.kt`: +6-7 val (wordCardBackground #EDE9F6, cardShadow-цвет, draftBorder #DCD6F2, draftBadgeBg #EEECEF, destructive #D64545, divider #F0EEF7, editUnderline #C2C1E8, fabEnabled/fabDisabled/fabShadow); переиспользовать formTextSecondary/formTextHint из фичи create_dictionary.
- **Не трогаем**: reducer-механику (commit/flush/чипы), общие core:ui-виджеты, диалоги-логику, тесты-контракт (221 шт.).

## Объём

~12 файлов UI wordcard (рестайл) + 4 точки Mate-цепочки флага (entity/usecase/state/reducer — суммарно ~15 строк) + строки + токены + 2 новых теста. Заметно больше файлов, чем у формы словаря, но паттерн отработан; сложность средняя — вся хитрая механика остаётся как есть.
