package com.silentvoix.app.data.phrases

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.silentvoix.app.domain.phrases.PhraseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [PhraseRepository] in a Preferences DataStore on this device. Phrases are single lines
 * ([PhraseRepository.normalize]), which lets the list be stored as one newline-separated string
 * and keep its order.
 */
class DataStorePhraseRepository(private val dataStore: DataStore<Preferences>) : PhraseRepository {

    override val phrases: Flow<List<String>> = dataStore.data.map { it.toPhrases() }

    override suspend fun add(text: String) {
        val phrase = PhraseRepository.normalize(text)
        if (phrase.isEmpty()) return
        dataStore.edit { prefs ->
            val others = prefs.toPhrases().filterNot { it.equals(phrase, ignoreCase = true) }
            prefs[SAVED] = (listOf(phrase) + others).take(PhraseRepository.MAX_PHRASES).joinToString(SEPARATOR)
        }
    }

    override suspend fun remove(phrase: String) {
        dataStore.edit { prefs ->
            prefs[SAVED] = prefs.toPhrases().filterNot { it == phrase }.joinToString(SEPARATOR)
        }
    }

    private fun Preferences.toPhrases(): List<String> =
        this[SAVED]?.split(SEPARATOR)?.filter { it.isNotBlank() }.orEmpty()

    private companion object {
        const val SEPARATOR = "\n"
        val SAVED = stringPreferencesKey("saved_phrases")
    }
}
