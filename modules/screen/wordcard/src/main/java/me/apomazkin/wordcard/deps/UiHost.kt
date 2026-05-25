package me.apomazkin.wordcard.deps

/**
 * Абстракция для one-shot UI-уведомлений (snackbar).
 */
interface UiHost {

    /**
     * Показать snackbar без действия. Возвращается после dismiss/timeout.
     *
     * @param messageRes текст сообщения.
     */
    suspend fun showSnackbar(messageRes: Int)

    /**
     * Показать snackbar с action-кнопкой.
     *
     * @param messageRes текст сообщения.
     * @param actionLabelRes текст action-кнопки.
     * @return `true` если нажата action-кнопка, `false` если dismiss/timeout.
     */
    suspend fun showSnackbarWithAction(messageRes: Int, actionLabelRes: Int): Boolean
}
