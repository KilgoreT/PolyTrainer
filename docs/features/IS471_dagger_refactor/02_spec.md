# Spec: DI Graph — принципы и целевое состояние

## Принципы правильного DI графа

### 1. Single Responsibility Component
Один компонент = один scope/слой. Не мешать DB + UI + Network в одном компоненте.

### 2. No proxy components
Компонент без модулей, который только пробрасывает зависимости — бесполезен. Удалять.

### 3. Unidirectional dependency
Зависимости идут в одну сторону: core → domain → presentation → app. Обратных нет.

### 4. Flat over deep
Плоский граф лучше глубокого. `A → B → C → D` хуже чем `A → B, A → C, A → D`.

### 5. No circular workarounds
Если нужен отдельный компонент чтобы разорвать цикл — граф спроектирован неправильно. Инфраструктурные зависимости (logger) должны быть на самом нижнем уровне.

### 6. Component per feature
Каждая фича имеет свой subcomponent/component. App не знает про все фичи.

### 7. Constructor injection everywhere
Никакого service locator. Зависимости приходят через конструктор или @BindsInstance.

### 8. Scoped where needed
@Singleton только для реально разделяемых объектов (DB, Prefs, Logger). UseCase — unscoped.

### 9. Small modules
Модуль провайдит 1-3 вещи. God-module с 10+ includes — антипаттерн.

### 10. Interface segregation
Provider interface экспонирует только то что нужно потребителю. Монолитный provider с 9+ методами — нарушение.
