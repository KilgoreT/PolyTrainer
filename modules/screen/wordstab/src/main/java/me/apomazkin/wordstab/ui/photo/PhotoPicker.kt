package me.apomazkin.wordstab.ui.photo

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.wordstab.LogTags
import me.apomazkin.wordstab.deps.ScanLauncher
import java.io.File
import java.io.IOException

private const val AUTHORITY_SUFFIX = ".fileprovider"
private const val PHOTOS_DIR = "photos"

/**
 * Два системных пути к снимку — приложение камеры (`TakePicture`, файл в
 * кеше через FileProvider) и фотопикер (`PickVisualMedia`, без разрешений).
 * Запускает их эффект вкладки через [ScanLauncher]; снимок уходит колбэком
 * наружу, дальше — сообщение mate. Отмена в камере или галерее ничего не
 * шлёт: диалог выбора остаётся как был (TFP-18).
 */
@Stable
class PhotoPicker internal constructor(
    private val context: Context,
    private val takePicture: ManagedActivityResultLauncher<Uri, Boolean>,
    private val pickImage: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>,
    private val pendingCameraUri: MutableState<Uri?>,
) : ScanLauncher {

    /** Нужен `<queries>` на IMAGE_CAPTURE в манифесте, иначе на API 30+ всегда false. */
    fun isCameraAvailable(): Boolean =
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).resolveActivity(context.packageManager) != null

    /**
     * Камеру могли отключить между проверкой и запуском; SecurityException —
     * если CAMERA объявит чужая библиотека. Файл под снимок не создался (нет
     * места) — тоже false, камера не открывается (TFP-07.2).
     */
    override fun launchCamera(): Boolean {
        val uri = newPhotoUri(context) ?: return false
        pendingCameraUri.value = uri
        val isLaunched = try {
            takePicture.launch(uri)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
        if (!isLaunched) {
            pendingCameraUri.value = null
            discardPhoto(context, uri)
        }
        return isLaunched
    }

    override fun launchGallery() {
        pickImage.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }
}

/**
 * Регистрирует launcher'ы камеры и галереи.
 * @param onPicked снимок получен: файл камеры или картинка галереи, доступ к
 * которой уже закреплён.
 * @param logger в него пишется провал закрепления доступа.
 */
@Composable
internal fun rememberPhotoPicker(
    onPicked: (Uri) -> Unit,
    logger: LexemeLogger,
): PhotoPicker {
    val context = LocalContext.current
    // Uri снимка переживает пересоздание Activity: камера — отдельный
    // процесс, наш могут убить, пока она открыта.
    val pendingCameraUri = rememberSaveable { mutableStateOf<Uri?>(null) }
    val takePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { saved ->
        val uri = pendingCameraUri.value
        pendingCameraUri.value = null
        when {
            uri == null -> Unit
            saved -> onPicked(uri)
            else -> discardPhoto(context, uri)
        }
    }
    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            keepReadAccess(context, uri, logger)
            onPicked(uri)
        }
    }
    return remember(context, takePicture, pickImage) {
        PhotoPicker(
            context = context,
            takePicture = takePicture,
            pickImage = pickImage,
            pendingCameraUri = pendingCameraUri,
        )
    }
}

/**
 * Новый пустой файл под снимок; null — файл не создался. Файл снимка камеры
 * удаляет юзкейс после распознавания, незаполненный — [discardPhoto].
 */
private fun newPhotoUri(context: Context): Uri? {
    val dir = File(context.cacheDir, PHOTOS_DIR)
    val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
    val isCreated = try {
        dir.mkdirs()
        file.createNewFile()
        file.exists()
    } catch (e: IOException) {
        false
    }
    if (!isCreated) return null
    return FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)
}

/** Убрать пустой файл под снимок, который камера так и не заполнила. */
private fun discardPhoto(context: Context, uri: Uri) {
    runCatching { context.contentResolver.delete(uri, null, null) }
}

/**
 * Закрепить доступ к картинке галереи до отправки сообщения: он должен
 * пережить гибель процесса, пока экран читает снимок (TFP-22.4). Не вышло —
 * в лог, экран покажет ошибку чтения.
 */
private fun keepReadAccess(context: Context, uri: Uri, logger: LexemeLogger) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }.onFailure { error ->
        logger.w(
            tag = LogTags.VOCAB,
            message = "keepReadAccess failed: uri=$uri",
            throwable = error,
        )
    }
}
