package me.apomazkin.wordcard.mate

import androidx.compose.runtime.Stable
import me.apomazkin.group.AddMembershipOutcome
import me.apomazkin.group.RemoveMembershipOutcome
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.mate.Effect
import me.apomazkin.mate.ReducerLogging
import me.apomazkin.mate.ReducerResult
import me.apomazkin.wordcard.LogTags

/**
 * IS493 Э5 (D22): блок групп карточки слова — state, атомы, мапперы.
 * Конвенция StateAtoms/ReducerLogging (project-architecture §Reducer):
 * Msg → видимая цепочка атомов, каждый атом логируется logStep.
 */

/** Группа в UI карточки (лёгкий тип — GroupNode в state не тащим). */
@Stable
data class GroupUi(
    val id: Long,
    val name: String,
)

/**
 * Э5 (D22.2): state блока групп карточки.
 * Чипы — derived: [dictGroups] ∩ [wordGroupIds] (алфавит уже в
 * dictGroups — Collator handler'а).
 */
@Stable
data class GroupsBlockState(
    /** Живые группы словаря (список пикера), отсортированы Collator'ом. */
    val dictGroups: List<GroupUi> = emptyList(),
    /** Членства слова; меняет ТОЛЬКО подписка wordGroups (D22.3). */
    val wordGroupIds: Set<Long> = emptySet(),
    val isPickerOpen: Boolean = false,
    /** Keyed in-flight (В3): галочки этих групп задизейблены; участвует
     * в exit-guard карточки (hasInFlightCommits — flush-on-back А11). */
    val inFlight: Set<Long> = emptySet(),
) {
    /** Чипы шапки: группы слова по алфавиту словарного списка. */
    val chips: List<GroupUi>
        get() = dictGroups.filter { it.id in wordGroupIds }
}

private typealias CardResult = ReducerResult<WordCardState, Effect>

/**
 * Атомарные экстеншны блока групп (member extensions — логгер через
 * dispatch receiver [ReducerLogging]); [WordCardReducer] и тесты
 * НАСЛЕДУЮТ. Старые ветки карточки конвенцией не покрыты — только Э5.
 */
abstract class GroupBlockAtoms(logger: LexemeLogger) : ReducerLogging(logger) {

    override val logTag: String = LogTags.WORDCARD

    /**
     * Открыть пикер групп (иконка шапки либо тап по чипу — В4).
     * Меняет только [GroupsBlockState.isPickerOpen] → true. Эффектов нет.
     */
    fun WordCardState.openGroupPicker(): CardResult {
        logStep("openGroupPicker")
        return copy(groupsBlock = groupsBlock.copy(isPickerOpen = true)) to emptySet()
    }

    /**
     * Закрыть пикер (dismiss). [GroupsBlockState.inFlight] НЕ трогается
     * (ревью Mate-6: летящая запись долетает, чипы поправит подписка).
     * Эффектов нет.
     */
    fun WordCardState.closeGroupPicker(): CardResult {
        logStep("closeGroupPicker")
        return copy(groupsBlock = groupsBlock.copy(isPickerOpen = false)) to emptySet()
    }

    /**
     * Принять эмиссию подписки групп СЛОВАРЯ (пикер живой).
     * @param groups группы словаря, отсортированы Collator'ом handler'а.
     * Меняет только [GroupsBlockState.dictGroups]. Эффектов нет.
     */
    fun WordCardState.applyDictGroups(groups: List<GroupUi>): CardResult {
        logStep("applyDictGroups", "ids" to groups.map { it.id })
        return copy(groupsBlock = groupsBlock.copy(dictGroups = groups)) to emptySet()
    }

    /**
     * Принять эмиссию подписки членств СЛОВА — единственный мутатор
     * [GroupsBlockState.wordGroupIds] (D22.3: оптимистичных правок нет,
     * галочка/чип — всегда факт БД).
     * @param ids id живых групп слова.
     * Эффектов нет.
     */
    fun WordCardState.applyWordGroups(ids: Set<Long>): CardResult {
        logStep("applyWordGroups", "ids" to ids)
        return copy(groupsBlock = groupsBlock.copy(wordGroupIds = ids)) to emptySet()
    }

    /**
     * Пометить membership-запись группы летящей: галочка дизейблится
     * (keyed in-flight, В3); сам эффект мутации добавляет ветка
     * (`withEffect`) — направление знает только она.
     * @param groupId группа тапнутой галочки.
     */
    fun WordCardState.markMembershipInFlight(groupId: Long): CardResult {
        logStep("markMembershipInFlight", "groupId" to groupId)
        return copy(
            groupsBlock = groupsBlock.copy(inFlight = groupsBlock.inFlight + groupId),
        ) to emptySet()
    }

    /**
     * Снять in-flight группы ([Msg.MembershipDone]/[Msg.MembershipFailed]).
     * БЕЗ guard'а на пикер (ревью Mate-6): Done после dismiss обязан
     * чистить — иначе галочка вечно задизейблена. Эффектов нет.
     */
    fun WordCardState.clearMembershipInFlight(groupId: Long): CardResult {
        logStep("clearMembershipInFlight", "groupId" to groupId)
        return copy(
            groupsBlock = groupsBlock.copy(inFlight = groupsBlock.inFlight - groupId),
        ) to emptySet()
    }
}

// === Мапперы outcome → плоский Msg (handler зовёт ДО отправки; прецедент
// groupstab/MutationMappers) ===

/** Таблица исходов add: все — Done (NotFound-исходы тихие, правит подписка). */
fun AddMembershipOutcome.toMembershipMsg(groupId: Long): Msg = when (this) {
    is AddMembershipOutcome.Added -> Msg.MembershipDone(groupId)
    is AddMembershipOutcome.AlreadyIn -> Msg.MembershipDone(groupId)
    is AddMembershipOutcome.GroupNotFound -> Msg.MembershipDone(groupId)
    is AddMembershipOutcome.WordNotFound -> Msg.MembershipDone(groupId)
}

/** Таблица исходов remove: оба — Done (NotFound идемпотентен). */
fun RemoveMembershipOutcome.toMembershipMsg(groupId: Long): Msg = when (this) {
    is RemoveMembershipOutcome.Removed -> Msg.MembershipDone(groupId)
    is RemoveMembershipOutcome.NotFound -> Msg.MembershipDone(groupId)
}
