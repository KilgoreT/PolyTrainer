package me.apomazkin.feature_vocabulary_impl.ui.wordList

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputEditText
import me.apomazkin.feature_vocabulary_impl.R

class AddLangDialog(
    private val listener: AddLangDialogListener
) : DialogFragment() {

    interface AddLangDialogListener {
        fun addLang(name: String, code: String)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater;
            val view = inflater.inflate(R.layout.dialog_add_lang, null)

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(view)
                // Add action buttons
                .setPositiveButton("Add",
                    DialogInterface.OnClickListener { dialog, id ->
                        val name = view.findViewById<TextInputEditText>(R.id.name).text.toString()
                        val code = view.findViewById<TextInputEditText>(R.id.code).text.toString()
                        listener.addLang(name, code)
                        // sign in the user ...
                    })
                .setNegativeButton("Cancel",
                    DialogInterface.OnClickListener { dialog, id ->
                        getDialog()!!.cancel()
                    })
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}