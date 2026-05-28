package com.gym.frontend.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gym.frontend.ui.theme.*
import com.gym.frontend.ui.shared.*
import com.gym.shared.domain.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Image
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.layout.ContentScale

@Composable
fun MembersListScreen(
    requestOpenAddDialog: Boolean = false,
    onAddDialogRequestConsumed: () -> Unit = {}
) {
    val isDark = LocalIsDarkMode.current
    val scope = rememberCoroutineScope()
    // In a real app, this would be injected via Koin
    val repository = remember { MembersRepository(MembersService()) }
    
    var members by remember { mutableStateOf<List<Member>>(emptyList()) }
    var plans by remember { mutableStateOf<List<GymPlan>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    var showDialog by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<Member?>(null) }

    // --- Search & Sort State ---
    var searchQuery by remember { mutableStateOf("") }
    var currentSortColumn by remember { mutableStateOf("JOIN_DATE") }
    var sortAscending by remember { mutableStateOf(false) }
    var statusFilter by remember { mutableStateOf<String?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }

    val filteredMembers = remember(members, searchQuery, currentSortColumn, sortAscending, statusFilter) {
        members
            .filter { 
                (statusFilter == null || it.status.uppercase() == statusFilter) &&
                (it.name.contains(searchQuery, ignoreCase = true) || it.email.contains(searchQuery, ignoreCase = true))
            }
            .sortedWith { a, b ->
                val factor = if (sortAscending) 1 else -1
                when (currentSortColumn) {
                    "NAME" -> factor * a.name.compareTo(b.name)
                    "STATUS" -> factor * a.status.compareTo(b.status)
                    "PLAN" -> factor * (a.currentPlan ?: "").compareTo(b.currentPlan ?: "")
                    else -> factor * a.joinDate.compareTo(b.joinDate)
                }
            }
    }

    fun refresh() {
        isLoading = true
        scope.launch {
            repository.getMembers()
                .onSuccess { 
                    members = it 
                    isLoading = false
                }
                .onFailure { 
                    errorMessage = it.message ?: "Failed to load members"
                    isLoading = false
                }
            repository.getPlans().onSuccess { plans = it }
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    LaunchedEffect(requestOpenAddDialog) {
        if (requestOpenAddDialog) {
            editingMember = null
            showDialog = true
            onAddDialogRequestConsumed()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        "Active Members",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Manage your elite community at the Kinetic Atelier. Tracking performance and participation across the ecosystem.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.width(600.dp)
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Search Bar
                    Surface(
                        modifier = Modifier.width(300.dp).height(48.dp),
                        color = if (isDark) Level1Section else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp), tint = OnSurfaceDim)
                            Spacer(Modifier.width(8.dp))
                            androidx.compose.foundation.text.BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(TealPrimary),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) Text("Search members...", color = OnSurfaceDim, style = MaterialTheme.typography.bodyMedium)
                                    innerTextField()
                                }
                            )
                        }
                    }

                    // Filter Button with Dropdown
                    Box {
                        Surface(
                            modifier = Modifier.clickable { showFilterMenu = true },
                            color = if (statusFilter != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else (if (isDark) Level1Section else MaterialTheme.colorScheme.surfaceVariant), 
                            shape = RoundedCornerShape(24.dp),
                            border = if (statusFilter != null) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.FilterList, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(statusFilter ?: "Filters", style = MaterialTheme.typography.labelLarge, color = if (statusFilter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false },
                            modifier = Modifier.background(if (isDark) Level1Section else Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Members") },
                                onClick = { statusFilter = null; showFilterMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Active") },
                                onClick = { statusFilter = "ACTIVE"; showFilterMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Inactive") },
                                onClick = { statusFilter = "INACTIVE"; showFilterMenu = false }
                            )
                        }
                    }

                    KineticButton(
                        onClick = { 
                            editingMember = null
                            showDialog = true 
                        },
                        text = "+ Add Member",
                        modifier = Modifier.width(180.dp).height(48.dp)
                    )
                }
            }

    var isDialogSuccess by remember { mutableStateOf(false) }

    if (showDialog) {
        MemberFormDialog(
            member = editingMember,
            plans = plans,
            onDismiss = { 
                showDialog = false 
                isDialogSuccess = false
                errorMessage = null 
            },
            isSuccess = isDialogSuccess,
            onSave = { request ->
                errorMessage = null
                scope.launch {
                    val result = if (editingMember == null) {
                        repository.createMember(request)
                    } else {
                        repository.updateMember(editingMember!!.id, request)
                    }
                    result.onSuccess {
                        isDialogSuccess = true
                        errorMessage = null
                        refresh()
                    }.onFailure {
                        errorMessage = it.message ?: "Action failed"
                    }
                }
            },
            errorMessage = errorMessage,
            onClearError = { errorMessage = null }
        )
    }

            Spacer(Modifier.height(32.dp))

            // --- MAIN CONTENT (STATS ROW) ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Surface(
                    modifier = Modifier.weight(1f).height(160.dp), 
                    shape = RoundedCornerShape(32.dp), 
                    color = if (isDark) Level1Section else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Outlined.Groups, contentDescription = null, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp).size(80.dp).alpha(0.05f))
                        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.Center) {
                            Text("TOTAL CAPACITY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            Text("1,284", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("12% increase this month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f).height(160.dp), 
                    shape = RoundedCornerShape(32.dp), 
                    color = if (isDark) Level1Section else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("CHECK-INS (TODAY)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = LavenderTertiary)
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("342", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(" / 500 cap", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
                            VolumeBar("MON", 0.4f, false)
                            VolumeBar("TUE", 0.6f, false)
                            VolumeBar("WED", 0.75f, false)
                            VolumeBar("THU", 0.9f, true)
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))

            // --- DATA ROWS ---
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                shape = RoundedCornerShape(32.dp),
                color = if (isDark) Level1Section else Color(0xFFF0F2F2).copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    // Table Header
                    val toggleSort: (String) -> Unit = { col ->
                        if (currentSortColumn == col) {
                            sortAscending = !sortAscending
                        } else {
                            currentSortColumn = col
                            sortAscending = true
                        }
                    }

                    Row(modifier = Modifier.padding(horizontal = 32.dp, vertical = 20.dp)) {
                        TableHeader("MEMBER", Modifier.weight(3f).height(25.dp), sortable = true, isSorted = currentSortColumn == "NAME", ascending = sortAscending, onClick = { toggleSort("NAME") })
                        TableHeader("STATUS", Modifier.weight(1.5f).height(25.dp), sortable = true, isSorted = currentSortColumn == "STATUS", ascending = sortAscending, onClick = { toggleSort("STATUS") })
                        TableHeader("PLAN", Modifier.weight(2f).height(25.dp), sortable = true, isSorted = currentSortColumn == "PLAN", ascending = sortAscending, onClick = { toggleSort("PLAN") })
                        TableHeader("EXPIRES AT", Modifier.weight(2f).height(25.dp))
                        TableHeader("JOIN DATE", Modifier.weight(2f).height(25.dp), sortable = true, isSorted = currentSortColumn == "JOIN_DATE", ascending = sortAscending, onClick = { toggleSort("JOIN_DATE") })
                        TableHeader("ACTION", Modifier.width(60.dp))
                    }

                    when {
                        isLoading -> {
                            Column {
                                repeat(3) { MemberSkeleton(isDark) }
                            }
                        }
                        errorMessage != null -> {
                            ErrorState(errorMessage!!, onRetry = {
                                scope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    repository.getMembers()
                                        .onSuccess { members = it; isLoading = false }
                                        .onFailure { errorMessage = it.message; isLoading = false }
                                }
                            })
                        }
                        members.isEmpty() -> {
                            EmptyState(isDark)
                        }
                        else -> {
                            filteredMembers.forEach { member ->
                                val dateStr = try {
                                    val local = member.joinDate.toLocalDateTime(TimeZone.currentSystemDefault())
                                    "${local.month.name.take(3)} ${local.dayOfMonth}, ${local.year}"
                                } catch (e: Exception) { "Jan 12, 2024" }
                                
                                val expDateStr = member.expirationDate?.let {
                                    val local = it.toLocalDateTime(TimeZone.currentSystemDefault())
                                    "${local.dayOfMonth}/${local.monthNumber}/${local.year}"
                                } ?: "No payment"

                                MemberRow(
                                    name = member.name,
                                    email = member.email,
                                    status = member.status.uppercase(),
                                    plan = member.currentPlan ?: "Standard Plan",
                                    expirationDate = expDateStr,
                                    date = dateStr,
                                    profileImageUrl = member.profileImageUrl,
                                    lastCheckin = "Not available",
                                    location = "STRENGTH STUDIO",
                                    isDark = isDark,
                                    onEdit = {
                                        editingMember = member
                                        showDialog = true
                                    }
                                )
                            }
                        }
                    }
                    
                    // Pagination
                    if (!isLoading && errorMessage == null && members.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Showing ${filteredMembers.size} members", style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("‹", fontSize = 20.sp, color = OnSurfaceNeutral)
                                PaginationDot("1", true)
                                Text("›", fontSize = 20.sp, color = OnSurfaceNeutral)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // --- FOOTER ANALYTICS / TRENDS ---
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 60.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Surface(modifier = Modifier.weight(2f), shape = RoundedCornerShape(32.dp), color = if (isDark) Level1Section else Color(0xFFF0F2F2).copy(alpha = 0.5f)) {
                    Row(modifier = Modifier.padding(32.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Member Growth Trends", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Kinetic Atelier has seen a 12% increase in active memberships this quarter. Personalized training remains the highest-rated service.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceDim
                            )
                            Spacer(Modifier.height(24.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                                Column {
                                    Text("842", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                                    Text("TOTAL COMMUNITY", style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim)
                                }
                                Column {
                                    Text("96%", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                                    Text("RETENTION RATE", style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim)
                                }
                            }
                        }
                        
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier.width(180.dp).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Text("Download Quarterly Report", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color.Black)
                        }
                    }
                }
                
                Surface(
                    modifier = Modifier.weight(1f), 
                    shape = RoundedCornerShape(32.dp), 
                    color = if (isDark) Level1Section else Color(0xFF003D44)
                ) {
                    Column(modifier = Modifier.padding(32.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Surface(color = Color.White.copy(alpha = 0.1f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center) { Text("⚡", color = Color.White) }
                            }
                            Surface(color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)) {
                                Text("NEW FEATURE", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = Color.White, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Text("Atelier Intelligence", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "AI-driven scheduling suggestions are now available for all Platinum members.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(Modifier.weight(1f))
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).align(Alignment.End), contentAlignment = Alignment.Center) {
                            Text("👥+", color = Color.White)
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
        )
    }
}

@Composable
fun MemberRow(name: String, email: String, status: String, plan: String, expirationDate: String, date: String, profileImageUrl: String?, lastCheckin: String, location: String, isDark: Boolean, onEdit: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        color = if (isDark) Color.Transparent else Color.White,
        shape = if (isDark) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(3f), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.LightGray)) {
                    val avatarRes = getAvatarResource(profileImageUrl)
                    if (avatarRes != null) {
                        Image(
                            painter = painterResource(avatarRes),
                            contentDescription = "Profile Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(imageVector = (Icons.Outlined.Person), contentDescription = "Profile icon", tint = Color.White, modifier = Modifier.align(Alignment.Center))
                    }
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(if (status == "ACTIVE") Color(0xFF4CAF50) else Color.Gray).border(2.dp, Color.White, CircleShape).align(Alignment.BottomEnd))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Text(email, color = OnSurfaceDim, style = MaterialTheme.typography.labelSmall)
                }
            }
            
            Box(modifier = Modifier.weight(1.5f)) {
                Surface(
                    color = if (status == "ACTIVE") {
                        if (isDark) Color(0xFF1B2E2D) else Color(0xFFE0F2F1)
                    } else {
                        if (isDark) Color(0xFF2C1B1B) else Color(0xFFF0F2F2)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        status,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = if (status == "ACTIVE") TealPrimary else Color.Gray,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Column(modifier = Modifier.weight(2.5f)) {
                val lines = plan.split("\n")
                Text(lines[0], style = MaterialTheme.typography.bodyMedium)
                if (lines.size > 1) {
                    Text(lines[1], style = MaterialTheme.typography.labelSmall, color = Color(0xFF5C6BC0), fontWeight = FontWeight.Bold)
                }
            }
            
            Column(modifier = Modifier.weight(2f)) {
                Text(expirationDate, style = MaterialTheme.typography.bodyMedium, color = if (status == "ACTIVE") Color.Unspecified else Color(0xFFD32F2F))
                Text("EXPIRATION", style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim, fontWeight = FontWeight.Bold)
            }

            Column(modifier = Modifier.weight(2f)) {
                Text(date, style = MaterialTheme.typography.bodyMedium)
                Text(location, style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim, fontWeight = FontWeight.Bold)
            }
            
            Box(modifier = Modifier.width(60.dp), contentAlignment = Alignment.CenterEnd) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit Member", tint = TealPrimary)
                }
            }
        }
    }
}

