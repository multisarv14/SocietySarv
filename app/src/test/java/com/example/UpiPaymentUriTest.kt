package com.example

import com.example.util.UpiQr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The address a UPI app is handed when a member taps Pay by QR.
 *
 * The old builder used form encoding, which writes a space as `+` and is not
 * what a URI query expects - enough to leave a payee name or note mangled in
 * the app that opens it.
 */
class UpiPaymentUriTest {

    @Test
    fun `a payment request carries the payee, the amount and the note`() {
        val uri = UpiQr.paymentUri(
            vpa = "greenvalley@okhdfcbank",
            payeeName = "Green Valley CHS",
            amount = 5_500.0,
            transactionNote = "Maintenance A-202 August 2026"
        )

        assertEquals(
            "upi://pay?pa=greenvalley%40okhdfcbank&pn=Green%20Valley%20CHS" +
                "&am=5500.00&cu=INR&tn=Maintenance%20A-202%20August%202026",
            uri
        )
    }

    @Test
    fun `spaces are percent-encoded, never turned into plus signs`() {
        val uri = UpiQr.paymentUri("a@b", "Sai Krupa Society", 100.0, "July dues")!!

        assertTrue(uri.contains("pn=Sai%20Krupa%20Society"))
        assertTrue(!uri.contains("+"))
    }

    @Test
    fun `a society with no UPI id gets no payment address at all`() {
        assertNull(UpiQr.paymentUri("", "Green Valley CHS", 5_500.0, "dues"))
        assertNull(UpiQr.paymentUri("   ", "Green Valley CHS", 5_500.0, "dues"))
    }

    @Test
    fun `a settled bill asks for no amount rather than for zero rupees`() {
        // UPI apps reject a ₹0.00 request outright, so the amount is left off
        // and the member simply lands on the society as payee.
        val uri = UpiQr.paymentUri("a@b", "Society", 0.0, "dues")!!

        assertTrue(!uri.contains("am="))
        assertTrue(uri.contains("cu=INR"))
    }

    @Test
    fun `paise are always written to two places`() {
        val uri = UpiQr.paymentUri("a@b", "Society", 1234.5, "dues")!!

        assertTrue(uri.contains("am=1234.50"))
    }

    @Test
    fun `a name in Devanagari survives the round trip as UTF-8`() {
        val uri = UpiQr.paymentUri("a@b", "सोसायटी", 100.0, "dues")!!

        // UTF-8 for "सोसायटी", percent-encoded byte by byte.
        assertTrue(uri.contains("pn=%E0%A4%B8%E0%A5%8B%E0%A4%B8%E0%A4%BE%E0%A4%AF%E0%A4%9F%E0%A5%80"))
    }

    @Test
    fun `an unnamed society and an empty note still produce a usable address`() {
        val uri = UpiQr.paymentUri("a@b", "", 100.0, "")!!

        assertEquals("upi://pay?pa=a%40b&pn=Housing%20Society&am=100.00&cu=INR", uri)
    }

    @Test
    fun `surrounding whitespace on a UPI id is trimmed rather than encoded`() {
        val uri = UpiQr.paymentUri("  society@okaxis  ", "Society", 100.0, "dues")!!

        assertTrue(uri.startsWith("upi://pay?pa=society%40okaxis&"))
    }
}
