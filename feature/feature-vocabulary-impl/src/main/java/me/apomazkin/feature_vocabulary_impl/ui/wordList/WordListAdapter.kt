package me.apomazkin.feature_vocabulary_impl.ui.wordList

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_definition.view.*
import kotlinx.android.synthetic.main.item_word.view.*
import me.apomazkin.core_db_api.entity.*
import me.apomazkin.feature_vocabulary_impl.R
import me.apomazkin.feature_vocabulary_impl.databinding.ItemWordBinding

class WordListAdapter(
    private var data: List<WordWithDefinition>,
    private val listener: NewWordListAdapterListener
) : RecyclerView.Adapter<WordListAdapter.NewWordHolder>() {

    fun setData(list: List<WordWithDefinition>) {
        data = list
        notifyDataSetChanged()
    }

    override fun getItemCount() = data.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewWordHolder {
        val binding = DataBindingUtil.inflate<ItemWordBinding>(
            LayoutInflater.from(parent.context),
            R.layout.item_word,
            parent,
            false
        )
        return NewWordHolder(binding)
    }

    override fun onBindViewHolder(holder: NewWordHolder, position: Int) {
        holder.bind(data[position])
    }


    inner class NewWordHolder(
        private val binding: ItemWordBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            setupClickHandler()
        }

        private fun setupClickHandler() {
            binding.root
                .btn_add_definition.setOnClickListener {
                    listener.onAddDefinition(data[adapterPosition].word.id)

                }
            binding.root
                .btn_more_actions.setOnClickListener { view ->
                    PopupMenu(view.context, view).apply {
                        inflate(R.menu.word_actions)
                        setOnMenuItemClickListener { menuItem ->
                            when (menuItem.itemId) {
                                R.id.word_action_add_definition -> {
                                    listener.onAddDefinition(data[adapterPosition].word.id)
                                    true
                                }
                                R.id.word_action_edit_word -> {
                                    true
                                }
                                R.id.word_action_delete_word -> {
                                    listener.onRemoveWord(data[adapterPosition].word.id)
                                    true
                                }
                                else -> false
                            }
                        }
                        show()
                    }
                }
        }

        fun bind(value: WordWithDefinition) {
            binding.entry.text = value.word.word ?: "undefined"
            binding.containerDefinition.removeAllViews()
            value.definitionList.forEach {
                val viewItem = LayoutInflater.from(binding.root.context)
                    .inflate(
                        R.layout.item_definition,
                        binding.containerDefinition,
                        false
                    )
                viewItem.tvWordClass.text = when (it.wordClass) {
                    is Verb -> viewItem.resources.getString(R.string.common_verb_label)
                    is Noun -> viewItem.resources.getString(R.string.common_noun_label)
                    is Adjective -> viewItem.resources.getString(R.string.common_adjective_label)
                    is Adverb -> viewItem.resources.getString(R.string.common_adverb_label)
                    else -> "undefined"
                }
                viewItem.tvDefinition.text = it.definition
                viewItem.btn_more_definition_action
                    .setOnClickListener { view ->
                        PopupMenu(view.context, view).apply {
                            inflate(R.menu.definition_actions)
                            setOnMenuItemClickListener { menuItem ->
                                when (menuItem.itemId) {
                                    R.id.definition_edit_action -> {
                                        true
                                    }
                                    R.id.definition_delete_action -> {
                                        listener.onDeleteDefinition(it.id)
                                        true
                                    }
                                    else -> false
                                }
                            }
                            show()
                        }
                    }
                binding.containerDefinition.addView(viewItem)
            }
        }

    }

    interface NewWordListAdapterListener {
        fun onEditWord(id: Long?)
        fun onRemoveWord(id: Long?)
        fun onAddDefinition(id: Long?)
        fun onEditDefinition(id: Long?)
        fun onDeleteDefinition(id: Long?)
    }

}