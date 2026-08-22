package toybox.tools

/**
 * CPython's `random` module (MT19937), ported exactly.
 *
 * The sound generator's determinism contract is stronger than "same spec":
 * regenerating must produce the very same bytes as the committed assets, and
 * the noise layers draw their samples from this stream. Kotlin's own Random
 * would give same-character-but-different-bytes audio, so the generator is
 * seeded and stepped precisely as CPython does it: init_by_array seeding on
 * the 32-bit words of the absolute seed, and `random()` as genrand_res53.
 */
class CpythonRandom(seed: Long) {

    private val mt = IntArray(N)
    private var index = N + 1

    init {
        initByArray(wordsOf(seed))
    }

    /** Python's random.random(): a double in [0, 1) with 53-bit resolution. */
    fun random(): Double {
        val a = nextInt32() ushr 5
        val b = nextInt32() ushr 6
        return (a * 67108864.0 + b) / 9007199254740992.0
    }

    /** Python's random.uniform(a, b). */
    fun uniform(low: Double, high: Double): Double = low + (high - low) * random()

    private fun nextInt32(): Int {
        if (index >= N) {
            generate()
            index = 0
        }
        var y = mt[index]
        y = y xor (y ushr 11)
        y = y xor ((y shl 7) and 0x9d2c5680.toInt())
        y = y xor ((y shl 15) and 0xefc60000.toInt())
        y = y xor (y ushr 18)
        index++
        return y
    }

    private fun generate() {
        for (i in 0 until N) {
            val y = (mt[i] and UPPER_MASK) or (mt[(i + 1) % N] and LOWER_MASK)
            mt[i] = mt[(i + M) % N] xor (y ushr 1) xor (if (y and 1 != 0) MATRIX_A else 0)
        }
    }

    private fun initGenrand(s: Int) {
        mt[0] = s
        for (i in 1 until N) {
            val prev = mt[i - 1]
            mt[i] = 1812433253 * (prev xor (prev ushr 30)) + i
        }
        index = N
    }

    private fun initByArray(key: IntArray) {
        initGenrand(19650218)
        var i = 1
        var j = 0
        var k = maxOf(N, key.size)
        while (k > 0) {
            val prev = mt[i - 1]
            mt[i] = (mt[i] xor ((prev xor (prev ushr 30)) * 1664525)) + key[j] + j
            i++
            j++
            if (i >= N) {
                mt[0] = mt[N - 1]
                i = 1
            }
            if (j >= key.size) j = 0
            k--
        }
        k = N - 1
        while (k > 0) {
            val prev = mt[i - 1]
            mt[i] = (mt[i] xor ((prev xor (prev ushr 30)) * 1566083941)) - i
            i++
            if (i >= N) {
                mt[0] = mt[N - 1]
                i = 1
            }
            k--
        }
        mt[0] = UPPER_MASK
    }

    companion object {
        private const val N = 624
        private const val M = 397
        private const val MATRIX_A = 0x9908b0df.toInt()
        private const val UPPER_MASK = 0x80000000.toInt()
        private const val LOWER_MASK = 0x7fffffff

        /** CPython random_seed: the 32-bit little-endian words of |seed|. */
        private fun wordsOf(seed: Long): IntArray {
            var v = if (seed < 0) seed.inv() + 1 else seed
            if (v == 0L) return IntArray(0)
            val words = ArrayList<Int>(4)
            while (v != 0L) {
                words += (v and 0xFFFFFFFFL).toInt()
                v = v ushr 32
            }
            return words.toIntArray()
        }
    }
}
