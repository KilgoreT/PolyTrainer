package me.apomazkin.feature_vocabulary_impl.ui.wordList

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

class SignInContract : ActivityResultContract<GoogleSignInOptions, GoogleSignInAccount?>() {

    override fun createIntent(context: Context, input: GoogleSignInOptions): Intent =
        GoogleSignIn.getClient(context, input).signInIntent

    override fun parseResult(resultCode: Int, intent: Intent?): GoogleSignInAccount? {

        val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(intent)

        try {
            return task.getResult(ApiException::class.java)
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.d("###", "signInResult:failed code=" + e.statusCode)
            Log.d("###", "onActivityResult: ${e.message}")
//                updateUI(null)
            return null
        }
    }
}