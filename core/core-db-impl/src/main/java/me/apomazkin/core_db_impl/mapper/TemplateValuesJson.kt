package me.apomazkin.core_db_impl.mapper

import me.apomazkin.lexeme.CaptionedTextValues
import me.apomazkin.lexeme.ChoiceValues
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
 * Single-field шаблоны (TEXT/IMAGE) хранят единственное поле `"value"`.
 * IS491: CAPTIONED_TEXT — первый multi-field шаблон: поля `"text"` + `"caption"`;
 * caption опционален — при null ключ `"caption"` в envelope опускается.
 *
 * Fail-soft контракт (aspect `forward_compat_unknown`):
 *  - malformed JSON → null + Crashlytics-лог.
 *  - unknown primitive type → null + лог.
 *  - schema-mismatch (template ждёт text, json содержит image) → null + лог.
 *  - IS491, исключение для опционального поля: битый `caption` НЕ роняет значение —
 *    парсится как caption=null + лог (text обязателен, его сбой → null как обычно).
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

    // IS486: payload CHOICE живёт в колонке option_id, JSON — пустой envelope (value NOT NULL).
    is ChoiceValues -> JSONObject().put("fields", JSONObject()).toString()

    // IS491: два именованных поля; null caption → ключ опускается.
    is CaptionedTextValues -> JSONObject().apply {
        put(
            "fields",
            JSONObject().apply {
                put(
                    "text",
                    JSONObject().apply {
                        put("type", "text")
                        put("value", text.value)
                    }
                )
                caption?.let {
                    put(
                        "caption",
                        JSONObject().apply {
                            put("type", "text")
                            put("value", it.value)
                        }
                    )
                }
            }
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
    when (template) {
        ComponentTemplate.TEXT -> parseTextPrimitive(fields.getJSONObject("value"), logger)
            ?.let { TextValues(it) }

        ComponentTemplate.IMAGE -> {
            val valueObj = fields.getJSONObject("value")
            when (val type = valueObj.getString("type")) {
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

        // IS486: значение CHOICE не парсится из JSON — payload в option_id;
        // вызов этой функции для CHOICE — ошибка call-site (fail-soft).
        ComponentTemplate.CHOICE -> {
            logger.e(
                tag = TEMPLATE_VALUES_JSON_TAG,
                message = "CHOICE value is stored via option_id, not JSON — wrong call-site"
            )
            null
        }

        // IS491: text обязателен (сбой → null); caption опционален —
        // отсутствующий ключ = null, битый caption = null + лог, text выживает.
        ComponentTemplate.CAPTIONED_TEXT -> {
            val text = parseTextPrimitive(fields.getJSONObject("text"), logger)
            if (text == null) {
                null
            } else {
                val caption = if (fields.has("caption")) {
                    parseTextPrimitive(fields.getJSONObject("caption"), logger)
                } else {
                    null
                }
                CaptionedTextValues(text = text, caption = caption)
            }
        }
    }
} catch (e: JSONException) {
    logger.e(tag = TEMPLATE_VALUES_JSON_TAG, message = "malformed JSON: ${e.message}")
    null
}

/** Текстовое поле envelope → [Primitive.Text]; не-text тип → null + лог. */
private fun parseTextPrimitive(
    fieldObj: JSONObject,
    logger: LexemeLogger,
): Primitive.Text? = when (val type = fieldObj.getString("type")) {
    "text" -> Primitive.Text(fieldObj.getString("value"))
    else -> {
        logger.e(
            tag = TEMPLATE_VALUES_JSON_TAG,
            message = "schema mismatch: expected text field, json type='$type'"
        )
        null
    }
}

private const val TEMPLATE_VALUES_JSON_TAG = "TemplateValuesJson"
