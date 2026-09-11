package com.example

import com.example.data.entity.SocietyInfo
import com.example.data.entity.WingPlan
import com.example.data.entity.WingUnitCount
import com.example.data.entity.billingMonths
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two pieces of billing arithmetic the whole app leans on: how a wing
 * expands into unit numbers, and which months a society can be billed for.
 */
class BillingRulesTest {

    @Test
    fun `a wing spreads its units evenly over its floors`() {
        val plan = WingPlan(
            name = "A",
            totalFloors = 2,
            unitCounts = listOf(WingUnitCount("1 BHK", 3), WingUnitCount("2 BHK", 3))
        )

        assertEquals(6, plan.totalUnits)
        assertEquals(
            listOf("A-101", "A-102", "A-103", "A-201", "A-202", "A-203"),
            plan.generateUnitNumbers()
        )
    }

    @Test
    fun `unit types line up one for one with the generated numbers`() {
        val plan = WingPlan(
            name = "A",
            totalFloors = 2,
            unitCounts = listOf(WingUnitCount("1 BHK", 3), WingUnitCount("2 BHK", 1))
        )

        val numbers = plan.generateUnitNumbers()
        val types = plan.unitTypesInOrder()

        assertEquals(numbers.size, types.size)
        assertEquals(listOf("1 BHK", "1 BHK", "1 BHK", "2 BHK"), types)
    }

    @Test
    fun `a remainder lands on the last floor rather than being lost`() {
        val plan = WingPlan("B", totalFloors = 3, unitCounts = listOf(WingUnitCount("1 BHK", 13)))

        val numbers = plan.generateUnitNumbers()

        assertEquals(13, numbers.size)
        assertEquals("B-101", numbers.first())
        assertEquals("B-303", numbers.last())
    }

    @Test
    fun `a wing with no floors entered still creates its units on one floor`() {
        val plan = WingPlan("S", totalFloors = 0, unitCounts = listOf(WingUnitCount("Shop", 4)))

        assertEquals(4, plan.totalUnits)
        assertEquals(listOf("S-101", "S-102", "S-103", "S-104"), plan.generateUnitNumbers())
    }

    @Test
    fun `a wing with no unit types creates nothing`() {
        val plan = WingPlan("C", totalFloors = 5)

        assertEquals(0, plan.totalUnits)
        assertTrue(plan.generateUnitNumbers().isEmpty())
    }

    @Test
    fun `billing months run from the configured start month to this month`() {
        val threeMonthsAgo = monthsFromNow(-3)
        val society = society(billingStartMonth = threeMonthsAgo)

        val months = society.billingMonths()

        assertEquals(4, months.size)
        assertEquals(monthsFromNow(0), months.first())
        assertEquals(threeMonthsAgo, months.last())
    }

    @Test
    fun `a start month in the future yields only that month`() {
        val nextMonth = monthsFromNow(1)

        assertEquals(listOf(nextMonth), society(billingStartMonth = nextMonth).billingMonths())
    }

    @Test
    fun `a society with no start month falls back to the last twelve months`() {
        val months = society(billingStartMonth = "").billingMonths()

        assertEquals(12, months.size)
        assertEquals(monthsFromNow(0), months.first())
        assertEquals(monthsFromNow(-11), months.last())
    }

    @Test
    fun `an unparseable start month falls back rather than producing nothing`() {
        val months = society(billingStartMonth = "not a month").billingMonths()

        assertTrue(months.isNotEmpty())
        assertEquals(monthsFromNow(0), months.first())
    }

    private fun monthsFromNow(offset: Int): String {
        val calendar = Calendar.getInstance().apply { add(Calendar.MONTH, offset) }
        return SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(calendar.time)
    }

    private fun society(billingStartMonth: String) = SocietyInfo(
        name = "Test Society",
        registrationNumber = "REG-1",
        societyCode = "TS001",
        address = "",
        totalWings = 1,
        totalFlats = 0,
        bankAccountName = "",
        bankAccountNumber = "",
        ifscCode = "",
        currentBalanceCash = 0.0,
        currentBalanceBank = 0.0,
        billingStartMonth = billingStartMonth
    )
}
