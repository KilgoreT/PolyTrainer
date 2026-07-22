package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_impl.entity.ComponentTypeDb
import me.apomazkin.core_db_impl.entity.toApiEntity
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.DependencyTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

/**
 * IS486 фаза 1, зона D3: `ComponentTypeDb.toApiEntity` — XOR depends-ссылок → [DependencyTarget].
 * phase1_plan.md § Зона D.
 *
 * Состояния (spec §10): обе NULL → цель «лексема»; заполнена одна → зависимый
 * от компонента / от опции; обе → нелегально (fail-soft null).
 */
class ComponentTypeDbMapperTest {

    private val now = Date(0L)

    private fun db(
        core: Boolean = false,
        enabled: Boolean = true,
        dependsOnTypeId: Long? = null,
        dependsOnOptionId: Long? = null,
        templateKey: String = "text",
    ) = ComponentTypeDb(
        id = 1L,
        systemKey = null,
        dictionaryId = 1L,
        name = "type",
        templateKey = templateKey,
        position = 0,
        core = core,
        enabled = enabled,
        dependsOnTypeId = dependsOnTypeId,
        dependsOnOptionId = dependsOnOptionId,
        createdAt = now,
        updatedAt = now,
    )

    @Test
    fun `both refs null maps to Lexeme target`() {
        val api = db().toApiEntity()

        assertEquals(DependencyTarget.Lexeme, api?.dependsOn)
    }

    @Test
    fun `type ref maps to Component target`() {
        val api = db(dependsOnTypeId = 5L).toApiEntity()

        assertEquals(DependencyTarget.Component(ComponentTypeId(5L)), api?.dependsOn)
    }

    @Test
    fun `option ref maps to Option target`() {
        val api = db(dependsOnOptionId = 7L).toApiEntity()

        assertEquals(DependencyTarget.Option(7L), api?.dependsOn)
    }

    @Test
    fun `both refs filled is illegal - fail-soft null`() {
        val api = db(dependsOnTypeId = 5L, dependsOnOptionId = 7L).toApiEntity()

        assertNull(api)
    }

    @Test
    fun `core and enabled are mapped`() {
        val api = db(core = true, enabled = false).toApiEntity()

        assertEquals(true, api?.core)
        assertEquals(false, api?.enabled)
    }

    @Test
    fun `unknown templateKey still fail-soft null - regression`() {
        val api = db(templateKey = "unknown").toApiEntity()

        assertNull(api)
    }
}
