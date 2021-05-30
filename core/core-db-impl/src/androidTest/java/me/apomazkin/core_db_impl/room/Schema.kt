package me.apomazkin.core_db_impl.room

object Schema {

    const val databaseName = "TestDatabaseName"

    object Word : TableName, ColumnId {
        override val tableName = "words"
        const val columnWord = "word"
        const val columnAddDate = "addDate"
        const val columnChangeDate = "changeDate"
    }

    object Definition : TableName, ColumnId {
        override val tableName = "definitions"
        const val columnDefinition = "definition"
        const val columnWordId = "wordId"
        const val columnWordClass = "wordClass"
        const val columnOptions = "options"
    }

    object WriteQuiz : TableName, ColumnId {
        override val tableName = "writeQuiz"
        const val columnDefinitionId = "definitionId"
        const val columnGrade = "grade"
        const val columnScore = "score"
        const val columnAddDate = "addDate"
        const val columnLastSelectDate = "lastSelectDate"
    }

    object Hint : TableName, ColumnId {
        override val tableName = "hint"
        const val columnDefinitionId = "definitionId"
        const val columnValue = "value"
        const val columnAddDate = "addDate"
        const val columnChangeDate = "changeDate"
    }

}

interface TableName {
    val tableName: String
}

interface ColumnId {
    val columnId: String
        get() = "id"
}