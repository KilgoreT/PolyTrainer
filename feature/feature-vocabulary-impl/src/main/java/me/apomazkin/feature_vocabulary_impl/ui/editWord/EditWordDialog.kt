package me.apomazkin.feature_vocabulary_impl.ui.editWord

import android.view.Gravity
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import me.apomazkin.core_base.ui.BaseDialogFragment
import me.apomazkin.feature_vocabulary_impl.R
import me.apomazkin.feature_vocabulary_impl.databinding.DialogEditWordBinding
import me.apomazkin.feature_vocabulary_impl.di.FeatureVocabularyComponent
import javax.inject.Inject

class EditWordDialog : BaseDialogFragment<DialogEditWordBinding>() {

    private val args: EditWordDialogArgs by navArgs()


    @Inject
    lateinit var factory: ViewModelProvider.Factory
    lateinit var model: EditWordViewModel

    override fun getLayoutId() = R.layout.dialog_edit_word
    override fun gravity() = Gravity.BOTTOM

    override fun inject() = FeatureVocabularyComponent.get().inject(this)

    override fun setupBinding(binding: DialogEditWordBinding) {
        model = ViewModelProvider(this, factory)[EditWordViewModel::class.java]
        model.setup(args.id, args.value)
        binding.model = model
    }

    override fun initView(view: View) {
        super.initView(view)
        model.setup(args.id, args.value)
    }

}