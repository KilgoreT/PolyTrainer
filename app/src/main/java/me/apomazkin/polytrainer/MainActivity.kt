package me.apomazkin.polytrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.Navigation
import me.apomazkin.core_db_api.CoreDbProvider
import me.apomazkin.feature_bottom_menu_api.FeatureBottomMenuApi
import me.apomazkin.polytrainer.ui.theme.AppTheme
import javax.inject.Inject

class MainActivity : ComponentActivity() {

    @Inject
    lateinit var coreDbProvider: CoreDbProvider

    private lateinit var featureBottomMenuApi: FeatureBottomMenuApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        App.getComponent().inject(this)

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Text(
                        text = "ZHOPA",
                        color = Color.Red,
                        fontSize = 40.sp
                    )

                }
            }
        }

//        setContentView(R.layout.activity_main)
//
//
//        featureBottomMenuApi = FeatureBottomMenuComponent.getOrInit(getNavController())
//        featureBottomMenuApi.featureBottomMenuNavigator().start()

    }

    private fun getNavController(): NavController {
        return Navigation.findNavController(this, R.id.mainFragmentContainer)
    }

}
