package me.apomazkin.polytrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import me.apomazkin.core_db_api.CoreDbProvider
//import me.apomazkin.feature_bottom_menu_api.FeatureBottomMenuApi
import me.apomazkin.polytrainer.route.RootRouter
import me.apomazkin.polytrainer.ui.theme.AppTheme
import javax.inject.Inject

class MainActivity : ComponentActivity() {

    @Inject
    lateinit var coreDbProvider: CoreDbProvider

//    private lateinit var featureBottomMenuApi: FeatureBottomMenuApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

//        App.getComponent().inject(this)

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    RootRouter(navController = navController)
                }
            }
        }

//        setContentView(R.layout.activity_main)
//
//
//        featureBottomMenuApi = FeatureBottomMenuComponent.getOrInit(getNavController())
//        featureBottomMenuApi.featureBottomMenuNavigator().start()

    }

//    private fun getNavController(): NavController {
//        return Navigation.findNavController(this, R.id.mainFragmentContainer)
//    }

}
