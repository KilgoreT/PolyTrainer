package me.apomazkin.polytrainer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.apomazkin.core_db.CoreDb
import me.apomazkin.core_db_api.CoreDBApi

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val qqq = CoreDb()
        val qqqq = CoreDBApi()
    }
}
