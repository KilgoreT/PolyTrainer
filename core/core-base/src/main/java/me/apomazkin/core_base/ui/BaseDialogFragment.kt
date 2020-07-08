package me.apomazkin.core_base.ui

import android.app.Dialog
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.DialogFragment

abstract class BaseDialogFragment<T : ViewDataBinding?> : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        inject()
        val binding = DataBindingUtil.inflate<T>(
            inflater,
            getLayoutId(),
            container,
            false
        )
        setupBinding(binding)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    override fun onStart() {
        super.onStart()
        setupLayout()
        setupLayoutParams()
        setupCancelable()
    }


    private fun setupLayout() {
        dialog?.window?.setLayout(weight(), height())
    }

    private fun setupLayoutParams() {
        val params = dialog?.window?.attributes
        params?.gravity = gravity()
        dialog?.window?.attributes = params
    }

    private fun setupCancelable() {
        dialog?.setCanceledOnTouchOutside(isCanceled())
    }

    abstract fun getLayoutId(): Int
    abstract fun inject()
    abstract fun setupBinding(binding: T)

    protected open fun initView() {}
    protected open fun weight() = WindowManager.LayoutParams.MATCH_PARENT
    protected open fun height() = WindowManager.LayoutParams.WRAP_CONTENT
    protected open fun gravity() = Gravity.CENTER
    protected open fun isCanceled() = true

}