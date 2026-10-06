package me.apomazkin.dictionary.form.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.apomazkin.dictionary.R
import me.apomazkin.dictionary.model.CountryFlagItem
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.formBackground
import me.apomazkin.theme.formTextSecondary
import me.apomazkin.theme.formTextTertiary
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.ImageFlagWidget
import me.apomazkin.ui.preview.PreviewWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val FLAG_SIZE = 56.dp
private val RING_WIDTH = 2.5.dp
private val BADGE_SIZE = 24.dp
private val BADGE_BORDER = 2.dp
private val BADGE_ICON_SIZE = 11.dp

/**
 * Флаг ячейки сетки. Флаги country-data на API 24+ — векторы с гербами
 * (до ~100 КБ XML); разбор `painterResource` в главном потоке стоил
 * ~3 мс на ячейку и съедал анимацию открытия формы. Здесь вектор
 * растеризуется в фоне под высоту ячейки и кэшируется на процесс; до
 * готовности — пустой круг (фон ячейки). Вид — как у [ImageFlagWidget].
 */
@Composable
private fun AsyncFlagImage(
    @DrawableRes flagRes: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val heightPx = with(LocalDensity.current) { FLAG_SIZE.roundToPx() }
    // Состояние привязано к флагу: `produceState` держит старое значение при
    // смене ключей, и переиспользованная ячейка сетки (фильтр 250 → 1)
    // показывала чужой флаг (IS525, найдено ручником: «Мексика» с флагом
    // Афганистана).
    var bitmap by remember(flagRes, heightPx) {
        mutableStateOf(FlagBitmapCache.get(flagRes, heightPx))
    }
    LaunchedEffect(flagRes, heightPx) {
        if (bitmap == null) {
            bitmap = withContext(Dispatchers.Default) {
                FlagBitmapCache.rasterize(context, flagRes, heightPx)
            }
        }
    }
    val imageModifier = modifier
        .size(24.dp)
        .clip(CircleShape)
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            contentScale = ContentScale.FillHeight,
            modifier = imageModifier,
        )
    } ?: Box(modifier = imageModifier)
}

/** Растры флагов на процесс: ключ — ресурс и высота в px. */
private object FlagBitmapCache {

    /** ~250 флагов 56 dp на xxhdpi — около 10 МБ; с запасом под плотности. */
    private const val MAX_BYTES = 16 * 1024 * 1024

    private val cache = object : LruCache<Long, ImageBitmap>(MAX_BYTES) {
        override fun sizeOf(key: Long, value: ImageBitmap): Int = value.width * value.height * 4
    }

    fun get(@DrawableRes res: Int, heightPx: Int): ImageBitmap? = cache.get(key(res, heightPx))

    fun rasterize(context: Context, @DrawableRes res: Int, heightPx: Int): ImageBitmap? {
        val drawable = context.getDrawable(res) ?: return null
        val w = drawable.intrinsicWidth
        val h = drawable.intrinsicHeight
        val widthPx = if (w > 0 && h > 0) (heightPx.toLong() * w / h).toInt() else heightPx
        val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, bitmap.width, bitmap.height)
        drawable.draw(Canvas(bitmap))
        return bitmap.asImageBitmap().also { cache.put(key(res, heightPx), it) }
    }

    private fun key(res: Int, heightPx: Int): Long = (res.toLong() shl 32) or heightPx.toLong()
}

/**
 * Сетка флагов 4 колонки (Figma 5027:1140-1234). Выбранный флаг: кольцо + бейдж-галочка
 * + акцентная bold-подпись (5027:1142-1154). При активном фильтре без совпадений —
 * empty-state «Ничего не найдено» (производная приходит параметром [isFilterActive],
 * сам виджет о flagFilter не знает).
 */
@Composable
internal fun FlagGridWidget(
    flags: List<CountryFlagItem>,
    selectedFlag: CountryFlagItem?,
    isFilterActive: Boolean,
    onFlagClick: (CountryFlagItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (flags.isEmpty() && isFilterActive) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(id = R.string.dictionary_flags_not_found),
                style = LexemeStyle.BodyM,
                color = formTextSecondary,
            )
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(flags, key = { it.numericCode }) { flag ->
            val isSelected = selectedFlag?.numericCode == flag.numericCode
            FlagGridItem(
                flag = flag,
                isSelected = isSelected,
                onFlagClick = onFlagClick,
            )
        }
    }
}

@Composable
private fun FlagGridItem(
    flag: CountryFlagItem,
    isSelected: Boolean,
    onFlagClick: (CountryFlagItem) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.padding(4.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = formBackground,
                onClick = { onFlagClick(flag) },
                modifier = Modifier
                    .size(FLAG_SIZE)
                    .let {
                        if (isSelected) it.border(
                            width = RING_WIDTH,
                            color = LexemeColor.primary,
                            shape = CircleShape,
                        ) else it
                    },
            ) {
                Box(
                    modifier = Modifier.padding(if (isSelected) RING_WIDTH + 2.dp else 0.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncFlagImage(
                        flagRes = flag.flagRes,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(BADGE_SIZE)
                        .border(width = BADGE_BORDER, color = formBackground, shape = CircleShape)
                        .padding(BADGE_BORDER)
                        .background(color = LexemeColor.primary, shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_confirm),
                        contentDescription = null,
                        tint = whiteColor,
                        modifier = Modifier.size(BADGE_ICON_SIZE),
                    )
                }
            }
        }
        Text(
            text = flag.localizedName,
            style = if (isSelected) LexemeStyle.BodySBold else LexemeStyle.BodyS,
            color = if (isSelected) LexemeColor.primary else formTextTertiary,
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(72.dp),
        )
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        FlagGridWidget(
            flags = emptyList(),
            selectedFlag = null,
            isFilterActive = false,
            onFlagClick = {},
        )
    }
}

@PreviewWidget
@Composable
private fun PreviewEmptySearch() {
    AppTheme {
        FlagGridWidget(
            flags = emptyList(),
            selectedFlag = null,
            isFilterActive = true,
            onFlagClick = {},
        )
    }
}
