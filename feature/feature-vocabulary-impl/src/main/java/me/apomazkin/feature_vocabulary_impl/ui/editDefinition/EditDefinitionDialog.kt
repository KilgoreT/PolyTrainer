package me.apomazkin.feature_vocabulary_impl.ui.editDefinition

import android.view.Gravity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import me.apomazkin.core_base.ui.BaseDialogFragment
import me.apomazkin.feature_vocabulary_impl.R
import me.apomazkin.feature_vocabulary_impl.databinding.DialogEditDefinitionBinding
import me.apomazkin.feature_vocabulary_impl.di.FeatureVocabularyComponent
import me.apomazkin.feature_vocabulary_impl.viewModelFactory.VocabularyViewModelFactory
import javax.inject.Inject

class EditDefinitionDialog : BaseDialogFragment<DialogEditDefinitionBinding>() {

    private val args: EditDefinitionDialogArgs by navArgs()

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    override fun getLayoutId() = R.layout.dialog_edit_definition
    override fun gravity() = Gravity.BOTTOM

    override fun inject() = FeatureVocabularyComponent.get().inject(this)

    override fun setupBinding(binding: DialogEditDefinitionBinding) {
        (factory as VocabularyViewModelFactory).setId(args.id)
        val model = ViewModelProvider(this, factory)[EditDefinitionViewModel::class.java]
        binding.model = model
    }
}