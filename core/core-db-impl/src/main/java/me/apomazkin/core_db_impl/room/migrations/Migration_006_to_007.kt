package me.apomazkin.core_db_impl.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val migration_6_7 = object : Migration(6, 7) {
    
    override fun migrate(db: SupportSQLiteDatabase) {
        
        // 1. Получаем все дублирующиеся numericCode
        val cursor = db.query(
            """
        SELECT numericCode
        FROM languages
        GROUP BY numericCode
        HAVING COUNT(*) > 1
    """
        )
        
        val duplicateCodes = mutableListOf<Int>()
        
        while (cursor.moveToNext()) {
            duplicateCodes.add(cursor.getInt(0))
        }
        cursor.close()
        
        // 2. Проходимся по каждому дубликату
        for (code in duplicateCodes) {
            val duplicatesCursor = db.query(
                """
            SELECT rowid
            FROM languages
            WHERE numericCode = $code
        """
            )
            
            val rowIds = mutableListOf<Long>()
            while (duplicatesCursor.moveToNext()) {
                rowIds.add(duplicatesCursor.getLong(0))
            }
            duplicatesCursor.close()
            
            // 3. Первую запись оставляем как есть, остальные — меняем
            for ((index, rowId) in rowIds.drop(1).withIndex()) {
                val newCode = code + (index + 1) * 7
                db.execSQL(
                    """
                UPDATE languages
                SET numericCode = $newCode
                WHERE rowid = $rowId
            """
                )
            }
        }
        
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_languages_numericCode ON languages (numericCode)")
    }
}