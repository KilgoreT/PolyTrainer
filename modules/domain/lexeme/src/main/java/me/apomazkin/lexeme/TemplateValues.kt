package me.apomazkin.lexeme

/**
 * Sealed-замена `ComponentValueData`. Per-template variant — compile-time exhaustive
 * `when` на read/write path.
 *
 * MVP — [TextValues] + [ImageValues]. Composite variants (`QuoteWithSourceValues`,
 * `ImageWithCaptionValues`, ...) добавляются как новые data class : TemplateValues
 * в будущих фичах.
 *
 * См. `concept/typed_views.md` § Domain.
 */
sealed interface TemplateValues

data class TextValues(
    val value: Primitive.Text,
) : TemplateValues

data class ImageValues(
    val value: Primitive.Image,
) : TemplateValues

/**
 * IS486: значение CHOICE — выбранная опция.
 * [optionId] — id строки `component_options`; сама опция и есть значение,
 * текст лейбла не копируется.
 */
data class ChoiceValues(
    val optionId: Long,
) : TemplateValues
