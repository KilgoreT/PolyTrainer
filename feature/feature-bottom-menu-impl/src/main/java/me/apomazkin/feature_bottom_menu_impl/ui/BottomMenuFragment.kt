package me.apomazkin.feature_bottom_menu_impl.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import kotlinx.android.synthetic.main.fragment_bottom_menu.*
import me.apomazkin.feature_bottom_menu_impl.R

class BottomMenuFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_bottom_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val appBarConfiguration = AppBarConfiguration
            .Builder(
                setOf(
                    R.id.blankFragment01,
                    R.id.blankFragment02
                )
            )
            .build()
        NavigationUI.setupActionBarWithNavController(
            requireActivity() as AppCompatActivity,
            getNavController(),
            appBarConfiguration
        )
        bottomMenu.setOnNavigationItemReselectedListener { }
        bottomMenu.setupWithNavController(getNavController())

    }

    private fun getNavController(): NavController {
        return Navigation.findNavController(requireActivity(), R.id.tabItemFragment)
    }

}