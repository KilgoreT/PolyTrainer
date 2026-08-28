package me.apomazkin.core_db_api.entity

/**
 * IS493: строка id-среза membership словаря (architecture §2.5).
 * `groupId == null` — слово вне живых групп.
 */
data class MembershipSliceApiEntity(
    val wordId: Long,
    val groupId: Long?,
)
