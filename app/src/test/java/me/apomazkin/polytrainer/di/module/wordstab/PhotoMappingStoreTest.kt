package me.apomazkin.polytrainer.di.module.wordstab

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.wordstab.deps.PhotoMapping
import me.apomazkin.wordstab.deps.PhotoTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Хранилище соответствий по словарю (AWP-06): кодирование, разбор мусора, изоляция ключей. */
class PhotoMappingStoreTest {

    private val prefsProvider = mockk<PrefsProvider>()
    private val store = PhotoMappingStore(prefsProvider = prefsProvider, logger = mockk<LexemeLogger>(relaxed = true))

    private val mapping = PhotoMapping(
        translation = PhotoTarget.Component(1L),
        gloss = PhotoTarget.None,
        examples = PhotoTarget.Component(4L),
        partOfSpeech = PhotoTarget.Component(3L),
    )

    @Test
    fun `AWP-06_2 AWP-06_7 round trip keeps components and explicit none`() {
        val encoded = PhotoMappingStore.encode(mapping)

        assertEquals("translation=1,gloss=none,examples=4,partOfSpeech=3", encoded)
        assertEquals(mapping, PhotoMappingStore.decode(encoded))
    }

    @Test
    fun `garbage missing kind or unknown kind gives null`() {
        assertNull(PhotoMappingStore.decode("translation=abc,gloss=none,examples=4,partOfSpeech=3"))
        assertNull(PhotoMappingStore.decode("translation=1,gloss=none,examples=4"))
        assertNull(PhotoMappingStore.decode("translation=1,gloss=none,examples=4,partOfSpeech=3,group=7"))
        assertNull(PhotoMappingStore.decode(""))
    }

    @Test
    fun `AWP-06_1 no key reads as nothing remembered`() = runTest {
        coEvery { prefsProvider.getStringByRawKey("photo_words_mapping_dict_1") } returns null

        assertNull(store.read(1L))
    }

    @Test
    fun `AWP-06_2 remembered mapping is read back`() = runTest {
        coEvery { prefsProvider.getStringByRawKey("photo_words_mapping_dict_1") } returns "translation=1,gloss=2,examples=4,partOfSpeech=3"

        assertEquals(mapping.copy(gloss = PhotoTarget.Component(2L)), store.read(1L))
    }

    @Test
    fun `AWP-06_8 write stores only the mapping under the dictionary key`() = runTest {
        coEvery { prefsProvider.setStringByRawKey(any(), any()) } just Runs

        store.write(7L, mapping)

        coVerify { prefsProvider.setStringByRawKey("photo_words_mapping_dict_7", "translation=1,gloss=none,examples=4,partOfSpeech=3") }
    }
}
