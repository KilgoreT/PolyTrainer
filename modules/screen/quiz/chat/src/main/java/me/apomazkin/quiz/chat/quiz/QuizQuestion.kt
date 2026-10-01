package me.apomazkin.quiz.chat.quiz

import androidx.compose.ui.text.AnnotatedString

/**
 * Вопрос раунда в структурном виде: лента собирает пузырь из частей, а
 * не из размеченной строки.
 *
 * [header] — имя ядра, которым спрашивают («Перевод», имя
 *   пользовательского ядра); в пузыре идёт отдельной верхней строкой.
 * [badge] — короткая метка атрибута лексемы (часть речи) слева от
 *   значения; null — без метки.
 * [value] — значение ядра: по нему надо вспомнить слово.
 * [debugHeader] — служебная шапка с грейдами, только в debug-режиме.
 */
data class QuizQuestion(
    val header: String,
    val badge: String?,
    val value: String,
    val debugHeader: AnnotatedString?,
)
