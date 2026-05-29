package fm.mrc.sfeduchat.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("session")

class SessionPreferences(private val context: Context) {
    private val currentUidKey = stringPreferencesKey("current_uid")

    val currentUid: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[currentUidKey]
    }

    suspend fun setCurrentUid(uid: String?) {
        println("DEBUG SessionPreferences: setCurrentUid called with uid=$uid")
        context.dataStore.edit { prefs ->
            if (uid == null) prefs.remove(currentUidKey) else prefs[currentUidKey] = uid
        }
    }
}
