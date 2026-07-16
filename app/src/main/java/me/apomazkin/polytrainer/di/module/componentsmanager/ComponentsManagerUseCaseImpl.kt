package me.apomazkin.polytrainer.di.module.componentsmanager

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.apomazkin.components_manager.LogTags
import me.apomazkin.components_manager.deps.ComponentsManagerUseCase
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.CreateComponentOutcome
import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.core_db_api.entity.EditComponentOutcome
import me.apomazkin.core_db_api.entity.SoftDeleteComponentOutcome
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentUsage
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.EditOutcome
import me.apomazkin.lexeme.Scope
import me.apomazkin.lexeme.UserDefinedTypesSnapshot
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.logger.LogTags as FeatureLogTags
import me.apomazkin.polytrainer.mapper.toDomain
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.prefs.quizPickerPrefKey
import javax.inject.Inject

/**
 * Business-impl `ComponentsManagerUseCase` (IS481, M13).
 *
 * - `flowAllUserDefinedTypes` — map от `lexemeApi.flowAllUserDefinedTypesWithUsage()`.
 * - CRUD-методы — pre-validation (NameEmpty) → data-API call → mapping API outcome
 *   → domain outcome. Все wrap'ы в try/catch → `Failure(cause)` + `logger.e`.
 * - F125 retrofit: каждый catch site rethrow'ит `CancellationException` (структурная
 *   отмена не должна заворачиваться в Failure outcome).
 * - F127 retrofit: `resetQuizPickerPrefsBestEffort` вынесен за outer try/catch в
 *   `softDeleteComponent` — F103 best-effort invariant сохранён, прерывать caller'а
 *   per-pref fail не должен.
 * - `softDeleteComponent` — после успешного DB soft-delete сбрасываются
 *   per-dict quiz picker prefs (F049 option B). Best-effort: prefs failure
 *   логируется но не propagate в outcome (F103).
 */
