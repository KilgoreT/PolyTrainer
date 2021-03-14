package me.apomazkin.core_util

import android.content.Context

fun Int.toPx(context: Context): Float = this * context.resources.displayMetrics.density
fun Int.toDp(context: Context): Int = this / context.resources.displayMetrics.density.toInt()