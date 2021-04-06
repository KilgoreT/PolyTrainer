package me.apomazkin.feature_vocabulary_impl.ui.wordList

import android.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_word_list.*
import me.apomazkin.core_base.ui.BaseFragment
import me.apomazkin.core_db_api.entity.Word
import me.apomazkin.feature_vocabulary_impl.R
import me.apomazkin.feature_vocabulary_impl.databinding.FragmentWordListBinding
import me.apomazkin.feature_vocabulary_impl.di.FeatureVocabularyComponent
import javax.inject.Inject


class WordListFragment : BaseFragment<FragmentWordListBinding>(),
    WordListAdapter.NewWordListAdapterListener {

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private lateinit var listViewModel: WordListViewModel
    private val adapter: WordListAdapter = WordListAdapter(this)

    override fun inject() = FeatureVocabularyComponent.get().inject(this)

    override fun getLayoutId() = R.layout.fragment_word_list

    override fun initViewModel(binding: FragmentWordListBinding) {
        listViewModel = ViewModelProvider(this, factory)[WordListViewModel::class.java]
        binding.model = listViewModel
    }

    override fun initView() {
        super.initView()

        container.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )
        container.adapter = adapter
        listViewModel.data.observe(this) { result -> adapter.setData(result) }
    }

    override fun onEditWord(word: Word) {
        listViewModel.editWord(word)
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