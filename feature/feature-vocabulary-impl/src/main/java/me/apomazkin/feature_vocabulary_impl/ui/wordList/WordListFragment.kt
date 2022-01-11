package me.apomazkin.feature_vocabulary_impl.ui.wordList

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import me.apomazkin.core_base.ui.BaseFragment
import me.apomazkin.core_db_api.entity.Definition
import me.apomazkin.core_db_api.entity.Word
import me.apomazkin.core_interactor.LangGod
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

        binding.container.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )
        binding.container.adapter = adapter
        listViewModel.data.observe(this) { result -> adapter.setData(result) }
        listViewModel.argh.observe(this) { result ->
            Toast.makeText(
                requireContext(),
                result,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onSearchTranslation(word: String?) {
        word?.let {
            val intent = Intent(
                Intent.ACTION_VIEW,
//                Uri.parse("https://dictionary.cambridge.org/dictionary/english-russian/$it")
                Uri.parse("https://www.ldoceonline.com/dictionary/$it")
            )
            startActivity(intent)
        }
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

    override fun onEditDefinition(definition: Definition) {
        listViewModel.editDefinition(definition.id ?: -1)
    }

    override fun onDeleteDefinition(id: Long?) {
        listViewModel.deleteDefinition(id)
    }

    override fun onAddSample(definitionId: Long) {
        listViewModel.addSample(definitionId)
    }

    override fun argh() {
        listViewModel.argh()
    }

    override fun arghDelete() {
        listViewModel.arghDelete()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.lang, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.en -> {
                LangGod.langId = 0
                listViewModel.loadData()
                return true
            }
            R.id.fr -> {
                LangGod.langId = 1
                listViewModel.loadData()
                return true
            }
        }
        return false
    }

}