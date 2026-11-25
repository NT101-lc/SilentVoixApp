package com.silentvoix.app.domain.phrases

import kotlinx.coroutines.flow.Flow

/** The user's own phrases for the Speak screen, newest first, single-line and capped. */
interface PhraseRepository {
    val phrases: Flow<List<String>>

    /** Saves [text] at the top of the list. Blank text is ignored; a phrase saved again moves to the top. */
    suspend fun add(text: String)

    suspend fun remove(phrase: String)

    companion object {
        const val MAX_PHRASES = 40
        const val MAX_LENGTH = 200
        private val Whitespace = Regex("\\s+")

        /** A phrase as it is stored and spoken: one line, single spaces, at most [MAX_LENGTH] characters. */
        fun normalize(text: String): String = text.trim().replace(Whitespace, " ").take(MAX_LENGTH).trim()
    }
}
