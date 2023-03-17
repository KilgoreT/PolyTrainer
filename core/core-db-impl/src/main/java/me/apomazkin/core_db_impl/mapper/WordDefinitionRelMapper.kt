package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.Term
import me.apomazkin.core_db_api.entity.TermMate
import me.apomazkin.core_db_impl.entity.WordDefinitionRel

fun WordDefinitionRel.toAppData() = Term(
    word = this.wordDb.toAppEntity(),
    definitionList = definitionSampleRelList.toAppEntity()
)

fun WordDefinitionRel.toMateApp() = TermMate(
    word = this.wordDb.toAppEntity(),
    defList = definitionSampleRelList.toMateApp()
)