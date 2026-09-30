package com.silentvoix.app.data.phrases

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The user's own phrases for the Speak screen, newest first, kept in a Preferences DataStore on
 * this device. Phrases are single lines: whitespace is collapsed before saving, which also lets
 * the list be stored as one newline-separated string and keep its order.
 */
class PhraseRepository(private val dataStore: DataStore<Preferences>) {

    val phrases: Flow<List<String>> = dataStore.data.map { it.toPhrases() }

    /** Saves [text] at the top of the list. Blank text is ignored; a phrase saved again moves to the top. */
    suspend fun add(text: String) {
        val phrase = normalize(text)
        if (phrase.isEmpty()) return
        dataStore.edit { prefs ->
            val others = prefs.toPhrases().filterNot { it.equals(phrase, ignoreCase = true) }
            prefs[SAVED] = (listOf(phrase) + others).take(MAX_PHRASES).joinToString(SEPARATOR)
        }
    }

    suspend fun remove(phrase: String) {
        dataStore.edit { prefs ->
            prefs[SAVED] = prefs.toPhrases().filterNot { it == phrase }.joinToString(SEPARATOR)
        }
    }

    private fun Preferences.toPhrases(): List<String> =
        this[SAVED]?.split(SEPARATOR)?.filter { it.isNotBlank() }.orEmpty()

    companion object {
        const val MAX_PHRASES = 40
        const val MAX_LENGTH = 200
        private const val SEPARATOR = "\n"
        private val SAVED = stringPreferencesKey("saved_phrases")
        private val Whitespace = Regex("\\s+")

        /** A phrase as it is stored and spoken: one line, single spaces, at most [MAX_LENGTH] characters. */
        fun normalize(text: String): String = text.trim().replace(Whitespace, " ").take(MAX_LENGTH).trim()
    }
}
