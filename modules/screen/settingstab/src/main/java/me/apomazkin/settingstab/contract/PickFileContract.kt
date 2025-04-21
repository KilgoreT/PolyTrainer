package me.apomazkin.settingstab.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

class PickFileContract : ActivityResultContract<Array<String>, Uri?>() {
    
    override fun createIntent(context: Context, input: Array<String>): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = if (input.isEmpty()) "*/*" else input[0]
            putExtra(Intent.EXTRA_MIME_TYPES, input)
        }
    }
    
    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == Activity.RESULT_OK) {
            intent?.data
        } else {
            null
        }
    }
}