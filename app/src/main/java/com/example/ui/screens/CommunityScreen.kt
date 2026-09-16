package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Complaint
import com.example.data.entity.ComplaintCategory
import com.example.data.entity.EMERGENCY_PHONE_DIGITS
import com.example.data.entity.EmergencyContact
import com.example.data.entity.Notice
import com.example.data.entity.NoticeCategory
import com.example.data.entity.Resident
import com.example.data.entity.TicketStatus
import com.example.data.entity.UserRole
import com.example.data.localization.AppLanguage
import com.example.ui.components.DialogWidth
import com.example.ui.components.WideDialogProperties
import com.example.data.localization.AppStrings
import com.example.ui.components.TicketStatusBadge
import com.example.ui.theme.GoldTertiary
import com.example.ui.theme.MintSecondary
import com.example.ui.components.SearchableDropdownField
import com.example.ui.theme.SlateOutlineVariant
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusOverdueBg
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealPrimaryContainer
import com.example.viewmodel.SocietyViewModel

@Composable
fun CommunityScreen(
    viewModel: SocietyViewModel,
    currentUser: Resident?,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf(
        AppStrings.get("notice_board", language),
        AppStrings.get("helpdesk_title", language),
        AppStrings.get("emergency_title", language),
        AppStrings.get("vehicle_directory", language)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = TealPrimary,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                    color = TealPrimary
                )
            }
        ) {
            subTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSubTab == index,
                    onClick = { selectedSubTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selectedSubTab == index) FontWeight.Bold else FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    },
                    modifier = Modifier.testTag("community_subtab_$index")
                )
            }
        }

        when (selectedSubTab) {
            0 -> NoticeBoardView(viewModel = viewModel, currentUser = currentUser, language = language)
            1 -> HelpdeskView(viewModel = viewModel, currentUser = currentUser, language = language)
            2 -> EmergencyDirectoryView(viewModel = viewModel, currentUser = currentUser, language = language)
            3 -> VehicleDirectoryView(viewModel = viewModel, currentUser = currentUser, language = language)
        }
    }
}

