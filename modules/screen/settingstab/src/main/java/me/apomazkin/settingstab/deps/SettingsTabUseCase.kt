package me.apomazkin.settingstab.deps

import android.net.Uri

interface SettingsTabUseCase {
    suspend fun exportData(uri: Uri): Uri
    suspend fun importData(uri: Uri): Boolean
}