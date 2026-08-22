package toybox.tools

import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.outputStream

/**
 * The app's six sound effects, synthesized to spec: tiny, license-free, and
 * deterministic down to the byte (the noise layers draw from a ported
 * CPython RNG, so regenerating rewrites nothing).
 *
 * The religious constraint is a design input here, not an afterthought: there
 * is no music in this app. Five of the six sounds are deliberately inharmonic:
 * noise bursts and damped non-integer partials, so they read as physical
 * events (wood, cloth, a landing) rather than as notes. Only [chime] has a
 * pitch, it is a single struck bell, and the app never plays it twice inside
 * 1200 ms, because two pitched notes in sequence make an interval and
 * intervals are where melody starts.
 */
object SoundGen {

    private const val RATE = 44100
    private val NAMES = listOf("sfx_tick", "sfx_thud", "sfx_rustle", "sfx_hollow", "sfx_clink", "sfx_chime")

    // -- DSP ------------------------------------------------------------------

    /** Percussive envelope: near-instant attack, exponential decay. */
    private fun envelope(n: Int, attack: Double, decay: Double, curve: Double = 3.0): DoubleArray {
        val out = DoubleArray(n)
        val a = maxOf(1, (attack * RATE).toInt())
        // The decay term divides as a float, never truncated: that half-sample
        // matters once the result hits 16-bit quantization.
        val denom = maxOf(1.0, decay * RATE)
        for (i in 0 until n) {
            out[i] = if (i < a) {
                i.toDouble() / a
            } else {
                val t = (i - a).toDouble() / denom
                kotlin.math.exp(-curve * t)
            }
        }
        return out
    }

    private fun noise(n: Int, rng: CpythonRandom): DoubleArray =
        DoubleArray(n) { rng.uniform(-1.0, 1.0) }

    /** One-pole low-pass; enough to turn white noise into something wooden. */
    private fun lowpass(samples: DoubleArray, cutoff: Double): DoubleArray {
        val alpha = 1.0 - kotlin.math.exp(-2.0 * Math.PI * cutoff / RATE)
        val out = DoubleArray(samples.size)
        var prev = 0.0
        for (i in samples.indices) {
            prev += alpha * (samples[i] - prev)
            out[i] = prev
        }
        return out
    }

    private fun highpass(samples: DoubleArray, cutoff: Double): DoubleArray {
        val low = lowpass(samples, cutoff)
        return DoubleArray(samples.size) { samples[it] - low[it] }
    }

    /** Sum of damped sinusoids. Non-integer ratios keep it inharmonic. */
    private fun partials(n: Int, freqs: DoubleArray, decays: DoubleArray, gains: DoubleArray): DoubleArray {
        val out = DoubleArray(n)
        for ((layer, f) in freqs.withIndex()) {
            val d = decays[layer]
            val g = gains[layer]
            for (i in 0 until n) {
                val t = i.toDouble() / RATE
                out[i] += g * kotlin.math.sin(2.0 * Math.PI * f * t) * kotlin.math.exp(-t / d)
            }
        }
        return out
    }

    private fun mix(vararg layers: DoubleArray): DoubleArray {
        val n = layers.maxOf { it.size }
        val out = DoubleArray(n)
        for (layer in layers) for (i in layer.indices) out[i] += layer[i]
        return out
    }

    private operator fun DoubleArray.times(k: Double): DoubleArray = DoubleArray(size) { this[it] * k }

    private fun applyEnv(samples: DoubleArray, env: DoubleArray): DoubleArray =
        DoubleArray(samples.size) { samples[it] * env[it] }

    // -- The six sounds ---------------------------------------------------------

    /** 60 ms: a soft dry wooden tap. Every count-tap. Must feel instant. */
    private fun tick(rng: CpythonRandom): DoubleArray {
        val n = (0.060 * RATE).toInt()
        val body = lowpass(noise(n, rng), 2600.0)
        val knock = partials(n, doubleArrayOf(420.0, 690.0), doubleArrayOf(0.012, 0.008), doubleArrayOf(0.6, 0.3))
        return applyEnv(mix(body, knock), envelope(n, 0.0006, 0.020, curve = 5.0))
    }

    /** 120 ms: an object landing in a tray. Weight, no ring. */
    private fun thud(rng: CpythonRandom): DoubleArray {
        val n = (0.120 * RATE).toInt()
        val low = partials(n, doubleArrayOf(95.0, 141.0), doubleArrayOf(0.045, 0.030), doubleArrayOf(1.0, 0.45))
        val grit = lowpass(noise(n, rng), 900.0)
        return applyEnv(mix(low, grit * 0.35), envelope(n, 0.001, 0.045, curve = 4.0))
    }

