package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.MaintenanceBill
import com.example.data.entity.PaymentStatus
import com.example.data.entity.VisitorLog
import com.example.data.entity.VisitorPurpose
import com.example.data.entity.VisitorStatus
import com.example.data.entity.derivedStatus
import com.example.data.entity.payableTotal
import com.example.data.entity.pendingAmount
import com.example.data.localization.AppLanguage
import com.example.data.localization.AppStrings
import com.example.ui.theme.GoogleAccentBlue
import com.example.ui.theme.GoogleAccentBlueContainer
import com.example.ui.theme.GoogleDangerContainer
import com.example.ui.theme.GoogleDangerRed
import com.example.ui.theme.GoogleSuccessContainer
import com.example.ui.theme.GoogleSuccessGreen
import com.example.ui.theme.GoogleTealContainer
import com.example.ui.theme.GoogleWarningAmber
import com.example.ui.theme.SlateOutlineVariant
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.TealPrimary

/**
 * Animated Card Container that provides a subtle slide-in and fade-in animation
 * when navigating between sections.
 */
@Composable
fun AnimatedSectionCard(
    delayMillis: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 350, delayMillis = delayMillis)) +
                slideInVertically(
                    initialOffsetY = { it / 6 },
                    animationSpec = tween(durationMillis = 400, delayMillis = delayMillis)
                ),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Maintenance Status Dashboard Component
 * Displays monthly dues, payment status, collection rate, and total pending amounts for society members.
 */
