package me.apomazkin.feature_add_word_impl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_text.view.*
import me.apomazkin.core_db_api.entity.Word

class NewWordListAdapter(
    private var data: List<Word>
) : RecyclerView.Adapter<NewWordListAdapter.NewWordHolder>() {

    fun setData(list: List<Word>) {
        data = list
        notifyDataSetChanged()
    }

    override fun getItemCount() = data.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewWordHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_text, parent, false)
        return NewWordHolder(view)
    }

    override fun onBindViewHolder(holder: NewWordHolder, position: Int) {
        holder.bind(data[position])
    }


    inner class NewWordHolder(
        private val view: View
    ) : RecyclerView.ViewHolder(view) {


        fun bind(value: Word) {
            view.entry.text = value.word
        }

    }

}