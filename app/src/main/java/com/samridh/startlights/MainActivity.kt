package com.samridh.startlights

import android.content.ComponentName
import android.os.Bundle
import android.os.VibrationEffect
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager


class MainActivity : ComponentActivity() {

    private val USE_MATRIX = true          // Phase 4: flip to true

    private var glyph: GlyphMatrixManager? = null
    private var frame by mutableStateOf(Frames.blank())
    private var status by mutableStateOf("")
    private lateinit var game: StartLightsGame

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val prefs = getSharedPreferences("stats", MODE_PRIVATE)
        game = StartLightsGame(prefs, onFrame = { f, msg -> show(f, msg) }, onJumpStart = { buzz() })
        setContent { GameScreen(frame, status) }
    }

    private fun show(f: IntArray, msg: String) {
        frame = f
        status = msg
        if (USE_MATRIX) glyph?.setAppMatrixFrame(f)
    }
    private fun buzz() {
        android.util.Log.d("StartLights", "buzz called")
        val vibrator = getSystemService(VibratorManager::class.java).defaultVibrator
        vibrator.vibrate(
            VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE),
            android.os.VibrationAttributes.createForUsage(android.os.VibrationAttributes.USAGE_MEDIA)
        )
    }

    override fun onStart() {
        super.onStart()
        game.reset()
        if (!USE_MATRIX) return
        val gm = GlyphMatrixManager.getInstance(applicationContext)
        glyph = gm
        gm.init(object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(name: ComponentName?) {
                gm.register(Glyph.DEVICE_25111p)
                gm.setAppMatrixFrame(frame)    // also wipes leftovers from a crash
            }
            override fun onServiceDisconnected(name: ComponentName?) {}
        })
    }

    override fun onStop() {
        game.stop()
        glyph?.closeAppMatrix()
        glyph?.unInit()
        glyph = null
        super.onStop()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event.repeatCount == 0) game.press(event.eventTime)
            return true                         // true = don't change the volume
        }
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
fun GameScreen(frame: IntArray, status: String) {
    Column(
        Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(Modifier.size(300.dp)) {
            val cell = size.width / Grid.SIZE
            for (r in 0 until Grid.SIZE) for (c in 0 until Grid.SIZE) {
                if (!Grid.exists(r, c)) continue
                val v = frame[Grid.idx(r, c)]
                val color = if (v == 0) Color(0xFF222222)
                else Color.Red.copy(alpha = 0.25f + 0.75f * v / 255f)
                drawCircle(color, radius = cell * 0.4f,
                    center = Offset(c * cell + cell / 2, r * cell + cell / 2))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(status, color = Color.White, fontSize = 20.sp)
    }
}