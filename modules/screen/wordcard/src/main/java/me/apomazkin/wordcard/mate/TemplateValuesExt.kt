package me.apomazkin.wordcard.mate

import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.TemplateValues
import me.apomazkin.lexeme.TextValues

/** G1: текст из TemplateValues (null если не текстовый шаблон). */
fun TemplateValues.asText(): String? = (this as? TextValues)?.value?.value

/** G2: TemplateValues из строки. */
fun textValuesOf(text: String): TemplateValues = TextValues(Primitive.Text(text))
