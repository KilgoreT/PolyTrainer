package me.apomazkin.settingstab.contract

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

class FileShareContract : ActivityResultContract<Uri, Unit>() {
    override fun createIntent(context: Context, input: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, input)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.let {
            Intent.createChooser(it, "Отправить файл через...")
        }
    }
    
    override fun parseResult(resultCode: Int, intent: Intent?) {}
}
