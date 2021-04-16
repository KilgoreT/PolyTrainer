package me.apomazkin.feature_vocabulary_api

interface FeatureVocabularyNavigation {

    fun start()

    fun editWordDialog(wordId: Long, currentValue: String)

    /**
     * Открытие диалога для добавление определения к слову.
     * @param id - wordId - внешний ключ для Word
     */
    fun addDefinitionDialog(id: Long)

    fun closeDialog()
    fun editDefinitionDialog(id: Long)
}
