package me.apomazkin.feature_vocabulary_impl.ui.addWord

import android.view.Gravity
import androidx.lifecycle.ViewModelProvider
import me.apomazkin.core_base.ui.BaseDialogFragment
import me.apomazkin.feature_vocabulary_impl.R
import me.apomazkin.feature_vocabulary_impl.databinding.DialogAddWordBinding
import me.apomazkin.feature_vocabulary_impl.di.FeatureVocabularyComponent
import javax.inject.Inject

class AddWordDialog : BaseDialogFragment<DialogAddWordBinding>() {

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    override fun getLayoutId() = R.layout.dialog_add_word
    override fun gravity() = Gravity.BOTTOM

    override fun inject() = FeatureVocabularyComponent.get().inject(this)

    override fun setupBinding(binding: DialogAddWordBinding) {
        val model = ViewModelProvider(this, factory)[AddWordViewModel::class.java]
        binding.model = model
    }

}