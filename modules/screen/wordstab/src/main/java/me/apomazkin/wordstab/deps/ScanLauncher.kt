package me.apomazkin.wordstab.deps

/**
 * Порт запуска системных приложений за снимком: камера и выбор снимка из
 * галереи. Эффекты вкладки открыть их сами не могут — launcher'ы живут в
 * композиции.
 */
interface ScanLauncher {

    /**
     * Открыть системную камеру (WFP-06.1).
     * @return false — камера не открылась: приложения нет, его отключили или
     * файл под снимок не создался (WFP-07).
     */
    fun launchCamera(): Boolean

    /** Открыть системный выбор снимка (WFP-06.2). */
    fun launchGallery()
}

/**
 * Держатель запуска с поздней привязкой: ViewModel живёт дольше Activity, а
 * launcher'ы — только пока жива композиция вкладки, поэтому [bind] зовёт
 * композиция, [unbind] — её уход. Без привязки камера не открывается
 * (false), галерея не делает ничего.
 */
class ScanLauncherHolder : ScanLauncher {

    @Volatile
    private var launcher: ScanLauncher? = null

    /** Привязать launcher текущей композиции. */
    fun bind(launcher: ScanLauncher) {
        this.launcher = launcher
    }

    /** Отвязать launcher: композиция ушла. */
    fun unbind() {
        launcher = null
    }

    override fun launchCamera(): Boolean = launcher?.launchCamera() ?: false

    override fun launchGallery() {
        launcher?.launchGallery()
    }
}
