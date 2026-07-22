package me.apomazkin.core_db_impl

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.ComponentTypeApiEntity
import me.apomazkin.core_db_api.entity.CreateComponentOutcome
import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.core_db_api.entity.DictionaryTypesSnapshot
import me.apomazkin.core_db_api.entity.EditComponentOutcome
import me.apomazkin.core_db_api.entity.ComponentOptionApiEntity
import me.apomazkin.core_db_api.entity.LexemeApiEntity
import me.apomazkin.core_db_api.entity.QuizConfigApiEntity
import me.apomazkin.core_db_api.entity.OptionCrudOutcome
import me.apomazkin.core_db_api.entity.RenameComponentOutcome
import me.apomazkin.core_db_api.entity.SetEnabledComponentOutcome
import me.apomazkin.core_db_api.entity.SoftDeleteComponentOutcome
import me.apomazkin.core_db_api.entity.TermApiEntity
import me.apomazkin.core_db_api.entity.UserDefinedTypesUsageSnapshot
import me.apomazkin.core_db_api.entity.WriteQuizComplexEntity
import me.apomazkin.core_db_api.entity.WriteQuizUpsertApiEntity
import me.apomazkin.core_db_impl.entity.ComponentOptionDb
import me.apomazkin.core_db_impl.entity.ComponentTypeDb
import me.apomazkin.core_db_impl.entity.ComponentValueDb
import me.apomazkin.core_db_impl.entity.DictionaryDb
import me.apomazkin.core_db_impl.entity.LexemeDb
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.core_db_impl.entity.WriteQuizDb
import me.apomazkin.core_db_impl.entity.toApiEntity
import me.apomazkin.core_db_impl.entity.toDb
import me.apomazkin.core_db_impl.mapper.toComponentTypeRefList
import me.apomazkin.core_db_impl.mapper.toJson
import me.apomazkin.core_db_impl.room.Database
import me.apomazkin.core_db_impl.room.WordDao
import me.apomazkin.core_db_impl.room.dao.ComponentOptionDao
import me.apomazkin.core_db_impl.room.dao.ComponentTypeDao
import me.apomazkin.core_db_impl.room.dao.ComponentValueDao
import me.apomazkin.core_db_impl.room.dao.QuizConfigDao
import me.apomazkin.lexeme.AcyclicityCheck
import me.apomazkin.lexeme.AffectedQuizConfig
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.CascadeEvent
import me.apomazkin.lexeme.CascadeValue
import me.apomazkin.lexeme.ChoiceValues
import me.apomazkin.lexeme.ComponentGraph
import me.apomazkin.lexeme.ComponentOption
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.CoreLossCheck
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.DependencyTarget
import me.apomazkin.lexeme.checkAcyclic
import me.apomazkin.lexeme.checkCoreLoss
import me.apomazkin.lexeme.isDegraded
import me.apomazkin.lexeme.planCascade
import me.apomazkin.lexeme.PartOfSpeechOption
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.Scope
import me.apomazkin.lexeme.TemplateValues
import me.apomazkin.lexeme.TextValues
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.logger.LogTags as FeatureLogTags
import java.util.Date
import javax.inject.Inject


