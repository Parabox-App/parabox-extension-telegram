package com.ojhdtapp.parabox.extension.telegram.core.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object DataStoreKeys{
    val AUTO_LOGIN = booleanPreferencesKey("auto_login")
    val FOREGROUND_SERVICE = booleanPreferencesKey("foreground_service")
}