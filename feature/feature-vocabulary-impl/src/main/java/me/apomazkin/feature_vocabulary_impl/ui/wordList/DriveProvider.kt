package me.apomazkin.feature_vocabulary_impl.ui.wordList

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val BACKUP_FOLDER_NAME = ".VocabularyBack"
private const val FOLDER_MIME = "application/vnd.google-apps.folder"
private const val SHEET_MIME = "application/vnd.google-apps.spreadsheet"

class DriveProvider(
    accountCredential: GoogleAccountCredential,
) {

    private val driveService: Drive

    init {
        driveService = Drive
            .Builder(
                NetHttpTransport(),
                JacksonFactory(),
                accountCredential
            )
            .setApplicationName("JJJJJ")
            .build()
    }

    suspend fun checkAppRoot() {
        withContext(Dispatchers.IO) {
            getAppRootFolder() ?: createAppRootFolder()
        }
    }

    private suspend fun getAppRootFolder(): File? {
        val fileList = driveService.Files()
            .list()
            .setFields("nextPageToken, files(id, name)")
            .setPageSize(10)
            .execute()

        return fileList.files.firstOrNull { it.name == BACKUP_FOLDER_NAME }
    }

    private suspend fun createAppRootFolder() {
        val rootAppFolder = File()
        rootAppFolder.name = BACKUP_FOLDER_NAME
//        rootAppFolder.parents = Collections.singletonList("appDataFolder")
        rootAppFolder.mimeType = FOLDER_MIME
        driveService.Files().create(rootAppFolder).execute()
    }

    suspend fun getAppRootFiles(): List<File> {
        return withContext(Dispatchers.IO) {
            val fileList = driveService.Files()
                .list()
//                    .setSpaces("appDataFolder")
                .setFields("nextPageToken, files(id, name, mimeType, parents, modifiedTime)")
                .setPageSize(100)
                .execute()
            fileList.files.filter { it.mimeType == SHEET_MIME }
        }
    }

    suspend fun getSheetFiles(): List<File> {
        return withContext(Dispatchers.IO) {
            val fileList = driveService.Files()
                .list()
                .setFields("nextPageToken, files(id, name, mimeType, parents, modifiedTime)")
                .setPageSize(100)
                .execute()
            fileList.files.filter { it.mimeType == SHEET_MIME }
        }
    }

    suspend fun moveFileFromRootToAppFolder(name: String) {
        withContext(Dispatchers.IO) {
            getAppRootFolder()?.let { appFolder ->
                getSheetFiles().firstOrNull { it.name == name }?.let {

                    val previousParents = StringBuilder()
                    for (parent in it.parents) {
                        previousParents.append(parent)
                        previousParents.append(',')
                    }

                    driveService.Files().update(it.id, null)
                        .setAddParents(appFolder.id)
                        .setRemoveParents(previousParents.toString())
                        .setFields("id, parents")
                        .execute()
                }
            }
        }
    }
}