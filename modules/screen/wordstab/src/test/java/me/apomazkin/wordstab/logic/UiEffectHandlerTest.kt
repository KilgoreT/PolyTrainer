package me.apomazkin.wordstab.logic

import kotlinx.coroutines.runBlocking
import me.apomazkin.wordstab.R
import me.apomazkin.wordstab.deps.ScanLauncher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Исполнитель UI-эффектов: запуск камеры и галереи через [ScanLauncher] и
 * сообщение в снекбар.
 */
class UiEffectHandlerTest {

    private class FakeLauncher(private val canOpenCamera: Boolean) : ScanLauncher {
        var cameraLaunches = 0
        var galleryLaunches = 0

        override fun launchCamera(): Boolean {
            cameraLaunches++
            return canOpenCamera
        }

        override fun launchGallery() {
            galleryLaunches++
        }
    }

    private fun run(launcher: ScanLauncher, effect: UiEffect): List<Msg> {
        val sent = mutableListOf<Msg>()
        runBlocking { UiEffectHandler(launcher).runEffect(effect) { sent += it } }
        return sent
    }

    @Test
    fun `TFP-06_1 launch camera opens the camera and sends nothing`() {
        val launcher = FakeLauncher(canOpenCamera = true)

        val sent = run(launcher, UiEffect.LaunchCamera)

        assertEquals(1, launcher.cameraLaunches)
        assertEquals(0, launcher.galleryLaunches)
        assertTrue(sent.isEmpty())
    }

    @Test
    fun `TFP-06_2 launch gallery opens the gallery and sends nothing`() {
        val launcher = FakeLauncher(canOpenCamera = true)

        val sent = run(launcher, UiEffect.LaunchGallery)

        assertEquals(0, launcher.cameraLaunches)
        assertEquals(1, launcher.galleryLaunches)
        assertTrue(sent.isEmpty())
    }

    @Test
    fun `TFP-07_1 TFP-07_2 AWP-53_6 camera that did not open answers CameraUnavailable`() {
        val launcher = FakeLauncher(canOpenCamera = false)

        val sent = run(launcher, UiEffect.LaunchCamera)

        assertEquals(
            listOf<Msg>(Msg.CameraUnavailable(errorRes = R.string.photo_words_error_camera)),
            sent,
        )
    }

    @Test
    fun `notification effect becomes the snackbar message`() {
        val sent = run(
            FakeLauncher(canOpenCamera = true),
            UiEffect.ShowNotification(message = "Слово удалено"),
        )

        assertEquals(
            listOf<Msg>(UiMsg.ShowNotification(message = "Слово удалено", show = true)),
            sent,
        )
    }
}
