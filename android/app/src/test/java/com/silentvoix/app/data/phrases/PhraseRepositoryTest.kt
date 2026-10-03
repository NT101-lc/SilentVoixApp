package com.silentvoix.app.data.phrases

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PhraseRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    /** A fresh DataStore on the same file, as after an app restart. */
    private fun openStore() = CoroutineScope(Dispatchers.IO + SupervisorJob()).let { scope ->
        scopes += scope
        PreferenceDataStoreFactory.create(scope = scope) { folder.root.resolve("phrases.preferences_pb") }
    }

    @After
    fun tearDown() = scopes.forEach { it.cancel() }

    @Test
    fun `nothing is saved to begin with`() = runBlocking {
        assertEquals(emptyList<String>(), PhraseRepository(openStore()).phrases.first())
    }

    @Test
    fun `phrases are listed newest first and survive a restart`() = runBlocking {
        val first = openStore()
        PhraseRepository(first).run {
            add("Cho tôi một ly cà phê sữa")
            add("Tôi đến đón con")
        }
        scopes.removeAt(0).coroutineContext.job.cancelAndJoin()

        assertEquals(
            listOf("Tôi đến đón con", "Cho tôi một ly cà phê sữa"),
            PhraseRepository(openStore()).phrases.first(),
        )
    }

    @Test
    fun `line breaks and extra spaces are collapsed so a phrase stays one item`() = runBlocking {
        val repository = PhraseRepository(openStore())
        repository.add("  Tôi cần\n\ngặp   bác sĩ \n")
        assertEquals(listOf("Tôi cần gặp bác sĩ"), repository.phrases.first())
    }

    @Test
    fun `blank text is not saved`() = runBlocking {
        val repository = PhraseRepository(openStore())
        repository.add("   \n ")
        assertEquals(emptyList<String>(), repository.phrases.first())
    }

    @Test
    fun `saving a phrase again moves it to the top instead of duplicating it`() = runBlocking {
        val repository = PhraseRepository(openStore())
        repository.add("Xin chào")
        repository.add("Cảm ơn")
        repository.add("xin chào")
        assertEquals(listOf("xin chào", "Cảm ơn"), repository.phrases.first())
    }

    @Test
    fun `removing a phrase leaves the others in order`() = runBlocking {
        val repository = PhraseRepository(openStore())
        listOf("Một", "Hai", "Ba").forEach { repository.add(it) }
        repository.remove("Hai")
        assertEquals(listOf("Ba", "Một"), repository.phrases.first())
    }

    @Test
    fun `the list is capped, dropping the oldest`() = runBlocking {
        val repository = PhraseRepository(openStore())
        repeat(PhraseRepository.MAX_PHRASES + 3) { repository.add("Câu $it") }
        val phrases = repository.phrases.first()
        assertEquals(PhraseRepository.MAX_PHRASES, phrases.size)
        assertEquals("Câu ${PhraseRepository.MAX_PHRASES + 2}", phrases.first())
        assertEquals("Câu 3", phrases.last())
    }

    @Test
    fun `very long text is cut to the maximum length`() {
        assertEquals(PhraseRepository.MAX_LENGTH, PhraseRepository.normalize("a".repeat(500)).length)
    }
}
