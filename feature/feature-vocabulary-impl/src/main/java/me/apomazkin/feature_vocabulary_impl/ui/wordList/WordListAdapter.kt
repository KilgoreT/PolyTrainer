package me.apomazkin.feature_vocabulary_impl.ui.wordList

import android.view.LayoutInflater
import android.view.ViewGroup
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
            setupLongClick()
        }

        private fun setupLongClick() {
            binding.root
                .btn_add_definition.setOnClickListener {
                    listener.onAddDefinition(data[adapterPosition].word.id)

                }
            binding.root
                .btn_delete_word.setOnClickListener {
                    listener.onRemoveWord(data[adapterPosition].word.id)
                }
        }

        fun bind(value: WordWithDefinition) {
            binding.entry.text = value.word.word ?: "undefined"
            binding.containerDefinition.removeAllViews()
            value.definitionList.forEach {
                val viewItem = LayoutInflater.from(binding.root.context)
                    .inflate(R.layout.item_definition, binding.containerDefinition, false)
                viewItem.tvWordClass.text = when (it.wordClass) {
                    is Verb -> viewItem.resources.getString(R.string.common_verb_label)
                    is Noun -> viewItem.resources.getString(R.string.common_noun_label)
                    is Adjective -> viewItem.resources.getString(R.string.common_adjective_label)
                    is Adverb -> viewItem.resources.getString(R.string.common_adverb_label)
                    else -> "undefined"
                }
                viewItem.tvDefinition.text = it.definition
                binding.containerDefinition.addView(viewItem)
            }
        }

    }

    interface NewWordListAdapterListener {
        fun onRemoveWord(id: Long?)
        fun onAddDefinition(id: Long?)
    }

}