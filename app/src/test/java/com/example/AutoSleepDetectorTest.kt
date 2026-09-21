package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.security.AutoSleepDetectorEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AutoSleepDetectorTest {

    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `auto sleep detector starts enabled with default 30s threshold`() {
        val engine = AutoSleepDetectorEngine(context, testScope)
        val state = engine.sleepState.value
        assertTrue(state.isEnabled)
        assertEquals(30, state.sleepThresholdSeconds)
    }

    @Test
    fun `fast forward triggers auto sleep arming callback`() {
        val engine = AutoSleepDetectorEngine(context, testScope)
        var armTriggered = false
        engine.onAutoArmTriggered = {
            armTriggered = true
        }

        engine.fastForwardSleepArming()

        assertTrue(armTriggered)
        assertTrue(engine.sleepState.value.isOwnerSleeping)
        assertTrue(engine.sleepState.value.isSleepArmed)
    }

    @Test
    fun `stranger touch during sleep triggers breach callback`() {
        val engine = AutoSleepDetectorEngine(context, testScope)
        var breachReported: String? = null
        engine.onStrangerTouchBreach = { detail ->
            breachReported = detail
        }

        engine.fastForwardSleepArming()
        engine.confirmStrangerBreach("Stranger test touch")

        assertNotNull(breachReported)
        assertTrue(engine.sleepState.value.lastVerificationResult?.contains("STRANGER") == true)
    }

    @Test
    fun `owner wake up touch verifies and disarms without alarm`() {
        val engine = AutoSleepDetectorEngine(context, testScope)
        var ownerWakeUpConfirmed = false
        engine.onOwnerWakeUpVerified = {
            ownerWakeUpConfirmed = true
        }

        engine.fastForwardSleepArming()
        val initialSessions = engine.sleepState.value.sleepSessionsLearnedCount

        engine.confirmOwnerWakeUp("Owner face recognized test")

        assertTrue(ownerWakeUpConfirmed)
        assertFalse(engine.sleepState.value.isOwnerSleeping)
        assertFalse(engine.sleepState.value.isSleepArmed)
        assertEquals(initialSessions + 1, engine.sleepState.value.sleepSessionsLearnedCount)
        assertTrue(engine.sleepState.value.lastVerificationResult?.contains("OWNER") == true)
    }
}
