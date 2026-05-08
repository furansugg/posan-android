package com.posan.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore by preferencesDataStore(name = "posan_session")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val USER_ID = longPreferencesKey("user_id")
    private val USER_NAME = stringPreferencesKey("user_name")
    private val USER_ROLE = stringPreferencesKey("user_role")

    val currentUserId: Flow<Long?> = context.sessionDataStore.data.map { it[USER_ID] }
    val currentUserName: Flow<String?> = context.sessionDataStore.data.map { it[USER_NAME] }
    val currentUserRole: Flow<String?> = context.sessionDataStore.data.map { it[USER_ROLE] }

    suspend fun setSession(id: Long, name: String, role: String) {
        context.sessionDataStore.edit { prefs ->
            prefs[USER_ID] = id
            prefs[USER_NAME] = name
            prefs[USER_ROLE] = role
        }
    }

    suspend fun clear() {
        context.sessionDataStore.edit { it.clear() }
    }
}