class ComponentsManagerUseCaseImpl @Inject constructor(
    private val lexemeApi: CoreDbApi.LexemeApi,
    private val dictionaryApi: CoreDbApi.DictionaryApi,
    private val prefsProvider: PrefsProvider,
    private val logger: LexemeLogger,
) : ComponentsManagerUseCase {

    override fun flowAllUserDefinedTypes(): Flow<UserDefinedTypesSnapshot> =
        lexemeApi.flowAllUserDefinedTypesWithUsage().map { api ->
            UserDefinedTypesSnapshot(
                types = api.types.map { it.toDomain() },
                usage = ComponentUsage(
                    valueCountByType = api.valueCountByType.mapKeys { ComponentTypeId(it.key) },
                    dictionaryIdsByType = api.dictionaryIdsByType.mapKeys { ComponentTypeId(it.key) },
                    dictionaryNames = api.dictionaryNames,
                ),
            )
        }

    override suspend fun createUserDefinedComponent(
        name: String,
        template: ComponentTemplate,
        isMultiple: Boolean,
        scope: Scope,
    ): CreateOutcome {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return CreateOutcome.NameEmpty
        return try {
            when (val r = lexemeApi.createUserDefinedComponent(trimmed, template, isMultiple, scope)) {
                is CreateComponentOutcome.Success ->
                    CreateOutcome.Success(r.types.map { it.toDomain() })
                CreateComponentOutcome.SameScopeCollision -> CreateOutcome.SameScopeCollision
                CreateComponentOutcome.CrossScopeCollision -> CreateOutcome.CrossScopeCollision
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(tag = LogTags.ALL_COMPONENTS, message = "createUserDefinedComponent failed: ${e.message}")
            CreateOutcome.Failure(e)
        }
    }

    override suspend fun previewDeletionImpact(
        typeId: ComponentTypeId,
    ): DeletionImpact? = try {
        lexemeApi.previewDeletionImpact(typeId.id)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.e(tag = LogTags.ALL_COMPONENTS, message = "previewDeletionImpact failed: ${e.message}")
        null
    }

    override suspend fun softDeleteComponent(
        typeId: ComponentTypeId,
    ): DeleteOutcome {
        // F127 retrofit: prefs reset вынесен за outer try/catch — DB commit уже произошёл,
        // best-effort prefs cleanup внутри `resetQuizPickerPrefsBestEffort` сам глотает
        // per-pref ошибки.
        val r = try {
            lexemeApi.softDeleteComponentType(typeId.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(tag = LogTags.ALL_COMPONENTS, message = "softDeleteComponent failed: ${e.message}")
            return DeleteOutcome.Failure(e)
        }
        return when (r) {
            is SoftDeleteComponentOutcome.Success -> {
                resetQuizPickerPrefsBestEffort(r.impact)
                DeleteOutcome.Success(r.impact)
            }
            SoftDeleteComponentOutcome.BuiltInProtected -> DeleteOutcome.BuiltInProtected
            SoftDeleteComponentOutcome.Removed -> DeleteOutcome.Removed
        }
    }

    override suspend fun editComponent(
        typeId: ComponentTypeId,
        name: String,
        template: ComponentTemplate,
        isMultiple: Boolean,
    ): EditOutcome {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return EditOutcome.NameEmpty
        return try {
            when (val r = lexemeApi.editComponentType(typeId.id, trimmed, template, isMultiple)) {
                is EditComponentOutcome.Success -> EditOutcome.Success(r.type.toDomain())
                EditComponentOutcome.SameScopeCollision -> EditOutcome.SameScopeCollision
                EditComponentOutcome.CrossScopeCollision -> EditOutcome.CrossScopeCollision
                is EditComponentOutcome.CardinalityDowngradeBlocked ->
                    EditOutcome.CardinalityDowngradeBlocked(r.impactedLexemeIds)
                EditComponentOutcome.TemplateImmutable -> EditOutcome.TemplateImmutable
                EditComponentOutcome.BuiltInProtected -> EditOutcome.BuiltInProtected
                EditComponentOutcome.Removed -> EditOutcome.Removed
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(
                tag = LogTags.ALL_COMPONENTS,
                message = "editComponent failed: ${e.message}",
            )
            EditOutcome.Failure(e)
        }
    }

    override fun flowDictionaries(): Flow<List<DictionaryApiEntity>> =
        dictionaryApi.flowDictionaryList()

    /**
     * F049 option B + F103 best-effort: для каждого `dictId` из `impact.affectedPrefs`
     * сбросить `quiz_picker_dict_<id>` pref. Любое исключение per-pref логируется
     * (warning) и проглатывается — overall outcome остаётся `Success`.
     *
     * `CancellationException` пробрасывается (F125), чтобы scope cancellation работал.
     */
    private suspend fun resetQuizPickerPrefsBestEffort(impact: DeletionImpact) {
        val total = impact.affectedPrefs.size
        logger.d(
            tag = FeatureLogTags.COMPONENT_CONSTRUCTOR,
            message = "resetQuizPickerPrefs start: count=$total",
        )
        var successCount = 0
        impact.affectedPrefs.forEach { dictId ->
            try {
                prefsProvider.setStringByRawKey(quizPickerPrefKey(dictId), null)
                successCount++
                logger.d(
                    tag = FeatureLogTags.COMPONENT_CONSTRUCTOR,
                    message = "resetQuizPickerPrefs ok: dictId=$dictId",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.w(
                    tag = LogTags.ALL_COMPONENTS,
                    message = "resetQuizPickerPrefsBestEffort: dictId=$dictId failed: ${e.message}",
                )
                logger.w(
                    tag = FeatureLogTags.COMPONENT_CONSTRUCTOR,
                    message = "resetQuizPickerPrefs fail: dictId=$dictId cause=${e.message}",
                )
            }
        }
        logger.d(
            tag = FeatureLogTags.COMPONENT_CONSTRUCTOR,
            message = "resetQuizPickerPrefs done: ok=$successCount/$total",
        )
    }
}

