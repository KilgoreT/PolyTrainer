package me.apomazkin.dictionary.form

import me.apomazkin.dictionary.DictionaryUseCase
import io.github.kilgoret.mate.MateEffectHandler
import javax.inject.Inject

/**
 * Исполнитель эффектов семейства [FlagFilterEffect]: разовое намерение
 * «обновить строку фильтра флагов» в use case. Сам живой список флагов
 * приходит подпиской [DictionaryFormSub.Flags] через
 * [DictionaryFormSubHandler] — обновлённый фильтр переэмитит поток.
 */
class FlagFilterEffectHandler @Inject constructor(
    private val dictionaryUseCase: DictionaryUseCase,
) : MateEffectHandler<DictionaryFormMsg, FlagFilterEffect> {

    override val effectFamily = FlagFilterEffect::class

    override suspend fun runEffect(
        effect: FlagFilterEffect,
        consumer: (DictionaryFormMsg) -> Unit,
    ) {
        when (effect) {
            is FlagFilterEffect.FilterFlags -> dictionaryUseCase.updateFilter(effect.query)
        }
    }
}
