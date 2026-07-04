# Approved findings — infra_design_tree iter 1

## F031 [architect] minor — approved

**Что чинить:** В Узле 4 (Migration_012_to_013.kt) переключиться на `LogTags.COMPONENT_CONSTRUCTOR` (import из Узла 1) вместо хардкоженого `private const val TAG`. Удалить `circular concerns` обоснование — оно фиктивно (dep уже есть в `:core:core-db-impl/build.gradle.kts:40`).

**Verdict:** walkthrough §6.2 подтверждает что :core:core-db-impl уже имеет dep на :modules:core:logger — обоснование "circular concerns" фиктивно, дубликация defeats цель Узла 1.

## F032 [architect] minor — approved

**Что чинить:** После применения F031 (LogTags ref в Узле 4) — обновить DAG: `Узел 4 depends: [1]`. Либо в случае оставления duplication branch — явно зафиксировать `depends: []` corrected for that branch only, убрать alternative path из design (single source of truth для implementation).

**Verdict:** design явно оставляет два варианта реализации, в alternative-branch Узел 4 действительно зависит от Узла 1 — DAG depends:[] корректен только для одной ветки.
