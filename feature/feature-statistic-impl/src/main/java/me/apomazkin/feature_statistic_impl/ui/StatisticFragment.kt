package me.apomazkin.feature_statistic_impl.ui

import androidx.lifecycle.ViewModelProvider
import me.apomazkin.core_base.ui.BaseFragment
import me.apomazkin.feature_statistic_impl.R
import me.apomazkin.feature_statistic_impl.databinding.FragmentStatisticBinding
import me.apomazkin.feature_statistic_impl.di.FeatureStatisticComponent
import javax.inject.Inject


class StatisticFragment : BaseFragment<FragmentStatisticBinding>() {

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private lateinit var viewModel: StatisticViewModel

    override fun inject() = FeatureStatisticComponent.get().inject(this)

    override fun getLayoutId() = R.layout.fragment_statistic

    override fun initViewModel(binding: FragmentStatisticBinding) {
        viewModel = ViewModelProvider(this, factory)[StatisticViewModel::class.java]
        binding.model = viewModel
    }

}