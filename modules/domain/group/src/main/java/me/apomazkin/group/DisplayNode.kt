package me.apomazkin.group

/**
 * Строка id-среза membership словаря (D7.3 — тип границы модулей;
 * Api-entity data-слоя в screen-модули не протекает).
 * `groupId == null` — слово вне живых групп.
 */
data class MembershipEntry(
    val wordId: Long,
    val groupId: Long?,
)

/**
 * IS493 Э3 (D14.1): живая группа на входе сборки дерева (тип границы,
 * как [MembershipEntry]). `parentGroupId == null` — корневая (в Э3 — все).
 */
data class GroupNode(
    val id: Long,
    val parentGroupId: Long?,
    val name: String,
)

/**
 * ADT дерева отображения вкладки «Группы» (architecture §2.2).
 */
sealed interface DisplayNode {

    /**
     * Реальная группа. В Э2 не строится (групп нет) — форма зафиксирована
     * под Э3+: прямые узлы «папка XOR лист» (А18), `subtreeWordCount` —
     * distinct слов поддерева (В3).
     */
    data class Group(
        val id: Long,
        val name: String,
        val children: List<Group>,
        val directWords: List<Long>,
        val subtreeWordCount: Int,
    ) : DisplayNode

    /**
     * «Все» — единственный виртуальный маркер (А7): все слова словаря,
     * включая разложенные по группам. Строки в БД нет; имя подставляет UI.
     */
    data class AllWords(
        val words: List<Long>,
        val count: Int,
    ) : DisplayNode
}

/**
 * Корень вкладки «Группы»: «Все» первым + корневые группы.
 */
data class DisplayTree(
    val allWords: DisplayNode.AllWords,
    val groups: List<DisplayNode.Group>,
)
