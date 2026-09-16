package com.example.util

/**
 * A real QR Code (ISO/IEC 18004, Model 2) encoder.
 *
 * The app used to draw a square of pseudo-random modules derived from a string
 * hash. It looked like a QR code and scanned as nothing at all, which is why
 * "Pay by QR" never opened a UPI app. This encodes the payload properly -
 * byte-mode segment, Reed-Solomon error correction, the eight data masks scored
 * against the standard penalty rules - so any scanner reads back exactly what
 * went in.
 *
 * Deliberately free of Android imports: the matrix is plain Kotlin, so the
 * encoder is covered by ordinary JVM unit tests and [QrRenderer] is the only
 * part that needs a device.
 */
object QrCode {

    /** How much of a damaged symbol can still be read back. */
    enum class Ecc(internal val formatBits: Int) {
        LOW(1),
        MEDIUM(0),
        QUARTILE(3),
        HIGH(2)
    }

    /** The smallest and largest symbol versions this encoder will produce. */
    private const val MIN_VERSION = 1
    private const val MAX_VERSION = 40

    /** Penalty weights for the four mask-scoring rules in the specification. */
    private const val PENALTY_N1 = 3
    private const val PENALTY_N2 = 3
    private const val PENALTY_N3 = 40
    private const val PENALTY_N4 = 10

    /**
     * One encoded symbol: a square grid where true is a dark module.
     *
     * [size] is the number of modules along one edge, and [get] is bounds
     * checked so a renderer can walk a quiet zone off the edge of the grid
     * without a special case.
     */
    class Symbol internal constructor(
        val version: Int,
        val size: Int,
        private val modules: Array<BooleanArray>
    ) {
        /** True when the module at (x, y) is dark. Off-grid reads are light. */
        operator fun get(x: Int, y: Int): Boolean =
            if (x in 0 until size && y in 0 until size) modules[y][x] else false
    }

    /**
     * Encodes [text] as a QR symbol, picking the smallest version that holds it.
     *
     * Throws [IllegalArgumentException] only when the payload is too long for a
     * version-40 symbol, which no UPI URI ever is.
     */
    fun encode(text: String, ecc: Ecc = Ecc.MEDIUM): Symbol {
        val data = text.toByteArray(Charsets.UTF_8)
        val version = smallestVersionFor(data.size, ecc)
        val codewords = buildCodewords(data, version, ecc)
        return Encoder(version, ecc).encode(codewords)
    }

    // --------------------------------------------------------- segment layout

    /** Bits a byte-mode segment of [byteCount] bytes occupies at [version]. */
    private fun byteSegmentBits(byteCount: Int, version: Int): Long {
        val countBits = if (version <= 9) 8 else 16
        return 4L + countBits + 8L * byteCount
    }

    private fun smallestVersionFor(byteCount: Int, ecc: Ecc): Int {
        for (version in MIN_VERSION..MAX_VERSION) {
            val capacityBits = numDataCodewords(version, ecc) * 8L
            if (byteSegmentBits(byteCount, version) <= capacityBits) return version
        }
        throw IllegalArgumentException("Data of $byteCount bytes is too long for a QR code")
    }

    /**
     * The data codewords for one byte-mode segment: mode indicator, character
     * count, the bytes themselves, a terminator, and the alternating pad bytes
     * the specification calls for.
     */
    private fun buildCodewords(data: ByteArray, version: Int, ecc: Ecc): ByteArray {
        val bits = BitBuffer()
        bits.append(0x4, 4)
        bits.append(data.size, if (version <= 9) 8 else 16)
        data.forEach { bits.append(it.toInt() and 0xFF, 8) }

        val capacityBits = numDataCodewords(version, ecc) * 8
        bits.append(0, minOf(4, capacityBits - bits.length))
        bits.append(0, (8 - bits.length % 8) % 8)

        var pad = 0xEC
        while (bits.length < capacityBits) {
            bits.append(pad, 8)
            pad = pad xor 0xFD // Alternates 0xEC and 0x11.
        }
        return bits.toBytes()
    }

    // ------------------------------------------------------------- capacities

