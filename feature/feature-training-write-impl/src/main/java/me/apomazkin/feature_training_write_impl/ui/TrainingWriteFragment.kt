package me.apomazkin.feature_training_write_impl.ui

import androidx.lifecycle.ViewModelProvider
import me.apomazkin.core_base.ui.BaseFragment
import me.apomazkin.feature_training_write_impl.R
import me.apomazkin.feature_training_write_impl.databinding.FragmentTrainingWriteBinding
import me.apomazkin.feature_training_write_impl.di.FeatureTrainingWriteComponent
import javax.inject.Inject


class TrainingWriteFragment : BaseFragment<FragmentTrainingWriteBinding>() {

    @Inject
    lateinit var factory: ViewModelProvider.Factory
    private lateinit var viewModel: TrainingWriteViewModel

    override fun inject() = FeatureTrainingWriteComponent.get().inject(this)

    override fun getLayoutId(): Int = R.layout.fragment_training_write

    override fun initViewModel(binding: FragmentTrainingWriteBinding) {
        viewModel = ViewModelProvider(this, factory)[TrainingWriteViewModel::class.java]
        binding.model = viewModel
    }

}