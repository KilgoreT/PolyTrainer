package me.apomazkin.feature_vocabulary_impl.ui.wordList

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.fragment_word_list.*
import me.apomazkin.core_base.ui.BaseFragment
import me.apomazkin.feature_vocabulary_impl.R
import me.apomazkin.feature_vocabulary_impl.WordListViewModel
import me.apomazkin.feature_vocabulary_impl.databinding.FragmentWordListBinding
import me.apomazkin.feature_vocabulary_impl.di.FeatureVocabularyComponent
import javax.inject.Inject


class WordListFragment : BaseFragment<FragmentWordListBinding>(),
    WordListAdapter.NewWordListAdapterListener {

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private lateinit var listViewModel: WordListViewModel
    private lateinit var adapter: WordListAdapter

    override fun inject() = FeatureVocabularyComponent.get().inject(this)

    override fun getLayoutId() = R.layout.fragment_word_list

    override fun initViewModel(binding: FragmentWordListBinding) {
        listViewModel = ViewModelProvider(this, factory)[WordListViewModel::class.java]
        binding.model = listViewModel
    }

    override fun initView() {
        super.initView()

//        val qqq = ValueAnimator()
//        val set = AnimatorInflater.loadAnimator(requireContext(), R.animator.open_scale) as AnimatorSet
//        set.setTarget(fab)
//        set.start()

//        fab.setOnClickListener {
//            fab2.
//        }

        listViewModel.data.observe(
            this,
            Observer { result ->
                if (::adapter.isInitialized) {
                    adapter.setData(result)
                } else {
                    adapter =
                        WordListAdapter(
                            result,
                            this
                        )
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