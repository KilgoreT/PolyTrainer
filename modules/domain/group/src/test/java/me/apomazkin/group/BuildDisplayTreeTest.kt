package me.apomazkin.group

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * IS493 / Э2: контракт `buildDisplayTree` (architecture §2.2, D7).
 *
 * «Все» = ВСЕ слова словаря (А7), включая разложенные по группам;
 * порядок входа сохраняется (глобальный порядок даёт slice, id DESC);
 * wordId дедуплицируются (мультичленство Э3 — строка на membership, D7.3).
 */
class BuildDisplayTreeTest {

    @Test
    fun `empty slice - empty AllWords and no groups`() {
        val tree = buildDisplayTree(emptyList())

        assertEquals(DisplayNode.AllWords(words = emptyList(), count = 0), tree.allWords)
        assertEquals(emptyList<DisplayNode.Group>(), tree.groups)
    }

    @Test
    fun `words without groups - all in AllWords in input order`() {
        val slice = listOf(
            MembershipEntry(wordId = 30, groupId = null),
            MembershipEntry(wordId = 20, groupId = null),
            MembershipEntry(wordId = 10, groupId = null),
        )

        val tree = buildDisplayTree(slice)

        assertEquals(listOf(30L, 20L, 10L), tree.allWords.words)
        assertEquals(3, tree.allWords.count)
    }

    @Test
    fun `words with membership - still included in AllWords`() {
        val slice = listOf(
            MembershipEntry(wordId = 30, groupId = 5),
            MembershipEntry(wordId = 20, groupId = null),
        )

        val tree = buildDisplayTree(slice)

        assertEquals(listOf(30L, 20L), tree.allWords.words)
        assertEquals(2, tree.allWords.count)
    }

    @Test
    fun `duplicate wordIds from multi-membership - deduplicated keeping first occurrence`() {
        val slice = listOf(
            MembershipEntry(wordId = 30, groupId = 5),
            MembershipEntry(wordId = 30, groupId = 7),
            MembershipEntry(wordId = 20, groupId = null),
            MembershipEntry(wordId = 20, groupId = 5),
        )

        val tree = buildDisplayTree(slice)

        assertEquals(listOf(30L, 20L), tree.allWords.words)
        assertEquals(2, tree.allWords.count)
    }

    @Test
    fun `groups are empty in stage 2 shape`() {
        val slice = listOf(
            MembershipEntry(wordId = 30, groupId = 5),
        )

        val tree = buildDisplayTree(slice)

        assertEquals(emptyList<DisplayNode.Group>(), tree.groups)
    }

    // === Э3 (D14.1): группы в дереве ===

    @Test
    fun `root groups sorted by comparator - cyrillic collation`() {
        // naturalOrder по code point ставит «Ёлка» ПОСЛЕ «Яма» (Ё = U+0401? нет:
        // строчная её позиция — суть в том, что бинарный порядок кириллицы
        // отличается от языкового). Collator обязан дать Аист, Ёлка, Яма.
        val collator = java.text.Collator.getInstance(java.util.Locale.forLanguageTag("ru-RU"))
        val comparator = Comparator<String> { a, b -> collator.compare(a, b) }
        val groups = listOf(
            GroupNode(id = 1, parentGroupId = null, name = "Яма"),
            GroupNode(id = 2, parentGroupId = null, name = "Ёлка"),
            GroupNode(id = 3, parentGroupId = null, name = "Аист"),
        )

        val tree = buildDisplayTree(emptyList(), groups, comparator)

        assertEquals(listOf("Аист", "Ёлка", "Яма"), tree.groups.map { it.name })
    }

    @Test
    fun `group counters - distinct words of group, zero for empty`() {
        val groups = listOf(
            GroupNode(id = 5, parentGroupId = null, name = "Дом"),
            GroupNode(id = 6, parentGroupId = null, name = "Быт"),
        )
        val slice = listOf(
            MembershipEntry(wordId = 30, groupId = 5),
            MembershipEntry(wordId = 30, groupId = 5),
            MembershipEntry(wordId = 20, groupId = 5),
            MembershipEntry(wordId = 10, groupId = null),
        )

        val tree = buildDisplayTree(slice, groups, naturalOrder())

        val dom = tree.groups.first { it.id == 5L }
        val byt = tree.groups.first { it.id == 6L }
        assertEquals(listOf(30L, 20L), dom.directWords)
        assertEquals(2, dom.subtreeWordCount)
        assertEquals(0, byt.subtreeWordCount)
        // «Все» — все слова словаря, включая разложенные (А7).
        assertEquals(3, tree.allWords.count)
    }

    @Test
    fun `membership with unknown groupId dropped - word stays in AllWords`() {
        // Контракт combine-skew (A-D): slice опережает groupTree.
        val groups = listOf(
            GroupNode(id = 5, parentGroupId = null, name = "Дом"),
        )
        val slice = listOf(
            MembershipEntry(wordId = 30, groupId = 99),
            MembershipEntry(wordId = 20, groupId = 5),
        )

        val tree = buildDisplayTree(slice, groups, naturalOrder())

        assertEquals(listOf(30L, 20L), tree.allWords.words)
        assertEquals(listOf(20L), tree.groups.first { it.id == 5L }.directWords)
    }

    @Test
    fun `node with broken parent skipped with subtree - fail-soft`() {
        val groups = listOf(
            GroupNode(id = 5, parentGroupId = null, name = "Дом"),
            GroupNode(id = 6, parentGroupId = 99, name = "Сирота"),
            GroupNode(id = 7, parentGroupId = 6, name = "Внук сироты"),
        )

        val tree = buildDisplayTree(emptyList(), groups, naturalOrder())

        assertEquals(listOf(5L), tree.groups.map { it.id })
    }
}
