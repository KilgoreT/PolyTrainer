package me.apomazkin.feature_training_write_impl.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import me.apomazkin.feature_training_write_impl.R

class TrainingWriteFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_training_write, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.actionBar?.title = "Piska"
        println()

//        val api = FeatureStatisticComponent.initAndGet(
//            featureContainer,
//            DaggerFeatureStatisticComponent_FeatureStatisticDependencyComponent
//                .builder()
//                .coreDbApi(CoreDbComponent.get(requireContext()).getCoreDbApi())
//                .build()
//        )
//        api.featureStatisticNavigation().start()
    }

    override fun onStop() {
        super.onStop()
//        FeatureStatisticComponent.destroyFeature()
    }

}