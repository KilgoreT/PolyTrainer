package me.apomazkin.core_db_impl.mapper

abstract class Mapper<T1, T2> {

    abstract fun map(value: T1): T2
    abstract fun reverseMap(value: T2): T1

    fun map(value: List<T1>): List<T2> {
        val result = mutableListOf<T2>()
        value.forEach {
            result.add(map(it))
        }
        return result
    }

    fun reverseMap(value: List<T2>): List<T1> {
        val result = mutableListOf<T1>()
        value.forEach {
            result.add(reverseMap(it))
        }
        return result
    }

}