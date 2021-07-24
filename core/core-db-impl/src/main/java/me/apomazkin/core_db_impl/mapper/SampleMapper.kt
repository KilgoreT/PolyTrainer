package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.Sample
import me.apomazkin.core_db_impl.entity.SampleDb

class SampleMapper : Mapper<SampleDb, Sample>() {

    override fun map(value: SampleDb) = Sample(
        value.id ?: -1,
        value.definitionId,
        value.value,
        value.source,
        value.addDate,
        value.changeDate,
    )

    override fun reverseMap(value: Sample) = SampleDb(
        value.id,
        value.definitionId,
        value.value,
        value.source,
        value.addDate,
        value.changeDate,
    )

}