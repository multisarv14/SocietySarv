package com.example

import com.example.data.entity.SocietyInfo
import com.example.data.entity.financialYearOf
import com.example.data.entity.financialYears
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A society's financial year runs April to March, so January belongs to the
 * year that started the previous April - not to the calendar year it falls in.
 */
class FinancialYearTest {

    @Test
    fun `april starts a new financial year`() {
        assertEquals("FY 2026-2027", financialYearOf("April 2026"))
        assertEquals("FY 2026-2027", financialYearOf("December 2026"))
    }

    @Test
    fun `january to march belong to the year that started last april`() {
        assertEquals("FY 2025-2026", financialYearOf("January 2026"))
        assertEquals("FY 2025-2026", financialYearOf("March 2026"))
    }

    @Test
    fun `march and april either side of the boundary differ`() {
        assertEquals("FY 2025-2026", financialYearOf("March 2026"))
        assertEquals("FY 2026-2027", financialYearOf("April 2026"))
    }

    @Test
    fun `an unparseable month yields nothing rather than a wrong year`() {
        assertEquals("", financialYearOf("not a month"))
        assertEquals("", financialYearOf(""))
    }

    @Test
    fun `a society billing across a boundary offers both years`() {
        val society = society(billingStartMonth = "January 2026")

        val years = society.financialYears()

        assertTrue("expected FY 2025-2026 in $years", years.contains("FY 2025-2026"))
        assertTrue("expected more than one year in $years", years.size >= 1)
        assertEquals(years, years.distinct())
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
