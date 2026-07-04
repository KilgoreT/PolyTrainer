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
import me.apomazkin.core_db_api.entity.LexemeApiEntity
import me.apomazkin.core_db_api.entity.QuizConfigApiEntity
import me.apomazkin.core_db_api.entity.RenameComponentOutcome
import me.apomazkin.core_db_api.entity.SoftDeleteComponentOutcome
import me.apomazkin.core_db_api.entity.TermApiEntity
import me.apomazkin.core_db_api.entity.TranslationApiEntity
import me.apomazkin.core_db_api.entity.UserDefinedTypesUsageSnapshot
import me.apomazkin.core_db_api.entity.WriteQuizComplexEntity
import me.apomazkin.core_db_api.entity.WriteQuizUpsertApiEntity
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
import me.apomazkin.core_db_impl.room.dao.ComponentTypeDao
import me.apomazkin.core_db_impl.room.dao.ComponentValueDao
import me.apomazkin.core_db_impl.room.dao.QuizConfigDao
import me.apomazkin.lexeme.AffectedQuizConfig
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.DeletionImpact
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
        private val wordDao: WordDao,
    ) : CoreDbApi.DictionaryApi {

        override suspend fun addDictionary(name: String, numericCode: Int?): Long {
            val currentDate = Date(System.currentTimeMillis())
            return wordDao.addDictionary(
                DictionaryDb(
                    numericCode = numericCode,
                    name = name,
                    addDate = currentDate,
                )
            )
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
                val typeDb = componentTypeDao.getBySystemKey(systemKey.key)
                    ?: error("Built-in component type not found for systemKey=${systemKey.key}")
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
                            is ComponentTypeRef.BuiltIn ->
                                componentTypeDao.getBySystemKey(ref.key.key)

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
                componentValueDao.update(
                    existing.copy(value = data.toJson(), updatedAt = now)
                )
                1
            }
        }

        override suspend fun deleteComponentValue(componentValueId: Long): Int {
            val existing = componentValueDao.getById(componentValueId) ?: return 0
            val lexemeId = existing.lexemeId
            componentValueDao.delete(componentValueId)
            return componentValueDao.countForLexeme(lexemeId)
        }

        override suspend fun getComponentTypes(dictionaryId: Long): List<ComponentTypeApiEntity> {
            return componentTypeDao.getTypesForDictionary(dictionaryId).mapNotNull { it.toApiEntity() }
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
            componentTypeDao.flowUserDefinedForDictionary(dictionaryId).map { types ->
                val typesApi = types.mapNotNull { it.toApiEntity() }
                val counts = componentValueDao.aggregatedValueCountPerTypeForDict(dictionaryId)
                val dictName = wordDao.getDictionaryById(dictionaryId)?.name.orEmpty()
                DictionaryTypesSnapshot(
                    dictionaryId = dictionaryId,
                    dictionaryName = dictName,
                    types = typesApi,
                    valueCountByType = counts.associate { it.typeId to it.count },
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
                Scope.Global -> listOf(
                    ComponentTypeDb(
                        systemKey = null,
                        dictionaryId = null,
                        name = name,
                        templateKey = template.key,
                        position = 0,
                        isMultiple = isMultiple,
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
                        createdAt = now,
                        updatedAt = now,
                    )
                }
            }
            val inserted = database.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    newRows.map { row ->
                        val id = componentTypeDao.insert(row)
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

            // Cardinality downgrade SELECT (F018) — только при true → false.
            // Real per-lexeme SELECT (data sub-flow #2): возвращает lexeme_id для
            // тех, у кого активных values >1. Empty list → downgrade legitimate
            // (один lexeme с одним value не блокируется).
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

            val now = Date(System.currentTimeMillis())
            database.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    componentTypeDao.update(
                        existing.copy(
                            name = name,
                            isMultiple = isMultiple,
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

            return DeletionImpact(
                valueCount = valueCount,
                dictionariesWithValues = dictIds,
                affectedQuizConfigs = affectedConfigs,
                affectedPrefs = affectedConfigs.map { it.dictionaryId }.distinct(),
            )
        }

        override suspend fun softDeleteComponentType(
            typeId: Long,
        ): SoftDeleteComponentOutcome {
            val type = componentTypeDao.getById(typeId)
                ?: return SoftDeleteComponentOutcome.BuiltInProtected
            if (type.removedAt != null) return SoftDeleteComponentOutcome.Removed
            if (type.systemKey != null) return SoftDeleteComponentOutcome.BuiltInProtected

            val impact = previewDeletionImpact(typeId)
                ?: return SoftDeleteComponentOutcome.BuiltInProtected

            val now = Date(System.currentTimeMillis())
            val oldName = type.name
            database.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    componentTypeDao.softDelete(typeId, now)
                    componentValueDao.softDeleteByTypeId(typeId, now)
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

        // ===== @Deprecated shim — A3 =====

        @Deprecated("Use addLexemeWithBuiltInComponent")
        override suspend fun addLexemeWithTranslation(
            wordId: Long,
            dictionaryId: Long,
            translation: TranslationApiEntity,
        ): Long = addLexemeWithBuiltInComponent(
            wordId = wordId,
            dictionaryId = dictionaryId,
            systemKey = BuiltInComponent.TRANSLATION,
            data = TextValues(Primitive.Text(translation.value)),
        )

        @Deprecated("Use updateComponentValue via generic path")
        override suspend fun updateLexemeTranslation(
            id: Long,
            translation: TranslationApiEntity?,
        ): Long? {
            val builtIn = componentTypeDao.getBySystemKey(BuiltInComponent.TRANSLATION.key)
                ?: return null
            val existing = componentValueDao.getForLexemeAndType(id, builtIn.id)
            val now = Date(System.currentTimeMillis())
            return when {
                translation == null -> {
                    if (existing != null) {
                        componentValueDao.delete(existing.id)
                    }
                    id
                }

                existing == null -> {
                    componentValueDao.insert(
                        ComponentValueDb(
                            lexemeId = id,
                            componentTypeId = builtIn.id,
                            value = TextValues(Primitive.Text(translation.value)).toJson(),
                            createdAt = now,
                            updatedAt = now,
                        )
                    )
                    id
                }

                else -> {
                    componentValueDao.update(
                        existing.copy(
                            value = TextValues(Primitive.Text(translation.value)).toJson(),
                            updatedAt = now,
                        )
                    )
                    id
                }
            }
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
