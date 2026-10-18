package com.samridh.startlights

object Grid {
    const val SIZE = 13
    // LEDs per row on the circular matrix (adds up to 137)
    private val ROW_WIDTHS = intArrayOf(5, 9, 11, 11, 13, 13, 13, 13, 13, 11, 11, 9, 5)

    fun exists(r: Int, c: Int): Boolean {
        if (r !in 0 until SIZE || c !in 0 until SIZE) return false
        val w = ROW_WIDTHS[r]
        val start = (SIZE - w) / 2
        return c >= start && c < start + w
    }

    fun idx(r: Int, c: Int) = r * SIZE + c
}

object Frames {
    private const val FULL = 255
    private const val DIM = 40

    // 3x5 pixel digits 0-9, read left-to-right, top-to-bottom
    private val FONT = arrayOf(
        "111101101101111", "010110010010111", "111001111100111", "111001111001111",
        "101101111001001", "111100111001111", "111100111101111", "111001001001001",
        "111101111101111", "111101111001111"
    )

    fun blank() = IntArray(Grid.SIZE * Grid.SIZE)

    fun ready() = blank().also { it[Grid.idx(6, 6)] = DIM }

    // n = how many of the 5 red bars are lit
    fun lights(n: Int) = blank().also { f ->
        for (i in 0 until n) {
            val col = 2 + i * 2
            for (row in 4..8) f[Grid.idx(row, col)] = FULL
        }
    }

    // Outer edge of the circle
    fun ring(level: Int = FULL) = blank().also { f ->
        for (r in 0 until Grid.SIZE) for (c in 0 until Grid.SIZE) {
            if (!Grid.exists(r, c)) continue
            val edge = listOf(r - 1 to c, r + 1 to c, r to c - 1, r to c + 1)
                .any { (a, b) -> !Grid.exists(a, b) }
            if (edge) f[Grid.idx(r, c)] = level
        }
    }

    // Reaction time as 3 digits; optional ring for "anticipated"
    fun number(ms: Long, withRing: Boolean = false): IntArray {
        val f = if (withRing) ring(DIM) else blank()
        val text = ms.coerceIn(0, 999).toString().padStart(3, '0')
        text.forEachIndexed { i, ch -> drawDigit(f, ch - '0', top = 4, left = 1 + i * 4) }
        return f
    }
    // Session progress: dots on row 10 at columns 2,4,6,8,10
    fun withProgress(frame: IntArray, done: Int, full: Boolean = false): IntArray {
        val f = frame.copyOf()
        val level = if (full) FULL else DIM
        for (i in 0 until done.coerceAtMost(5)) f[Grid.idx(10, 2 + i * 2)] = level
        return f
    }

    private fun drawDigit(f: IntArray, d: Int, top: Int, left: Int) {
        val p = FONT[d]
        for (r in 0 until 5) for (c in 0 until 3)
            if (p[r * 3 + c] == '1') f[Grid.idx(top + r, left + c)] = FULL
    }
}