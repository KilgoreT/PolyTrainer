package me.apomazkin.vocabulary.ui.widget.detailDialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.vocabulary.R

@Composable
fun WordDetailHeaderWidget(
    word: String,
    onClose: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier
                .clip(CircleShape)
                .clickable { onClose.invoke() }
                .padding(16.dp),
            imageVector = Icons.Default.Close,
            contentDescription = ""
        )
        Box(
            modifier = Modifier
                .weight(1F),
            contentAlignment = Alignment.Center,
        ) {
//            Text(
//                modifier = Modifier
//                    .clip(RoundedCornerShape(8.dp))
//                    .background(FFD9E3)
//                    .padding(horizontal = 8.dp),
//                text = word,
//                textAlign = TextAlign.Center,
//                style = MaterialTheme.typography.titleLarge,
//            )
        }
        TextButton(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            onClick = onSave
        ) {
            Text(
                text = stringResource(id = R.string.word_card_save_title),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}