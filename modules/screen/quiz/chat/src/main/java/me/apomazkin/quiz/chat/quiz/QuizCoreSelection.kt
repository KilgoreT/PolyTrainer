package me.apomazkin.quiz.chat.quiz

import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.toRef

/**
 * Ядра, которыми спрашивает раунд: сохранённый выбор, пересечённый с
 * кандидатами словаря; пустое пересечение (ничего не сохранено, выбор
 * устарел) — все кандидаты.
 *
 * Единственное место этого правила: меню показывает галки, а порция
 * фильтруется по одному и тому же результату, иначе они разойдутся.
 * Порядок — порядок кандидатов (по `position` словаря).
 */
fun resolveQuizCores(
    candidates: List<ComponentType>,
    selected: Set<ComponentTypeRef>,
): List<ComponentType> = candidates
    .filter { it.toRef() in selected }
    .ifEmpty { candidates }
