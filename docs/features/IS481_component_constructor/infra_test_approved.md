# Approved findings — infra_test.md, iteration 2

4 finding (1 critical, 3 minor). Все про синхронизацию Msg-name между артефактами + переход на existing helper + KDoc numbering.

## F072 [critical] — Msg-name mismatch infra_test ↔ design_tree

`infra_test.md` § 2 использует `Msg.OnPerDictionaryComponentsClick(dictionaryId=...)`. `infra_design_tree.md` id 15 (Message.kt) объявляет `data class OpenPerDictionaryComponents(val dictionaryId: Long) : Msg`. id 17 (Reducer) обрабатывает `is Msg.OpenPerDictionaryComponents`. Test ссылается на несуществующий идентификатор → compile fail при infra_implement.

**Что исправить:** в `infra_test.md` § 2 заменить **все** упоминания `Msg.OnPerDictionaryComponentsClick` → `Msg.OpenPerDictionaryComponents`. Затронуты test-имена / sequence shape / описания (в § 2 — два кейса, оба).

## F074 [minor] — Msg-name divergence scope ↔ test

`02_scope.md:121` декларирует `Msg.OpenComponentConstructor`, `infra_test.md` использует `OnPerDictionaryComponentsClick`, design_tree использует `OpenPerDictionaryComponents`. Три разных имени.

**Что исправить:** выбрать одно каноническое имя `Msg.OpenPerDictionaryComponents` (consistent с effect-naming `OpenPerDictionaryComponents` и уже закрытым design_tree). Применить:
- `02_scope.md:121` — заменить `Msg.OpenComponentConstructor` → `Msg.OpenPerDictionaryComponents`.
- `infra_test.md` § 2 — заменить `Msg.OnPerDictionaryComponentsClick` → `Msg.OpenPerDictionaryComponents` (см. F072).
- `infra_design_tree.md` — без изменений (уже правильно).

## F073 [minor] — use existing `assertEffects` helper

В `:modules:core:mate.test/MateTestHelper.kt:18-23` есть `ReducerResult.assertEffects(expectedEffects: Set<EFFECTS>)` — convention. Спека сейчас инструктирует raw `assertEquals(setOf<Effect>(...), result.effects())`.

**Что исправить:** в § 1 и § 2 заменить inline `assertEquals(setOf<Effect>(...), result.effects())` на `result.assertEffects(setOf(...))` (single helper call, без `<Effect>` generic). Helper-note в § 1 расширить: «`assertEffects(Set<...>)` для positive — используем; `assertStateUnchanged` отсутствует — для state immutability inline `assertEquals(initialState, result.state())`».

## F075 [minor] — KDoc numbering для двух новых кейсов

Существующий KDoc файла `DictionaryAppBarReducerTest.kt:19-29` нумерует test-кейсы 1-6 сквозно через все секции. Спека не уточняет — новые кейсы получают (7, 8) или (1, 2).

**Что исправить:** в § 2 явно: «Новые кейсы продолжают сквозную нумерацию — 7 и 8 (по convention существующего KDoc 1-6).»