    /** 400 ms: many things settling at once. The pour. */
    private fun rustle(rng: CpythonRandom): DoubleArray {
        val n = (0.400 * RATE).toInt()
        val layer = highpass(lowpass(noise(n, rng), 5200.0), 700.0)
        // Amplitude wobble so it sounds like many small events, not one hiss.
        val out = DoubleArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / RATE
            val wobble = 0.65 + 0.35 * kotlin.math.abs(
                kotlin.math.sin(2.0 * Math.PI * 11.3 * t + kotlin.math.sin(2.0 * Math.PI * 3.1 * t)),
            )
            out[i] = layer[i] * wobble
        }
        return applyEnv(out, envelope(n, 0.010, 0.150, curve = 2.5))
    }

    /** 80 ms: a knock on an empty box. The sound of "that did nothing". */
    private fun hollow(rng: CpythonRandom): DoubleArray {
        val n = (0.080 * RATE).toInt()
        val box = partials(n, doubleArrayOf(232.0, 351.0, 508.0), doubleArrayOf(0.030, 0.018, 0.010), doubleArrayOf(1.0, 0.5, 0.22))
        return applyEnv(mix(box, lowpass(noise(n, rng), 1800.0) * 0.18), envelope(n, 0.0008, 0.028, curve = 4.0))
    }

    /** 200 ms: one pebble into a glass jar. Bright, but deliberately inharmonic. */
    private fun clink(rng: CpythonRandom): DoubleArray {
        val n = (0.200 * RATE).toInt()
        // Ratios 1 : 2.76 : 5.40 are the classic inharmonic bar partials: bright
        // and glassy, but not a pitch you could hum.
        val glass = partials(n, doubleArrayOf(1180.0, 3257.0, 6372.0), doubleArrayOf(0.055, 0.030, 0.016), doubleArrayOf(1.0, 0.42, 0.16))
        return applyEnv(mix(glass, highpass(noise(n, rng), 3000.0) * 0.12), envelope(n, 0.0005, 0.060, curve = 3.5))
    }

    /** 450 ms: one soft struck bell. The only pitched sound in the app. */
    private fun chime(): DoubleArray {
        val n = (0.450 * RATE).toInt()
        val f = 587.33 // a single note, struck once, never followed by another
        val bell = partials(
            n,
            doubleArrayOf(f, f * 2.0, f * 3.01, f * 4.17),
            doubleArrayOf(0.230, 0.150, 0.090, 0.055),
            doubleArrayOf(1.0, 0.34, 0.15, 0.07),
        )
        return applyEnv(bell, envelope(n, 0.004, 0.190, curve = 2.2))
    }

    // -- Output -----------------------------------------------------------------

    /** Normalizes, fades the tail, and writes a canonical 16-bit mono WAV. */
    private fun write(outDir: Path, name: String, samples: DoubleArray, peak: Double) {
        val n = samples.size
        val high = samples.maxOf { kotlin.math.abs(it) }.takeIf { it > 0 } ?: 1.0
        val scale = peak / high
        val fade = minOf((0.006 * RATE).toInt(), n)
        val frames = ByteArrayOutputStream(n * 2)
        for (i in 0 until n) {
            var v = samples[i] * scale
            if (i >= n - fade) v *= (n - i).toDouble() / fade
            val q = (v * 32767.0).toInt().coerceIn(-32767, 32767)
            frames.write(q and 0xFF)
            frames.write((q shr 8) and 0xFF)
        }
        val path = outDir.resolve("$name.wav")
        path.outputStream().use { stream ->
            val data = frames.toByteArray()
            val header = ByteArray(44)
            fun putU32(at: Int, v: Int) {
                header[at] = (v and 0xFF).toByte(); header[at + 1] = ((v shr 8) and 0xFF).toByte()
                header[at + 2] = ((v shr 16) and 0xFF).toByte(); header[at + 3] = ((v shr 24) and 0xFF).toByte()
            }
            fun putU16(at: Int, v: Int) {
                header[at] = (v and 0xFF).toByte(); header[at + 1] = ((v shr 8) and 0xFF).toByte()
            }
            val riffSize = 36 + data.size
            "RIFF".toByteArray().copyInto(header, 0)
            putU32(4, riffSize)
            "WAVE".toByteArray().copyInto(header, 8)
            "fmt ".toByteArray().copyInto(header, 12)
            putU32(16, 16)
            putU16(20, 1) // PCM
            putU16(22, 1) // mono
            putU32(24, RATE)
            putU32(28, RATE * 2)
            putU16(32, 2)
            putU16(34, 16)
            "data".toByteArray().copyInto(header, 36)
            putU32(40, data.size)
            stream.write(header)
            stream.write(data)
        }
        val kb = Files.size(path) / 1024.0
        println("${path.absolutePathString()}  ${n * 1000 / RATE} ms  ${"%.1f".format(kb)} KB")
    }

    /** Regenerates every asset, in one fixed order, into [outDir]. */
    fun generateAll(outDir: Path) {
        Files.createDirectories(outDir)
        val rng = CpythonRandom(20260727L)
        write(outDir, "sfx_tick", tick(rng), peak = 0.55)
        write(outDir, "sfx_thud", thud(rng), peak = 0.75)
        write(outDir, "sfx_rustle", rustle(rng), peak = 0.55)
        write(outDir, "sfx_hollow", hollow(rng), peak = 0.50)
        write(outDir, "sfx_clink", clink(rng), peak = 0.62)
        write(outDir, "sfx_chime", chime(), peak = 0.70)
    }

    /**
     * Regenerates into a temp dir and byte-compares against the committed
     * WAVs; exits non-zero if any committed asset would change, so an
     * accidental binary edit cannot ride along unnoticed until release.
     */
    fun check(rawDir: Path): Int {
        val tmp = Files.createTempDirectory("count-and-play-sounds")
        generateAll(tmp)
        val bad = mutableListOf<String>()
        for (name in NAMES) {
            val committed = rawDir.resolve("$name.wav")
            if (!Files.exists(committed)) {
                bad += "${committed.absolutePathString()} is missing"
                continue
            }
            val fresh = Files.readAllBytes(tmp.resolve("$name.wav"))
            if (!fresh.contentEquals(Files.readAllBytes(committed))) {
                bad += "${committed.absolutePathString()} differs from a fresh regeneration"
            }
        }
        return if (bad.isNotEmpty()) {
            for (line in bad) println("MISMATCH: $line")
            1
        } else {
            println("All six sound assets match a fresh regeneration.")
            0
        }
    }
}
