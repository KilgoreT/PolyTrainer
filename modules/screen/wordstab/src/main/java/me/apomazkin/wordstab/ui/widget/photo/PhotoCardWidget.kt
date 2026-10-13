package me.apomazkin.wordstab.ui.widget.photo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.cardDivider
import me.apomazkin.theme.destructiveRed
import me.apomazkin.theme.formTextSecondary
import me.apomazkin.theme.formTextTertiary
import me.apomazkin.wordstab.R
import me.apomazkin.wordstab.deps.PhotoDictionaryForm
import me.apomazkin.wordstab.deps.PhotoTarget
import me.apomazkin.wordstab.logic.Msg
import me.apomazkin.wordstab.logic.PhotoWordsState
import me.apomazkin.wordstab.logic.photo.PhotoCard
import me.apomazkin.wordstab.logic.photo.PhotoCardLexeme
import me.apomazkin.wordstab.logic.photo.PhotoCardTarget
import me.apomazkin.wordstab.logic.photo.PhotoEditableText
import me.apomazkin.wordstab.logic.photo.PhotoOwnGroup
import me.apomazkin.wordstab.logic.photo.PhotoSheet
import me.apomazkin.wordstab.logic.photo.PhotoTextField
import me.apomazkin.wordstab.logic.photo.existingOf

/** Сколько первых текстов видно в свёрнутой карточке (набросок §1). */
private const val COLLAPSED_TEXTS = 3

/**
 * Карточка леммы (набросок §3): свёрнутая — три строки, развёрнутая —
 * «куда класть», своя группа, лексемы-кандидаты с частью речи, текстами,
 * примерами и пометками, «ещё N значений» текстом (AWP-05.5).
 */