// 1. Notice Board & Bye-Laws View
@Composable
fun NoticeBoardView(
    viewModel: SocietyViewModel,
    currentUser: Resident?,
    language: AppLanguage
) {
    val notices by viewModel.notices.collectAsState()
    var isPostNoticeOpen by remember { mutableStateOf(false) }

    if (isPostNoticeOpen) {
        PostNoticeDialog(
            language = language,
            onDismiss = { isPostNoticeOpen = false },
            onSubmit = { title, content, isPinned, cat ->
                viewModel.postNotice(title, content, isPinned, cat)
                isPostNoticeOpen = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.get("notice_board", language),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                if (currentUser?.role == UserRole.CHAIRMAN || currentUser?.role == UserRole.SECRETARY || currentUser?.role == UserRole.COMMITTEE || currentUser == null) {
                    Button(
                        onClick = { isPostNoticeOpen = true },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("post_notice_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(AppStrings.get("post_notice", language), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Bye-Laws Card snippet
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
                border = BorderStroke(1.dp, SlateOutlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Gavel, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.MARATHI) "महाराष्ट्र सहकारी गृहनिर्माण उपविधी व नियम" else "Maharashtra Housing Society Bye-Laws & Rules",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (language == AppLanguage.MARATHI)
                            "• शांतता वेळ: रात्री १० ते सकाळी ६\n• सदनिका अंतर्गत दुरुस्ती: सकाळी ९ ते संध्याकाळी ६ (रविवारी बंद)\n• भाडेकरू पोलीस व्हेरिफिकेशन व ना-हरकत अनिवार्य\n• पार्किंग जागेचा गैरवापर केल्यास प्रति दिन ₹५०० दंड"
                        else
                            "• Silence Hours: 10:00 PM to 06:00 AM\n• Unit Renovation Works: 09:00 AM to 06:00 PM (No Sunday work)\n• Police Verification and Tenant NOC mandatory for sub-letting\n• Misuse of designated parking slot attracts ₹500/day fine",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(notices) { notice ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (notice.isPinned) Color(0xFFFFFBEB) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    1.dp,
                    if (notice.isPinned) Color(0xFFFDE68A) else SlateOutlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            if (notice.isPinned) {
                                Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = GoldTertiary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = notice.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (notice.isPinned) Color(0xFF92400E) else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (currentUser?.role == UserRole.CHAIRMAN || currentUser?.role == UserRole.SECRETARY) {
                            IconButton(
                                onClick = { viewModel.deleteNotice(notice) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusOverdue, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = notice.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (notice.isPinned) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Published by: ${notice.authorRole}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TealPrimary
                        )
                        Text(
                            text = notice.publishedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// 2. Helpdesk & Complaints View
@Composable
fun HelpdeskView(
    viewModel: SocietyViewModel,
    currentUser: Resident?,
    language: AppLanguage
) {
    /*
     * Only what this user is allowed to see. A complaint is between the unit
     * that raised it and the committee that has to act on it, so the committee
     * gets every ticket and a member gets their own units' and nobody else's -
     * Unit 304's leak was being shown to Unit 202.
     */
    val complaints by viewModel.visibleComplaints.collectAsState()
    val myUnits by viewModel.myUnits.collectAsState()
    val activeUnit by viewModel.activeUnit.collectAsState()
    var isFileComplaintOpen by remember { mutableStateOf(false) }
    var showAllUnits by remember { mutableStateOf(false) }
    var complaintBeingEdited by remember { mutableStateOf<Complaint?>(null) }
    var complaintPendingDeletion by remember { mutableStateOf<Complaint?>(null) }

    complaintBeingEdited?.let { complaint ->
        EditComplaintDialog(
            complaint = complaint,
            language = language,
            onDismiss = { complaintBeingEdited = null },
            onSave = { category, description, onError ->
                viewModel.editMyComplaint(complaint, category, description) { ok, message ->
                    if (ok) {
                        viewModel.showMessage(message)
                        complaintBeingEdited = null
                    } else {
                        onError(message)
                    }
                }
            }
        )
    }

    complaintPendingDeletion?.let { complaint ->
        AlertDialog(
            modifier = DialogWidth,
            properties = WideDialogProperties,
            onDismissRequest = { complaintPendingDeletion = null },
            title = {
                Text(
                    text = if (language == AppLanguage.MARATHI) "तक्रार मागे घ्यायची?" else "Withdraw this complaint?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = complaint.description,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMyComplaint(complaint) { _, message -> viewModel.showMessage(message) }
                        complaintPendingDeletion = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusOverdue),
                    modifier = Modifier.testTag("confirm_delete_complaint_btn")
                ) {
                    Text(if (language == AppLanguage.MARATHI) "मागे घ्या" else "Withdraw")
                }
            },
            dismissButton = {
                TextButton(onClick = { complaintPendingDeletion = null }) {
                    Text(AppStrings.get("cancel", language))
                }
            }
        )
    }

    val activeUnitNumber = (activeUnit ?: currentUser)?.flatNumber.orEmpty()
    // Narrowed once more by the unit picker, for a member who holds several.
    val listedComplaints = if (myUnits.size > 1 && !showAllUnits) {
        complaints.filter { it.flatNumber.equals(activeUnitNumber, ignoreCase = true) }
    } else {
        complaints
    }

    if (isFileComplaintOpen) {
        FileComplaintDialog(
            currentUser = activeUnit ?: currentUser,
            myUnits = myUnits,
            language = language,
            onDismiss = { isFileComplaintOpen = false },
            onSubmit = { cat, desc, flat, name ->
                viewModel.fileComplaint(cat, desc, flat, name)
                isFileComplaintOpen = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.get("helpdesk_title", language),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Button(
                    onClick = { isFileComplaintOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("file_complaint_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStrings.get("file_complaint", language), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // A member who owns several units can narrow the list further still;
        // the committee, which sees every ticket, has nothing to narrow.
        if (myUnits.size > 1) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = !showAllUnits,
                        onClick = { showAllUnits = false },
                        label = {
                            Text(
                                text = "${AppStrings.get("select_unit", language)}: $activeUnitNumber",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.testTag("helpdesk_filter_my_unit")
                    )
                    FilterChip(
                        selected = showAllUnits,
                        onClick = { showAllUnits = true },
                        label = {
                            Text(
                                text = AppStrings.get("filter_all", language),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.testTag("helpdesk_filter_all")
                    )
                }
            }
        }

        items(listedComplaints) { complaint ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SlateOutlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${if (language == AppLanguage.MARATHI) complaint.category.labelMr else complaint.category.labelEn} • ${complaint.flatNumber}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "By: ${complaint.residentName} • Filed: ${complaint.filedDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TicketStatusBadge(status = complaint.status, language = language)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = complaint.description,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (complaint.resolutionNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Resolution: ${complaint.resolutionNotes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusPaid
                        )
                    }

                    /*
                     * The member who raised it may correct or withdraw it, but
                     * only while it is still open - once the committee has
                     * picked it up, editing it underneath them would rewrite
                     * what they are working from.
                     */
                    val isMine = myUnits.any { it.flatNumber.equals(complaint.flatNumber, ignoreCase = true) }
                    if (isMine && complaint.status == TicketStatus.OPEN) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { complaintBeingEdited = complaint },
                                modifier = Modifier.testTag("edit_complaint_btn_${complaint.id}")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = AppStrings.get("edit_profile", language),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TealPrimary
                                )
                            }
                            TextButton(
                                onClick = { complaintPendingDeletion = complaint },
                                modifier = Modifier.testTag("delete_complaint_btn_${complaint.id}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = StatusOverdue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (language == AppLanguage.MARATHI) "मागे घ्या" else "Withdraw",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StatusOverdue
                                )
                            }
                        }
                    }

                    // Admin resolution controls
                    if (currentUser?.role == UserRole.CHAIRMAN || currentUser?.role == UserRole.SECRETARY || currentUser?.role == UserRole.COMMITTEE) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (complaint.status != TicketStatus.RESOLVED) {
                                TextButton(
                                    onClick = {
                                        viewModel.updateComplaintStatus(complaint, TicketStatus.RESOLVED, "Resolved by Managing Committee")
                                    }
                                ) {
                                    Text(
                                        text = if (language == AppLanguage.MARATHI) "निवारण पूर्ण (Mark Resolved)" else "Mark Resolved",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = StatusPaid)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 3. Emergency Contacts Directory
@Composable
fun EmergencyDirectoryView(
    viewModel: SocietyViewModel,
    currentUser: Resident?,
    language: AppLanguage
) {
    val allContacts by viewModel.emergencyContacts.collectAsState()
    val context = LocalContext.current
    val canManage = currentUser?.role == UserRole.CHAIRMAN
    var editing by remember { mutableStateOf<EmergencyContact?>(null) }

    // Members get the numbers that are actually in service. The committee sees
    // the inactive ones too, so they can be switched back on.
    val contacts = if (canManage) allContacts else allContacts.filter { it.isActive }

    editing?.let { contact ->
        EmergencyContactDialog(
            contact = contact,
            language = language,
            onDismiss = { editing = null },
            onSave = { updated ->
                viewModel.saveEmergencyContact(updated) { _, message -> viewModel.showMessage(message) }
                editing = null
            },
            onDelete = if (contact.docId.isBlank()) null else {
                {
                    viewModel.deleteEmergencyContact(contact) { _, message -> viewModel.showMessage(message) }
                    editing = null
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = AppStrings.get("emergency_title", language),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (language == AppLanguage.MARATHI) "१-टॅप त्वरित आपत्कालीन कॉल सेवा" else "1-Tap Direct Emergency Calling Services",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Only the Chairman can add or change these numbers.
                if (canManage) {
                    Button(
                        onClick = { editing = EmergencyContact(titleEn = "", titleMr = "", name = "", phoneNumber = "", category = "Society") },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("add_emergency_contact_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(AppStrings.get("add_emergency_contact", language), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        items(contacts) { contact ->
            val isUrgent = contact.category.contains("Police", ignoreCase = true) ||
                    contact.category.contains("Fire", ignoreCase = true) ||
                    contact.category.contains("Ambulance", ignoreCase = true)

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SlateOutlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${contact.phoneNumber}")
                        }
                        context.startActivity(intent)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isUrgent) StatusOverdue.copy(alpha = 0.15f)
                                    else TealPrimaryContainer
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getEmergencyIcon(contact.category),
                                contentDescription = null,
                                tint = if (isUrgent) StatusOverdue else TealPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (language == AppLanguage.MARATHI) contact.titleMr else contact.titleEn,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                // Only the committee ever sees this - members
                                // are not shown inactive contacts at all.
                                if (!contact.isActive) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(shape = RoundedCornerShape(6.dp), color = StatusOverdueBg) {
                                        Text(
                                            text = "Inactive",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = StatusOverdue,
                                            modifier = Modifier
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                .testTag("emergency_inactive_chip")
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${contact.name} • ${contact.phoneNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (canManage) {
                        IconButton(
                            onClick = { editing = contact },
                            modifier = Modifier.testTag("edit_emergency_${contact.docId}")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit contact", tint = TealPrimary)
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = StatusPaidBg,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Call, contentDescription = "Call", tint = StatusPaid, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

// 4. Vehicle & Parking Directory View
@Composable
fun VehicleDirectoryView(
    viewModel: SocietyViewModel,
    currentUser: Resident?,
    language: AppLanguage
) {
    // Members only. A guard's or a sweeper's bay is not part of the residents'
    // parking list.
    val residents by viewModel.members.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    // A member sees who parks where, but not everybody's phone number - the
    // committee and the gate keep that. Members can still see their own.
    val isMember = currentUser?.role == UserRole.RESIDENT || currentUser?.role == UserRole.TENANT

    val filtered = residents.filter {
        it.parkingSlot.contains(searchQuery, ignoreCase = true) ||
                it.flatNumber.contains(searchQuery, ignoreCase = true) ||
                it.twoWheelerNumber.contains(searchQuery, ignoreCase = true) ||
                it.fourWheelerNumber.contains(searchQuery, ignoreCase = true) ||
                it.ownerName.contains(searchQuery, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by plate number, flat or parking slot") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(filtered) { resident ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SlateOutlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${resident.flatNumber} • ${resident.ownerName}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "🅿️ Slot: ${resident.parkingSlot.ifBlank { "N/A" }}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = TealPrimary
                        )
                        if (resident.fourWheelerNumber.isNotBlank()) {
                            Text(text = "🚗 4W: ${resident.fourWheelerNumber}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (resident.twoWheelerNumber.isNotBlank()) {
                            Text(text = "🛵 2W: ${resident.twoWheelerNumber}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    val isOwnRow = resident.flatNumber.equals(currentUser?.flatNumber, ignoreCase = true)
                    if (!isMember || isOwnRow) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SlateSurfaceVariant
                        ) {
                            Text(
                                text = "📞 ${resident.contactNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("parking_contact_${resident.flatNumber}")
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getEmergencyIcon(category: String): ImageVector {
    val lower = category.lowercase()
    return when {
        lower.contains("police") -> Icons.Default.LocalPolice
        lower.contains("fire") -> Icons.Default.Emergency
        lower.contains("ambulance") || lower.contains("hospital") -> Icons.Default.LocalHospital
        lower.contains("lift") -> Icons.Default.Security
        lower.contains("gate") || lower.contains("security") -> Icons.Default.Security
        else -> Icons.Default.SupportAgent
    }
}

// Dialog: Post Notice
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostNoticeDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSubmit: (title: String, content: String, isPinned: Boolean, cat: NoticeCategory) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isPinned by remember { mutableStateOf(false) }
    var selectedCat by remember { mutableStateOf(NoticeCategory.GENERAL) }
    var isCatExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        modifier = DialogWidth,
        properties = WideDialogProperties,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = AppStrings.get("post_notice", language),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Notice Title (विषय)") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = isCatExpanded,
                    onExpandedChange = { isCatExpanded = !isCatExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCat.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCatExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isCatExpanded,
                        onDismissRequest = { isCatExpanded = false }
                    ) {
                        NoticeCategory.values().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCat = cat
                                    isCatExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Notice Details (सविस्तर माहिती)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Pin to top (महत्त्वाची सूचना)")
                    Switch(checked = isPinned, onCheckedChange = { isPinned = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        onSubmit(title, content, isPinned, selectedCat)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text(AppStrings.get("submit", language))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", language))
            }
        }
    )
}

/** A member correcting the complaint they raised, while it is still open. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditComplaintDialog(
    complaint: Complaint,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (
        category: ComplaintCategory,
        description: String,
        onError: (String) -> Unit
    ) -> Unit
) {
    var selectedCat by remember(complaint) { mutableStateOf(complaint.category) }
    var desc by remember(complaint) { mutableStateOf(complaint.description) }
    var isCatExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        modifier = DialogWidth,
        properties = WideDialogProperties,
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (language == AppLanguage.MARATHI) "तक्रार संपादित करा" else "Edit Complaint",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${complaint.flatNumber} • ${complaint.filedDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TealPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = isCatExpanded,
                    onExpandedChange = { isCatExpanded = !isCatExpanded }
                ) {
                    OutlinedTextField(
                        value = if (language == AppLanguage.MARATHI) selectedCat.labelMr else selectedCat.labelEn,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Issue Type (तक्रार प्रकार)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCatExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isCatExpanded,
                        onDismissRequest = { isCatExpanded = false }
                    ) {
                        ComplaintCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(if (language == AppLanguage.MARATHI) cat.labelMr else cat.labelEn) },
                                onClick = { selectedCat = cat; isCatExpanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it; error = null },
                    label = { Text("Complaint Details (तपशील)") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_complaint_desc_input")
                )

                error?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedCat, desc) { message -> error = message } },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.testTag("save_complaint_btn")
            ) {
                Text(AppStrings.get("save", language))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", language))
            }
        }
    )
}

// Dialog: File Complaint
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileComplaintDialog(
    currentUser: Resident?,
    /**
     * Every unit the signed-in member holds. One unit fills the form in and
     * locks it; several turn the unit number into a picker, and the resident
     * name follows whichever unit is chosen.
     */
    myUnits: List<Resident> = emptyList(),
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSubmit: (cat: ComplaintCategory, desc: String, flat: String, name: String) -> Unit
) {
    var selectedCat by remember { mutableStateOf(ComplaintCategory.PLUMBING) }
    var desc by remember { mutableStateOf("") }
    // Filled in from the signed-in member - never typed by hand.
    var flat by remember(currentUser) { mutableStateOf(currentUser?.flatNumber.orEmpty()) }
    var isCatExpanded by remember { mutableStateOf(false) }

    val selectedUnit = myUnits.firstOrNull { it.flatNumber.equals(flat, ignoreCase = true) } ?: currentUser
    val name = selectedUnit?.ownerName.orEmpty().ifBlank { "Resident" }

    AlertDialog(
        modifier = DialogWidth,
        properties = WideDialogProperties,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = AppStrings.get("file_complaint", language),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = isCatExpanded,
                    onExpandedChange = { isCatExpanded = !isCatExpanded }
                ) {
                    OutlinedTextField(
                        value = if (language == AppLanguage.MARATHI) selectedCat.labelMr else selectedCat.labelEn,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Issue Type (तक्रार प्रकार)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCatExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isCatExpanded,
                        onDismissRequest = { isCatExpanded = false }
                    ) {
                        ComplaintCategory.values().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(if (language == AppLanguage.MARATHI) cat.labelMr else cat.labelEn) },
                                onClick = {
                                    selectedCat = cat
                                    isCatExpanded = false
                                }
                            )
                        }
                    }
                }

                // The unit and the name come from who is signed in. A member
                // with more than one unit chooses which one this is about.
                if (myUnits.size > 1) {
                    SearchableDropdownField(
                        label = AppStrings.get("unit_number", language),
                        value = flat,
                        options = myUnits.map { it.flatNumber },
                        onValueChange = { flat = it },
                        allowFreeText = false,
                        testTag = "complaint_unit_dropdown"
                    )
                } else {
                    OutlinedTextField(
                        value = flat,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text(AppStrings.get("unit_number", language)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("complaint_unit_field")
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Resident Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("complaint_resident_name_field")
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Complaint Details (तपशील)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (desc.isNotBlank()) {
                        onSubmit(selectedCat, desc, flat, name)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text(AppStrings.get("submit", language))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", language))
            }
        }
    )
}

/** Add / edit / delete one emergency contact. Chairman-only entry point. */
@Composable
fun EmergencyContactDialog(
    contact: EmergencyContact,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (EmergencyContact) -> Unit,
    onDelete: (() -> Unit)?
) {
    var titleEn by remember { mutableStateOf(contact.titleEn) }
    var titleMr by remember { mutableStateOf(contact.titleMr) }
    var name by remember { mutableStateOf(contact.name) }
    var phone by remember { mutableStateOf(contact.phoneNumber) }
    var category by remember { mutableStateOf(contact.category) }
    var isActive by remember { mutableStateOf(contact.isActive) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        modifier = DialogWidth,
        properties = WideDialogProperties,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (contact.docId.isBlank()) {
                    AppStrings.get("add_emergency_contact", language)
                } else {
                    AppStrings.get("emergency_title", language)
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = titleEn,
                    onValueChange = { titleEn = it },
                    label = { Text("Title (English)") },
                    placeholder = { Text("e.g. Society Plumber") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("emergency_title_input")
                )
                OutlinedTextField(
                    value = titleMr,
                    onValueChange = { titleMr = it },
                    label = { Text("Title (मराठी)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Contact Person / Agency") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { ch -> ch.isDigit() || ch == '+' }.take(15) },
                    label = { Text(AppStrings.get("mobile_number", language)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("emergency_phone_input")
                )
                SearchableDropdownField(
                    label = "Category",
                    value = category,
                    options = EMERGENCY_CATEGORIES,
                    onValueChange = { category = it }
                )

                // Inactive contacts stay on file for the committee but are not
                // shown to members - a number that is out of service must not
                // sit on an emergency list.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isActive) "Active" else "Inactive",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isActive) {
                                "Shown to every member."
                            } else {
                                "Kept on file, hidden from members."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        modifier = Modifier.testTag("emergency_active_switch")
                    )
                }

                error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("emergency_error_text")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = titleEn.isNotBlank() && phone.isNotBlank(),
                onClick = {
                    // An emergency number that cannot be dialled is worse than
                    // none at all, so the shape is checked before it is saved.
                    val digits = phone.filter { it.isDigit() }
                    if (digits.length !in EMERGENCY_PHONE_DIGITS) {
                        error = "Enter a valid phone number - 3 to 15 digits, " +
                            "so short codes like 100 and full mobile numbers both work."
                        return@Button
                    }
                    error = null
                    onSave(
                        contact.copy(
                            titleEn = titleEn.trim(),
                            titleMr = titleMr.trim().ifBlank { titleEn.trim() },
                            name = name.trim(),
                            phoneNumber = phone.trim(),
                            category = category.trim().ifBlank { "Society" },
                            isActive = isActive
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.testTag("save_emergency_contact_btn")
            ) {
                Text(AppStrings.get("submit", language))
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(AppStrings.get("delete", language), color = StatusOverdue)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(AppStrings.get("cancel", language))
                }
            }
        }
    )
}

private val EMERGENCY_CATEGORIES = listOf(
    "Police", "Fire", "Ambulance", "Hospital", "Security", "Plumber",
    "Electrician", "Lift Technician", "Water Tanker", "Society"
)
