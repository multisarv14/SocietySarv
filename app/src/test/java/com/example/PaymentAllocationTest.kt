package com.example

import com.example.data.entity.MaintenanceBill
import com.example.data.entity.PaymentStatus
import com.example.data.entity.allocatePayment
import com.example.data.entity.oldestMonthFirst
import com.example.data.entity.payableTotal
import com.example.data.entity.unitDues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * How one payment is spread over what a unit owes.
 *
 * These are the rules a chairman is actually working to at the collection desk:
 * take whatever the member hands over, clear the oldest arrears first, leave
 * the rest showing as pending, and carry an overpayment forward rather than
 * losing it.
 */
class PaymentAllocationTest {

    @Test
    fun `a part payment clears the oldest months and leaves the rest pending`() {
        // Unit 202: five months at 1,100 each, nothing paid, 5,500 outstanding.
        val bills = fiveMonthsOf(1_100.0)

        val allocation = allocatePayment(bills, 3_300.0)

        assertEquals(3_300.0, allocation.applied, 0.001)
        assertEquals(0.0, allocation.excess, 0.001)
        assertEquals(3, allocation.settledCount)
        assertEquals(
            listOf("April 2026", "May 2026", "June 2026"),
            allocation.months
        )
        // What is left is still owed, to the rupee.
        assertEquals(2_200.0, allocation.allocations.sumOf { it.pendingAfter }, 0.001)
    }

    @Test
    fun `an amount that stops mid-month leaves that month partly paid`() {
        val bills = fiveMonthsOf(1_100.0)

        val allocation = allocatePayment(bills, 2_500.0)

        assertEquals(2, allocation.settledCount)
        val third = allocation.allocations.first { it.bill.billMonth == "June 2026" }
        assertEquals(300.0, third.applied, 0.001)
        assertEquals(800.0, third.pendingAfter, 0.001)
        assertFalse(third.isSettled)
    }

    @Test
    fun `paying everything owed settles every month and leaves nothing over`() {
        val bills = fiveMonthsOf(1_100.0)

        val allocation = allocatePayment(bills, 5_500.0)

        assertEquals(5, allocation.settledCount)
        assertEquals(0.0, allocation.excess, 0.001)
        assertTrue(allocation.allocations.all { it.isSettled })
    }

    @Test
    fun `an overpayment is carried forward rather than forced onto the last bill`() {
        val bills = fiveMonthsOf(1_100.0)

        val allocation = allocatePayment(bills, 6_000.0)

        assertEquals(5_500.0, allocation.applied, 0.001)
        assertEquals(500.0, allocation.excess, 0.001)
        // No bill shows more collected against it than it ever charged.
        assertTrue(allocation.allocations.all { it.applied <= it.bill.payableTotal })
    }

    @Test
    fun `a payment against a unit that owes nothing becomes credit in full`() {
        val settled = listOf(bill("April 2026", 1_100.0, paid = 1_100.0))

        val allocation = allocatePayment(settled, 1_100.0)

        assertEquals(0.0, allocation.applied, 0.001)
        assertEquals(1_100.0, allocation.excess, 0.001)
        assertTrue(allocation.touched.isEmpty())
    }

    @Test
    fun `a partly paid month is topped up only by what it still owes`() {
        val bills = listOf(
            bill("April 2026", 1_100.0, paid = 900.0),
            bill("May 2026", 1_100.0)
        )

        val allocation = allocatePayment(bills, 500.0)

        val april = allocation.allocations.first { it.bill.billMonth == "April 2026" }
        assertEquals(200.0, april.applied, 0.001)
        assertTrue(april.isSettled)
        val may = allocation.allocations.first { it.bill.billMonth == "May 2026" }
        assertEquals(300.0, may.applied, 0.001)
    }

    @Test
    fun `the late fee is settled along with the month it sits on`() {
        val withLateFee = listOf(bill("April 2026", 1_100.0, lateFee = 22.0))

        val allocation = allocatePayment(withLateFee, 1_122.0)

        assertEquals(1, allocation.settledCount)
        assertEquals(0.0, allocation.excess, 0.001)
    }

