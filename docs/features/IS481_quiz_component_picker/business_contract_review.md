# Review: business_contract (iter 2)

## Iter 1 finding — закрыт

**Внутренняя несогласованность в payload `SaveQuizPickerSelection`** — fix применён корректно.

- Reducer-таблица (§Msg, line 73) теперь: `setOf(DatasourceEffect.SaveQuizPickerSelection(message.ref))` — single-arg, соответствует declaration `data class SaveQuizPickerSelection(val ref: ComponentTypeRef)` (line 103).
- Правило «dictionaryId не в Msg/Effect, резолвится в `DatasourceEffectHandler` через `useCase.getCurrentDictionaryId()`» (lines 87, 107) — соблюдено.
- `DatasourceEffectHandler` (line 114) корректно использует это правило: `val dictId = useCase.getCurrentDictionaryId() ?: return Empty; useCase.setQuizPickerSelection(dictId, ref)`.
- Cross-check section (line 230) описывает persist path без dictionaryId в Msg/Effect — согласован.

Pattern decision (line 89) «SelectQuizComponent идёт по pattern `Msg.EarliestOn` — click не меняет state directly, write to prefs → flow → state» — согласуется с walkthrough §3 и `ChatReducer.kt:40-51`.

## Новые блокеры

Не обнаружено. Контракт внутренне согласован, соответствует walkthrough и scope.

## Verdict

verdict: approved

_model: claude-opus-4-7[1m]_

## log_messages

- Iter 1 finding (SaveQuizPickerSelection payload mismatch) закрыт: reducer-таблица теперь использует single-arg `SaveQuizPickerSelection(message.ref)`, dictionaryId резолвится в Handler.
- Cross-check разделов (State/Msg/Effect/Handler/UseCase) консистентен: persist/restore path замкнут через UseCase, PrefsProvider — implementation detail.
- Verdict: approved. Новых блокеров не найдено.
