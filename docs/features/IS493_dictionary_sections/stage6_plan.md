# IS493 / Э6 — План реализации: импакт удаления + «удалить вместе со словами»

Основа: stage6_design_tree.md (D30–D32), решения юзера 2026-08-28.
Один коммит.

## Фаза 1. Данные

- [ ] Domain: `DeleteGroupWithWordsOutcome` (Success(deletedWords) |
      NotFound) в GroupOutcomes.kt.
- [ ] **androidTest СНАЧАЛА (red стабом TODO)** — новый блок:
      - deleteGroupWithWords: слова группы удалены ИЗ words (прямой
        счёт), их membership в ДРУГИХ группах вычищен каскадом, группа
        мертва; Success(deletedWords=N);
      - ГЛУБИНА каскада (ревью Data-3): COUNT lexemes И
        component_values по удалённым wordIds = 0;
      - слово в двух группах: после удаления группы A со словами — слово
        исчезло и из группы B (окно B переэмитило);
      - NotFound на мёртвой группе, слова НЕ тронуты (+ лог NotFound);
      - пустая группа → Success(0), поведение = обычному delete;
      - чанкование (ревью Data-2): chunkSize=3 (@VisibleForTesting) на
        7–8 словах — граница пересекается дважды, всё удалено атомарно;
      - samples-легаси (ревью Data-1): вручную вставленная строка
        samples удалённой лексемы вычищена транзакцией.
- [ ] DAO: `wordIdsOfGroup`, `deleteWordsByIds` (chunk на вызывающей),
      `deleteSamplesByWordIds` (подзапрос через lexemes).
- [ ] GroupApi + Impl: `deleteGroupWithWords(groupId, chunkSize=999)`
      (D30.2, транзакция §2.4, samples ДО words).

## Фаза 2. Groupstab

- [ ] **Юниты СНАЧАЛА (red прогоном — новые Msg/поля позволят честный
      red только на ветках; где нет — мутационная проверка)**:
      - askDeleteConfirm → объект (повторное открытие — галка снята);
        ToggleDeleteWords туда-сюда со значением в логе; no-op без
        конфирма И при isRecheckOpen (ревью Mate-2);
      - ConfirmDelete, порядок веток: isRecheckOpen ПЕРВЫМ →
        DeleteGroupWithWords + закрытие; галка+count>0 →
        openDeleteRecheck БЕЗ эффекта; галка при count==0 →
        игнорируется, обычный DeleteGroup (ревью Mate-3); галка снята →
        DeleteGroup;
      - DismissDelete из recheck → закрыто ВСЁ, эффектов нет;
      - closeConfirmForDeadGroup в цепочке SliceLoaded: группа умерла
        под конфирмом/recheck → конфирм закрыт (ревью Mate-1); жива →
        no-op;
      - смена словаря под конфирмом → закрыт (существующая цепочка);
      - сценарный: kebab → конфирм → галка → recheck → подтверждение →
        эффект WithWords → slice перерисовал; и негативный: recheck →
        dismiss → повторное открытие со снятой галкой.
- [ ] State: ConfirmDeleteState (D31.1); атомы D31.2 с logStep; Msg
      ToggleDeleteWords + плоский итог; handler: эффект
      DeleteGroupWithWords + event-лог.
- [ ] UI: динамический текст по галке, Checkbox, второй диалог;
      строки обеих локалей; явные цвета.

## Фаза 3. Верификация + ручники + док-синк

- [ ] Юниты groupstab/app, lint, assembleDebug; androidTest на девайсе.
- [ ] Мутационная проверка ≥3: (галка не сбрасывает stage → recheck
      требуется всегда при deleteWords; эффект recheck именно
      WithWords, не DeleteGroup; закрытие recheck по dismiss без
      эффекта).
- [ ] Ручники M16–M20 (дописать в stage5_manual_test.md — единый
      документ фичи):
      - M16 текст при N + смена подписи галкой (туда-обратно);
      - M17 НЕГАТИВНЫЙ контроль (ревью UX-4а): удаление с N≥1 и СНЯТОЙ
        галкой — слова живы в «Все» и другой группе, эффект именно
        DeleteGroup (не WithWords);
      - M18 отмена переспроса → ничего не удалено, повторный конфирм со
        снятой галкой;
      - M19 полный деструктив: слово в двух группах — после удаления A
        счётчик B упал, окно раскрытой B переэмитило, «Все» упал ровно
        на N (ревью UX-4б); кнопка «Удалить всё» была задизейблена
        первые ~500мс;
      - M20 анти-инвариант лога: DeleteGroupWithWords без
        openDeleteRecheck в логе = провал.
- [ ] rollout Э6 — актуализировать секцию (флоу юзера вместо старого
      «M слов с единственной группой»); architecture §2.5 —
      deleteGroupWithWords; память.

## Вне скоупа

- Undo удаления слов (нет и у одиночного удаления).
- Прогресс-диалог на огромных группах.
