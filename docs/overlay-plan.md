# Overlay — план реализации

## Решения

1. **Конфиг:** Runner ищет `forgeflow.yml` в корне проекта, мерджит с `forgeflow/config.yml` (deep merge, project переопределяет base, массивы заменяются целиком). Если проектного конфига нет — base работает как есть
2. **Overlay path:** дефолт в base config `overlay: null` (opt-in). Проектный `forgeflow.yml` задаёт путь для включения overlay
3. **Резолв:** единая функция `resolve_path(type, name, plan)` — сначала overlay (из plan.context.overlay), потом base
4. **select_flow:** мерджит списки flow из overlay и base. При совпадении имён — overlay wins. Принимает config (plan ещё нет)
5. **Несуществующий overlay:** warning, продолжить без overlay
6. **Overlay step:** может менять промпт и criteria, контракт (input/output) определяется flow yml
7. **Base flows не ссылаются на overlay.** Overlay flows могут ссылаться на base
8. **Overlay содержит только:** steps/, agents/custom/, flows/. Всё остальное (spec/, agents/embedded/, config, docs) — только в base
9. **Config → plan.context:** planning() копирует нужные поля из config в plan.context (overlay, workspace). После planning — config не нужен, всё в plan
10. **Resume:** overlay берётся из plan.context (зафиксирован на момент старта), config НЕ перечитывается для overlay. Гарантирует консистентность: done и pending шаги резолвятся из одного overlay

## Задачи

### 1. Config
- [x] 1.1. load_config(): deep merge base + project
- [x] 1.2. load_config: warning при несуществующем overlay dir

### 2. main()
- [x] 2.1. config = load_config() в начале main()
- [x] 2.2. config в select_flow и planning; run и resume — только plan

### 3. planning()
- [x] 3.1. config.overlay → plan.context.overlay
- [x] 3.2. config.workspace → plan.context
- [x] 3.3. Убрать прямое чтение config — получать как параметр

### 4. resolve_path
- [x] 4.1. resolve_path принимает plan, overlay из plan.context.overlay
- [x] 4.2. Расширение по типу — steps/agents → .md, flows → .yml
- [x] 4.3. Guard для agents/embedded — overlay игнорируется
- [x] 4.4. Assert: если overlay задан и файл существует в overlay, но результат base → error

### 5. Замена хардкодов
- [x] 5.1. execute_step → resolve_path
- [x] 5.2. resume → resolve_path
- [x] 5.3. resolve_input → resolve_path
- [x] 5.4. resolve_agent → resolve_path + принимает plan
- [x] 5.5. resolve_steps → resolve_path (вложенные flow)
- [x] 5.6. select_flow → принимает config

### 6. Conductor
- [x] 6.1. resolve_path в делегировании шагов

### 7. Проект
- [x] 7.1. Создать `docs/forgeflow-overlay/` (steps/, agents/custom/, flows/)
- [x] 7.2. Создать `forgeflow.yml` в корне PolyTrainer
- [x] 7.3. CLAUDE.md — не содержал overlay path, ок

## Верификация

- [x] Grep по runner spec на хардкоженые пути — ноль вхождений вне resolve_path и load_config
- [ ] Проверить: overlay dir не существует → warning, flow работает без overlay
- [ ] Проверить: overlay dir существует, overlay step есть → overlay step используется
- [ ] Проверить: overlay содержит agents/embedded/ → игнорируется (guard)
- [ ] Проверить: select_flow показывает flow из overlay и base
- [ ] Проверить: resume после dropout → overlay из plan.context, не из config
- [ ] Тест-кейс: положить step X в overlay, запустить flow, убедиться что в логе "resolved from overlay"
