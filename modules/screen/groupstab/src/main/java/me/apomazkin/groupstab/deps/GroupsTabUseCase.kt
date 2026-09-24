package me.apomazkin.groupstab.deps

import kotlinx.coroutines.flow.Flow
import me.apomazkin.group.CreateGroupOutcome
import me.apomazkin.group.DeleteGroupOutcome
import me.apomazkin.group.DeleteGroupWithWordsOutcome
import me.apomazkin.group.GroupNode
import me.apomazkin.group.MembershipEntry
import me.apomazkin.group.RenameGroupOutcome
import me.apomazkin.wordrow.entity.TermUiItem

/**
 * IS493 Э2/Э3: данные вкладки «Группы». Slice/groups — типы domain/group
 * (D7.3: Api-entity data-слоя в screen-модуль не протекает; маппинг — в
 * app-impl). Мутации — тонкая делегация в GroupApi (валидации в транзакции
 * data-слоя, §2.4).
 */
interface GroupsTabUseCase {

    /**
     * Живой id текущего словаря (null — «словарей нет»). Семантика —
     * как у words/host: один prefs-источник + fallback на первый
     * словарь, рассинхрона между вкладками нет.
     */
    fun flowCurrentDictId(): Flow<Long?>

    /**
     * Живой id-срез membership словаря (А13: наблюдает words + word_groups +
     * dictionary_groups; порядок id DESC — порядок «Слов»).
     */
    fun membershipSlice(dictionaryId: Long): Flow<List<MembershipEntry>>

    /** Живые группы словаря (Э3: все корневые). */
    fun groupTree(dictionaryId: Long): Flow<List<GroupNode>>

    /**
     * Живое окно контента «Все»: первые [limit] слов словаря, id DESC —
     * предикатный live-запрос (вставки/правки/удаления переэмичиваются сами).
     */
    fun flowWordsWindow(dictionaryId: Long, limit: Int): Flow<List<TermUiItem>>

    /**
     * Э5 (D21): живое окно контента ГРУППЫ — membership ∩ words,
     * id DESC LIMIT; add/remove membership переэмичиваются сами.
     */
    fun flowGroupWordsWindow(groupId: Long, limit: Int): Flow<List<TermUiItem>>

    suspend fun createGroup(dictionaryId: Long, name: String): CreateGroupOutcome

    suspend fun renameGroup(groupId: Long, name: String): RenameGroupOutcome

    suspend fun deleteGroup(groupId: Long): DeleteGroupOutcome

    /** Э6: ДЕСТРУКТИВ — группа вместе со всеми её словами (D30.2). */
    suspend fun deleteGroupWithWords(groupId: Long): DeleteGroupWithWordsOutcome
}
