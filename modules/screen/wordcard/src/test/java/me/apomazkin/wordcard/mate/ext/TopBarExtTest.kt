package me.apomazkin.wordcard.mate.ext

import me.apomazkin.wordcard.mate.TopBarState
import me.apomazkin.wordcard.mate.WordCardState
import me.apomazkin.wordcard.mate.WordState
import me.apomazkin.wordcard.mate.hideMenu
import me.apomazkin.wordcard.mate.showMenu
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for showMenu / hideMenu top-bar extensions.
 */
class TopBarExtTest {

    @Test
    fun `showMenu sets isMenuOpen true`() {
        val initial = WordCardState(
            isLoading = false,
            wordState = WordState.NotLoaded,
            topBarState = TopBarState(isMenuOpen = false),
        )

        val result = initial.showMenu()

        assertTrue(result.topBarState.isMenuOpen)
    }

    @Test
    fun `hideMenu sets isMenuOpen false`() {
        val initial = WordCardState(
            isLoading = false,
            wordState = WordState.NotLoaded,
            topBarState = TopBarState(isMenuOpen = true),
        )

        val result = initial.hideMenu()

        assertFalse(result.topBarState.isMenuOpen)
    }
}
