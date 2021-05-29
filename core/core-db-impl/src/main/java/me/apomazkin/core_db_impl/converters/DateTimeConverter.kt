package me.apomazkin.core_db_impl.converters

import androidx.room.TypeConverter
import java.util.*

class DateTimeConverter {

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    @TypeConverter
    fun timestampToDate(timestamp: Long?): Date? = timestamp?.let { Date(it) }

}