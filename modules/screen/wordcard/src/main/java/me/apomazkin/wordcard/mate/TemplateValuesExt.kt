package me.apomazkin.wordcard.mate

import me.apomazkin.lexeme.CaptionedTextValues
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.TemplateValues
import me.apomazkin.lexeme.TextValues

/** G1: текст из TemplateValues (null если шаблон текстом не выражается). */
fun TemplateValues.asText(): String? =
    when (this) {
        is TextValues -> value.value
        is CaptionedTextValues -> text.value // IS491: основной text — и есть «текст» значения
        else -> null
    }

/** IS491: подпись captioned-значения (null у остальных шаблонов и при отсутствии). */
fun TemplateValues.asCaption(): String? = (this as? CaptionedTextValues)?.caption?.value

/** G2: TemplateValues из строки. */
fun textValuesOf(text: String): TemplateValues = TextValues(Primitive.Text(text))

/** IS491: TemplateValues по шаблону — caption используется только captioned_text. */
fun templateValuesOf(
    template: ComponentTemplate,
    text: String,
    caption: String? = null,
): TemplateValues =
    when (template) {
        ComponentTemplate.CAPTIONED_TEXT ->
            CaptionedTextValues(
                text = Primitive.Text(text),
                caption = caption?.takeIf { it.isNotBlank() }?.let { Primitive.Text(it) },
            )
        else -> TextValues(Primitive.Text(text))
    }
