package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.Term
import me.apomazkin.core_db_impl.entity.WordDefinitionRel

fun WordDefinitionRel.toAppData() = Term(
    word = this.wordDb.toAppEntity(),
    definitionList = definitionSampleRelList.toAppEntity()
)