    @Test
    fun `nothing is taken from a payment of zero`() {
        val allocation = allocatePayment(fiveMonthsOf(1_100.0), 0.0)

        assertEquals(0.0, allocation.applied, 0.001)
        assertEquals(0.0, allocation.excess, 0.001)
        assertTrue(allocation.touched.isEmpty())
    }

    @Test
    fun `months are settled in calendar order, not the order they were entered`() {
        val jumbled = listOf(
            bill("June 2026", 1_100.0),
            bill("April 2026", 1_100.0),
            bill("May 2026", 1_100.0)
        )

        assertEquals(
            listOf("April 2026", "May 2026", "June 2026"),
            jumbled.oldestMonthFirst().map { it.billMonth }
        )
        assertEquals(listOf("April 2026"), allocatePayment(jumbled, 1_100.0).months)
    }

    @Test
    fun `months that straddle a year end still sort oldest first`() {
        val acrossYears = listOf(
            bill("January 2027", 1_100.0),
            bill("December 2026", 1_100.0),
            bill("March 2026", 1_100.0)
        )

        assertEquals(
            listOf("March 2026", "December 2026", "January 2027"),
            acrossYears.oldestMonthFirst().map { it.billMonth }
        )
    }

    @Test
    fun `the dues a member is shown separate this month from the arrears behind it`() {
        val bills = listOf(
            bill("April 2026", 1_100.0),
            bill("May 2026", 1_100.0, paid = 600.0),
            bill("June 2026", 1_200.0)
        )

        val dues = unitDues(bills)

        assertEquals(1_600.0, dues.previousPending, 0.001)
        assertEquals(1_200.0, dues.currentBill, 0.001)
        assertEquals(1_200.0, dues.currentPending, 0.001)
        assertEquals(2_800.0, dues.grossOutstanding, 0.001)
        assertEquals(2_800.0, dues.netPayable, 0.001)
        assertFalse(dues.isSettled)
    }

    @Test
    fun `credit already paid in is taken off what the member is asked for`() {
        val bills = listOf(bill("April 2026", 1_100.0), bill("May 2026", 1_100.0))

        val dues = unitDues(bills, excessCredit = 500.0)

        assertEquals(2_200.0, dues.grossOutstanding, 0.001)
        assertEquals(1_700.0, dues.netPayable, 0.001)
        assertEquals(0.0, dues.creditInHand, 0.001)
    }

    @Test
    fun `credit larger than the arrears leaves the member owing nothing`() {
        val bills = listOf(bill("April 2026", 1_100.0))

        val dues = unitDues(bills, excessCredit = 1_800.0)

        assertEquals(0.0, dues.netPayable, 0.001)
        assertEquals(700.0, dues.creditInHand, 0.001)
        assertTrue(dues.isSettled)
    }

    @Test
    fun `a unit with no bills at all owes nothing and shows nothing`() {
        val dues = unitDues(emptyList())

        assertEquals(0.0, dues.currentBill, 0.001)
        assertEquals(0.0, dues.grossOutstanding, 0.001)
        assertTrue(dues.isSettled)
    }

    private fun fiveMonthsOf(amount: Double) = listOf(
        bill("April 2026", amount),
        bill("May 2026", amount),
        bill("June 2026", amount),
        bill("July 2026", amount),
        bill("August 2026", amount)
    )

    private fun bill(
        month: String,
        base: Double,
        paid: Double = 0.0,
        lateFee: Double = 0.0
    ) = MaintenanceBill(
        docId = "$month-doc",
        flatNumber = "A-202",
        residentName = "Test Member",
        billMonth = month,
        baseAmount = base,
        lateFeeAmount = lateFee,
        totalAmount = base + lateFee,
        amountPaid = paid,
        status = PaymentStatus.UNPAID,
        dueDate = "10 ${month.take(3)} ${month.takeLast(4)}"
    )
}
