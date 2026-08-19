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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Complaint
import com.example.data.entity.ComplaintCategory
import com.example.data.entity.EmergencyContact
import com.example.data.entity.Notice
import com.example.data.entity.NoticeCategory
import com.example.data.entity.Resident
import com.example.data.entity.TicketStatus
import com.example.data.entity.UserRole
import com.example.data.localization.AppLanguage
import com.example.data.localization.AppStrings
import com.example.ui.components.TicketStatusBadge
import com.example.ui.theme.GoldTertiary
import com.example.ui.theme.MintSecondary
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
            2 -> EmergencyDirectoryView(viewModel = viewModel, language = language)
            3 -> VehicleDirectoryView(viewModel = viewModel, language = language)
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
                            "• Silence Hours: 10:00 PM to 06:00 AM\n• Flat Renovation Works: 09:00 AM to 06:00 PM (No Sunday work)\n• Police Verification and Tenant NOC mandatory for sub-letting\n• Misuse of designated parking slot attracts ₹500/day fine",
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
    val complaints by viewModel.complaints.collectAsState()
    var isFileComplaintOpen by remember { mutableStateOf(false) }

    if (isFileComplaintOpen) {
        FileComplaintDialog(
            currentUser = currentUser,
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
                    colors = ButtonDefaults.buttonColors(containerColor = StatusOverdue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("file_complaint_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStrings.get("file_complaint", language), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        items(complaints) { complaint ->
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
    language: AppLanguage
) {
    val contacts by viewModel.emergencyContacts.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = AppStrings.get("emergency_title", language),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = if (language == AppLanguage.MARATHI) "१-टॅप त्वरित आपत्कालीन कॉल सेवा" else "1-Tap Direct Emergency Calling Services",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                            Text(
                                text = if (language == AppLanguage.MARATHI) contact.titleMr else contact.titleEn,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${contact.name} • ${contact.phoneNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
    language: AppLanguage
) {
    val residents by viewModel.residents.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

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
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SlateSurfaceVariant
                    ) {
                        Text(
                            text = "📞 ${resident.contactNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
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

// Dialog: File Complaint
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileComplaintDialog(
    currentUser: Resident?,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSubmit: (cat: ComplaintCategory, desc: String, flat: String, name: String) -> Unit
) {
    var selectedCat by remember { mutableStateOf(ComplaintCategory.PLUMBING) }
    var desc by remember { mutableStateOf("") }
    var flat by remember { mutableStateOf(currentUser?.flatNumber ?: "") }
    var name by remember { mutableStateOf(currentUser?.ownerName ?: "Resident") }
    var isCatExpanded by remember { mutableStateOf(false) }

    AlertDialog(
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

                OutlinedTextField(
                    value = flat,
                    onValueChange = { flat = it },
                    label = { Text(AppStrings.get("unit_number", language)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Resident Name") },
                    modifier = Modifier.fillMaxWidth()
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
                colors = ButtonDefaults.buttonColors(containerColor = StatusOverdue)
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
