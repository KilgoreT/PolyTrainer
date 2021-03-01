package me.apomazkin.feature_training_write_impl.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import me.apomazkin.core_db_api.entity.Quiz
import me.apomazkin.feature_training_write_impl.R

class WriteQuizAdapter(
    private var list: List<Quiz>,
    private val listener: WriteQuizAdapterListener
) : RecyclerView.Adapter<WriteQuizAdapter.WriteQuizHolder>() {

    fun setData(result: List<Quiz>?) {
        result?.let {
            list = it
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WriteQuizHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_quiz, parent, false)
        return WriteQuizHolder(view)
    }

    override fun onBindViewHolder(holder: WriteQuizHolder, position: Int) {
        if (position < list.size) {
            holder.bind(list[position])
        } else {
            holder.bindSummary()
        }
    }

    override fun getItemCount() = list.size + 1

    inner class WriteQuizHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        init {

            view.findViewById<Button>(R.id.check).setOnClickListener { _ ->
                if (view.findViewById<EditText>(R.id.answer).text.toString() == list[adapterPosition].answer) {
                    setupSuccess(list[adapterPosition].answer)
                } else {
                    setupFail(list[adapterPosition].answer)
                }
                view.findViewById<EditText>(R.id.answer).text.clear()
            }
            view.findViewById<Button>(R.id.next).setOnClickListener { view ->
                listener.next(adapterPosition + 1)
            }
            view.findViewById<Button>(R.id.reload).setOnClickListener { _ ->
                listener.reload()
            }
        }

        private fun setupSuccess(answer: String?) {
            listener.onAnswerSuccess(answer)
            view.findViewById<Button>(R.id.check).visibility = View.GONE
            view.findViewById<Button>(R.id.next).visibility = View.VISIBLE
            view.findViewById<Button>(R.id.reload).visibility = View.GONE
        }

        private fun setupFail(answer: String?) {
            listener.onAnswerFail(answer)
            view.findViewById<Button>(R.id.check).visibility = View.GONE
            view.findViewById<Button>(R.id.next).visibility = View.VISIBLE
            view.findViewById<Button>(R.id.reload).visibility = View.GONE
        }

        fun bind(value: Quiz) {
            view.findViewById<TextView>(R.id.quiz).text = value.definition.definition
            view.findViewById<Button>(R.id.check).visibility = View.VISIBLE
            view.findViewById<Button>(R.id.next).visibility = View.GONE
            view.findViewById<Button>(R.id.reload).visibility = View.GONE
        }

        fun bindSummary() {
            view.findViewById<TextView>(R.id.quiz).text = view.resources.getString(R.string.done)
            view.findViewById<Button>(R.id.check).visibility = View.GONE
            view.findViewById<Button>(R.id.next).visibility = View.GONE
            view.findViewById<Button>(R.id.reload).visibility = View.VISIBLE
            listener.onSummary()
        }

    }

    interface WriteQuizAdapterListener {
        fun onAnswerSuccess(answer: String?)
        fun onAnswerFail(answer: String?)
        fun next(item: Int)
        fun reload()
        fun onSummary()
    }

}