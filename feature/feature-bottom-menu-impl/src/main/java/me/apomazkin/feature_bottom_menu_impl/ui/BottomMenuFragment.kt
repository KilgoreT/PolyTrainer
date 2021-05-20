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
import com.google.android.material.bottomnavigation.BottomNavigationView
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
        setupAppBar()
        setupBottomMenu(view)
        setupDestinationChangeListener()
    }

    private fun setupDestinationChangeListener() {
        getNavController().addOnDestinationChangedListener { controller, destination, arguments ->
            println()
        }
    }

    private fun setupAppBar() {
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.vocabularyTabFragment,
                R.id.trainingTabFragment,
                R.id.statisticTabFragment,
            )
        )
        NavigationUI.setupActionBarWithNavController(
            requireActivity() as AppCompatActivity,
            getNavController(),
            appBarConfiguration
        )
    }

    private fun setupBottomMenu(view: View) {
        view.findViewById<BottomNavigationView>(R.id.bottomMenu).apply {
            setOnNavigationItemReselectedListener { }
            setupWithNavController(getNavController())
        }
    }

    private fun getNavController(): NavController {
        return Navigation.findNavController(requireActivity(), R.id.tabItemFragment)
    }

}