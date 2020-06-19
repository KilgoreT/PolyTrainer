package me.apomazkin.feature_add_word_impl

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.fragment_new_word_list.*
import me.apomazkin.feature_add_word_impl.databinding.FragmentNewWordListBinding
import me.apomazkin.feature_add_word_impl.di.FeatureAddWordComponent
import javax.inject.Inject


class NewWordListFragment : me.apomazkin.core_base.ui.BaseFragment<FragmentNewWordListBinding>(),
    NewWordListAdapter.NewWordListAdapterListener {

    @Inject
    lateinit var factory: AddWordModelFabric

    private lateinit var viewModel: AddWordViewModel
    private lateinit var adapter: NewWordListAdapter

    override fun inject() = FeatureAddWordComponent.get().inject(this)

    override fun getLayoutId() = R.layout.fragment_new_word_list

    override fun initViewModel(binding: FragmentNewWordListBinding) {
        viewModel = ViewModelProvider(this, factory)[AddWordViewModel::class.java]
        binding.model = viewModel
    }

    override fun initView() {
        super.initView()
        viewModel.data.observe(this,
            Observer { result ->
                if (::adapter.isInitialized) {
                    adapter.setData(result)
                } else {
                    adapter = NewWordListAdapter(result, this)
                    container.adapter = adapter
                }
            }
        )
    }

    override fun onRemoveWord(id: Long?) {
        id?.let {
            viewModel.removeWord(it)
        }
    }

}