package me.apomazkin.core_db_impl.mapper

import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTypeRef
import org.json.JSONArray
import org.json.JSONObject

/**
 * JSON-сериализация `List<ComponentTypeRef>` для хранения в
 * `quiz_configs.component_refs`. Симметрична с `json_object()` в миграции.
 *
 * Формат:
 * - BuiltIn(BuiltInComponent.TRANSLATION) → `{"type":"builtin","key":"translation"}`
 * - UserDefined("Definition")            → `{"type":"user","name":"Definition"}`
 *
 * Discriminator `type` обязателен — нет внешнего ключа выбора variant.
 *
 * Defensive parser: corrupt JSON / unknown discriminator / unknown enum key
 * → `emptyList()` (не crash). Quiz UseCase реагирует на пустой config UX-блоком.
 */
fun List<ComponentTypeRef>.toJson(): String {
    val arr = JSONArray()
    forEach { ref ->
        val obj = JSONObject()
        when (ref) {
            is ComponentTypeRef.BuiltIn -> {
                obj.put("type", "builtin")
                obj.put("key", ref.key.key)
            }

            is ComponentTypeRef.UserDefined -> {
                obj.put("type", "user")
                obj.put("name", ref.name)
            }
        }
        arr.put(obj)
    }
    return arr.toString()
}

fun String.toComponentTypeRefList(): List<ComponentTypeRef> = try {
    val arr = JSONArray(this)
    List(arr.length()) { i ->
        val obj = arr.getJSONObject(i)
        when (val type = obj.getString("type")) {
            "builtin" -> {
                val keyStr = obj.getString("key")
                val key = BuiltInComponent.fromKey(keyStr)
                    ?: error("Unknown BuiltInComponent key: $keyStr")
                ComponentTypeRef.BuiltIn(key)
            }

            "user" -> ComponentTypeRef.UserDefined(obj.getString("name"))
            else -> error("Unknown ComponentTypeRef discriminator: $type")
        }
    }
} catch (_: Exception) {
    emptyList()
}
