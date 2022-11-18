package me.apomazkin.feature_vocabulary_impl.ui.wordList

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes
import com.google.api.services.sheets.v4.SheetsScopes
//import com.google.android.gms.auth.api.signin.GoogleSignInAccount
//import com.google.android.gms.auth.api.signin.GoogleSignInOptions
//import com.google.android.gms.common.api.Scope
import me.apomazkin.core_base.ui.BaseFragment
import me.apomazkin.core_db_api.entity.Definition
import me.apomazkin.core_db_api.entity.Word
import me.apomazkin.core_interactor.LangGod
import me.apomazkin.feature_vocabulary_impl.R
import me.apomazkin.feature_vocabulary_impl.databinding.FragmentWordListBinding
import me.apomazkin.feature_vocabulary_impl.di.FeatureVocabularyComponent
import javax.inject.Inject


val DRIVE_SCOPE = listOf(DriveScopes.DRIVE_FILE)

class WordListFragment : BaseFragment<FragmentWordListBinding>(),
    WordListAdapter.NewWordListAdapterListener {

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestScopes(Scope(SheetsScopes.SPREADSHEETS))
        .requestScopes(Scope(SheetsScopes.DRIVE))
        .requestScopes(Scope(DriveScopes.DRIVE))
        .requestScopes(Scope(DriveScopes.DRIVE_FILE))
        .requestScopes(Scope(DriveScopes.DRIVE_METADATA))
        .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
        .requestEmail()
        .build()

    private val signInContractBackup =
        registerForActivityResult(SignInContract()) { acc: GoogleSignInAccount? ->
            Log.d("###", "WordListFragment / 45 / : ")
            Log.d("###", "WordListFragment / 45 / : ${acc?.displayName}")
            acc?.also {
                val accountCredential = GoogleAccountCredential
                    .usingOAuth2(context, DRIVE_SCOPE)
                accountCredential.selectedAccount = it.account
                listViewModel.backUpAll(accountCredential)
            }
        }

    private val signInContractRestore =
        registerForActivityResult(SignInContract()) { acc: GoogleSignInAccount? ->
            acc?.also {
                val accountCredential = GoogleAccountCredential
                    .usingOAuth2(context, DRIVE_SCOPE)
                accountCredential.selectedAccount = it.account
                Log.d("###", "WordListFragment / 64 / : Restore: ${it.displayName}")
                listViewModel.restoreAll(accountCredential)
//            listViewModel.backUpAll(accountCredential)
            }
        }

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
            R.id.back -> {
                Log.d("###", "WordListFragment / 130 / onOptionsItemSelected: back")
                signInContractBackup.launch(gso)
            }
            R.id.restore -> {
                signInContractRestore.launch(gso)
            }
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