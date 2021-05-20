package me.apomazkin.core_base.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment

abstract class BaseFragment<DB : ViewDataBinding> : Fragment() {

    protected lateinit var binding: DB

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        inject()
        binding = DataBindingUtil.inflate(
            inflater,
            getLayoutId(),
            container,
            false
        )
        initViewModel(binding)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    abstract fun getLayoutId(): Int
    abstract fun inject()
    abstract fun initViewModel(binding: DB)
    open fun initView() {}

}