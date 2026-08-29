package me.apomazkin.core_db_api.entity

/**
 * IS493 Э3: живая группа словаря (D13.3). Audit-поля не нужны потребителям
 * (deleteImpact Э4 считается DAO-SQL).
 */
data class GroupApiEntity(
    val id: Long,
    val dictionaryId: Long,
    val parentGroupId: Long?,
    val name: String,
)

/**
 * IS493 Э3 (D12.3): резерв имён групп — строки «Все» всех локалей проекта.
 * Собирает app из ресурсов при сборке core-db-компонента; конструкторная
 * инъекция в GroupApiImpl (НЕ per-call параметр: второй клиент API не
 * сможет забыть проводку — ревью A-B).
 */
data class ReservedGroupNames(
    val values: Set<String>,
)
