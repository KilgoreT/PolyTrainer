package me.apomazkin.lexeme

/**
 * Типизированные значения примитивов компонента.
 *
 * Variants:
 * - [Text] — однострочный / многострочный текст;
 * - [Image] — URI ссылка на изображение (локальный uri/uri assets);
 * - [Color] — hex-представление цвета.
 *
 * Расширяется при появлении новых примитивов; compile-time exhaustive `when`
 * на read/write path.
 */
sealed interface Primitive {
    data class Text(val value: String) : Primitive
    data class Image(val uri: String) : Primitive
    data class Color(val hex: String) : Primitive
}
