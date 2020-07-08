package me.apomazkin.feature_vocabulary_impl.ui.addDefinition

import android.view.Gravity
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
    }

}