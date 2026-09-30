package me.apomazkin.quiz.chat.widget.motion

/** Проекция размещённого элемента ленты без Compose-типов: для [InsertShiftTracker.onLaidOut]. */
internal class LaidOutItem(
    val key: Any,
    /** Смещение в координатах ленты (в реверсе — от низа). */
    val offset: Int,
    /** Элемент кнопок (чипы, «Начать»), не сообщение. */
    val isAction: Boolean,
    /** Порядок сообщения; у кнопок null. */
    val order: Int?,
)

/**
 * Дистанция въезда новых элементов (px) = на сколько placement сдвинул
 * соседей: смещение прежнего нижнего сообщения (кнопки не в счёт — они
 * исчезают) между двумя раскладками, в координатах ленты от низа.
 * Положительная — соседи уехали вверх (вставили сообщение), отрицательная
 * — осели. Новые элементы стартуют на эту дистанцию ниже места (плюс
 * добавка [slideExtra]) и едут с колонкой одним куском. Взводится
 * ([armed]) только когда лента закреплена у конца через
 * `requestScrollToItem`; при подвозе скроллом из истории — 0.
 *
 * Считается после измерения ленты, до размещения ([onLaidOut]); элемент
 * сам ничего не регистрирует: LazyList переиспользует композицию
 * ушедшего элемента для нового, и всё, что элемент помнит о себе, может
 * пережить смену ключа. Поля обычные, не snapshot-state: читаются в
 * graphicsLayer, который перерисовывается по прогрессу анимации.
 */
internal class InsertShiftTracker {

    /** Взведён апдейтом набора элементов у конца ленты: следующая раскладка даст сдвиг. */
    var armed: Boolean = false

    private val distances = HashMap<Any, Float>()
    private val actionKeys = HashSet<Any>()
    private var maxLaidOutOrder = Int.MIN_VALUE
    private var hasLaidOut = false
    private var anchorKey: Any? = null
    private var anchorOffset = 0

    /**
     * Новое сообщение — с порядком выше всех уже размещённых. История
     * (в том числе после поворота, когда трекер создан заново) новой не
     * бывает: иначе старые пузыри въезжали бы и морфились заново при
     * скролле вверх. До первой раскладки новых нет.
     */
    fun isNewOrder(order: Int): Boolean = hasLaidOut && order > maxLaidOutOrder

    /** Дистанция въезда по ключу; null — переход этому ключу не выдан. */
    fun distanceOf(key: Any): Float? = distances[key]

    /**
     * Добавка к въезду по ключу: [extraPx] (нижний отступ ленты — старт
     * целиком под полем ввода) только при реальном въезде (сдвиг > 0);
     * при подвозе из истории и при оседании (сдвиг ≤ 0) — 0. Та же
     * добавка компенсируется в спуске аватара.
     */
    fun slideExtra(key: Any, extraPx: Float): Float =
        if ((distances[key] ?: 0f) > 0f) extraPx else 0f

    /**
     * Раскладка прошла: считает сдвиг якоря (если взведён) и раздаёт его
     * новым ключам этого прохода. Дистанция выдаётся один раз (пока
     * ключа нет в карте), чтобы промежуточные проходы не обнулили её
     * посреди въезда. Дистанции сообщений не чистятся (элемент может на
     * проход выпасть из видимых — и остался бы без неё навсегда); у
     * кнопок — чистятся при исчезновении, чтобы следующее появление
     * получило свою. [lastMessageIsNew] — чипы новые вместе с вопросом.
     */
    fun onLaidOut(items: List<LaidOutItem>, lastMessageIsNew: Boolean) {
        val shift = if (armed) {
            armed = false
            items.firstOrNull { it.key == anchorKey }
                ?.let { (it.offset - anchorOffset).toFloat() }
                ?: 0f
        } else {
            0f
        }
        var maxOrder = maxLaidOutOrder
        items.forEach { item ->
            val isNew = if (item.isAction) {
                actionKeys += item.key
                lastMessageIsNew
            } else {
                item.order?.let { isNewOrder(it) } ?: false
            }
            if (isNew && item.key !in distances) distances[item.key] = shift
            item.order?.let { if (it > maxOrder) maxOrder = it }
        }
        maxLaidOutOrder = maxOrder
        hasLaidOut = true

        val visibleKeys = items.mapTo(HashSet()) { it.key }
        actionKeys.forEach { key -> if (key !in visibleKeys) distances.remove(key) }
        items.firstOrNull { !it.isAction }?.let {
            anchorKey = it.key
            anchorOffset = it.offset
        }
    }
}