@Composable
fun MaintenanceStatusDashboard(
    bills: List<MaintenanceBill>,
    language: AppLanguage,
    onNavigateToMaintenance: () -> Unit,
    /**
     * False on a member's home: the Details button opens the committee's Bills
     * screen, which is not part of a member's menu, so it is left off.
     */
    showActions: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Derived from the line items, so an edited bill is reflected here without
    // depending on the total that happened to be stored on the document.
    val totalBilled = bills.sumOf { it.payableTotal }
    val totalCollected = bills.sumOf { it.amountPaid }
    val totalPending = bills.sumOf { it.pendingAmount }
    val paidCount = bills.count { it.derivedStatus == PaymentStatus.PAID }
    val pendingCount = bills.count { it.derivedStatus != PaymentStatus.PAID }
    val progress = if (totalBilled > 0) (totalCollected / totalBilled).toFloat().coerceIn(0f, 1f) else 0f

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SlateOutlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("maintenance_status_dashboard_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row with Title & Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GoogleTealContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PendingActions,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = AppStrings.get("maintenance_status_title", language),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (showActions) {
                                "${bills.size} ${if (language == AppLanguage.MARATHI) "नोंदणीकृत सदनिका" else if (language == AppLanguage.HINDI) "पंजीकृत फ्लैट" else "registered units"}"
                            } else {
                                // A member's card covers every unit they hold,
                                // so it says so rather than looking like one.
                                val units = bills.map { it.flatNumber.uppercase() }.distinct().size
                                "${bills.size} ${if (language == AppLanguage.MARATHI) "बिले" else if (language == AppLanguage.HINDI) "बिल" else "bills"}" +
                                    if (units > 1) " • $units units" else ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (showActions) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = GoogleTealContainer,
                        modifier = Modifier
                            .clickable { onNavigateToMaintenance() }
                            .testTag("view_maintenance_details_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (language == AppLanguage.MARATHI) "सविस्तर" else if (language == AppLanguage.HINDI) "विवरण" else "Details",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = TealPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metrics Summary Grid (3 Pillars: Total Dues, Collected, Total Pending)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total Billed Dues
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SlateSurfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = AppStrings.get("monthly_dues", language),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "₹%,.0f".format(totalBilled),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Total Collected
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GoogleSuccessContainer,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = AppStrings.get("collected_amount", language),
                            style = MaterialTheme.typography.labelSmall,
                            color = GoogleSuccessGreen
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "₹%,.0f".format(totalCollected),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = GoogleSuccessGreen
                        )
                    }
                }

                // Total Pending Amount
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GoogleDangerContainer,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = AppStrings.get("total_pending_amount", language),
                            style = MaterialTheme.typography.labelSmall,
                            color = GoogleDangerRed
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "₹%,.0f".format(totalPending),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = GoogleDangerRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar for Collection Status
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = AppStrings.get("collection_progress", language),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(progress * 100).toInt()}% • $paidCount ${AppStrings.get("status_paid", language)} / $pendingCount ${AppStrings.get("status_unpaid", language)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (progress >= 0.75f) GoogleSuccessGreen else GoogleWarningAmber
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (progress >= 0.75f) GoogleSuccessGreen else TealPrimary,
                    trackColor = SlateSurfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Gate Activity Feed Component
 * Displays visitor entries with timestamp and a small status badge.
 */
@Composable
fun GateActivityFeed(
    visitors: List<VisitorLog>,
    language: AppLanguage,
    onNavigateToGate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SlateOutlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("gate_activity_feed_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GoogleAccentBlueContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = GoogleAccentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = AppStrings.get("gate_activity_feed", language),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = AppStrings.get("recent_visitor_entries", language),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = GoogleAccentBlueContainer,
                    modifier = Modifier
                        .clickable { onNavigateToGate() }
                        .testTag("view_gate_terminal_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (language == AppLanguage.MARATHI) "सुरक्षा केंद्र" else if (language == AppLanguage.HINDI) "गेट केंद्र" else "Terminal",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = GoogleAccentBlue
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = GoogleAccentBlue,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            /*
             * The home screen carries a count, not the log itself. The entries
             * live behind Terminal, so the dashboard stays a summary and the
             * activity is read where it can be acted on.
             */
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (visitors.isEmpty()) {
                        when (language) {
                            AppLanguage.MARATHI -> "कोणतीही अभ्यागत नोंद उपलब्ध नाही"
                            AppLanguage.HINDI -> "कोई आगंतुक प्रविष्टि नहीं"
                            AppLanguage.ENGLISH -> "No visitor logs recorded today"
                        }
                    } else {
                        when (language) {
                            AppLanguage.MARATHI -> "${visitors.size} नोंदी - पाहण्यासाठी Terminal उघडा"
                            AppLanguage.HINDI -> "${visitors.size} प्रविष्टियां - देखने के लिए Terminal खोलें"
                            AppLanguage.ENGLISH -> "${visitors.size} entries - open Terminal to view them"
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("gate_feed_summary")
                )
            }
        }
    }
}

@Composable
fun SmallVisitorBadge(
    status: VisitorStatus,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isInside = status == VisitorStatus.INSIDE
    val bgColor = if (isInside) GoogleSuccessContainer else SlateSurfaceVariant
    val textColor = if (isInside) GoogleSuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
    val label = when (status) {
        VisitorStatus.INSIDE -> when (language) {
            AppLanguage.ENGLISH -> "Inside"
            AppLanguage.HINDI -> "परिसर में"
            AppLanguage.MARATHI -> "परिसरात"
        }
        VisitorStatus.EXITED -> when (language) {
            AppLanguage.ENGLISH -> "Exited"
            AppLanguage.HINDI -> "बाहर"
            AppLanguage.MARATHI -> "बाहेर"
        }
        VisitorStatus.PRE_APPROVED -> when (language) {
            AppLanguage.ENGLISH -> "Pre-Approved"
            AppLanguage.HINDI -> "स्वीकृत"
            AppLanguage.MARATHI -> "मंजूर"
        }
        VisitorStatus.REJECTED -> when (language) {
            AppLanguage.ENGLISH -> "Rejected"
            AppLanguage.HINDI -> "अस्वीकृत"
            AppLanguage.MARATHI -> "नामंजूर"
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                color = textColor
            )
        }
    }
}
