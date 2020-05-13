package me.apomazkin.polytrainer

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import me.apomazkin.core_db_api.CoreDbProvider
import me.apomazkin.feature_vocabulary.MockFragment
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var coreDbProvider: CoreDbProvider

    private val fragment = MockFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        App.getComponent().inject(this)

//        coreDbProvider.getCoreDbApi().insert("zhopa1")
        coreDbProvider.getCoreDbApi().insert("pizka1")

        Log.d("###", "123123")

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.mock, fragment)
            .commit()

    }

}