    /** Modules a version has available before function patterns are removed. */
    private fun numRawDataModules(version: Int): Int {
        var result = (16 * version + 128) * version + 64
        if (version >= 2) {
            val numAlign = version / 7 + 2
            result -= (25 * numAlign - 10) * numAlign - 55
            if (version >= 7) result -= 36
        }
        return result
    }

    /** Codewords left for the payload once error correction has taken its share. */
    private fun numDataCodewords(version: Int, ecc: Ecc): Int =
        numRawDataModules(version) / 8 -
            ECC_CODEWORDS_PER_BLOCK[ecc.ordinal][version] *
            NUM_ECC_BLOCKS[ecc.ordinal][version]

    // ------------------------------------------------------- error correction

    /**
     * Splits the data into blocks, appends each block's Reed-Solomon remainder,
     * and interleaves the result the way the specification lays it out.
     */
    private fun addEccAndInterleave(data: ByteArray, version: Int, ecc: Ecc): ByteArray {
        val numBlocks = NUM_ECC_BLOCKS[ecc.ordinal][version]
        val blockEccLen = ECC_CODEWORDS_PER_BLOCK[ecc.ordinal][version]
        val rawCodewords = numRawDataModules(version) / 8
        val numShortBlocks = numBlocks - rawCodewords % numBlocks
        val shortBlockLen = rawCodewords / numBlocks

        val divisor = reedSolomonDivisor(blockEccLen)
        val blocks = ArrayList<ByteArray>(numBlocks)
        var read = 0
        for (i in 0 until numBlocks) {
            val dataLen = shortBlockLen - blockEccLen + if (i < numShortBlocks) 0 else 1
            val chunk = data.copyOfRange(read, read + dataLen)
            read += dataLen
            val block = chunk.copyOf(shortBlockLen + 1)
            val remainder = reedSolomonRemainder(chunk, divisor)
            remainder.copyInto(block, block.size - blockEccLen)
            blocks += block
        }

        val result = ByteArray(rawCodewords)
        var written = 0
        for (i in 0 until blocks[0].size) {
            for (j in blocks.indices) {
                // Short blocks have no byte at the padding position; skip it.
                if (i == shortBlockLen - blockEccLen && j < numShortBlocks) continue
                result[written++] = blocks[j][i]
            }
        }
        return result
    }

    /** Generator polynomial coefficients for a remainder of [degree] bytes. */
    private fun reedSolomonDivisor(degree: Int): ByteArray {
        val result = ByteArray(degree)
        result[degree - 1] = 1
        var root = 1
        repeat(degree) {
            for (j in 0 until degree) {
                result[j] = gfMultiply(result[j].toInt() and 0xFF, root).toByte()
                if (j + 1 < degree) result[j] = (result[j].toInt() xor result[j + 1].toInt()).toByte()
            }
            root = gfMultiply(root, 0x02)
        }
        return result
    }

    private fun reedSolomonRemainder(data: ByteArray, divisor: ByteArray): ByteArray {
        val result = ByteArray(divisor.size)
        for (b in data) {
            val factor = (b.toInt() xor result[0].toInt()) and 0xFF
            result.copyInto(result, 0, 1, result.size)
            result[result.size - 1] = 0
            for (i in result.indices) {
                result[i] = (
                    result[i].toInt() xor gfMultiply(divisor[i].toInt() and 0xFF, factor)
                    ).toByte()
            }
        }
        return result
    }

    /** Multiplication in GF(2^8) modulo the QR field polynomial 0x11D. */
    private fun gfMultiply(x: Int, y: Int): Int {
        var z = 0
        for (i in 7 downTo 0) {
            z = (z shl 1) xor ((z ushr 7) * 0x11D)
            z = z xor (((y ushr i) and 1) * x)
        }
        return z and 0xFF
    }

    // -------------------------------------------------------- matrix assembly

    /**
     * Draws one symbol: function patterns first, then the interleaved
     * codewords, then whichever of the eight masks scores best.
     */
    private class Encoder(private val version: Int, private val ecc: Ecc) {
        private val size = version * 4 + 17
        private val modules = Array(size) { BooleanArray(size) }
        private val isFunction = Array(size) { BooleanArray(size) }

