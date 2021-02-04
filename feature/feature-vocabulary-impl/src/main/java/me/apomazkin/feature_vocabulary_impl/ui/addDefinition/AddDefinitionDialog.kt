package me.apomazkin.feature_vocabulary_impl.ui.addDefinition

import android.view.Gravity
import android.widget.ArrayAdapter
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import me.apomazkin.core_base.ui.BaseDialogFragment
import me.apomazkin.feature_vocabulary_impl.R
import me.apomazkin.feature_vocabulary_impl.databinding.DialogAddDefinitionBinding
import me.apomazkin.feature_vocabulary_impl.di.FeatureVocabularyComponent
import me.apomazkin.feature_vocabulary_impl.viewModelFactory.VocabularyViewModelFactory
import javax.inject.Inject

class AddDefinitionDialog : BaseDialogFragment<DialogAddDefinitionBinding>() {

    private val args: AddDefinitionDialogArgs by navArgs()

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    override fun getLayoutId() = R.layout.dialog_add_definition
    override fun gravity() = Gravity.BOTTOM

    override fun inject() = FeatureVocabularyComponent.get().inject(this)

    override fun setupBinding(binding: DialogAddDefinitionBinding) {
        (factory as VocabularyViewModelFactory).setId(args.id)
        val model = ViewModelProvider(this, factory)[AddDefinitionViewModel::class.java]
        binding.model = model

        val chipGroup = binding.chipGroup
        chipGroup.setOnCheckedChangeListener(model)

        val spinner = binding.spinner
        spinner.onItemSelectedListener = model
        ArrayAdapter.createFromResource(
            binding.root.context,
            R.array.word_type_array,
            android.R.layout.simple_spinner_item,
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

    }

}