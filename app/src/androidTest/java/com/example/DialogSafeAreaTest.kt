package com.example

import android.view.WindowInsets as AndroidWindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.components.DialogWidth
import com.example.ui.components.WideDialogProperties
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every dialog in this app is an `AlertDialog` wearing [DialogWidth] and
 * [WideDialogProperties], with a long scrolling body in its text slot. From
 * Android 15 on, an app targeting SDK 35+ is edge to edge whether it asks to be
 * or not and `decorFitsSystemWindows` is ignored, so without an inset of its
 * own that body pushed Cancel and Submit down behind the navigation bar.
 *
 * Compose's own bounds are measured against the dialog's window rather than the
 * display, and that window covers the whole display - so comparing the two
 * proves nothing. This converts the button's position to screen coordinates and
 * checks it against the real navigation bar inset.
 */
@RunWith(AndroidJUnit4::class)
class DialogSafeAreaTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun longFormKeepsItsButtonsClearOfTheNavigationBar() {
        var submitBottomOnScreen = 0f
        var navBarTopOnScreen = 0f
        var navBarInsetPx = 0

        composeRule.setContent {
            val view = LocalView.current

            AlertDialog(
                modifier = DialogWidth,
                properties = WideDialogProperties,
                onDismissRequest = {},
                title = { Text("Generate Maintenance Bill") },
                text = {
                    // Far more rows than any of the real forms carry, so the
                    // body is certain to be longer than the screen.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(40) { Text("Field $it") }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {},
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            val windowOrigin = IntArray(2)
                            view.getLocationOnScreen(windowOrigin)
                            submitBottomOnScreen =
                                windowOrigin[1] + coordinates.boundsInWindow().bottom

                            val display = view.rootWindowInsets
                            navBarInsetPx = display
                                .getInsets(AndroidWindowInsets.Type.navigationBars())
                                .bottom
                            navBarTopOnScreen =
                                (view.resources.displayMetrics.heightPixels - navBarInsetPx).toFloat()
                        }
                    ) { Text("Submit") }
                },
                dismissButton = { TextButton(onClick = {}) { Text("Cancel") } }
            )
        }
        composeRule.waitForIdle()

        assertTrue(
            "the navigation bar inset read as ${navBarInsetPx}px - " +
                "run this on a device showing a navigation bar",
            navBarInsetPx > 0
        )
        assertTrue(
            "Submit ends at ${submitBottomOnScreen}px on screen, below the top of the " +
                "${navBarInsetPx}px navigation bar at ${navBarTopOnScreen}px",
            submitBottomOnScreen <= navBarTopOnScreen + 1f
        )
    }
}
