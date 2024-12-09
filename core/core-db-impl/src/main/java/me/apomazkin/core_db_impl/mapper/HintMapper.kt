package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.Hint
import me.apomazkin.core_db_impl.entity.HintDb

class HintMapper : Mapper<HintDb, Hint>() {

    override fun map(value: HintDb) = Hint(
        id = value.id ?: -1,
        lexemeId = value.lexemeId,
        value = value.value,
        addDate = value.addDate,
        changeDate = value.changeDate,
    )

    override fun reverseMap(value: Hint) = HintDb(
        id = value.id,
        lexemeId = value.lexemeId,
        value = value.value,
        addDate = value.addDate,
        changeDate = value.changeDate,
    )

}