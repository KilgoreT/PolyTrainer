package me.apomazkin.polytrainer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
import me.apomazkin.core_db_api.CoreDbProvider
import me.apomazkin.feature_bottom_menu_impl.di.FeatureBottomMenuComponent
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var coreDbProvider: CoreDbProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        App.getComponent().inject(this)

//        coreDbProvider.getCoreDbApi().insert("zhopa1")
//        coreDbProvider.getCoreDbApi().insert("pizka1")

        val api = FeatureBottomMenuComponent.getOrInit(getNavController())
        api.featureBottomMenuNavigator().start()
    }

    private fun getNavController(): NavController {
        return Navigation.findNavController(this, R.id.mainFragmentContainer)
    }

}
