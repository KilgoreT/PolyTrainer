package me.apomazkin.core_db_impl.mapper

import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ImageValues
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.TemplateValues
import me.apomazkin.lexeme.TextValues
import me.apomazkin.logger.LexemeLogger
import org.json.JSONException
import org.json.JSONObject

/**
 * JSON envelope для M13: `{"fields": {"<fieldName>": {"type": "<primType>", "<typedPayload>": ...}}}`.
 *
 * - `"text"`  → `{"type":"text","value":"..."}`
 * - `"image"` → `{"type":"image","uri":"..."}`
 * - `"color"` → `{"type":"color","hex":"..."}` (зарезервирован под `Primitive.Color`).
 *
 * MVP M13 поддерживает только [TextValues] и [ImageValues]. Composite (multi-field)
 * values добавляются в будущих фичах без breaking changes в envelope.
 *
 * Fail-soft контракт (aspect `forward_compat_unknown`):
 *  - malformed JSON → null + Crashlytics-лог.
 *  - unknown primitive type → null + лог.
 *  - schema-mismatch (template ждёт text, json содержит image) → null + лог.
 *
 * Caller-mapper ([me.apomazkin.core_db_impl.entity.toApiEntity]) обрабатывает `null`
 * как skip компонента.
 */
fun TemplateValues.toJson(): String = when (this) {
    is TextValues -> JSONObject().apply {
        put(
            "fields",
            JSONObject().put(
                "value",
                JSONObject().apply {
                    put("type", "text")
                    put("value", value.value)
                }
            )
        )
    }.toString()

    is ImageValues -> JSONObject().apply {
        put(
            "fields",
            JSONObject().put(
                "value",
                JSONObject().apply {
                    put("type", "image")
                    put("uri", value.uri)
                }
            )
        )
    }.toString()
}

fun parseTemplateValues(
    json: String,
    template: ComponentTemplate,
    logger: LexemeLogger,
): TemplateValues? = try {
    val root = JSONObject(json)
    val fields = root.getJSONObject("fields")
    val valueObj = fields.getJSONObject("value")
    val type = valueObj.getString("type")
    when (template) {
        ComponentTemplate.TEXT -> when (type) {
            "text" -> TextValues(Primitive.Text(valueObj.getString("value")))
            else -> {
                logger.e(
                    tag = TEMPLATE_VALUES_JSON_TAG,
                    message = "schema mismatch: template=TEXT, json type='$type'"
                )
                null
            }
        }

        ComponentTemplate.IMAGE -> when (type) {
            "image" -> ImageValues(Primitive.Image(valueObj.getString("uri")))
            else -> {
                logger.e(
                    tag = TEMPLATE_VALUES_JSON_TAG,
                    message = "schema mismatch: template=IMAGE, json type='$type'"
                )
                null
            }
        }
    }
} catch (e: JSONException) {
    logger.e(tag = TEMPLATE_VALUES_JSON_TAG, message = "malformed JSON: ${e.message}")
    null
}

private const val TEMPLATE_VALUES_JSON_TAG = "TemplateValuesJson"
