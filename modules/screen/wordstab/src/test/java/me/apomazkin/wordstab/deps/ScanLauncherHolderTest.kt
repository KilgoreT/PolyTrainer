package me.apomazkin.wordstab.deps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Держатель запуска с поздней привязкой: без привязки камеры нет, галерея молчит. */
class ScanLauncherHolderTest {

    private class FakeLauncher(private val canOpenCamera: Boolean = true) : ScanLauncher {
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

    @Test
    fun `unbound holder cannot open the camera and ignores the gallery`() {
        val holder = ScanLauncherHolder()

        assertFalse(holder.launchCamera())
        holder.launchGallery()
    }

    @Test
    fun `bound holder passes both launches and the camera answer to the launcher`() {
        val holder = ScanLauncherHolder()
        val launcher = FakeLauncher()
        holder.bind(launcher)

        assertTrue(holder.launchCamera())
        holder.launchGallery()

        assertEquals(1, launcher.cameraLaunches)
        assertEquals(1, launcher.galleryLaunches)
    }

    @Test
    fun `bound holder returns false when the launcher cannot open the camera`() {
        val holder = ScanLauncherHolder()
        holder.bind(FakeLauncher(canOpenCamera = false))

        assertFalse(holder.launchCamera())
    }

    @Test
    fun `unbind returns the holder to the unbound state`() {
        val holder = ScanLauncherHolder()
        val launcher = FakeLauncher()
        holder.bind(launcher)

        holder.unbind()

        assertFalse(holder.launchCamera())
        holder.launchGallery()
        assertEquals(0, launcher.cameraLaunches)
        assertEquals(0, launcher.galleryLaunches)
    }

    @Test
    fun `bind of a new launcher replaces the previous one`() {
        val holder = ScanLauncherHolder()
        val first = FakeLauncher()
        val second = FakeLauncher()
        holder.bind(first)

        holder.bind(second)
        holder.launchCamera()

        assertEquals(0, first.cameraLaunches)
        assertEquals(1, second.cameraLaunches)
    }
}
