package me.apomazkin.settingstab.contract

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract


class SendEmailContract : ActivityResultContract<String, Unit>() {
    override fun createIntent(context: Context, input: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            //            data = Uri.parse("mailto:lexeme.app@gmail.com")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("lexeme.app@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, "input subject")
            putExtra(Intent.EXTRA_TEXT, "input text")
        }
    }
    
    override fun parseResult(resultCode: Int, intent: Intent?) {
        // Почтовые клиенты обычно не возвращают результат, просто открываются
    }
}