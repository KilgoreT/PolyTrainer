package me.apomazkin.feature_vocabulary_impl.ui.wordList

import android.app.AlertDialog
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
            { result ->
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

    override fun onEditWord(id: Long?) {
        TODO("Not yet implemented")
    }

    override fun onRemoveWord(id: Long?) {
        showRemoveWarning(id)
    }

    private fun showRemoveWarning(id: Long?) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove word?")
            .setCancelable(true)
            .setNegativeButton("Cancel") { _, _ -> }
            .setPositiveButton("Ok") { _, _ ->
                id?.let {
                    listViewModel.removeWord(it)
                }
            }
            .create()
            .show()
    }

    override fun onAddDefinition(id: Long?) {
        listViewModel.addDefinition(id)
    }

    override fun onEditDefinition(id: Long?) {
        listViewModel.editDefinition(id)
    }

    override fun onDeleteDefinition(id: Long?) {
        listViewModel.deleteDefinition(id)
    }

}