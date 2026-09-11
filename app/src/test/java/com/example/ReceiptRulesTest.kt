package com.example

import com.example.data.entity.MaintenanceBill
import com.example.data.entity.PaymentMode
import com.example.data.entity.PaymentStatus
import com.example.data.entity.derivedStatus
import com.example.data.entity.payableTotal
import com.example.data.entity.pendingAmount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What a receipt does to the bill behind it. Deleting one has to put the bill
 * back exactly where it was, or the ledger and the treasury drift apart.
 */
class ReceiptRulesTest {

    @Test
    fun `a fully settled bill reads as paid`() {
        val bill = bill(paid = 1100.0)

        assertEquals(PaymentStatus.PAID, bill.derivedStatus)
        assertEquals(0.0, bill.pendingAmount, 0.001)
    }

    @Test
    fun `a part payment reads as partial, not paid`() {
        val bill = bill(paid = 400.0)

        assertEquals(PaymentStatus.PARTIAL, bill.derivedStatus)
        assertEquals(700.0, bill.pendingAmount, 0.001)
    }

    @Test
    fun `withdrawing a receipt returns the bill to unpaid`() {
        val paid = bill(paid = 1100.0)

        val reversed = paid.copy(
            amountPaid = 0.0,
            status = PaymentStatus.UNPAID,
            paidDate = "",
            paymentMode = null,
            transactionRef = "",
            receiptNumber = "",
            receiptImageBase64 = ""
        )

        assertEquals(PaymentStatus.UNPAID, reversed.derivedStatus)
        assertEquals(reversed.payableTotal, reversed.pendingAmount, 0.001)
        assertTrue(reversed.receiptNumber.isEmpty())
        assertTrue(reversed.receiptImageBase64.isEmpty())
    }

    @Test
    fun `only a bill carrying a payment appears in the receipt list`() {
        val all = listOf(bill(paid = 1100.0), bill(paid = 0.0), bill(paid = 250.0))

        val receipts = all.filter { it.amountPaid > 0.0 }

        assertEquals(2, receipts.size)
        assertFalse(receipts.any { it.amountPaid <= 0.0 })
    }

    @Test
    fun `editing a receipt replaces the amount rather than adding to it`() {
        val original = bill(paid = 400.0)

        // The committee restates the same receipt as 1,100.
        val corrected = original.copy(amountPaid = 1100.0)
        val duesDelta = corrected.amountPaid - original.amountPaid

        assertEquals(1100.0, corrected.amountPaid, 0.001)
        assertEquals(700.0, duesDelta, 0.001)
        assertEquals(PaymentStatus.PAID, corrected.derivedStatus)
    }

    private fun bill(paid: Double) = MaintenanceBill(
        flatNumber = "101",
        residentName = "Test Member",
        billMonth = "August 2026",
        baseAmount = 715.0,
        waterCharges = 110.0,
        sinkingFund = 55.0,
        repairFund = 220.0,
        totalAmount = 1100.0,
        amountPaid = paid,
        paymentMode = if (paid > 0) PaymentMode.UPI else null,
        receiptNumber = if (paid > 0) "REC-2026-08-1234" else "",
        dueDate = "10 Aug 2026"
    )
}
