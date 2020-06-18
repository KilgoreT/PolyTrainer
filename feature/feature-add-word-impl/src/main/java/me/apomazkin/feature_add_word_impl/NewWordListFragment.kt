package me.apomazkin.feature_add_word_impl

import androidx.lifecycle.ViewModelProvider
import me.apomazkin.feature_add_word_impl.databinding.FragmentNewWordListBinding
import me.apomazkin.feature_add_word_impl.di.FeatureAddWordComponent
import javax.inject.Inject


class NewWordListFragment : me.apomazkin.core_base.ui.BaseFragment<FragmentNewWordListBinding>() {

    @Inject
    lateinit var factory: AddWordModelFabric


    override fun inject() {
        FeatureAddWordComponent.get().inject(this)
    }


    override fun getLayoutId() = R.layout.fragment_new_word_list

    override fun initViewModel(binding: FragmentNewWordListBinding) {
        val model = ViewModelProvider(this, factory)[AddWordViewModel::class.java]
        binding.model = model

    }

}