package com.example.data.entity

/**
 * How money a member hands over is spread across what they owe.
 *
 * A society is paid in round figures, not in bill-sized instalments: one member
 * pays five months of arrears at once, another pays ₹3,300 against ₹5,500 of
 * them, a third overpays by a month. All three are the same operation - take
 * the amount, settle the oldest arrears first, and keep whatever is left over
 * as credit against the months still to be billed.
 *
 * Kept apart from the ViewModel so the arithmetic can be tested on its own and
 * so the receipt sheet, the bulk-collection sheet and My Unit can never come to
 * different conclusions about what a unit owes.
 */

/** The month a bill belongs to, as a sortable number. Unparseable months sort first. */
fun billMonthKey(billMonth: String): Long = runCatching {
    java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.ENGLISH).parse(billMonth)?.time ?: 0L
}.getOrDefault(0L)

/** Bills in the order arrears are settled: oldest month first. */
fun List<MaintenanceBill>.oldestMonthFirst(): List<MaintenanceBill> =
    sortedWith(compareBy({ billMonthKey(it.billMonth) }, { it.id }))

/** One bill, and how much of a payment landed on it. */
data class BillAllocation(
    val bill: MaintenanceBill,
    val applied: Double
) {
    /** The bill as it stands once [applied] has been recorded against it. */
    val pendingAfter: Double
        get() = roundToPaise((bill.payableTotal - bill.amountPaid - applied).coerceAtLeast(0.0))

    /** True when this payment clears the bill outright. */
    val isSettled: Boolean get() = pendingAfter <= 0.009
}

/**
 * The result of spreading one payment over a unit's bills.
 *
 * [excess] is what the payment could not be used for - the member paid more
 * than they owed - and is carried forward against the next month's bill rather
 * than being lost or refunded.
 */
data class PaymentAllocation(
    val allocations: List<BillAllocation>,
    val excess: Double
) {
    /** What actually landed on bills, excess aside. */
    val applied: Double get() = roundToPaise(allocations.sumOf { it.applied })

    /** Bills this payment touched at all. */
    val touched: List<BillAllocation> get() = allocations.filter { it.applied > 0.009 }

    /** Bills this payment cleared outright. */
    val settledCount: Int get() = touched.count { it.isSettled }

    /** Months this payment went against, oldest first, for the receipt note. */
    val months: List<String> get() = touched.map { it.bill.billMonth }
}

/**
 * Spreads [amount] over [bills], oldest month first.
 *
 * Bills that are already settled are skipped rather than overpaid, a partly
 * paid bill is topped up only to what is still owed on it, and anything left
 * once every bill is clear comes back as [PaymentAllocation.excess].
 */
fun allocatePayment(bills: List<MaintenanceBill>, amount: Double): PaymentAllocation {
    var remaining = roundToPaise(amount.coerceAtLeast(0.0))
    val allocations = bills.oldestMonthFirst().map { bill ->
        val owed = bill.pendingAmount
        val applied = if (remaining <= 0.009 || owed <= 0.009) 0.0 else roundToPaise(minOf(remaining, owed))
        remaining = roundToPaise(remaining - applied)
        BillAllocation(bill, applied)
    }
    return PaymentAllocation(allocations, roundToPaise(remaining.coerceAtLeast(0.0)))
}

/**
 * What a unit owes, in the three parts a member is actually shown.
 *
 * Previous arrears and the current month are kept apart because a member reads
 * them differently - "this month" is a bill, "arrears" is a debt - and the
 * excess a member has already paid is subtracted from both so the figure they
 * are asked for is the one they should hand over.
 */
data class UnitDues(
    /** Everything still owed on bills before the latest billed month. */
    val previousPending: Double,
    /** What the latest billed month came to, late fee included. */
    val currentBill: Double,
    /** Still owed on the latest billed month alone. */
    val currentPending: Double,
    /** Credit the member is already in, carried forward from an overpayment. */
    val excessCredit: Double
) {
    /** Everything owed across every bill, before any credit is applied. */
    val grossOutstanding: Double get() = roundToPaise(previousPending + currentPending)

    /** What the member is actually asked to pay: what is owed, less their credit. */
    val netPayable: Double get() = roundToPaise((grossOutstanding - excessCredit).coerceAtLeast(0.0))

    /** Credit left over once every outstanding bill is covered by it. */
    val creditInHand: Double
        get() = roundToPaise((excessCredit - grossOutstanding).coerceAtLeast(0.0))

    /** True when the member owes nothing after their credit is taken into account. */
    val isSettled: Boolean get() = netPayable <= 0.009
}

/**
 * Works [UnitDues] out from a unit's own bills and its carried-forward credit.
 *
 * The "current" month is the newest bill the unit has, so this reads the same
 * whether the society bills on the 1st or catches up three months late.
 */
fun unitDues(bills: List<MaintenanceBill>, excessCredit: Double = 0.0): UnitDues {
    val ordered = bills.oldestMonthFirst()
    val latest = ordered.lastOrNull()
    val earlier = if (ordered.isEmpty()) emptyList() else ordered.dropLast(1)
    return UnitDues(
        previousPending = roundToPaise(earlier.sumOf { it.pendingAmount }),
        currentBill = roundToPaise(latest?.payableTotal ?: 0.0),
        currentPending = roundToPaise(latest?.pendingAmount ?: 0.0),
        excessCredit = roundToPaise(excessCredit.coerceAtLeast(0.0))
    )
}