class CoreDbApiImpl @Inject constructor(
    private val wordDao: WordDao,
    private val logger: LexemeLogger,
) : CoreDbApi {

    class DbInstanceImpl @Inject constructor(
        private val db: Database,
    ) : CoreDbApi.DbInstance {

        override suspend fun instance(): String {
            return System.identityHashCode(db).toString()
        }

        override suspend fun closeDatabase() {
            withContext(Dispatchers.IO) {
                db.close()
            }
        }

        override suspend fun openDatabase() {
            withContext(Dispatchers.IO) {
                db.openHelper.writableDatabase.isOpen
            }
        }

        override suspend fun isDatabaseOpen(): Boolean {
            return db.isOpen
        }

        override fun getDbInfo(): CoreDbApi.DbInfo {
            val dbName: String? = db.openHelper.databaseName
            val dbVersion: Int = db.openHelper.readableDatabase.version
            val dbPath: String? = db.openHelper.readableDatabase.path
            val isOpen: Boolean = db.isOpen
            return CoreDbApi.DbInfo(
                mem = System.identityHashCode(db).toString(),
                name = dbName ?: "",
                version = dbVersion,
                path = dbPath ?: "",
                isOpen = isOpen,
            )
        }
    }

    class DictionaryApiImpl @Inject constructor(
        private val database: Database,
        private val wordDao: WordDao,
        private val componentTypeDao: ComponentTypeDao,
        private val componentOptionDao: ComponentOptionDao,
    ) : CoreDbApi.DictionaryApi {

        /**
         * IS486: словарь рождается вместе со своими builtin-компонентами (spec §9.5) —
         * атомарно, в одной транзакции: перевод (TEXT, ядро) + часть речи (CHOICE,
         * не-ядро) с опциями-ключами из домена ([PartOfSpeechOption]).
         * Идемпотентность — проверка «(ключ, словарь)» перед вставкой
         * (UNIQUE(system_key) дропнут, spec §11).
         */
        override suspend fun addDictionary(name: String, numericCode: Int?): Long {
            val now = Date(System.currentTimeMillis())
            return database.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    val dictId = wordDao.addDictionary(
                        DictionaryDb(
                            numericCode = numericCode,
                            name = name,
                            addDate = now,
                        )
                    )
                    seedBuiltInsForDictionary(dictId, now)
                    dictId
                }
            }
        }

        private suspend fun seedBuiltInsForDictionary(dictId: Long, now: Date) {
            if (componentTypeDao.getBySystemKeyForDictionary(BuiltInComponent.TRANSLATION.key, dictId) == null) {
                componentTypeDao.insert(
                    ComponentTypeDb(
                        systemKey = BuiltInComponent.TRANSLATION.key,
                        dictionaryId = dictId,
                        name = null,
                        templateKey = ComponentTemplate.TEXT.key,
                        position = 0,
                        isMultiple = false,
                        core = true,
                        enabled = true,
                        createdAt = now,
                        updatedAt = now,
                    )
                )
            }
            if (componentTypeDao.getBySystemKeyForDictionary(BuiltInComponent.PART_OF_SPEECH.key, dictId) == null) {
                val posTypeId = componentTypeDao.insert(
                    ComponentTypeDb(
                        systemKey = BuiltInComponent.PART_OF_SPEECH.key,
                        dictionaryId = dictId,
                        name = null,
                        templateKey = ComponentTemplate.CHOICE.key,
                        position = 1,
                        isMultiple = false,
                        core = false,
                        enabled = true,
                        createdAt = now,
                        updatedAt = now,
                    )
                )
                PartOfSpeechOption.entries.forEach { option ->
                    componentOptionDao.insert(
                        ComponentOptionDb(
                            componentTypeId = posTypeId,
                            systemKey = option.key,
                            label = null,
                            position = option.ordinal,
                            createdAt = now,
                            updatedAt = now,
                        )
                    )
                }
            }
        }

        override suspend fun getDictionary(numericCode: Int): DictionaryApiEntity? {
            return wordDao.getDictionaryByNumeric(numericCode)
                ?.let { return it.toApiEntity() }
        }

        override suspend fun getDictionaryById(id: Long): DictionaryApiEntity? {
            return wordDao.getDictionaryById(id)?.toApiEntity()
        }

        override suspend fun getDictionaryList(): List<DictionaryApiEntity> {
            return wordDao.getDictionaries().map { it.toApiEntity() }
        }

        override suspend fun updateDictionary(id: Long, name: String, numericCode: Int?) {
            wordDao.updateDictionary(id, name, numericCode, System.currentTimeMillis())
        }

        override suspend fun deleteDictionary(id: Long) {
            wordDao.deleteDictionary(id)
        }

        override fun flowDictionaryList(): Flow<List<DictionaryApiEntity>> {
            return wordDao.flowDictionaries().map { it.toApiEntity() }
        }

    }

    class TermApiImpl @Inject constructor(
        private val wordDao: WordDao,
        private val logger: LexemeLogger,
    ) : CoreDbApi.TermApi {

        override suspend fun getTermList(dictionaryId: Int): List<TermApiEntity> {
            return wordDao.getTermList(dictionaryId).map { it.toApiEntity(logger) }
        }

        override suspend fun searchTerms(
            pattern: String,
            dictionaryId: Long,
        ): List<TermApiEntity> {
            return wordDao.searchTerms(pattern, dictionaryId).map { it.toApiEntity(logger) }
        }

        override fun searchTermsPaging(
            pattern: String,
            dictionaryId: Int,
        ): Flow<PagingData<TermApiEntity>> {
            return Pager(
                config = PagingConfig(
                    pageSize = 50,
                    prefetchDistance = 10,
                    enablePlaceholders = false
                ),
                pagingSourceFactory = {
                    wordDao.searchTermsPaging(pattern, dictionaryId)
                }
            ).flow.map { pagingData ->
                pagingData.map {
                    it.toApiEntity(logger)
                }
            }
        }

        override suspend fun getTermById(id: Long): TermApiEntity? {
            return wordDao.getTermById(id = id)?.toApiEntity(logger)
        }
    }

    class WordApiImpl @Inject constructor(
        private val wordDao: WordDao,
    ) : CoreDbApi.WordApi {
        override fun addWordSuspend(value: String, dictionaryId: Int): Long {
            val currentDate = Date(System.currentTimeMillis())
            return wordDao.addWordSuspend(
                WordDb(
                    value = value,
                    dictionaryId = dictionaryId.toLong(),
                    addDate = currentDate,
                )
            )
        }

        //TODO kilg 30.06.2025 03:42 проверить рекурсив.
        // может можно упроситить
        override suspend fun deleteWordSuspend(id: Long): Int {
            wordDao.getWordSuspend(id).also { word ->
                wordDao.removeSampleSuspend(
                    *word.lexemeListDb
                        .map { it.sampleDbList }
                        .flatten()
                        .toTypedArray()
                )
                wordDao.deleteDefinitionsSuspend(
                    *word.lexemeListDb.map { it.lexemeDb }.toTypedArray()
                )
                return wordDao.removeWordSuspend(id)
            }
        }

        override suspend fun updateWordSuspend(id: Long, value: String): Boolean {
            val wordRel = wordDao.getWordSuspend(id)
            val wordDb = wordRel.wordDb.copy(value = value)
            return wordDao.updateWorldSuspend(wordDb) == 1
        }

    }

    class LexemeApiImpl @Inject constructor(
        private val database: Database,
        private val wordDao: WordDao,
        private val componentTypeDao: ComponentTypeDao,
        private val componentValueDao: ComponentValueDao,
        private val componentOptionDao: ComponentOptionDao,
        private val quizConfigDao: QuizConfigDao,
        private val logger: LexemeLogger,
    ) : CoreDbApi.LexemeApi {

        override suspend fun getLexemeById(id: Long): LexemeApiEntity? {
            return wordDao.getLexemeById(id)?.toApiEntity(logger)
        }

        override suspend fun addLexeme(wordId: Long): Long {
            val date = Date(System.currentTimeMillis())
            return wordDao.addLexeme(
                LexemeDb(
                    wordId = wordId,
                    addDate = date,
                )
            )
        }

        override suspend fun deleteLexeme(id: Long): Int {
            return wordDao.deleteLexemeById(id)
        }

        // ===== IS481 generic API (M13) =====

        override suspend fun addLexemeWithBuiltInComponent(
            wordId: Long,
            dictionaryId: Long,
            systemKey: BuiltInComponent,
            data: TemplateValues,
        ): Long = database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                // IS486: builtin пословарные — lookup в рамках словаря лексемы.
                val typeDb = componentTypeDao.getBySystemKeyForDictionary(systemKey.key, dictionaryId)
                    ?: error("Built-in component type not found for systemKey=${systemKey.key} in dictionary=$dictionaryId")
                // F170 guard: built-in активны по contract'у getBySystemKey (removed_at IS NULL),
                // повтор check не требуется. Но для symmetry / defensive coding оставим.
                check(typeDb.removedAt == null) {
                    "Cannot insert ComponentValue for soft-deleted type ${typeDb.id}"
                }
                val date = Date(System.currentTimeMillis())
                wordDao.addLexemeWithComponents(
                    lexemeDb = LexemeDb(wordId = wordId, addDate = date),
                    dictionaryId = dictionaryId,
                    components = listOf(
                        ComponentValueDb(
                            lexemeId = 0L, // перезапишется в WordDao.addLexemeWithComponents
                            componentTypeId = typeDb.id,
                            value = data.toJson(),
                            createdAt = date,
                            updatedAt = date,
                        )
                    ),
                )
            }
        }

        override suspend fun addLexemeWithUserDefinedComponent(
            wordId: Long,
            dictionaryId: Long,
            name: String,
            data: TemplateValues,
        ): Long? = database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                val typeDb = componentTypeDao.getTypesForDictionary(dictionaryId)
                    .firstOrNull { it.systemKey == null && it.name == name }
                    ?: run {
                        logger.e(
                            tag = LogTags.DB,
                            message = "addLexemeWithUserDefinedComponent: type name=$name not found in dictionary=$dictionaryId"
                        )
                        return@immediateTransaction null
                    }
                check(typeDb.removedAt == null) {
                    "Cannot insert ComponentValue for soft-deleted type ${typeDb.id}"
                }
                val date = Date(System.currentTimeMillis())
                wordDao.addLexemeWithComponents(
                    lexemeDb = LexemeDb(wordId = wordId, addDate = date),
                    dictionaryId = dictionaryId,
                    components = listOf(
                        ComponentValueDb(
                            lexemeId = 0L,
                            componentTypeId = typeDb.id,
                            value = data.toJson(),
                            createdAt = date,
                            updatedAt = date,
                        )
                    ),
                )
            }
        }

        override suspend fun addLexemeWithComponents(
            wordId: Long,
            dictionaryId: Long,
            components: List<Pair<ComponentTypeRef, TemplateValues>>,
        ): Long? {
            if (components.isEmpty()) return null
            return database.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    val typesInDict = componentTypeDao.getTypesForDictionary(dictionaryId)
                    val resolvedPairs = components.map { (ref, data) ->
                        val typeDb = when (ref) {
                            // IS486: builtin пословарные — резолв из типов словаря.
                            is ComponentTypeRef.BuiltIn ->
                                typesInDict.firstOrNull { it.systemKey == ref.key.key }

                            is ComponentTypeRef.UserDefined ->
                                typesInDict.firstOrNull { it.systemKey == null && it.name == ref.name }
                        }
                        if (typeDb == null) {
                            logger.e(
                                tag = LogTags.DB,
                                message = "addLexemeWithComponents: unresolved ref=$ref in dictionary=$dictionaryId"
                            )
                            return@immediateTransaction null
                        }
                        typeDb to data
                    }

                    // F169/F170 pre-check: cardinality + не soft-deleted.
                    val groupedByType = resolvedPairs.groupBy { it.first.id }
                    for ((typeId, group) in groupedByType) {
                        val type = group.first().first
                        check(type.removedAt == null) {
                            "Cannot insert ComponentValue for soft-deleted type $typeId"
                        }
                        check(type.isMultiple || group.size <= 1) {
                            "Cardinality violation: type $typeId is_multiple=false, но передано ${group.size} values"
                        }
                    }

                    val date = Date(System.currentTimeMillis())
                    wordDao.addLexemeWithComponents(
                        lexemeDb = LexemeDb(wordId = wordId, addDate = date),
                        dictionaryId = dictionaryId,
                        components = resolvedPairs.map { (typeDb, data) ->
                            ComponentValueDb(
                                lexemeId = 0L,
                                componentTypeId = typeDb.id,
                                value = data.toJson(),
                                createdAt = date,
                                updatedAt = date,
                            )
                        },
                    )
                }
            }
        }

        override suspend fun addComponentValue(
            lexemeId: Long,
            componentTypeId: Long,
            data: TemplateValues,
        ): Long = database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                val type = componentTypeDao.getById(componentTypeId)
                    ?: error("Type not found: $componentTypeId")
                check(type.removedAt == null) {
                    "Cannot insert ComponentValue for soft-deleted type $componentTypeId"
                }
                val now = Date(System.currentTimeMillis())
                componentValueDao.insertSingleSafe(
                    value = ComponentValueDb(
                        lexemeId = lexemeId,
                        componentTypeId = componentTypeId,
                        value = data.toJson(),
                        // IS486: payload CHOICE — колонка option_id (JSON — пустой envelope).
                        optionId = (data as? ChoiceValues)?.optionId,
                        createdAt = now,
                        updatedAt = now,
                    ),
                    isMultiple = type.isMultiple,
                )
            }
        }

        override suspend fun updateComponentValue(
            componentValueId: Long,
            data: TemplateValues,
        ): Int = database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                val existing = componentValueDao.getById(componentValueId)
                    ?: return@immediateTransaction 0
                val type = componentTypeDao.getById(existing.componentTypeId)
                    ?: error("Type not found: ${existing.componentTypeId}")
                check(type.removedAt == null) {
                    "Cannot update ComponentValue for soft-deleted type ${existing.componentTypeId}"
                }
                val now = Date(System.currentTimeMillis())
                val newOptionId = (data as? ChoiceValues)?.optionId
                // IS486 value-триггер (spec §9.2): смена значения = деактивация старого
                // состояния → каскад поддерева (для TEXT план пуст — no-op; для CHOICE
                // умирают зависимые от старой опции). Само значение живёт — исключаем из плана.
                if (existing.optionId != newOptionId) {
                    cascadeResetValues(
                        dictionaryId = type.dictionaryId,
                        event = CascadeEvent.ValueRemoved(existing.id),
                        now = now,
                        excludeIds = setOf(existing.id),
                    )
                }
                componentValueDao.update(
                    existing.copy(value = data.toJson(), optionId = newOptionId, updatedAt = now)
                )
                1
            }
        }

        override suspend fun deleteComponentValue(componentValueId: Long): Int =
            database.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    val existing = componentValueDao.getById(componentValueId)
                        ?: return@immediateTransaction 0
                    val lexemeId = existing.lexemeId
                    val type = componentTypeDao.getById(existing.componentTypeId)
                    // IS486 value-триггер (spec §9.2): смерть значения → каскад поддерева
                    // (мульти-цель гаснет последним значением — планировщик это учитывает).
                    // Каскадные потомки — soft delete; само значение — существующий путь.
                    cascadeResetValues(
                        dictionaryId = type?.dictionaryId,
                        event = CascadeEvent.ValueRemoved(existing.id),
                        now = Date(System.currentTimeMillis()),
                        excludeIds = setOf(existing.id),
                    )
                    componentValueDao.delete(componentValueId)
                    componentValueDao.countForLexeme(lexemeId)
                }
            }

        override suspend fun getComponentTypes(dictionaryId: Long): List<ComponentTypeApiEntity> {
            return componentTypeDao.getTypesForDictionary(dictionaryId).mapNotNull { it.toApiEntity() }
        }

        override suspend fun getComponentOptions(componentTypeId: Long): List<ComponentOptionApiEntity> {
            return componentOptionDao.getForType(componentTypeId).map { row ->
                ComponentOptionApiEntity(
                    id = row.id,
                    componentTypeId = row.componentTypeId,
                    systemKey = row.systemKey,
                    label = row.label,
                    position = row.position,
                    removedAt = row.removedAt,
                )
            }
        }

        override suspend fun getQuizConfig(
            dictionaryId: Long,
            quizMode: String,
        ): QuizConfigApiEntity? {
            return quizConfigDao.getByDictionaryAndMode(dictionaryId, quizMode)?.toApiEntity()
        }

        // ===== Component constructor (IS481, M13) — 6 NEW methods =====

        override fun flowAllUserDefinedTypesWithUsage(): Flow<UserDefinedTypesUsageSnapshot> =
            combine(
                componentTypeDao.flowAllUserDefined(),
                wordDao.flowDictionaries(),
            ) { types, dicts ->
                val typesApi = types.mapNotNull { it.toApiEntity() }
                val countsList = componentValueDao.aggregatedValueCountPerType()
                val dictPairs = componentValueDao.typeDictPairs()
                UserDefinedTypesUsageSnapshot(
                    types = typesApi,
                    valueCountByType = countsList.associate { it.typeId to it.count },
                    dictionaryIdsByType = dictPairs
                        .groupBy { it.typeId }
                        .mapValues { entry -> entry.value.map { it.dictId }.toSet() },
                    dictionaryNames = dicts
                        .mapNotNull { d -> d.id?.let { id -> id to d.name } }
                        .toMap(),
                )
            }

        override fun flowUserDefinedTypesForDictionary(
            dictionaryId: Long,
        ): Flow<DictionaryTypesSnapshot> =
            // IS486 фаза 3: builtin включены (рубильник в конструкторе, spec §4).
            // combine трёх flow — снапшот пере-эмитится и при CRUD опций, и при
            // изменении значений (девайс-баг 2026-07-21: наблюдение только
            // component_types оставляло UI со stale-опциями после их удаления).
            combine(
                componentTypeDao.flowTypesForDictionary(dictionaryId),
                componentOptionDao.flowForDictionary(dictionaryId),
                componentValueDao.flowAggregatedValueCountPerTypeForDict(dictionaryId),
            ) { types, options, counts ->
                val typesApi = types.mapNotNull { it.toApiEntity() }
                val dictName = wordDao.getDictionaryById(dictionaryId)?.name.orEmpty()
                val optionsByType = options
                    .groupBy { it.componentTypeId }
                    .mapValues { (_, rows) -> rows.map { it.toOptionApi() } }
                DictionaryTypesSnapshot(
                    dictionaryId = dictionaryId,
                    dictionaryName = dictName,
                    types = typesApi,
                    valueCountByType = counts.associate { it.typeId to it.count },
                    optionsByType = optionsByType,
                )
            }

        override fun flowTypesForDictionary(
            dictionaryId: Long,
        ): Flow<List<ComponentTypeApiEntity>> =
            componentTypeDao.flowTypesForDictionary(dictionaryId).map { types ->
                types.mapNotNull { it.toApiEntity() }
            }

        override suspend fun createUserDefinedComponent(
            name: String,
            template: ComponentTemplate,
            isMultiple: Boolean,
            scope: Scope,
            core: Boolean,
            dependsOnTypeId: Long?,
            dependsOnOptionId: Long?,
            optionLabels: List<String>,
        ): CreateComponentOutcome {
            // 1. two-prong SELECT (per aspect userdefined_identity_invariant)
            val sameScope = when (scope) {
                Scope.Global -> componentTypeDao.findActiveGlobalByName(name)
                is Scope.PerDictionaries -> scope.ids.firstNotNullOfOrNull {
                    componentTypeDao.findActiveUserDefinedByName(it, name)
                }
            }
            if (sameScope != null) return CreateComponentOutcome.SameScopeCollision

            val crossScope = when (scope) {
                Scope.Global -> componentTypeDao.countActivePerDictByName(name) > 0
                is Scope.PerDictionaries -> componentTypeDao.findActiveGlobalByName(name) != null
            }
            if (crossScope) return CreateComponentOutcome.CrossScopeCollision

            // 2. INSERT N rows (atomic в одной транзакции)
            val now = Date(System.currentTimeMillis())
            val newRows: List<ComponentTypeDb> = when (scope) {
                // IS486: цель/ядро приходят параметрами (дефолты = legacy-семантика spec §11).
                Scope.Global -> listOf(
                    ComponentTypeDb(
                        systemKey = null,
                        dictionaryId = null,
                        name = name,
                        templateKey = template.key,
                        position = 0,
                        isMultiple = isMultiple,
                        core = core,
                        enabled = true,
                        dependsOnTypeId = dependsOnTypeId,
                        dependsOnOptionId = dependsOnOptionId,
                        createdAt = now,
                        updatedAt = now,
                    )
                )

                is Scope.PerDictionaries -> scope.ids.map { dictId ->
                    ComponentTypeDb(
                        systemKey = null,
                        dictionaryId = dictId,
                        name = name,
                        templateKey = template.key,
                        position = 0,
                        isMultiple = isMultiple,
                        core = core,
                        enabled = true,
                        dependsOnTypeId = dependsOnTypeId,
                        dependsOnOptionId = dependsOnOptionId,
                        createdAt = now,
                        updatedAt = now,
                    )
                }
            }
            val inserted = database.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    newRows.map { row ->
                        val id = componentTypeDao.insert(row)
                        // IS486: стартовые варианты CHOICE — в той же транзакции.
                        if (template == ComponentTemplate.CHOICE) {
                            optionLabels.forEachIndexed { index, label ->
                                componentOptionDao.insert(
                                    ComponentOptionDb(
                                        componentTypeId = id,
                                        systemKey = null,
                                        label = label,
                                        position = index,
                                        createdAt = now,
                                        updatedAt = now,
                                    )
                                )
                            }
                        }
                        row.copy(id = id)
                    }
                }
            }.mapNotNull { it.toApiEntity() }
            return CreateComponentOutcome.Success(inserted)
        }

        override suspend fun renameComponentType(
            typeId: Long,
            newName: String,
        ): RenameComponentOutcome {
            val existing = componentTypeDao.getById(typeId)
                ?: return RenameComponentOutcome.BuiltInProtected
            if (existing.removedAt != null) return RenameComponentOutcome.Removed
            if (existing.systemKey != null) return RenameComponentOutcome.BuiltInProtected
            val oldName = existing.name
                ?: error("user-defined ComponentType без name: typeId=$typeId")

            // Collision checks — те же two-prong что в create.
            val sameScope = if (existing.dictionaryId != null) {
                componentTypeDao.findActiveUserDefinedByName(existing.dictionaryId, newName)
            } else {
                componentTypeDao.findActiveGlobalByName(newName)
            }
            if (sameScope != null && sameScope.id != typeId) {
                return RenameComponentOutcome.SameScopeCollision
            }

            val crossScope = if (existing.dictionaryId != null) {
                componentTypeDao.findActiveGlobalByName(newName) != null
            } else {
                componentTypeDao.countActivePerDictByName(newName) > 0
            }
            if (crossScope) return RenameComponentOutcome.CrossScopeCollision

            val now = Date(System.currentTimeMillis())
            database.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    componentTypeDao.renameUserDefined(typeId, newName, now)
                    cascadeRenameInQuizConfigs(oldName, newName)
                }
            }

            val updatedApi = componentTypeDao.getById(typeId)?.toApiEntity()
                ?: return RenameComponentOutcome.BuiltInProtected
            return RenameComponentOutcome.Success(updatedApi)
        }

        override suspend fun editComponentType(
            typeId: Long,
            name: String,
            template: ComponentTemplate,
            isMultiple: Boolean,
            core: Boolean,
            dependsOnTypeId: Long?,
            dependsOnOptionId: Long?,
        ): EditComponentOutcome {
            val existing = componentTypeDao.getById(typeId)
                ?: return EditComponentOutcome.BuiltInProtected
            if (existing.systemKey != null) return EditComponentOutcome.BuiltInProtected
            if (existing.removedAt != null) return EditComponentOutcome.Removed

            val oldName = existing.name
                ?: error("user-defined ComponentType без name: typeId=$typeId")
            val oldTemplate = ComponentTemplate.fromKey(existing.templateKey)
                ?: return EditComponentOutcome.TemplateImmutable

            // Defense-in-depth (F017): UseCase обычно перехватывает раньше.
            if (oldTemplate != template) return EditComponentOutcome.TemplateImmutable

            // IS486 (spec §7.5): CHOICE всегда single.
            if (template == ComponentTemplate.CHOICE && isMultiple) {
                return EditComponentOutcome.MultiForbiddenForChoice
            }

            // Cardinality downgrade SELECT (F018) — только при true → false.
            if (existing.isMultiple && !isMultiple) {
                val impacted = componentValueDao.findLexemesWithMultipleValuesForType(typeId)
                if (impacted.isNotEmpty()) {
                    return EditComponentOutcome.CardinalityDowngradeBlocked(impacted)
                }
            }

            // Collision checks — игнорируем self при rename.
            if (name != oldName) {
                val sameScope = if (existing.dictionaryId != null) {
                    componentTypeDao.findActiveUserDefinedByName(existing.dictionaryId, name)
                } else {
                    componentTypeDao.findActiveGlobalByName(name)
                }
                if (sameScope != null && sameScope.id != typeId) {
                    return EditComponentOutcome.SameScopeCollision
                }

                val crossScope = if (existing.dictionaryId != null) {
                    componentTypeDao.findActiveGlobalByName(name) != null
                } else {
                    componentTypeDao.countActivePerDictByName(name) > 0
                }
                if (crossScope) return EditComponentOutcome.CrossScopeCollision
            }

            // IS486 фаза 3: перепривязка цели (spec §9.4).
            val targetChanged = existing.core != core ||
                existing.dependsOnTypeId != dependsOnTypeId ||
                existing.dependsOnOptionId != dependsOnOptionId
            val newTarget = when {
                dependsOnTypeId != null -> DependencyTarget.Component(ComponentTypeId(dependsOnTypeId))
                dependsOnOptionId != null -> DependencyTarget.Option(dependsOnOptionId)
                else -> DependencyTarget.Lexeme
            }
            if (targetChanged && existing.dictionaryId != null) {
                val graph = loadGraph(existing.dictionaryId)
                // Потеря ядра (ядро → не-лексема/не-ядро): запрет для последнего (spec §7.8).
                val losesCore = existing.core && !core
                if (losesCore && graph.checkCoreLoss(ComponentTypeId(typeId)) == CoreLossCheck.LastEnabledCore) {
                    return EditComponentOutcome.LastEnabledCore
                }
                // Ацикличность (spec §8) — только для не-лексемных целей.
                if (newTarget != DependencyTarget.Lexeme &&
                    graph.checkAcyclic(ComponentTypeId(typeId), newTarget) == AcyclicityCheck.CycleDetected
                ) {
                    return EditComponentOutcome.CycleDetected
                }
            }

            val now = Date(System.currentTimeMillis())
            database.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    if (targetChanged && existing.dictionaryId != null) {
                        // IS486 умный сброс (решение 2026-07-21, spec §9.4): гаснут только
                        // значения лексем, где НОВОЕ условие не выполнено (+ поддеревья) —
                        // fixpoint по графу с подменённой целью, начальных жертв нет.
                        val plan = rebindCascadePlan(existing.dictionaryId, typeId, newTarget, core)
                        if (plan.isNotEmpty()) {
                            componentValueDao.softDeleteByIds(plan.toList(), now)
                        }
                    }
                    componentTypeDao.update(
                        existing.copy(
                            name = name,
                            isMultiple = isMultiple,
                            core = core,
                            dependsOnTypeId = dependsOnTypeId,
                            dependsOnOptionId = dependsOnOptionId,
                            updatedAt = now,
                        )
                    )
                    if (name != oldName) {
                        cascadeRenameInQuizConfigs(oldName, name)
                    }
                }
            }

            val updatedApi = componentTypeDao.getById(typeId)?.toApiEntity()
                ?: return EditComponentOutcome.BuiltInProtected
            return EditComponentOutcome.Success(updatedApi)
        }

        /** IS486: граф словаря (включая removed — ссылки не зануляются). */
        private suspend fun loadGraph(dictionaryId: Long): ComponentGraph {
            val typeRows = componentTypeDao.getAllForDictionaryWithRemoved(dictionaryId)
            val optionRows = componentOptionDao.getAllForTypes(typeRows.map { it.id })
            return ComponentGraph(
                types = typeRows.mapNotNull { it.toDomainType() },
                options = optionRows.map { row ->
                    ComponentOption(
                        id = row.id,
                        componentTypeId = ComponentTypeId(row.componentTypeId),
                        systemKey = row.systemKey,
                        label = row.label,
                        position = row.position,
                        removedAt = row.removedAt,
                    )
                },
            )
        }

        // ===== IS486 фаза 3: рубильник + CRUD опций =====

        override suspend fun setComponentEnabled(
            typeId: Long,
            enabled: Boolean,
        ): SetEnabledComponentOutcome {
            val existing = componentTypeDao.getById(typeId)
                ?: return SetEnabledComponentOutcome.Removed
            if (existing.removedAt != null) return SetEnabledComponentOutcome.Removed
            if (existing.enabled == enabled) {
                val api = existing.toApiEntity() ?: return SetEnabledComponentOutcome.Removed
                return SetEnabledComponentOutcome.Success(api)
            }
            // Выключение последнего включённого ядра — отказ (spec §7.8).
            if (!enabled && existing.core && existing.dictionaryId != null) {
                val graph = loadGraph(existing.dictionaryId)
                if (graph.checkCoreLoss(ComponentTypeId(typeId)) == CoreLossCheck.LastEnabledCore) {
                    return SetEnabledComponentOutcome.LastEnabledCore
                }
            }
            val now = Date(System.currentTimeMillis())
            componentTypeDao.update(existing.copy(enabled = enabled, updatedAt = now))
            val api = componentTypeDao.getById(typeId)?.toApiEntity()
                ?: return SetEnabledComponentOutcome.Removed
            return SetEnabledComponentOutcome.Success(api)
        }

        override suspend fun addComponentOption(
            componentTypeId: Long,
            label: String,
        ): OptionCrudOutcome {
            val type = componentTypeDao.getById(componentTypeId) ?: return OptionCrudOutcome.Removed
            if (type.removedAt != null) return OptionCrudOutcome.Removed
            // Решение §21.2: опции builtin нередактируемы (defense-in-depth).
            if (type.systemKey != null) return OptionCrudOutcome.BuiltInProtected
            val now = Date(System.currentTimeMillis())
            val position = componentOptionDao.nextPosition(componentTypeId)
            val id = componentOptionDao.insert(
                ComponentOptionDb(
                    componentTypeId = componentTypeId,
                    systemKey = null,
                    label = label,
                    position = position,
                    createdAt = now,
                    updatedAt = now,
                )
            )
            val row = componentOptionDao.getById(id) ?: return OptionCrudOutcome.Removed
            return OptionCrudOutcome.Success(row.toOptionApi())
        }

        override suspend fun renameComponentOption(
            optionId: Long,
            label: String,
        ): OptionCrudOutcome {
            val existing = componentOptionDao.getById(optionId) ?: return OptionCrudOutcome.Removed
            if (existing.removedAt != null) return OptionCrudOutcome.Removed
            // Решение §21.2: опции builtin нередактируемы (defense-in-depth).
            if (componentTypeDao.getById(existing.componentTypeId)?.systemKey != null) {
                return OptionCrudOutcome.BuiltInProtected
            }
            componentOptionDao.updateLabel(optionId, label, Date(System.currentTimeMillis()))
            val row = componentOptionDao.getById(optionId) ?: return OptionCrudOutcome.Removed
            return OptionCrudOutcome.Success(row.toOptionApi())
        }

        override suspend fun deleteComponentOption(optionId: Long): OptionCrudOutcome {
            val existing = componentOptionDao.getById(optionId) ?: return OptionCrudOutcome.Removed
            if (existing.removedAt != null) return OptionCrudOutcome.Removed
            val owner = componentTypeDao.getById(existing.componentTypeId)
            // Решение §21.2: опции builtin нередактируемы (defense-in-depth).
            if (owner?.systemKey != null) return OptionCrudOutcome.BuiltInProtected
            val dictionaryId = owner?.dictionaryId
            // Полный impact как в preview (ревью 2026-07-21): деградирующие потомки +
            // разбивка свои/потомки — до транзакции, по живому состоянию.
            val degraded = if (dictionaryId == null) emptyList() else {
                loadGraph(dictionaryId).types.filter { t ->
                    t.removedAt == null && (t.dependsOn as? DependencyTarget.Option)?.optionId == optionId
                }.map { it.id }
            }
            val directChoiceCount = componentValueDao.getActiveByTypeIds(listOf(existing.componentTypeId))
                .count { it.optionId == optionId }
            val now = Date(System.currentTimeMillis())
            var resetCount = 0
            database.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    // Комбинированный каскад К4+К5 (spec §9.3): значения-выборы + поддеревья
                    // зависимых; зависимые компоненты становятся degraded вычисляемо.
                    resetCount = cascadeResetValuesCounting(
                        dictionaryId = dictionaryId,
                        event = CascadeEvent.OptionRemoved(optionId),
                        now = now,
                    )
                    componentOptionDao.softDelete(optionId, now)
                }
            }
            return OptionCrudOutcome.Deleted(
                DeletionImpact(
                    valueCount = directChoiceCount,
                    dictionariesWithValues = listOfNotNull(dictionaryId),
                    affectedQuizConfigs = emptyList(),
                    affectedPrefs = emptyList(),
                    degradedComponents = degraded,
                    descendantValueCount = (resetCount - directChoiceCount).coerceAtLeast(0),
                )
            )
        }

        override suspend fun previewOptionDeletionImpact(optionId: Long): DeletionImpact? {
            val option = componentOptionDao.getById(optionId) ?: return null
            if (option.removedAt != null) return null
            val owner = componentTypeDao.getById(option.componentTypeId) ?: return null
            val dictionaryId = owner.dictionaryId ?: return null

            // Dry-run комбинированного каскада (spec §9.3): значения-выборы + поддеревья.
            val plan = cascadePlan(dictionaryId, CascadeEvent.OptionRemoved(optionId))
            // Деградирующие компоненты: живые типы, зависящие от этой опции.
            val graph = loadGraph(dictionaryId)
            val degraded = graph.types.filter { t ->
                t.removedAt == null && (t.dependsOn as? DependencyTarget.Option)?.optionId == optionId
            }.map { it.id }
            // Прямые выборы опции — для разбивки valueCount vs descendantValueCount.
            val directChoiceCount = componentValueDao.getActiveByTypeIds(listOf(option.componentTypeId))
                .count { it.optionId == optionId }

            return DeletionImpact(
                valueCount = directChoiceCount,
                dictionariesWithValues = listOf(dictionaryId),
                affectedQuizConfigs = emptyList(),
                affectedPrefs = emptyList(),
                degradedComponents = degraded,
                descendantValueCount = (plan.size - directChoiceCount).coerceAtLeast(0),
            )
        }

        private fun ComponentOptionDb.toOptionApi() = ComponentOptionApiEntity(
            id = id,
            componentTypeId = componentTypeId,
            systemKey = systemKey,
            label = label,
            position = position,
            removedAt = removedAt,
        )

        /**
         * Cascade rename — все configs где `user:<oldName>` появляется получают
         * `user:<newName>`. Вызывается внутри `database.useWriterConnection { it.immediateTransaction { ... } }`.
         */
        private suspend fun cascadeRenameInQuizConfigs(oldName: String, newName: String) {
            quizConfigDao.getAllConfigs().forEach { config ->
                val refs = config.componentRefs.toComponentTypeRefList()
                val updated = refs.map { ref ->
                    if (ref is ComponentTypeRef.UserDefined && ref.name == oldName) {
                        ComponentTypeRef.UserDefined(newName)
                    } else ref
                }
                val willWrite = updated != refs
                logger.d(
                    tag = FeatureLogTags.COMPONENT_CONSTRUCTOR,
                    message = "cascade rename: configId=${config.id} refs=${refs.size}→${updated.size} write=$willWrite oldName=$oldName newName=$newName",
                )
                if (willWrite) {
                    quizConfigDao.updateComponentRefs(config.id, updated.toJson())
                }
            }
        }

        override suspend fun previewDeletionImpact(typeId: Long): DeletionImpact? {
            val type = componentTypeDao.getById(typeId) ?: return null
            if (type.systemKey != null) return null
            val typeName = type.name

            val valueCount = componentValueDao.countActiveByTypeId(typeId)
            val dictIds = componentValueDao.dictionaryIdsForTypeId(typeId)

            val affectedConfigs = quizConfigDao.getAllConfigs().mapNotNull { config ->
                val refs = config.componentRefs.toComponentTypeRefList()
                if (typeName != null && refs.any {
                        it is ComponentTypeRef.UserDefined && it.name == typeName
                    }
                ) {
                    AffectedQuizConfig(
                        dictionaryId = config.dictionaryId,
                        quizMode = config.quizMode,
                    )
                } else null
            }

            // IS486 (spec §5): деградирующие потомки + счётчик каскада вниз (dry-run плана).
            var degraded: List<ComponentTypeId> = emptyList()
            var descendantCount = 0
            if (type.dictionaryId != null) {
                val graph = loadGraph(type.dictionaryId)
                val optionIds = graph.options
                    .filter { it.componentTypeId == ComponentTypeId(typeId) }
                    .map { it.id }
                    .toSet()
                degraded = graph.types.filter { t ->
                    t.removedAt == null && when (val target = t.dependsOn) {
                        is DependencyTarget.Component -> target.typeId == ComponentTypeId(typeId)
                        is DependencyTarget.Option -> target.optionId in optionIds
                        DependencyTarget.Lexeme -> false
                    }
                }.map { it.id }
                val plan = cascadePlan(type.dictionaryId, CascadeEvent.ComponentRemoved(ComponentTypeId(typeId)))
                descendantCount = (plan.size - valueCount).coerceAtLeast(0)
            }

            return DeletionImpact(
                valueCount = valueCount,
                dictionariesWithValues = dictIds,
                affectedQuizConfigs = affectedConfigs,
                affectedPrefs = affectedConfigs.map { it.dictionaryId }.distinct(),
                degradedComponents = degraded,
                descendantValueCount = descendantCount,
            )
        }

        override suspend fun softDeleteComponentType(
            typeId: Long,
        ): SoftDeleteComponentOutcome {
            val type = componentTypeDao.getById(typeId)
                ?: return SoftDeleteComponentOutcome.BuiltInProtected
            if (type.removedAt != null) return SoftDeleteComponentOutcome.Removed
            if (type.systemKey != null) return SoftDeleteComponentOutcome.BuiltInProtected

            // IS486 (spec §7.8): удаление последнего включённого ядра словаря — отказ.
            if (type.core && type.enabled && type.dictionaryId != null) {
                val graph = loadGraph(type.dictionaryId)
                if (graph.checkCoreLoss(ComponentTypeId(typeId)) == CoreLossCheck.LastEnabledCore) {
                    return SoftDeleteComponentOutcome.LastEnabledCore
                }
            }

            val impact = previewDeletionImpact(typeId)
                ?: return SoftDeleteComponentOutcome.BuiltInProtected

            val now = Date(System.currentTimeMillis())
            val oldName = type.name
            database.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    componentTypeDao.softDelete(typeId, now)
                    // IS486 каскад-модуль: план сбросов по всему поддереву зависимых
                    // (spec §9.4) — вместо точечного softDeleteByTypeId.
                    // Дети-компоненты НЕ удаляются: degraded вычисляется по ссылке.
                    cascadeResetValues(
                        dictionaryId = type.dictionaryId,
                        event = CascadeEvent.ComponentRemoved(ComponentTypeId(typeId)),
                        now = now,
                    )
                    if (oldName != null) {
                        quizConfigDao.getAllConfigs().forEach { config ->
                            val refs = config.componentRefs.toComponentTypeRefList()
                            val filtered = refs.filter {
                                !(it is ComponentTypeRef.UserDefined && it.name == oldName)
                            }
                            val willWrite = filtered != refs
                            logger.d(
                                tag = FeatureLogTags.COMPONENT_CONSTRUCTOR,
                                message = "cascade soft-delete: configId=${config.id} refs=${refs.size}→${filtered.size} write=$willWrite removedName=$oldName",
                            )
                            if (willWrite) {
                                quizConfigDao.updateComponentRefs(config.id, filtered.toJson())
                            }
                        }
                    }
                }
            }

            return SoftDeleteComponentOutcome.Success(impact)
        }

        // IS486: deprecated-шимы addLexemeWithTranslation / updateLexemeTranslation
        // выпилены (phase1_plan § Зона C) — внешних вызовов не было, а глобального
        // getBySystemKey с пословарными builtin больше не существует.

        /**
         * IS486 каскад-исполнитель (фаза 1): собирает граф словаря (включая
         * removed — ссылки не зануляются), строит план чистым планировщиком
         * ([planCascade]) и исполняет одним UPDATE. Вызывается СТРОГО внутри
         * открытой транзакции. Value-триггеры (смена/удаление значения) — фаза 2.
         */
        private suspend fun cascadeResetValues(
            dictionaryId: Long?,
            event: CascadeEvent,
            now: Date,
            excludeIds: Set<Long> = emptySet(),
        ) {
            cascadeResetValuesCounting(dictionaryId, event, now, excludeIds)
        }

        /** Как [cascadeResetValues], но возвращает число сброшенных значений (impact). */
        private suspend fun cascadeResetValuesCounting(
            dictionaryId: Long?,
            event: CascadeEvent,
            now: Date,
            excludeIds: Set<Long> = emptySet(),
        ): Int {
            if (dictionaryId == null) {
                // Глобальных кастомов в проде не существует (spec §11); defensive skip.
                logger.e(
                    tag = LogTags.DB,
                    message = "cascadeResetValues: global component (dictionaryId=null) — cascade skipped",
                )
                return 0
            }
            val plan = cascadePlan(dictionaryId, event) - excludeIds
            if (plan.isNotEmpty()) {
                componentValueDao.softDeleteByIds(plan.toList(), now)
            }
            return plan.size
        }

        /**
         * IS486 умный сброс (решение 2026-07-21): план перепривязки — граф словаря
         * с УЖЕ подменённой целью узла + fixpoint без начальных жертв
         * ([CascadeEvent.TargetRebound]). Возвращает id значений к сбросу.
         */
        private suspend fun rebindCascadePlan(
            dictionaryId: Long,
            typeId: Long,
            newTarget: DependencyTarget,
            newCore: Boolean,
        ): Set<Long> {
            val graph = loadGraph(dictionaryId)
            val rebound = ComponentGraph(
                types = graph.types.map {
                    if (it.id == ComponentTypeId(typeId)) it.copy(dependsOn = newTarget, core = newCore) else it
                },
                options = graph.options,
            )
            val typeRows = componentTypeDao.getAllForDictionaryWithRemoved(dictionaryId)
            val values = componentValueDao.getActiveByTypeIds(typeRows.map { it.id })
                .map { row ->
                    CascadeValue(
                        id = row.id,
                        lexemeId = row.lexemeId,
                        typeId = ComponentTypeId(row.componentTypeId),
                        optionId = row.optionId,
                    )
                }
            return rebound.planCascade(values, CascadeEvent.TargetRebound(ComponentTypeId(typeId)))
        }

        override suspend fun previewRebindImpact(
            typeId: Long,
            core: Boolean,
            dependsOnTypeId: Long?,
            dependsOnOptionId: Long?,
        ): DeletionImpact? {
            val type = componentTypeDao.getById(typeId) ?: return null
            if (type.removedAt != null || type.systemKey != null) return null
            val dictionaryId = type.dictionaryId ?: return null
            val newTarget = when {
                dependsOnTypeId != null -> DependencyTarget.Component(ComponentTypeId(dependsOnTypeId))
                dependsOnOptionId != null -> DependencyTarget.Option(dependsOnOptionId)
                else -> DependencyTarget.Lexeme
            }
            val plan = rebindCascadePlan(dictionaryId, typeId, newTarget, core)
            val ownCount = if (plan.isEmpty()) 0 else {
                componentValueDao.getActiveByTypeIds(listOf(typeId)).count { it.id in plan }
            }
            return DeletionImpact(
                valueCount = ownCount,
                dictionariesWithValues = if (plan.isEmpty()) emptyList() else listOf(dictionaryId),
                affectedQuizConfigs = emptyList(),
                affectedPrefs = emptyList(),
                descendantValueCount = (plan.size - ownCount).coerceAtLeast(0),
            )
        }

        /** IS486: план каскада (dry-run) — без исполнения; для impact-превью. */
        private suspend fun cascadePlan(dictionaryId: Long, event: CascadeEvent): Set<Long> {
            val typeRows = componentTypeDao.getAllForDictionaryWithRemoved(dictionaryId)
            val graph = loadGraph(dictionaryId)
            val values = componentValueDao.getActiveByTypeIds(typeRows.map { it.id })
                .map { row ->
                    CascadeValue(
                        id = row.id,
                        lexemeId = row.lexemeId,
                        typeId = ComponentTypeId(row.componentTypeId),
                        optionId = row.optionId,
                    )
                }
            return graph.planCascade(values, event)
        }

        /** Db → domain для планировщика; fail-soft на нелегальном XOR/шаблоне. */
        private fun ComponentTypeDb.toDomainType(): ComponentType? {
            val tpl = ComponentTemplate.fromKey(templateKey) ?: return null
            val target = when {
                dependsOnTypeId != null && dependsOnOptionId != null -> return null
                dependsOnTypeId != null -> DependencyTarget.Component(ComponentTypeId(dependsOnTypeId))
                dependsOnOptionId != null -> DependencyTarget.Option(dependsOnOptionId)
                else -> DependencyTarget.Lexeme
            }
            return ComponentType(
                id = ComponentTypeId(id),
                systemKey = systemKey?.let(BuiltInComponent::fromKey),
                dictionaryId = dictionaryId,
                name = name,
                template = tpl,
                position = position,
                isMultiple = isMultiple,
                core = core,
                enabled = enabled,
                dependsOn = target,
                createdAt = createdAt,
                updatedAt = updatedAt,
                removedAt = removedAt,
            )
        }
    }

    class QuizApiImpl @Inject constructor(
        private val wordDao: WordDao,
        private val logger: LexemeLogger,
    ) : CoreDbApi.QuizApi {

        override suspend fun addWriteQuiz(
            dictionaryId: Long,
            lexemeId: Long,
        ): Long {
            return wordDao.addWriteQuiz(
                WriteQuizDb.create(
                    dictionaryId = dictionaryId,
                    lexemeId = lexemeId
                )
            )
        }

        override suspend fun updateWriteQuiz(entity: List<WriteQuizUpsertApiEntity>): Int {
            return wordDao.updateWriteQuiz(entity.toDb())
        }

        override suspend fun getWriteQuizIds(
            grade: Int,
            dictionaryId: Long,
        ): List<Long> {
            return wordDao.getWriteQuizIds(grade = grade, langId = dictionaryId)
        }

        override suspend fun getWriteQuizByIds(
            ids: List<Long>,
        ): List<WriteQuizComplexEntity> {
            logger.d(tag = LogTags.DB, message = "getWriteQuizByIds: ${ids.size} ids")
            val result = wordDao.getWriteQuizByIds(ids).map { it.toApiEntity(logger) }
            logger.d(tag = LogTags.DB, message = "getWriteQuizByIds: returned ${result.size} items")
            return result
        }

        override suspend fun getEarliestWriteQuizList(
            limit: Int,
            dictionaryId: Long,
        ): List<WriteQuizComplexEntity> {
            return wordDao.getEarliest(
                langId = dictionaryId,
                limit = limit,
            ).map { it.toApiEntity(logger) }
        }

        override suspend fun getFrequentMistakesWriteQuizList(
            limit: Int,
            dictionaryId: Long
        ): List<WriteQuizComplexEntity> {
            return wordDao.getFrequentMistakes(
                langId = dictionaryId,
                limit = limit,
            ).map { it.toApiEntity(logger) }
        }
    }

    class StatisticApiImpl @Inject constructor(
        private val wordDao: WordDao
    ) : CoreDbApi.StatisticApi {

        override fun flowWordCount(dictionaryId: Int): Flow<Int> =
            wordDao.flowWordCount(langId = dictionaryId)

        override fun flowLexemeCount(dictionaryId: Int): Flow<Int> =
            wordDao.flowLexemeCount(langId = dictionaryId)

        override fun flowQuizCount(dictionaryId: Int, maxGrade: Int): Flow<Map<Int, Int>> {
            val gradeRange = 0..maxGrade
            val flows: List<Flow<Pair<Int, Int>>> = gradeRange.map { grade ->
                wordDao.flowQuizCount(dictionaryId, grade).map { count -> grade to count }
            }

            return combine(flows) { pairs: Array<Pair<Int, Int>> ->
                pairs.toMap()
            }
        }

    }
}