@Composable
internal fun PhotoCardWidget(
    card: PhotoCard,
    state: PhotoWordsState,
    form: PhotoDictionaryForm,
    sendMessage: (Msg) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        CardHead(card = card, form = form, sendMessage = sendMessage)
        if (card.isExpanded) {
            CardChoices(card = card, form = form, sendMessage = sendMessage)
            card.lexemes.forEach { lexeme ->
                HorizontalDivider(color = cardDivider, modifier = Modifier.padding(start = 16.dp))
                LexemeRow(card = card, lexeme = lexeme, state = state, form = form, sendMessage = sendMessage)
            }
            if (card.hiddenCount > 0) {
                Text(
                    text = stringResource(id = R.string.photo_add_more, lexemesCount(card.hiddenCount)),
                    style = LexemeStyle.BodyS,
                    color = formTextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        } else {
            CollapsedTexts(card = card)
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

/** Лемма с пометкой «есть в словаре», слова текста и своя группа, крестик карточки. */
@Composable
private fun CardHead(
    card: PhotoCard,
    form: PhotoDictionaryForm,
    sendMessage: (Msg) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { sendMessage(Msg.TogglePhotoCard(card.lemma)) }
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = card.lemma,
                style = LexemeStyle.BodyXLBold,
                color = formTextTertiary,
                modifier = Modifier.weight(1f),
            )
            if (card.existingCount > 0) {
                Text(
                    text = if (card.existingCount == 1) {
                        stringResource(id = R.string.photo_add_exists)
                    } else {
                        stringResource(id = R.string.photo_add_exists_count, card.existingCount)
                    },
                    style = LexemeStyle.BodyS,
                    color = LexemeColor.primary,
                )
            }
            Text(
                text = if (card.isExpanded) "▴" else "▸",
                style = LexemeStyle.BodyM,
                color = formTextSecondary,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
            Cross(onClick = { sendMessage(Msg.DeletePhotoCard(card.lemma)) })
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(id = R.string.photo_add_in_text, card.sourceWords.joinToString(", ")),
                style = LexemeStyle.BodyS,
                color = formTextSecondary,
            )
            val own = card.ownGroup
            if (own is PhotoOwnGroup.Group) {
                Text(
                    text = "⌂ ${form.groups.firstOrNull { it.id == own.groupId }?.name.orEmpty()}",
                    style = LexemeStyle.BodyS,
                    color = LexemeColor.primary,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CollapsedTexts(card: PhotoCard) {
    val texts = card.lexemes.take(COLLAPSED_TEXTS).joinToString(" · ") { it.translation.text }
    Text(
        text = if (card.lexemes.size > COLLAPSED_TEXTS) "$texts …" else texts,
        style = LexemeStyle.BodyS,
        color = formTextTertiary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

/** «Куда: …▾» (AWP-18) и «Группа: …▾» (AWP-08) у развёрнутой карточки. */
@Composable
private fun CardChoices(
    card: PhotoCard,
    form: PhotoDictionaryForm,
    sendMessage: (Msg) -> Unit,
) {
    val hasExisting = form.existingOf(card.lemma).isNotEmpty()
    val hasGroups = form.groups.isNotEmpty()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Choice(
            label = stringResource(id = R.string.photo_add_where),
            value = targetName(card, form),
            enabled = hasExisting,
            onClick = { sendMessage(Msg.OpenPhotoSheet(PhotoSheet.CardTarget(card.lemma))) },
        )
        Choice(
            label = stringResource(id = R.string.photo_add_group),
            value = ownGroupName(card.ownGroup, form),
            enabled = hasGroups,
            onClick = { sendMessage(Msg.OpenPhotoSheet(PhotoSheet.CardGroup(card.lemma))) },
        )
    }
}

@Composable
private fun Choice(
    label: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(modifier = Modifier.clickable(enabled = enabled) { onClick() }) {
        Text(text = label, style = LexemeStyle.BodyS, color = formTextSecondary)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (enabled) "$value ▾" else value,
            style = LexemeStyle.BodyS,
            color = if (enabled) LexemeColor.primary else formTextTertiary,
        )
    }
}

@Composable
private fun targetName(
    card: PhotoCard,
    form: PhotoDictionaryForm,
): String = when (val target = card.target) {
    is PhotoCardTarget.Existing -> form.existingLemmas.firstOrNull { it.wordId == target.wordId }
        ?.let { "«${it.text}»${if (it.coreValues.isEmpty()) "" else " (${it.coreValues.first()})"}" }
        ?: stringResource(id = R.string.photo_add_new_word)
    PhotoCardTarget.NewWord -> stringResource(id = R.string.photo_add_new_word)
}

@Composable
private fun ownGroupName(
    own: PhotoOwnGroup,
    form: PhotoDictionaryForm,
): String = when (own) {
    PhotoOwnGroup.AsCommon -> stringResource(id = R.string.photo_add_group_as_common)
    PhotoOwnGroup.All -> stringResource(id = R.string.photo_add_group_all)
    is PhotoOwnGroup.Group -> form.groups.firstOrNull { it.id == own.groupId }?.name ?: stringResource(id = R.string.photo_add_group_as_common)
}

/**
 * Строка лексемы-кандидата (AWP-10, набросок §3): часть речи «глагол ▾»,
 * текст перевода (нажатие — правка), под ним толкование и примеры с
 * крестиками, пометы и подпись серым, пометки «англ.», «есть», «не
 * попадёт в ядро», «изменено»; уточнения родителя — с отступом.
 */
@Composable
private fun LexemeRow(
    card: PhotoCard,
    lexeme: PhotoCardLexeme,
    state: PhotoWordsState,
    form: PhotoDictionaryForm,
    sendMessage: (Msg) -> Unit,
) {
    val indent = if (lexeme.parentKey != null) 32.dp else 16.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent, end = 8.dp, top = 8.dp, bottom = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PartOfSpeechChip(card = card, lexeme = lexeme, state = state, form = form, sendMessage = sendMessage)
            Spacer(modifier = Modifier.weight(1f))
            Marks(lexeme = lexeme)
            Cross(onClick = { sendMessage(Msg.DeletePhotoLexeme(card.lemma, lexeme.key)) })
        }
        if (lexeme.notes.isNotEmpty() || lexeme.caption != null) {
            Text(
                text = listOfNotNull(lexeme.notes.takeIf { it.isNotEmpty() }?.joinToString(", "), lexeme.caption).joinToString(" · "),
                style = LexemeStyle.BodyS,
                color = formTextSecondary,
            )
        }
        EditableLine(
            text = lexeme.translation,
            style = LexemeStyle.BodyM,
            onClick = { sendMessage(Msg.OpenPhotoSheet(PhotoSheet.TextEdit(card.lemma, lexeme.key, PhotoTextField.Translation, lexeme.translation.text))) },
        )
        lexeme.gloss?.let { gloss ->
            EditableLine(
                text = gloss,
                style = LexemeStyle.BodyS,
                onClick = { sendMessage(Msg.OpenPhotoSheet(PhotoSheet.TextEdit(card.lemma, lexeme.key, PhotoTextField.Gloss, gloss.text))) },
            )
        }
        if (state.mapping.examples != PhotoTarget.None) {
            lexeme.examples.forEachIndexed { index, example ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EditableLine(
                        text = example,
                        style = LexemeStyle.BodyS,
                        quoted = true,
                        modifier = Modifier.weight(1f),
                        onClick = { sendMessage(Msg.OpenPhotoSheet(PhotoSheet.TextEdit(card.lemma, lexeme.key, PhotoTextField.Example(index), example.text))) },
                    )
                    Cross(onClick = { sendMessage(Msg.DeletePhotoExample(card.lemma, lexeme.key, index)) })
                }
            }
        }
    }
}

@Composable
private fun PartOfSpeechChip(
    card: PhotoCard,
    lexeme: PhotoCardLexeme,
    state: PhotoWordsState,
    form: PhotoDictionaryForm,
    sendMessage: (Msg) -> Unit,
) {
    val canChoose = state.mapping.partOfSpeech != PhotoTarget.None && form.partOfSpeechOptions.isNotEmpty()
    val option = form.partOfSpeechOptions.firstOrNull { it.id == lexeme.partOfSpeechOptionId }
    val label = if (option != null) partOfSpeechShortLabel(option) else stringResource(id = R.string.photo_add_part_of_speech_none)
    Text(
        text = if (canChoose) "$label ▾" else label,
        style = LexemeStyle.BodyS,
        color = if (canChoose) LexemeColor.primary else formTextSecondary,
        modifier = Modifier
            .clickable(enabled = canChoose) { sendMessage(Msg.OpenPhotoSheet(PhotoSheet.PartOfSpeech(card.lemma, lexeme.key))) }
            .padding(vertical = 2.dp),
    )
}

@Composable
private fun Marks(lexeme: PhotoCardLexeme) {
    val marks = buildList {
        if (lexeme.isEnglishFallback) add(stringResource(id = R.string.photo_add_english_mark) to formTextSecondary)
        if (lexeme.translation.isEdited || lexeme.gloss?.isEdited == true || lexeme.examples.any { it.isEdited }) {
            add(stringResource(id = R.string.photo_add_edited_mark) to formTextSecondary)
        }
        if (lexeme.exists) add(stringResource(id = R.string.photo_add_exists_mark) to LexemeColor.primary)
        if (lexeme.lacksCore) add(stringResource(id = R.string.photo_add_no_core_mark) to destructiveRed)
    }
    marks.forEach { (text, color) ->
        Text(
            text = text,
            style = LexemeStyle.BodyS,
            color = color,
            modifier = Modifier.padding(end = 8.dp),
        )
    }
}

@Composable
private fun EditableLine(
    text: PhotoEditableText,
    style: androidx.compose.ui.text.TextStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    quoted: Boolean = false,
) {
    Text(
        text = if (quoted) "«${text.text}»" else text.text,
        style = style,
        color = formTextTertiary,
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 2.dp),
    )
}

@Composable
internal fun Cross(onClick: () -> Unit) {
    Text(
        text = "×",
        style = LexemeStyle.BodyXL,
        color = formTextSecondary,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 2.dp),
    )
}
