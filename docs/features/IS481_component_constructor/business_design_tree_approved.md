# Approved findings — business_design_tree.md, iteration 6

1 critical (F098).

## F098 [critical] — несуществующий `BaseNavigationEffect.Back`

Reducer #37 и зеркально #47 используют `BaseNavigationEffect.Back` с import `me.apomazkin.mate.base.BaseNavigationEffect`. Этот symbol не существует. В `:modules:core:mate` есть:

- Package: `me.apomazkin.mate` (НЕ `me.apomazkin.mate.base`).
- Тип: `NavigationEffect` (sealed interface) с variants включая `Back`.
- Canonical usage: `WordCardReducer.kt:248` → `NavigationEffect.Back` с import `me.apomazkin.mate.NavigationEffect`.

**Что исправить в #37 и #47:**

- `BaseNavigationEffect.Back` → `NavigationEffect.Back`.
- import `me.apomazkin.mate.base.BaseNavigationEffect` → `me.apomazkin.mate.NavigationEffect`.
- Удалить заметку «Если на проекте используется другая convention для Back — заменить» (дезориентирующая, теперь конкретно).
