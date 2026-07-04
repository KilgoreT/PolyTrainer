package me.apomazkin.tools

/**
 * F129/F142 fallback: snackbar / log message for [Throwable]. Prefer [Throwable.message]
 * → simple class name → "unknown". Never returns null — гарантирует, что UI не
 * показывает "Failed: null".
 *
 * Shared util — see CM/PerDict Reducer для исторического дубликата (F142 cleanup).
 */
fun Throwable.failureLabel(): String =
    message ?: this::class.simpleName ?: "unknown"
