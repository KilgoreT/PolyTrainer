package me.apomazkin.polytrainer.di.module.settingstab

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.settingstab.deps.SettingsTabUseCase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

//TODO kilg 20.04.2025 21:02 нехорошо провайдить контекст в use case.
// Варианты:
// 1. передавать контекст как параметр в методы (та же жопа, только снизу)
// 2. выделить documentfile api в отдельный модуль и провайдить его в use case
private const val MIME_TYPE = "application/octet-stream"

class SettingsTabUseCaseImpl @Inject constructor(
    private val context: Context,
    private val dbInstance: CoreDbApi.DbInstance,
) : SettingsTabUseCase {
    
    override suspend fun exportData(uri: Uri): Uri {
        
        return withContext(Dispatchers.IO) {
            val info = dbInstance.getDbInfo()
            async(Dispatchers.IO) {
                dbInstance.closeDatabase()
            }.await()
            val dbFilePath = info.path
            val dbFile = File(dbFilePath)
            if (!dbFile.exists()) {
                throw IllegalStateException("Database file does not exist")
            }
            val pickedDir = DocumentFile.fromTreeUri(context, uri)
            if (pickedDir == null || !pickedDir.isDirectory) {
                throw IllegalArgumentException("Invalid destination folder")
            }
            val dbVersion = info.version
            val exportFile = pickedDir
                .createFile(MIME_TYPE, "LexemeDb$dbVersion.sqlite")
                ?: throw IOException("Unable to create export file")
            
            async {
                context.contentResolver
                    .openOutputStream(exportFile.uri)
                    ?.use { outputStream ->
                        FileInputStream(dbFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
            }.await()
            
            return@withContext exportFile.uri
        }
    }
    
    override suspend fun importData(uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                
                val pickedFile = DocumentFile.fromSingleUri(context, uri)
                if (pickedFile == null || !pickedFile.isFile) {
                    return@withContext false
                }
                
                val dbPath = dbInstance.getDbInfo().path
                val dbFile = File(dbPath)
                
                async(Dispatchers.IO) {
                    dbInstance.closeDatabase()
                }.await()
                
                context.contentResolver
                    .openInputStream(uri)
                    ?.use { inputStream ->
                        FileOutputStream(dbFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    ?: return@withContext false
                
                return@withContext true
            } catch (e: Exception) {
                return@withContext false
            } finally {
                async(Dispatchers.IO) {
                    dbInstance.openDatabase()
                }.await()
            }
        }
    }
}