        fun encode(dataCodewords: ByteArray): Symbol {
            drawFunctionPatterns()
            drawCodewords(addEccAndInterleave(dataCodewords, version, ecc))

            var bestMask = 0
            var bestPenalty = Int.MAX_VALUE
            for (mask in 0..7) {
                applyMask(mask)
                drawFormatBits(mask)
                val penalty = penaltyScore()
                if (penalty < bestPenalty) {
                    bestPenalty = penalty
                    bestMask = mask
                }
                applyMask(mask) // XOR is its own inverse, so this undoes it.
            }
            applyMask(bestMask)
            drawFormatBits(bestMask)

            return Symbol(version, size, modules)
        }

        private fun setFunctionModule(x: Int, y: Int, isDark: Boolean) {
            modules[y][x] = isDark
            isFunction[y][x] = true
        }

        private fun drawFunctionPatterns() {
            for (i in 0 until size) {
                setFunctionModule(6, i, i % 2 == 0)
                setFunctionModule(i, 6, i % 2 == 0)
            }

            drawFinderPattern(3, 3)
            drawFinderPattern(size - 4, 3)
            drawFinderPattern(3, size - 4)

            val alignPositions = alignmentPatternPositions()
            val last = alignPositions.size - 1
            for (i in alignPositions.indices) {
                for (j in alignPositions.indices) {
                    // The three corners already carry finder patterns.
                    val isCorner = (i == 0 && j == 0) ||
                        (i == 0 && j == last) ||
                        (i == last && j == 0)
                    if (!isCorner) drawAlignmentPattern(alignPositions[i], alignPositions[j])
                }
            }

            // A placeholder so these modules count as function ones; the real
            // format bits go in once the mask has been chosen.
            drawFormatBits(0)
            drawVersion()
        }

        private fun drawFinderPattern(x: Int, y: Int) {
            for (dy in -4..4) {
                for (dx in -4..4) {
                    val dist = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                    val xx = x + dx
                    val yy = y + dy
                    if (xx in 0 until size && yy in 0 until size) {
                        setFunctionModule(xx, yy, dist != 2 && dist != 4)
                    }
                }
            }
        }

