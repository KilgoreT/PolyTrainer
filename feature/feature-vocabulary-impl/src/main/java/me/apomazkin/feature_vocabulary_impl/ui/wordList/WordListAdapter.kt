package me.apomazkin.feature_vocabulary_impl.ui.wordList

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import me.apomazkin.core_db_api.entity.*
import me.apomazkin.core_db_api.entity.Grade.*
import me.apomazkin.feature_vocabulary_impl.R

class WordListAdapter(
    private val listener: NewWordListAdapterListener
) : RecyclerView.Adapter<WordListAdapter.NewWordHolder>() {

    private var data: MutableList<Term> = mutableListOf()

    fun setData(list: List<Term>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount() = data.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewWordHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_word, parent, false)
        return NewWordHolder(
            view = view,
            onSearchTranslate = { position -> listener.onSearchTranslation(data[position].word.value) },
            onEditWordClick = { position -> listener.onEditWord(data[position].word) },
            onAddDefinitionClick = { position -> listener.onAddDefinition(data[position].word.id) },
            onRemoveWordClick = { position -> listener.onRemoveWord(data[position].word.id) },
            onUpdateDefinition = { definition -> listener.onEditDefinition(definition) },
            onRemoveDefinitionClick = { definitionId -> listener.onDeleteDefinition(definitionId) },
            onArgh = { listener.argh() },
            onArghDelete = { listener.arghDelete() }
        )
    }

    override fun onBindViewHolder(holder: NewWordHolder, position: Int) {
        holder.bind(data[position])
    }


    class NewWordHolder(
        private val view: View,
        private val onSearchTranslate: (Int) -> Unit,
        private val onEditWordClick: (Int) -> Unit,
        private val onRemoveWordClick: (Int) -> Unit,
        private val onAddDefinitionClick: (Int) -> Unit,
        private val onUpdateDefinition: (Definition) -> Unit,
        private val onRemoveDefinitionClick: (Long) -> Unit,
        private val onArgh: () -> Unit,
        private val onArghDelete: () -> Unit,
    ) : RecyclerView.ViewHolder(view) {

        private val btnAddDefinition = view.findViewById<ImageView>(R.id.btn_add_definition)
        private val btnMoreAction = view.findViewById<ImageView>(R.id.btn_more_actions)
        private val entry = view.findViewById<TextView>(R.id.entry)
        private val containerDefinition = view.findViewById<ViewGroup>(R.id.containerDefinition)

        init {
            setupClickHandler()
        }

        private fun setupClickHandler() {
            btnAddDefinition
                .setOnClickListener {
                    onAddDefinitionClick(adapterPosition)
                }

            entry.setOnClickListener { onSearchTranslate(adapterPosition) }

            btnMoreAction
                .setOnClickListener { view ->
                    PopupMenu(view.context, view).apply {
                        inflate(R.menu.word_actions)
                        setOnMenuItemClickListener { menuItem ->
                            when (menuItem.itemId) {
                                R.id.word_action_add_definition -> {
                                    onAddDefinitionClick(adapterPosition)
                                    true
                                }
                                R.id.word_action_edit_word -> {
                                    onEditWordClick(adapterPosition)
                                    true
                                }
                                R.id.word_action_delete_word -> {
                                    onRemoveWordClick(adapterPosition)
                                    true
                                }
                                R.id.argh -> {
                                    onArgh.invoke()
                                    true
                                }
                                R.id.argh_del -> {
                                    onArghDelete.invoke()
                                    true
                                }
                                else -> false
                            }
                        }
                        show()
                    }
                }
        }

        fun bind(value: Term) {
            entry.text = value.word.value ?: "undefined"
            containerDefinition.removeAllViews()
            val lastDefinition = value.definitionList.size - 1
            value.definitionList.forEachIndexed { index, definition ->
                val viewItem = LayoutInflater.from(view.context)
                    .inflate(
                        R.layout.item_definition,
                        containerDefinition,
                        false
                    )
                viewItem.findViewById<TextView>(R.id.tvWordClass).text =
                    when (definition.wordClass) {
                        is Verb -> viewItem.resources.getString(R.string.common_verb_label)
                        is Noun -> viewItem.resources.getString(R.string.common_noun_label)
                        is Adjective -> viewItem.resources.getString(R.string.common_adjective_label)
                        is Adverb -> viewItem.resources.getString(R.string.common_adverb_label)
                        else -> "undefined"
                    }
                viewItem.findViewById<TextView>(R.id.tvDefinition).text = definition.value
                if (index == lastDefinition) {
                    viewItem.findViewById<View>(R.id.divider).visibility = View.GONE
                }
                when (definition.wordClass?.grade) {
                    A1 -> viewItem.findViewById<ImageView>(R.id.ivGrade)
                        .setImageResource(R.drawable.ic_grade_a1)
                    A2 -> viewItem.findViewById<ImageView>(R.id.ivGrade)
                        .setImageResource(R.drawable.ic_grade_a2)
                    B1 -> viewItem.findViewById<ImageView>(R.id.ivGrade)
                        .setImageResource(R.drawable.ic_grade_b1)
                    B2 -> viewItem.findViewById<ImageView>(R.id.ivGrade)
                        .setImageResource(R.drawable.ic_grade_b2)
                    C1 -> viewItem.findViewById<ImageView>(R.id.ivGrade)
                        .setImageResource(R.drawable.ic_grade_c1)
                    C2 -> viewItem.findViewById<ImageView>(R.id.ivGrade)
                        .setImageResource(R.drawable.ic_grade_c2)
                    null -> viewItem.findViewById<ImageView>(R.id.ivGrade).visibility =
                        View.INVISIBLE
                }
                viewItem.findViewById<ImageView>(R.id.btn_more_definition_action)
                    .setOnClickListener { view ->
                        PopupMenu(view.context, view).apply {
                            inflate(R.menu.definition_actions)
                            setOnMenuItemClickListener { menuItem ->
                                when (menuItem.itemId) {
                                    R.id.definition_edit_action -> {
                                        onUpdateDefinition(definition)
                                        true
                                    }
                                    R.id.definition_delete_action -> {
                                        onRemoveDefinitionClick(
                                            definition.id ?: throw IllegalArgumentException("Zhopa")
                                        )
                                        true
                                    }
                                    else -> false
                                }
                            }
                            show()
                        }
                    }
                containerDefinition.addView(viewItem)
            }
        }

    }

    interface NewWordListAdapterListener {
        fun onSearchTranslation(word: String?)
        fun onEditWord(word: Word)
        fun onRemoveWord(id: Long?)
        fun onAddDefinition(id: Long?)
        fun onEditDefinition(definition: Definition)
        fun onDeleteDefinition(id: Long?)
        fun argh()
        fun arghDelete()
    }

}