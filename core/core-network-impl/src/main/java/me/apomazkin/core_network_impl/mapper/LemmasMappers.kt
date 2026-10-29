package me.apomazkin.core_network_impl.mapper

import me.apomazkin.core_network_api.entity.LemmasRequestApiEntity
import me.apomazkin.core_network_api.entity.LemmasResult
import me.apomazkin.core_network_impl.net.LemmasRequestNet
import me.apomazkin.core_network_impl.net.LemmasResponseNet

/* Транспорт ↔ зеркало `lemmasFromTokens`, один в один: запрос — toNet(), ответ — toApiEntity(). */

/** Запрос на провод: языки словаря и токены как есть, в том же порядке (LFT-02). */
internal fun LemmasRequestApiEntity.toNet(): LemmasRequestNet = LemmasRequestNet(
    learningLanguage = learningLanguage,
    translationLanguage = translationLanguage,
    tokens = tokens.map { it.toNet() },
)

/** Тело 200 в зеркало один в один (LFT-14). */
internal fun LemmasResponseNet.toApiEntity(): LemmasResult.Success = LemmasResult.Success(
    text = text,
    translation = translation,
    lexical = lexical,
    proper = proper,
    function = function,
    unparsed = unparsed,
)
