package com.example

import com.example.data.entity.FlatType
import com.example.data.entity.Resident
import com.example.data.entity.SocietyInfo
import com.example.data.entity.UnitTypeConfig
import com.example.data.entity.monthlyTariffFor
import com.example.data.entity.hasBalancedMaintenancePercents
import com.example.data.entity.maintenancePercentTotal
import com.example.data.entity.splitMaintenance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * How a month's maintenance is divided between the society's funds. The shares
 * are of the amount, not additions to it, so they must always add back up to it.
 */
class MaintenanceSplitTest {

    @Test
    fun `the configured example splits exactly as specified`() {
        val split = society(water = 10.0, sinking = 5.0, repair = 20.0, other = 65.0)
            .splitMaintenance(1100.0)

        assertEquals(110.0, split.water, 0.001)
        assertEquals(55.0, split.sinking, 0.001)
        assertEquals(220.0, split.repair, 0.001)
        assertEquals(715.0, split.other, 0.001)
        assertEquals(1100.0, split.total, 0.001)
    }

    @Test
    fun `rounding leftovers land on other so the total still matches`() {
        // 33.33% of 1000 is 333.30, which does not divide cleanly.
        val split = society(water = 10.0, sinking = 5.0, repair = 33.33, other = 51.67)
            .splitMaintenance(1000.0)

        assertEquals(100.0, split.water, 0.001)
        assertEquals(50.0, split.sinking, 0.001)
        assertEquals(333.3, split.repair, 0.001)
        assertEquals(1000.0, split.total, 0.001)
    }

    @Test
    fun `every share is rounded to paise`() {
        val split = society(water = 33.333, sinking = 33.333, repair = 33.334, other = 0.0)
            .splitMaintenance(100.0)

        listOf(split.water, split.sinking, split.repair, split.other).forEach { amount ->
            assertEquals(amount, Math.round(amount * 100) / 100.0, 0.0)
        }
        assertEquals(100.0, split.total, 0.001)
    }

    @Test
    fun `a zero amount splits into nothing`() {
        val split = society(water = 10.0, sinking = 5.0, repair = 20.0, other = 65.0)
            .splitMaintenance(0.0)

        assertEquals(0.0, split.total, 0.001)
    }

    @Test
    fun `percentages that add up to a hundred are balanced`() {
        val balanced = society(water = 10.0, sinking = 5.0, repair = 20.0, other = 65.0)

        assertEquals(100.0, balanced.maintenancePercentTotal, 0.001)
        assertTrue(balanced.hasBalancedMaintenancePercents)
    }

    @Test
    fun `percentages that do not add up to a hundred are flagged`() {
        val lopsided = society(water = 10.0, sinking = 5.0, repair = 20.0, other = 50.0)

        assertEquals(85.0, lopsided.maintenancePercentTotal, 0.001)
        assertFalse(lopsided.hasBalancedMaintenancePercents)
    }

    private fun society(water: Double, sinking: Double, repair: Double, other: Double) = SocietyInfo(
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
        waterChargesPercent = water,
        sinkingFundPercent = sinking,
        repairFundPercent = repair,
        otherMaintenancePercent = other
    )
}

/**
 * Which tariff a unit is charged. The society's own unit type is what the
 * committee prices, so it has to win over the built-in slabs.
 */
class UnitTariffTest {

    @Test
    fun `the unit's own type is what it is charged`() {
        val society = society(
            types = listOf(UnitTypeConfig("1 BHK", 900.0), UnitTypeConfig("2 BHK", 1400.0))
        )

        assertEquals(900.0, society.monthlyTariffFor(unit(unitType = "1 BHK")), 0.001)
        assertEquals(1400.0, society.monthlyTariffFor(unit(unitType = "2 BHK")), 0.001)
    }

    @Test
    fun `the type is matched whatever the case`() {
        val society = society(types = listOf(UnitTypeConfig("Shop", 3000.0)))

        assertEquals(3000.0, society.monthlyTariffFor(unit(unitType = "shop")), 0.001)
    }

    @Test
    fun `a unit with no type falls back to its slab rather than nothing`() {
        val society = society(types = listOf(UnitTypeConfig("1 BHK", 900.0)))

        val charged = society.monthlyTariffFor(unit(unitType = "", flatType = FlatType.TWO_BHK))

        assertEquals(2200.0, charged, 0.001)
    }

    @Test
    fun `a type with no tariff yet falls back rather than billing zero`() {
        val society = society(types = listOf(UnitTypeConfig("Office", 0.0)))

        val charged = society.monthlyTariffFor(unit(unitType = "Office", flatType = FlatType.TWO_BHK))

        assertEquals(2200.0, charged, 0.001)
    }

    private fun unit(unitType: String, flatType: FlatType = FlatType.TWO_BHK) = Resident(
        flatNumber = "A-101",
        wing = "A",
        ownerName = "Test Member",
        contactNumber = "9999999999",
        unitType = unitType,
        flatType = flatType
    )

    private fun society(types: List<UnitTypeConfig>) = SocietyInfo(
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
        unitTypes = types
    )
}

