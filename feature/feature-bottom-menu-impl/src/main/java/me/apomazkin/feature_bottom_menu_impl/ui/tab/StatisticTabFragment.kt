package me.apomazkin.feature_bottom_menu_impl.ui.tab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_statistic_tab.*
import me.apomazkin.feature_bottom_menu_impl.R
import me.apomazkin.feature_statistic_impl.di.FeatureStatisticComponent

class StatisticTabFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_statistic_tab, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val api = FeatureStatisticComponent.initAndGet(
            featureContainer,
            requireContext()
        )
        api.featureStatisticNavigation().start()
    }

    override fun onStop() {
        super.onStop()
        FeatureStatisticComponent.destroyFeature()
    }

}