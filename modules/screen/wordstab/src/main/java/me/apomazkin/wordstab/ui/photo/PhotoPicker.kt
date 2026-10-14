package me.apomazkin.wordstab.ui.photo

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
import java.io.File

private const val AUTHORITY_SUFFIX = ".fileprovider"
private const val PHOTOS_DIR = "photos"

/**
 * IS527: два системных пути к снимку — приложение камеры (`TakePicture`,
 * файл в кеше через FileProvider) и фотопикер (`PickVisualMedia`, без
 * разрешений). Результат — колбэками наружу, дальше — сообщения mate.
 */
@Stable
class PhotoPicker internal constructor(
    private val context: Context,
    private val takePicture: ManagedActivityResultLauncher<Uri, Boolean>,
    private val pickImage: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>,
    private val pendingCameraUri: MutableState<Uri?>,
) {

    /** Нужен `<queries>` на IMAGE_CAPTURE в манифесте, иначе на API 30+ всегда false. */
    fun isCameraAvailable(): Boolean =
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).resolveActivity(context.packageManager) != null

    fun launchCamera() {
        val uri = newPhotoUri(context)
        pendingCameraUri.value = uri
        takePicture.launch(uri)
    }

    fun launchGallery() {
        pickImage.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }
}

@Composable
internal fun rememberPhotoPicker(
    onPicked: (Uri) -> Unit,
    onCancelled: () -> Unit,
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
        if (saved && uri != null) onPicked(uri) else onCancelled()
    }
    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) onPicked(uri) else onCancelled()
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

/** Новый файл под снимок; удаляет его юзкейс после распознавания. */
private fun newPhotoUri(context: Context): Uri {
    val dir = File(context.cacheDir, PHOTOS_DIR).apply { mkdirs() }
    val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)
}