        private fun drawAlignmentPattern(x: Int, y: Int) {
            for (dy in -2..2) {
                for (dx in -2..2) {
                    setFunctionModule(
                        x + dx,
                        y + dy,
                        maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)) != 1
                    )
                }
            }
        }

        private fun alignmentPatternPositions(): IntArray {
            if (version == 1) return IntArray(0)
            val numAlign = version / 7 + 2
            val step = if (version == 32) 26 else {
                (version * 4 + numAlign * 2 + 1) / (numAlign * 2 - 2) * 2
            }
            val result = ArrayList<Int>(numAlign)
            var pos = size - 7
            while (result.size < numAlign - 1) {
                result.add(0, pos)
                pos -= step
            }
            result.add(0, 6)
            return result.toIntArray()
        }

        private fun drawFormatBits(mask: Int) {
            val data = ecc.formatBits shl 3 or mask
            var rem = data
            repeat(10) { rem = (rem shl 1) xor ((rem ushr 9) * 0x537) }
            val bits = ((data shl 10) or rem) xor 0x5412

            for (i in 0..5) setFunctionModule(8, i, getBit(bits, i))
            setFunctionModule(8, 7, getBit(bits, 6))
            setFunctionModule(8, 8, getBit(bits, 7))
            setFunctionModule(7, 8, getBit(bits, 8))
            for (i in 9..14) setFunctionModule(14 - i, 8, getBit(bits, i))

            for (i in 0..7) setFunctionModule(size - 1 - i, 8, getBit(bits, i))
            for (i in 8..14) setFunctionModule(8, size - 15 + i, getBit(bits, i))
            setFunctionModule(8, size - 8, true) // Always dark.
        }

        private fun drawVersion() {
            if (version < 7) return
            var rem = version
            repeat(12) { rem = (rem shl 1) xor ((rem ushr 11) * 0x1F25) }
            val bits = (version shl 12) or rem

            for (i in 0 until 18) {
                val bit = getBit(bits, i)
                val a = size - 11 + i % 3
                val b = i / 3
                setFunctionModule(a, b, bit)
                setFunctionModule(b, a, bit)
            }
        }

        /** Lays the codewords out in the standard two-module-wide zigzag. */
        private fun drawCodewords(data: ByteArray) {
            var i = 0
            var right = size - 1
            while (right >= 1) {
                if (right == 6) right = 5 // The vertical timing column is skipped.
                for (vert in 0 until size) {
                    for (j in 0..1) {
                        val x = right - j
                        val upward = ((right + 1) and 2) == 0
                        val y = if (upward) size - 1 - vert else vert
                        if (!isFunction[y][x] && i < data.size * 8) {
                            modules[y][x] = getBit(data[i ushr 3].toInt() and 0xFF, 7 - (i and 7))
                            i++
                        }
                    }
                }
                right -= 2
            }
        }

        private fun applyMask(mask: Int) {
            for (y in 0 until size) {
                for (x in 0 until size) {
                    val invert = when (mask) {
                        0 -> (x + y) % 2 == 0
                        1 -> y % 2 == 0
                        2 -> x % 3 == 0
                        3 -> (x + y) % 3 == 0
                        4 -> (x / 3 + y / 2) % 2 == 0
                        5 -> x * y % 2 + x * y % 3 == 0
                        6 -> (x * y % 2 + x * y % 3) % 2 == 0
                        else -> ((x + y) % 2 + x * y % 3) % 2 == 0
                    }
                    if (invert && !isFunction[y][x]) modules[y][x] = !modules[y][x]
                }
            }
        }

        /**
         * How badly a masked symbol reads: long same-colour runs, solid 2x2
         * blocks, finder-like sequences, and an unbalanced dark/light ratio.
         */
        private fun penaltyScore(): Int {
            var result = 0

            for (y in 0 until size) {
                var runColor = false
                var runLength = 0
                val runHistory = IntArray(7)
                for (x in 0 until size) {
                    if (modules[y][x] == runColor) {
                        runLength++
                        if (runLength == 5) result += PENALTY_N1 else if (runLength > 5) result++
                    } else {
                        addRunToHistory(runLength, runHistory)
                        if (!runColor) result += countFinderPatterns(runHistory) * PENALTY_N3
                        runColor = modules[y][x]
                        runLength = 1
                    }
                }
                result += terminateRunAndCount(runColor, runLength, runHistory) * PENALTY_N3
            }

            for (x in 0 until size) {
                var runColor = false
                var runLength = 0
                val runHistory = IntArray(7)
                for (y in 0 until size) {
                    if (modules[y][x] == runColor) {
                        runLength++
                        if (runLength == 5) result += PENALTY_N1 else if (runLength > 5) result++
                    } else {
                        addRunToHistory(runLength, runHistory)
                        if (!runColor) result += countFinderPatterns(runHistory) * PENALTY_N3
                        runColor = modules[y][x]
                        runLength = 1
                    }
                }
                result += terminateRunAndCount(runColor, runLength, runHistory) * PENALTY_N3
            }

            for (y in 0 until size - 1) {
                for (x in 0 until size - 1) {
                    val color = modules[y][x]
                    if (color == modules[y][x + 1] &&
                        color == modules[y + 1][x] &&
                        color == modules[y + 1][x + 1]
                    ) {
                        result += PENALTY_N2
                    }
                }
            }

            var dark = 0
            for (row in modules) {
                for (color in row) if (color) dark++
            }
            val total = size * size
            val k = (kotlin.math.abs(dark * 20 - total * 10) + total - 1) / total - 1
            return result + k * PENALTY_N4
        }

        private fun countFinderPatterns(runHistory: IntArray): Int {
            val n = runHistory[1]
            val core = n > 0 &&
                runHistory[2] == n && runHistory[3] == n * 3 &&
                runHistory[4] == n && runHistory[5] == n
            return (if (core && runHistory[0] >= n * 4 && runHistory[6] >= n) 1 else 0) +
                (if (core && runHistory[6] >= n * 4 && runHistory[0] >= n) 1 else 0)
        }

        private fun terminateRunAndCount(
            runColor: Boolean,
            runLength: Int,
            runHistory: IntArray
        ): Int {
            var length = runLength
            if (runColor) {
                addRunToHistory(length, runHistory)
                length = 0
            }
            addRunToHistory(length + size, runHistory) // The light border past the edge.
            return countFinderPatterns(runHistory)
        }

        private fun addRunToHistory(runLength: Int, runHistory: IntArray) {
            var length = runLength
            if (runHistory[0] == 0) length += size // The light border before the first run.
            runHistory.copyInto(runHistory, 1, 0, runHistory.size - 1)
            runHistory[0] = length
        }
    }

    private fun getBit(value: Int, index: Int): Boolean = ((value ushr index) and 1) != 0

    /** Collects bits most-significant first until they are read out as bytes. */
    private class BitBuffer {
        private val bits = ArrayList<Boolean>()
        val length: Int get() = bits.size

        fun append(value: Int, bitCount: Int) {
            for (i in bitCount - 1 downTo 0) bits += getBit(value, i)
        }

        fun toBytes(): ByteArray {
            val result = ByteArray((bits.size + 7) / 8)
            bits.forEachIndexed { i, bit ->
                if (bit) result[i ushr 3] = (result[i ushr 3].toInt() or (1 shl (7 - (i and 7)))).toByte()
            }
            return result
        }
    }

    /*
     * The two tables the whole format hangs on, indexed [ecc][version]. Index 0
     * of each row is padding so the version can be used as the index directly.
     * Rows run LOW, MEDIUM, QUARTILE, HIGH - the order of [Ecc].
     */

    private val ECC_CODEWORDS_PER_BLOCK = arrayOf(
        intArrayOf(
            -1, 7, 10, 15, 20, 26, 18, 20, 24, 30, 18, 20, 24, 26, 30, 22, 24, 28, 30, 28,
            28, 28, 28, 30, 30, 26, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30
        ),
        intArrayOf(
            -1, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26, 30, 22, 22, 24, 24, 28, 28, 26, 26,
            26, 26, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28
        ),
        intArrayOf(
            -1, 13, 22, 18, 26, 18, 24, 18, 22, 20, 24, 28, 26, 24, 20, 30, 24, 28, 28, 26,
            30, 28, 30, 30, 30, 30, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30
        ),
        intArrayOf(
            -1, 17, 28, 22, 16, 22, 28, 26, 26, 24, 28, 24, 28, 22, 24, 24, 30, 28, 28, 26,
            28, 30, 24, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30
        )
    )

    private val NUM_ECC_BLOCKS = arrayOf(
        intArrayOf(
            -1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 4, 4, 4, 4, 4, 6, 6, 6, 6, 7,
            8, 8, 9, 9, 10, 12, 12, 12, 13, 14, 15, 16, 17, 18, 19, 19, 20, 21, 22, 24, 25
        ),
        intArrayOf(
            -1, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5, 5, 8, 9, 9, 10, 10, 11, 13, 14,
            16, 17, 17, 18, 20, 21, 23, 25, 26, 28, 29, 31, 33, 35, 37, 38, 40, 43, 45, 47, 49
        ),
        intArrayOf(
            -1, 1, 1, 2, 2, 4, 4, 6, 6, 8, 8, 8, 10, 12, 16, 12, 17, 16, 18, 21,
            20, 23, 23, 25, 27, 29, 34, 34, 35, 38, 40, 43, 45, 48, 51, 53, 56, 59, 62, 65, 68
        ),
        intArrayOf(
            -1, 1, 1, 2, 4, 4, 4, 5, 5, 8, 9, 9, 10, 12, 12, 17, 16, 18, 21, 20,
            23, 23, 25, 27, 29, 34, 34, 35, 38, 40, 43, 45, 48, 51, 53, 56, 59, 62, 65, 68, 70
        )
    )
}
