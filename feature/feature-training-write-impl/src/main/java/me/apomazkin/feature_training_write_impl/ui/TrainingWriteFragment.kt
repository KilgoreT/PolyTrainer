package me.apomazkin.feature_training_write_impl.ui

import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import kotlinx.android.synthetic.main.fragment_training_write.*
import me.apomazkin.core_base.ui.BaseFragment
import me.apomazkin.feature_training_write_impl.R
import me.apomazkin.feature_training_write_impl.databinding.FragmentTrainingWriteBinding
import me.apomazkin.feature_training_write_impl.di.FeatureTrainingWriteComponent
import javax.inject.Inject


class TrainingWriteFragment : BaseFragment<FragmentTrainingWriteBinding>(),
    WriteQuizAdapter.WriteQuizAdapterListener {

    @Inject
    lateinit var factory: ViewModelProvider.Factory
    private lateinit var viewModel: TrainingWriteViewModel
    private lateinit var adapter: WriteQuizAdapter

    override fun inject() = FeatureTrainingWriteComponent.get().inject(this)

    override fun getLayoutId(): Int = R.layout.fragment_training_write

    override fun initViewModel(binding: FragmentTrainingWriteBinding) {
        viewModel = ViewModelProvider(this, factory)[TrainingWriteViewModel::class.java]
        binding.model = viewModel
    }

    override fun initView() {
        super.initView()

        viewModel.data.observe(
            this,
            { result ->
                if (::adapter.isInitialized) {
                    adapter.setData(result)
                    adapter.notifyDataSetChanged()
                    containerQuiz.scrollToPosition(0)
                    viewModel.currentQuizTitle.postValue("1 quiz")
                } else {
                    adapter = WriteQuizAdapter(result, this)
                    val layout = LinearLayoutManager(
                        requireContext(),
                        LinearLayoutManager.VERTICAL,
                        false
                    )
                    containerQuiz.layoutManager = layout
                    containerQuiz.adapter = adapter
                    val snap = PagerSnapHelper()
                    snap.attachToRecyclerView(containerQuiz)
                    viewModel.currentQuizTitle.postValue("1 quiz")
                }
            }
        )
    }

    override fun onAnswerSuccess(answer: String?) {
        viewModel.currentQuizTitle.postValue("Win: $answer")

    }

    override fun onAnswerFail(answer: String?) {
        viewModel.currentQuizTitle.postValue("False: $answer")
    }

    override fun next(item: Int) {
        viewModel.currentQuiz.postValue(item)
        viewModel.currentQuizTitle.postValue("${item + 1} quiz")
        containerQuiz.scrollToPosition(item)
    }

    override fun reload() {
        Toast.makeText(requireContext(), "Again", Toast.LENGTH_SHORT).show()
        viewModel.loadData()
    }

    override fun onSummary() {
        viewModel.currentQuizTitle.postValue("Summary")
    }

}