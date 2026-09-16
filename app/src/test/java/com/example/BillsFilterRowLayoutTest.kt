package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.example.data.entity.PaymentStatus
import com.example.data.localization.AppLanguage
import com.example.ui.screens.BillsFilterAndActionsRow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The bill list's header used to put three filter chips and two buttons in one
 * `SpaceBetween` row. On a 360dp phone that did not fit: New Bill was squeezed
 * to its minimum width, its label wrapped a character at a time into a tall
 * sliver at the screen edge, and the height it forced on the row opened a band
 * of dead space above the search box.
 *
 * Robolectric draws with a stand-in font far narrower than a real device's, so
 * measuring widths here would not reproduce that overflow - the old row fits
 * comfortably under these metrics. What these tests pin instead is the shape of
 * the fix, which does not depend on font metrics: the actions and the chips get
 * a line each, so there is no longer a width the two can compete for.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h640dp-mdpi")
class BillsFilterRowLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setHeader(language: AppLanguage) {
        composeRule.setContent {
            Box(modifier = Modifier.requiredWidth(SCREEN_WIDTH)) {
                BillsFilterAndActionsRow(
                    statusFilter = PaymentStatus.UNPAID,
                    onSelectStatus = {},
                    canEdit = true,
                    language = language,
                    onBulkCollect = {},
                    onGenerateBill = {}
                )
            }
        }
    }

    @Test
    fun `the actions keep a line to themselves, above the chips`() {
        setHeader(AppLanguage.ENGLISH)

        // This is the invariant the old layout broke: while the buttons shared
        // a line with the chips, neither had a width it could count on.
        val actionsBottom = listOf("bulk_collect_btn", "generate_bill_btn")
            .maxOf { composeRule.onNodeWithTag(it).getUnclippedBoundsInRoot().bottom.value }

        listOf("bills_filter_unpaid", "bills_filter_partial", "bills_filter_paid").forEach { tag ->
            val chipTop = composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot().top.value
            assertTrue(
                "$tag starts at $chipTop, which is level with actions ending at $actionsBottom",
                chipTop >= actionsBottom - 0.5f
            )
        }
    }

    @Test
    fun `both actions fit across the screen`() {
        setHeader(AppLanguage.MARATHI) // The longer of the two languages.

        listOf("bulk_collect_btn", "generate_bill_btn").forEach { tag ->
            val bounds = composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()
            assertTrue("$tag starts off the left edge at ${bounds.left}", bounds.left.value >= -0.5f)
            assertTrue(
                "$tag runs past the right edge to ${bounds.right}",
                bounds.right.value <= SCREEN_WIDTH.value + 0.5f
            )
        }
    }

    @Test
    fun `action labels are held to one line`() {
        setHeader(AppLanguage.MARATHI)

        // A squeezed button wraps its label downwards rather than clipping it,
        // which is what turned New Bill into a tall green sliver. `maxLines = 1`
        // is what stops it, and a one-line button stays well under 64dp.
        listOf("bulk_collect_btn", "generate_bill_btn").forEach { tag ->
            val height = composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot().height
            assertTrue("$tag is $height tall - its label has wrapped", height.value <= 64f)
        }
    }

    @Test
    fun `the chips share a line, starting at the left edge`() {
        setHeader(AppLanguage.ENGLISH)

        // The chip row scrolls sideways, so a later chip may sit past the right
        // edge - but they must stay on one line, in order, and the first must
        // start on screen.
        val bounds = listOf("bills_filter_unpaid", "bills_filter_partial", "bills_filter_paid")
            .map { composeRule.onNodeWithTag(it).getUnclippedBoundsInRoot() }

        assertTrue("the first chip starts at ${bounds[0].left}", bounds[0].left.value >= -0.5f)
        bounds.zipWithNext { left, right ->
            assertTrue(
                "chips wrapped: one top is ${left.top}, the next is ${right.top}",
                kotlin.math.abs(left.top.value - right.top.value) <= 0.5f
            )
            assertTrue(
                "chips overlap: one ends at ${left.right}, the next starts at ${right.left}",
                right.left.value >= left.right.value - 0.5f
            )
        }
    }

    private companion object {
        val SCREEN_WIDTH = 360.dp
    }
}
