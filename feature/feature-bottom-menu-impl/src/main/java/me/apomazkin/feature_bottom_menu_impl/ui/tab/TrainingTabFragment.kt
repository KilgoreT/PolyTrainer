package me.apomazkin.feature_bottom_menu_impl.ui.tab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import me.apomazkin.core_db.di.CoreDbComponent
import me.apomazkin.feature_bottom_menu_impl.R
import me.apomazkin.feature_bottom_menu_impl.di.FeatureBottomMenuComponent
import me.apomazkin.feature_training_list_impl.di.DaggerFeatureTrainingListComponent_FeatureTrainingListDependencyComponent
import me.apomazkin.feature_training_list_impl.di.FeatureTrainingListComponent
import javax.inject.Inject

class TrainingTabFragment : Fragment() {

    @Inject
    lateinit var parentController: NavController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_training_tab, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        FeatureBottomMenuComponent.get().inject(this)

        val api = FeatureTrainingListComponent.initAndGet(
            parentController,
            getNavController(),
            DaggerFeatureTrainingListComponent_FeatureTrainingListDependencyComponent
                .builder()
                .coreDbApi(CoreDbComponent.get(requireContext()).getCoreDbApi())
                .build()
        )
        api.featureTrainingListNavigator().start()
    }

    override fun onStop() {
        super.onStop()
        FeatureTrainingListComponent.destroyFeature()
    }

    private fun getNavController() =
        Navigation.findNavController(requireActivity(), R.id.trainingListFragment)

}