package com.example.util

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Turning a payment request into something a UPI app will actually open.
 *
 * Two separate jobs live here: building the `upi://pay` address correctly, and
 * painting a scannable symbol for it. Both used to be wrong - the address was
 * encoded with form rules that corrupt a VPA, and the "QR code" was a hash
 * pattern - which together is why Pay by QR did nothing.
 */
object UpiQr {

    /** Light modules around the symbol; without them scanners refuse to lock on. */
    private const val QUIET_ZONE_MODULES = 4

    /**
     * Builds the NPCI payment address:
     * `upi://pay?pa=<vpa>&pn=<name>&am=<amount>&cu=INR&tn=<note>`.
     *
     * Returns null when the society has no UPI id - there is nothing real to
     * pay into, and a QR built from an invented address takes a member's money
     * nowhere.
     *
     * The amount is written only when it is positive, so a QR for a fully
     * settled bill opens the payee rather than a ₹0.00 request the UPI apps
     * reject outright.
     */
    fun paymentUri(
        vpa: String,
        payeeName: String,
        amount: Double,
        transactionNote: String
    ): String? {
        val cleanVpa = vpa.trim()
        if (cleanVpa.isBlank()) return null

        return buildString {
            append("upi://pay?pa=").append(encode(cleanVpa))
            append("&pn=").append(encode(payeeName.trim().ifBlank { "Housing Society" }))
            if (amount > 0.0) append("&am=").append("%.2f".format(amount))
            append("&cu=INR")
            val note = transactionNote.trim()
            if (note.isNotBlank()) append("&tn=").append(encode(note))
        }
    }

    /**
     * Percent-encoding for a URI query value.
     *
     * `URLEncoder` is form encoding, not URI encoding: it writes a space as `+`
     * and escapes `.`-adjacent characters differently, which is enough to turn
     * a valid VPA into one no bank resolves. This keeps the unreserved set and
     * percent-encodes everything else, space included.
     */
    private fun encode(value: String): String = buildString {
        value.toByteArray(Charsets.UTF_8).forEach { byte ->
            val code = byte.toInt() and 0xFF
            val ch = code.toChar()
            val isUnreserved = code < 128 &&
                (ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9' || ch in "-_.~")
            if (isUnreserved) append(ch) else append('%').append("%02X".format(code))
        }
    }

    /**
     * Paints [content] as a scannable QR bitmap [sizePx] pixels square.
     *
     * The module size is rounded down to a whole number of pixels and the
     * symbol is centred in what is left, so every module stays the same size -
     * uneven modules are the usual reason a rendered code will not scan.
     */
    fun bitmap(content: String, sizePx: Int = 512): Bitmap {
        val symbol = QrCode.encode(content)
        val modulesPerSide = symbol.size + QUIET_ZONE_MODULES * 2
        val modulePx = (sizePx / modulesPerSide).coerceAtLeast(1)
        val drawnPx = modulePx * modulesPerSide
        val canvasPx = maxOf(sizePx, drawnPx)
        val offset = (canvasPx - drawnPx) / 2

        val pixels = IntArray(canvasPx * canvasPx) { Color.WHITE }
        for (row in 0 until symbol.size) {
            for (col in 0 until symbol.size) {
                if (!symbol[col, row]) continue
                val left = offset + (col + QUIET_ZONE_MODULES) * modulePx
                val top = offset + (row + QUIET_ZONE_MODULES) * modulePx
                for (y in top until top + modulePx) {
                    val base = y * canvasPx
                    for (x in left until left + modulePx) {
                        pixels[base + x] = Color.BLACK
                    }
                }
            }
        }

        return Bitmap.createBitmap(pixels, canvasPx, canvasPx, Bitmap.Config.ARGB_8888)
    }
}
