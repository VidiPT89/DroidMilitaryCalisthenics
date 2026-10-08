package dev.ividi.militarycalisthenics.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.ividi.militarycalisthenics.model.*
import dev.ividi.militarycalisthenics.planengine.PlanEngine
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class PlanRepositoryTest {
    private class MemoryStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
        override val data = MutableStateFlow(initial)
        var failWrites = false
        private val lock = Mutex()
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = lock.withLock {
            val updated = transform(data.value)
            if (failWrites) throw IOException("Disk unavailable")
            data.value = updated
            updated
        }
    }

    private fun plan() = PlanEngine.generate(UserProfile(75.0, 175.0, 28,
        level = FitnessLevel.BEGINNER, goal = Goal.FAT_LOSS, daysPerWeek = 4))

    @Test
    fun `failed transaction keeps plan history and acknowledgement intact`() = runBlocking {
        val store = MemoryStore()
        val repository = PlanRepository(store)
        val original = repository.update { it.copy(plan = plan(), completionAcknowledged = true) }
        store.failWrites = true
        try {
            repository.update { it.copy(weightHistory = listOf(WeightEntry(1, 80.0)), completionAcknowledged = false) }
            fail("Expected write failure")
        } catch (_: IOException) { }
        assertEquals(original, repository.stateFlow.first())
    }

    @Test
    fun `corrupt stored JSON is not replaced by an empty plan`() = runBlocking {
        val key = stringPreferencesKey("training_plan")
        val store = MemoryStore(preferencesOf(key to "{broken"))
        val repository = PlanRepository(store)
        assertTrue(runCatching { repository.stateFlow.first() }.isFailure)
        assertTrue(runCatching { repository.update { it.copy(plan = plan()) } }.isFailure)
        assertEquals("{broken", store.data.value[key])
    }

    @Test
    fun `plan history and acknowledgement are saved together`() = runBlocking {
        val store = MemoryStore()
        val repository = PlanRepository(store)
        val expected = repository.update { it.copy(plan = plan(), weightHistory = listOf(WeightEntry(1, 75.0))) }
        assertEquals(expected, repository.stateFlow.first())
        assertFalse(repository.stateFlow.first().completionAcknowledged)
    }

    @Test
    fun `concurrent updates transform the latest state without lost entries`() = runBlocking {
        val repository = PlanRepository(MemoryStore())
        (1L..20L).map { index -> async {
            repository.update { it.copy(weightHistory = it.weightHistory + WeightEntry(index, 75.0)) }
        } }.awaitAll()
        assertEquals((1L..20L).toSet(), repository.stateFlow.first().weightHistory.map { it.timestampMillis }.toSet())
    }

    @Test
    fun `invalid changes are rejected without replacing stored data`() = runBlocking {
        val repository = PlanRepository(MemoryStore())
        val original = repository.update { it.copy(plan = plan()) }
        assertTrue(runCatching {
            repository.update { it.copy(weightHistory = listOf(WeightEntry(1, Double.NaN))) }
        }.isFailure)
        assertEquals(original, repository.stateFlow.first())
    }

    @Test
    fun `returned state matches normalized stored state`() = runBlocking {
        val repository = PlanRepository(MemoryStore())
        val saved = repository.update {
            it.copy(weightHistory = listOf(WeightEntry(2, 76.0), WeightEntry(1, 75.0)), reminderHour = 99)
        }
        assertEquals(saved, repository.stateFlow.first())
        assertEquals(23, saved.reminderHour)
        assertEquals(listOf(1L, 2L), saved.weightHistory.map { it.timestampMillis })
    }

    @Test
    fun `unrelated stored preferences survive updates`() = runBlocking {
        val key = stringPreferencesKey("future_preference")
        val store = MemoryStore(preferencesOf(key to "preserve"))
        PlanRepository(store).update { it.copy(plan = plan()) }
        assertEquals("preserve", store.data.value[key])
    }

    @Test
    fun `completion updates preserve deload and marking complete is idempotent`() {
        val plan = plan()
        assertTrue(plan.weeks.last().isDeload)
        val updated = plan.toggleWorkout(3, 0, completed = true)
        assertTrue(updated.weeks.last().isDeload)
        assertTrue(updated.weeks.last().workouts.first().completed)
        assertEquals(updated, updated.toggleWorkout(3, 0, completed = true))
        assertFalse(updated.toggleWorkout(3, 0).weeks.last().workouts.first().completed)
    }
}
