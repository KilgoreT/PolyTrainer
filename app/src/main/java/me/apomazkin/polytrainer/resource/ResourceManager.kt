package me.apomazkin.polytrainer.resource

import android.content.Context
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import me.apomazkin.ui.resource.ResourceManager
import javax.inject.Inject

class ResourceManagerImpl @Inject constructor(
    private val ctx: Context
) : ResourceManager {
    override fun stringByResId(@StringRes id: Int): String {
        return ctx.resources.getString(id)
    }
    
    override fun stringByArrayId(@ArrayRes id: Int): String {
        return ctx.resources.getStringArray(id).random()
    }
    
}