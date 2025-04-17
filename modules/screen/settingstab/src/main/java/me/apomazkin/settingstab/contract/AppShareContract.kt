package me.apomazkin.settingstab.contract

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

class AppShareContract : ActivityResultContract<String, Unit>() {
    override fun createIntent(context: Context, input: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, input)
        }.let {
            Intent.createChooser(it, "Поделиться через...")
        }
    }
    
    override fun parseResult(resultCode: Int, intent: Intent?) {}
}
