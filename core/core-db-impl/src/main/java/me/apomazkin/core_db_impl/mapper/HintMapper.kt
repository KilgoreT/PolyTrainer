package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.Hint
import me.apomazkin.core_db_impl.entity.HintDb

class HintMapper : Mapper<HintDb, Hint>() {

    override fun map(value: HintDb) = Hint(
        value.id ?: -1,
        value.definitionId,
        value.value,
        value.addDate,
        value.changeDate,
    )

    override fun reverseMap(value: Hint) = HintDb(
        id = value.id,
        definitionId = value.definitionId,
        value = value.value,
        addDate = value.addDate,
        changeDate = value.changeDate,
    )

}