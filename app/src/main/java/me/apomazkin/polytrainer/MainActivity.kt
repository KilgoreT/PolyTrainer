package me.apomazkin.polytrainer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
import me.apomazkin.core_db_api.CoreDbProvider
import me.apomazkin.feature_bottom_menu_api.FeatureBottomMenuApi
import me.apomazkin.feature_bottom_menu_impl.di.FeatureBottomMenuComponent
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var coreDbProvider: CoreDbProvider

    private lateinit var featureBottomMenuApi: FeatureBottomMenuApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        App.getComponent().inject(this)

        featureBottomMenuApi = FeatureBottomMenuComponent.getOrInit(getNavController())
        featureBottomMenuApi.featureBottomMenuNavigator().start()

    }

    private fun getNavController(): NavController {
        return Navigation.findNavController(this, R.id.mainFragmentContainer)
    }

}
