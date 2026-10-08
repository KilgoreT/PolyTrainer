package me.apomazkin.polytrainer.di.module.textrecognition

import android.content.Context
import dagger.Module
import dagger.Provides
import me.apomazkin.textrecognition.MlKitTextRecognizer
import me.apomazkin.textrecognition.TextRecognizer
import javax.inject.Singleton

@Module
class TextRecognizerModule {

    /** IS527: один клиент ML Kit на процесс — его создание дорогое. */
    @Singleton
    @Provides
    fun provideTextRecognizer(context: Context): TextRecognizer = MlKitTextRecognizer(context)
}
