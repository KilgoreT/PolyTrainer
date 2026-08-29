package me.apomazkin.group

/**
 * Чистая сборка DisplayTree из id-среза membership и живых групп
 * (architecture §2.2, D7 v1 + D14 Э3).
 *
 * - «Все» = distinct wordId среза с сохранением порядка первого вхождения
 *   (глобальный порядок обеспечивает slice — id DESC, D6.3); дедуп
 *   обязателен: строка на membership, слово в двух группах даёт две
 *   строки (D7.3).
 * - Группы (Э3): корневые узлы, сиблинги отсортированы [comparator]
 *   (locale-aware Collator инжектится UI-слоем, А8); `directWords` —
 *   distinct wordId по groupId; `subtreeWordCount` — distinct по
 *   поддереву (Э3: узлы без детей).
 * - Контракт combine-skew (D14.1, ревью A-D): membership с groupId,
 *   отсутствующим во входе [groups], ОТБРАСЫВАЕТСЯ — слово остаётся в
 *   «Все»; transient до следующей эмиссии.
 * - Fail-soft (А2): узел с parentGroupId, отсутствующим во входе, —
 *   пропуск с поддеревом.
 * - Default'ы сохранены намеренно (зелёная коммит-нарезка, T-6); риск
 *   «забытый Collator» закрыт тестом кириллицы + мутационной проверкой.
 */
fun buildDisplayTree(
    slice: List<MembershipEntry>,
    groups: List<GroupNode> = emptyList(),
    comparator: Comparator<String> = naturalOrder(),
): DisplayTree {
    val distinctWordIds = LinkedHashSet<Long>(slice.size)
    slice.forEach { distinctWordIds.add(it.wordId) }
    val words = distinctWordIds.toList()
    val allWords = DisplayNode.AllWords(words = words, count = words.size)
    if (groups.isEmpty()) {
        return DisplayTree(allWords = allWords, groups = emptyList())
    }

    val knownIds = groups.mapTo(HashSet()) { it.id }
    // Combine-skew drop (D14.1): membership с неизвестным groupId
    // отбрасывается — слово остаётся в «Все».
    val directByGroup = LinkedHashMap<Long, LinkedHashSet<Long>>()
    slice.forEach { entry ->
        val groupId = entry.groupId ?: return@forEach
        if (groupId !in knownIds) return@forEach
        directByGroup.getOrPut(groupId) { LinkedHashSet() }.add(entry.wordId)
    }

    val childrenByParent = groups.groupBy { it.parentGroupId }
    val byName = Comparator<GroupNode> { a, b -> comparator.compare(a.name, b.name) }

    // Дерево строится ТОЛЬКО от корней (parent == null): узлы с битым
    // parent (и недостижимые циклы) в обход не попадают — fail-soft
    // «пропуск с поддеревом» (А2) по построению.
    fun buildGroup(node: GroupNode): Pair<DisplayNode.Group, Set<Long>> {
        val childPairs = (childrenByParent[node.id] ?: emptyList())
            .sortedWith(byName)
            .map { buildGroup(it) }
        val direct = directByGroup[node.id]?.toList() ?: emptyList()
        val subtreeWords = LinkedHashSet(direct)
        childPairs.forEach { subtreeWords.addAll(it.second) }
        val group = DisplayNode.Group(
            id = node.id,
            name = node.name,
            children = childPairs.map { it.first },
            directWords = direct,
            subtreeWordCount = subtreeWords.size,
        )
        return group to subtreeWords
    }

    val roots = (childrenByParent[null] ?: emptyList())
        .sortedWith(byName)
        .map { buildGroup(it).first }
    return DisplayTree(allWords = allWords, groups = roots)
}