@Composable
fun VolumeBar(day: String, height: Float, active: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (active) TealPrimary.copy(alpha = 0.2f) else OnSurfaceDim.copy(alpha = 0.1f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(height)
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .background(if (active) TealPrimary else OnSurfaceNeutral.copy(alpha = 0.5f))
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(day, style = MaterialTheme.typography.labelSmall, color = if (active) OnSurfaceNeutral else OnSurfaceDim)
    }
}

@Composable
fun MemberSkeleton(isDark: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(if (isDark) Color.White.copy(0.05f) else Color.LightGray.copy(0.3f)))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(3f)) {
            Box(modifier = Modifier.width(120.dp).height(16.dp).background(if (isDark) Color.White.copy(0.05f) else Color.LightGray.copy(0.3f)))
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.width(180.dp).height(12.dp).background(if (isDark) Color.White.copy(0.05f) else Color.LightGray.copy(0.3f)))
        }
        Box(modifier = Modifier.weight(1.5f).height(24.dp).clip(RoundedCornerShape(8.dp)).background(if (isDark) Color.White.copy(0.05f) else Color.LightGray.copy(0.3f)))
        Box(modifier = Modifier.weight(2.5f).height(16.dp).background(if (isDark) Color.White.copy(0.05f) else Color.LightGray.copy(0.3f)))
        Box(modifier = Modifier.weight(2f))
        Box(modifier = Modifier.width(60.dp))
    }
}

@Composable
fun EmptyState(isDark: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.PersonOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = OnSurfaceDim)
        Spacer(Modifier.height(16.dp))
        Text("No members found", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Try adding your first member to see them here.", color = OnSurfaceDim)
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Red.copy(0.7f))
        Spacer(Modifier.height(16.dp))
        Text("Connection Error", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(message, color = OnSurfaceDim, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("Retry Connection")
        }
    }
}
