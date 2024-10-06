package me.apomazkin.polytrainer.logger

import android.util.Log
import me.apomazkin.ui.logger.LexemeLogger
import javax.inject.Inject

class LexemeLoggerImpl @Inject constructor() : LexemeLogger {
    override fun log(tag: String, message: String) {
        Log.d(tag, message)
    }
}