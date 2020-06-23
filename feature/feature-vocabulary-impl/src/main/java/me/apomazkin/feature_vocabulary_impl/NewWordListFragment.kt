package me.apomazkin.feature_vocabulary_impl

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.fragment_new_word_list.*
import me.apomazkin.feature_vocabulary_impl.databinding.FragmentNewWordListBinding
import me.apomazkin.feature_vocabulary_impl.di.FeatureVocabularyComponent
import me.apomazkin.feature_vocabulary_impl.viewModelFactory.VocabularyViewModelFactory
import javax.inject.Inject


class NewWordListFragment : me.apomazkin.core_base.ui.BaseFragment<FragmentNewWordListBinding>(),
    NewWordListAdapter.NewWordListAdapterListener {

    @Inject
    lateinit var factory: VocabularyViewModelFactory

    private lateinit var listViewModel: WordListViewModel
    private lateinit var adapter: NewWordListAdapter

    override fun inject() = FeatureVocabularyComponent.get().inject(this)

    override fun getLayoutId() = R.layout.fragment_new_word_list

    override fun initViewModel(binding: FragmentNewWordListBinding) {
        listViewModel = ViewModelProvider(this, factory)[WordListViewModel::class.java]
        binding.model = listViewModel
    }

    override fun initView() {
        super.initView()
        listViewModel.data.observe(this,
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
            listViewModel.removeWord(it)
        }
    }

    override fun onAddDefinition(id: Long?) {
        listViewModel.addDefinition(id)
    }

}