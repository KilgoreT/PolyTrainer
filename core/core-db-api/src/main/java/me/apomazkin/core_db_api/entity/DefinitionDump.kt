package me.apomazkin.core_db_api.entity

data class DefinitionDump(
    val id: Long? = null,
    val wordId: Long? = null,
    val definition: String? = null,
    val wordClass: String? = null,
    val options: Long = 0
) {
    companion object {
        const val RANGE = "Definition!A1:E"
    }
}
