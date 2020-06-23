package me.apomazkin.feature_vocabulary_impl

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import me.apomazkin.core_db_api.entity.WordWithDefinition
import me.apomazkin.feature_vocabulary_impl.databinding.ItemTextBinding

class NewWordListAdapter(
    private var data: List<WordWithDefinition>,
    private val listener: NewWordListAdapterListener
) : RecyclerView.Adapter<NewWordListAdapter.NewWordHolder>() {

    fun setData(list: List<WordWithDefinition>) {
        data = list
        notifyDataSetChanged()
    }

    override fun getItemCount() = data.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewWordHolder {
        val binding = DataBindingUtil.inflate<ItemTextBinding>(
            LayoutInflater.from(parent.context),
            R.layout.item_text,
            parent,
            false
        )
        return NewWordHolder(binding)
    }

    override fun onBindViewHolder(holder: NewWordHolder, position: Int) {
        holder.bind(data[position])
    }


    inner class NewWordHolder(
        private val binding: ItemTextBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            setupLongClick()
        }

        private fun setupLongClick() {
            binding.root.setOnClickListener {
                listener.onAddDefinition(data[adapterPosition].word.id)
            }
            binding.root.setOnLongClickListener {
                listener.onRemoveWord(data[adapterPosition].word.id)
                return@setOnLongClickListener true
            }
        }

        fun bind(value: WordWithDefinition) {
            binding.entry.text = value.word.word ?: "undefined"
        }

    }

    interface NewWordListAdapterListener {
        fun onRemoveWord(id: Long?)
        fun onAddDefinition(id: Long?)
    }

}