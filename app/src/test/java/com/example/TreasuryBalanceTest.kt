package com.example

import com.example.data.entity.ExpenseCategory
import com.example.data.entity.ExpenseRecord
import com.example.data.entity.MaintenanceBill
import com.example.data.entity.NonFeeRevenue
import com.example.data.entity.PaymentMode
import com.example.data.entity.RevenueSource
import com.example.data.entity.SocietyInfo
import com.example.data.entity.treasuryBalance
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The society's balance is worked out from its own ledger, so it can never
 * drift away from the entries behind it.
 */
class TreasuryBalanceTest {

    @Test
    fun `a society with no entries at all holds nothing`() {
        val balance = society().treasuryBalance(emptyList(), emptyList(), emptyList())

        assertEquals(0.0, balance.cash, 0.001)
        assertEquals(0.0, balance.bank, 0.001)
        assertEquals(0.0, balance.total, 0.001)
    }

    @Test
    fun `deleting the bills takes their collections back out again`() {
        val paid = listOf(bill(1100.0, PaymentMode.UPI), bill(1100.0, PaymentMode.UPI))

        val withBills = society().treasuryBalance(paid, emptyList(), emptyList())
        val afterDeletion = society().treasuryBalance(emptyList(), emptyList(), emptyList())

        assertEquals(2200.0, withBills.bank, 0.001)
        // This is the case that used to leave 2,200 stranded on the society.
        assertEquals(0.0, afterDeletion.bank, 0.001)
    }

    @Test
    fun `cash and bank are kept apart`() {
        val balance = society().treasuryBalance(
            bills = listOf(bill(500.0, PaymentMode.CASH), bill(800.0, PaymentMode.BANK_TRANSFER)),
            expenses = listOf(expense(200.0, PaymentMode.CASH)),
            revenues = listOf(revenue(300.0, PaymentMode.UPI))
        )

        assertEquals(300.0, balance.cash, 0.001)
        assertEquals(1100.0, balance.bank, 0.001)
    }

    @Test
    fun `an opening balance is carried forward`() {
        val balance = society(openingBank = 5000.0, openingCash = 250.0)
            .treasuryBalance(
                bills = listOf(bill(1100.0, PaymentMode.UPI)),
                expenses = listOf(expense(600.0, PaymentMode.BANK_TRANSFER)),
                revenues = emptyList()
            )

        assertEquals(250.0, balance.cash, 0.001)
        assertEquals(5500.0, balance.bank, 0.001)
    }

    @Test
    fun `an unpaid bill adds nothing`() {
        val balance = society().treasuryBalance(
            bills = listOf(bill(0.0, null)),
            expenses = emptyList(),
            revenues = emptyList()
        )

        assertEquals(0.0, balance.total, 0.001)
    }

    private fun bill(paid: Double, mode: PaymentMode?) = MaintenanceBill(
        flatNumber = "101",
        residentName = "Test Member",
        billMonth = "August 2026",
        baseAmount = 1100.0,
        totalAmount = 1100.0,
        amountPaid = paid,
        paymentMode = mode,
        dueDate = "10 Aug 2026"
    )

    private fun expense(amount: Double, mode: PaymentMode) = ExpenseRecord(
        title = "Test",
        category = ExpenseCategory.MISCELLANEOUS,
        amount = amount,
        date = "01 Aug 2026",
        vendorName = "Vendor",
        paymentMode = mode
    )

    private fun revenue(amount: Double, mode: PaymentMode) = NonFeeRevenue(
        title = "Test",
        sourceCategory = RevenueSource.HALL_RENTAL,
        amount = amount,
        date = "01 Aug 2026",
        payerName = "Payer",
        paymentMode = mode
    )

    private fun society(openingBank: Double = 0.0, openingCash: Double = 0.0) = SocietyInfo(
        name = "Test Society",
        registrationNumber = "REG-1",
        societyCode = "TS001",
        address = "",
        totalWings = 1,
        totalFlats = 0,
        bankAccountName = "",
        bankAccountNumber = "",
        ifscCode = "",
        currentBalanceCash = openingCash,
        currentBalanceBank = openingBank
    )
}
