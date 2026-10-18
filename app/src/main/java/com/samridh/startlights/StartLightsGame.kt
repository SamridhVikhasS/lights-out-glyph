package com.samridh.startlights

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlin.random.Random

class StartLightsGame(
    private val prefs: SharedPreferences,
    private val onFrame: (IntArray, String) -> Unit,
    private val onJumpStart: () -> Unit
) {
    enum class State { READY, LIGHTS_ON, HOLD, GO, RESULT, JUMP_START }

    companion object { const val SESSION_SIZE = 5 }

    private val handler = Handler(Looper.getMainLooper())
    private var state = State.READY
    private var lit = 0
    private var goTime = 0L
    private val times = mutableListOf<Long>()      // valid times this session

    // Saved on the phone; 0 means "no record yet"
    private var bestSingle: Long
        get() = prefs.getLong("best_single", 0L)
        set(v) = prefs.edit().putLong("best_single", v).apply()
    private var bestMedian: Long
        get() = prefs.getLong("best_median", 0L)
        set(v) = prefs.edit().putLong("best_median", v).apply()

    private fun fmt(v: Long) = if (v == 0L) "-" else "$v ms"

    // Every normal frame gets the progress dots added
    private fun show(frame: IntArray, msg: String) =
        onFrame(Frames.withProgress(frame, times.size), msg)

    fun reset() {
        handler.removeCallbacksAndMessages(null)
        times.clear()
        state = State.READY
        show(Frames.ready(), "Best: ${fmt(bestSingle)}  |  Best median: ${fmt(bestMedian)}")
    }

    fun stop() = handler.removeCallbacksAndMessages(null)

    fun press(pressTime: Long) {
        when (state) {
            State.READY, State.RESULT -> startSequence()
            State.LIGHTS_ON, State.HOLD -> jumpStart()
            State.GO -> finish(pressTime - goTime)
            State.JUMP_START -> { /* ignore while flashing */ }
        }
    }

    private fun startSequence() {
        if (times.size == SESSION_SIZE) times.clear()   // last session finished: start fresh
        state = State.LIGHTS_ON
        lit = 0
        nextLight()
    }

    private fun nextLight() {
        lit++
        show(Frames.lights(lit), "Lights...")
        if (lit < 5) {
            handler.postDelayed({ nextLight() }, 1000)
        } else {
            state = State.HOLD
            handler.postDelayed({ lightsOut() }, Random.nextLong(200, 3001))
        }
    }

    private fun lightsOut() {
        state = State.GO
        show(Frames.blank(), "GO!")
        goTime = SystemClock.uptimeMillis()
    }

    private fun finish(ms: Long) {
        state = State.RESULT
        if (ms < 100) {
            show(Frames.number(ms, withRing = true), "Anticipated! $ms ms doesn't count")
            return
        }
        times.add(ms)
        if (bestSingle == 0L || ms < bestSingle) bestSingle = ms

        if (times.size < SESSION_SIZE) {
            show(Frames.number(ms), "$ms ms   (${times.size}/$SESSION_SIZE)")
        } else {
            val median = times.sorted()[SESSION_SIZE / 2]
            val newBest = bestMedian == 0L || median < bestMedian
            if (newBest) bestMedian = median
            onFrame(
                Frames.withProgress(Frames.number(median), SESSION_SIZE, full = true),
                "Session median: $median ms" + if (newBest) "  NEW BEST!" else "  (best: $bestMedian ms)"
            )
        }
    }

    private fun jumpStart() {
        handler.removeCallbacksAndMessages(null)
        state = State.JUMP_START
        onJumpStart()
        flash(6)
    }

    private fun flash(remaining: Int) {
        onFrame(if (remaining % 2 == 0) Frames.ring() else Frames.blank(), "JUMP START!")
        if (remaining > 1) {
            handler.postDelayed({ flash(remaining - 1) }, 250)
        } else {
            state = State.RESULT
            show(Frames.ready(), "Jump start, doesn't count. Press to retry (${times.size}/$SESSION_SIZE)")
        }
    }
}