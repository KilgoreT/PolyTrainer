package me.apomazkin.polytrainer.di.module.groupstab

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.group.CreateGroupOutcome
import me.apomazkin.group.DeleteGroupOutcome
import me.apomazkin.group.GroupNode
import me.apomazkin.group.MembershipEntry
import me.apomazkin.group.RenameGroupOutcome
import me.apomazkin.groupstab.deps.GroupsTabUseCase
import me.apomazkin.polytrainer.mapper.toDomain
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.wordrow.entity.TermUiItem
import me.apomazkin.wordrow.entity.toUiItem
import javax.inject.Inject

/**
 * IS493 Э2/Э3: тонкая делегация в CoreDbApi (D11). Маппинги:
 * slice/groups Api → domain/group (Api-entity в screen-модуль не протекает,
 * D7.3); контент TermApiEntity → TermUiItem — как WordsTabUseCaseImpl.
 * Валидации мутаций — в транзакции data-слоя (§2.4), здесь их НЕТ.
 */
class GroupsTabUseCaseImpl @Inject constructor(
    private val groupApi: CoreDbApi.GroupApi,
    private val termApi: CoreDbApi.TermApi,
    private val dictionaryApi: CoreDbApi.DictionaryApi,
    private val prefsProvider: PrefsProvider,
) : GroupsTabUseCase {

    override fun flowCurrentDictId(): Flow<Long?> = prefsProvider
        .getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG)
        .map { id: Long? ->
            // null — валидное «словарей нет»; fallback на первый словарь —
            // как у words/host (id в prefs может отсутствовать/протухнуть).
            (id?.let { dictionaryApi.getDictionaryById(it) }
                ?: dictionaryApi.getDictionaryList().firstOrNull())
                ?.id
        }

    override fun membershipSlice(dictionaryId: Long): Flow<List<MembershipEntry>> =
        groupApi.membershipSlice(dictionaryId).map { slice ->
            slice.map { MembershipEntry(wordId = it.wordId, groupId = it.groupId) }
        }

    override fun groupTree(dictionaryId: Long): Flow<List<GroupNode>> =
        groupApi.groupTree(dictionaryId).map { groups ->
            groups.map {
                GroupNode(id = it.id, parentGroupId = it.parentGroupId, name = it.name)
            }
        }

    override suspend fun createGroup(dictionaryId: Long, name: String): CreateGroupOutcome =
        groupApi.createGroup(dictionaryId = dictionaryId, name = name)

    override suspend fun renameGroup(groupId: Long, name: String): RenameGroupOutcome =
        groupApi.renameGroup(groupId = groupId, name = name)

    override suspend fun deleteGroup(groupId: Long): DeleteGroupOutcome =
        groupApi.deleteGroup(groupId = groupId)

    override suspend fun deleteGroupWithWords(
        groupId: Long,
    ): me.apomazkin.group.DeleteGroupWithWordsOutcome =
        groupApi.deleteGroupWithWords(groupId = groupId)

    override fun flowWordsWindow(dictionaryId: Long, limit: Int): Flow<List<TermUiItem>> =
        termApi.flowTermsWindow(dictionaryId = dictionaryId, limit = limit)
            .map { terms -> terms.map { it.toUi() } }

    override fun flowGroupWordsWindow(groupId: Long, limit: Int): Flow<List<TermUiItem>> =
        groupApi.flowGroupWordsWindow(groupId = groupId, limit = limit)
            .map { terms -> terms.map { it.toUi() } }
}

/** Контент окна: TermApiEntity → TermUiItem (как WordsTabUseCaseImpl). */
private fun me.apomazkin.core_db_api.entity.TermApiEntity.toUi(): TermUiItem =
    TermUiItem(
        id = word.id,
        wordValue = word.value,
        dictionaryId = word.dictionaryId,
        addDate = word.addDate,
        changeDate = word.changeDate,
        lexemeList = lexemes
            .map { it.toDomain() }
            .map { it.toUiItem() },
    )
