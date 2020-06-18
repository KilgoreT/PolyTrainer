package me.apomazkin.feature_bottom_menu_impl.ui.tab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import me.apomazkin.core_db.di.CoreDbComponent
import me.apomazkin.feature_add_word_impl.di.DaggerFeatureAddWordComponent_FeatureAddWordDependencyComponent
import me.apomazkin.feature_add_word_impl.di.FeatureAddWordComponent
import me.apomazkin.feature_bottom_menu_impl.R

class BlankFragment01 : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_blank01, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val api = FeatureAddWordComponent
            .initAndGet(
                getNavController(),
                DaggerFeatureAddWordComponent_FeatureAddWordDependencyComponent
                    .builder()
                    .coreDbApi(CoreDbComponent.get(requireContext()).getCoreDbApi())
                    .build()
            )
        api.featureAddWordNavigation().start()
    }

    private fun getNavController() =
        Navigation.findNavController(requireActivity(), R.id.addWordFragment)
}