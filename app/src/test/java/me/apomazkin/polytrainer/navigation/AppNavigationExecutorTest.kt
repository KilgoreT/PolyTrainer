package me.apomazkin.polytrainer.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import io.github.kilgoret.mate.navigation.NavCommand
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Исполнитель навигации: результат экрана лексем-кандидатов (AWP-23). Библиотека
 * знает только `push` и `pop`, поэтому счёт возвращается псевдоэкраном.
 */
class AppNavigationExecutorTest {

    private val root = mockk<NavHostController>(relaxed = true)
    private val tabs = mockk<NavHostController>(relaxed = true)
    private val executor = AppNavigationExecutor(logger = mockk(relaxed = true))

    @Before
    fun setUp() {
        executor.bind(rootNavController = root, tabsNavController = tabs, finishApp = {})
    }

    @Test
    fun `AWP-23_1 the count goes to the entry under the scan screen, then the screen is popped`() {
        val handle = SavedStateHandle()
        val below = mockk<NavBackStackEntry> { every { savedStateHandle } returns handle }
        every { tabs.previousBackStackEntry } returns below

        executor.execute(NavCommand.Push(AppScreen.ScanCandidatesAdded(lexemeCount = 3, wordCount = 2)))

        assertArrayEquals(longArrayOf(3L, 2L), handle.get<LongArray>(SCAN_CANDIDATES_ADDED))
        verify(exactly = 1) { tabs.popBackStack() }
    }

    @Test
    fun `AWP-23_1 the count is read back as it was written`() {
        val written = AppScreen.ScanCandidatesAdded(lexemeCount = 17, wordCount = 4)

        assertArrayEquals(longArrayOf(17L, 4L), written.toSavedState())
        assertEquals(written, written.toSavedState().toScanCandidatesAdded())
    }

    @Test
    fun `AWP-23_1 without the tab entry under the screen the screen is still popped`() {
        every { tabs.previousBackStackEntry } returns null

        executor.execute(NavCommand.Push(AppScreen.ScanCandidatesAdded(lexemeCount = 1, wordCount = 1)))

        verify(exactly = 1) { tabs.popBackStack() }
    }
}
