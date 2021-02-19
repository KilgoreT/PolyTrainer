package me.apomazkin.feature_training_list_impl.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import me.apomazkin.core_db.di.CoreDbComponent
import me.apomazkin.feature_training_list_impl.R
import me.apomazkin.feature_training_list_impl.di.FeatureTrainingListComponent
import me.apomazkin.feature_training_write_impl.di.DaggerFeatureTrainingWriteComponent_FeatureTrainingListDependencyComponent
import me.apomazkin.feature_training_write_impl.di.FeatureTrainingWriteComponent
import javax.inject.Inject
import javax.inject.Named

class TrainingListFragment : Fragment() {

    @Inject
    @Named("parent")
    lateinit var parentController: NavController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_training_list, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        FeatureTrainingListComponent.get().inject(this)

        val button = view.findViewById<Button>(R.id.btn)
        button.setOnClickListener { view ->
            openTrain()
        }

//        val api = FeatureStatisticComponent.initAndGet(
//            featureContainer,
//            DaggerFeatureStatisticComponent_FeatureStatisticDependencyComponent
//                .builder()
//                .coreDbApi(CoreDbComponent.get(requireContext()).getCoreDbApi())
//                .build()
//        )
//        api.featureStatisticNavigation().start()
    }

    private fun openTrain() {
        val api = FeatureTrainingWriteComponent
            .initAndGet(
                navController = parentController,
                DaggerFeatureTrainingWriteComponent_FeatureTrainingListDependencyComponent
                    .builder()
                    .coreDbApi(CoreDbComponent.get(requireContext()).getCoreDbApi())
                    .build()
            )
        api.featureTrainingWriteNavigator().start()
    }

    override fun onStop() {
        super.onStop()
//        FeatureStatisticComponent.destroyFeature()
    }

}