package com.gym.frontend.ui.admin

import androidx.compose.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gym.frontend.ui.shared.*
import com.gym.frontend.ui.theme.*
import com.gym.shared.domain.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
fun MembersListScreen(
    requestOpenAddDialog: Boolean = false,
    onAddDialogRequestConsumed: () -> Unit = {}
) {
    val viewModel = koinInject<MembersListViewModel>()
    val scope = rememberCoroutineScope()
    val isDark = LocalIsDarkMode.current

    val uiState by viewModel.uiState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentSortColumn by viewModel.currentSortColumn.collectAsState()
    val sortAscending by viewModel.sortAscending.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<Member?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadData(scope)
    }

    LaunchedEffect(requestOpenAddDialog) {
        if (requestOpenAddDialog) {
            editingMember = null
            showDialog = true
            onAddDialogRequestConsumed()
        }
    }

    LaunchedEffect(actionState) {
        when (actionState) {
            is MembersListActionState.SaveSuccess -> {
                showDialog = false
                editingMember = null
                viewModel.resetActionState()
            }
            is MembersListActionState.SaveError -> {
                snackbarHostState.showSnackbar((actionState as MembersListActionState.SaveError).message)
                viewModel.resetActionState()
            }
            else -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MembersListContent(
            uiState = uiState,
            searchQuery = searchQuery,
            statusFilter = statusFilter,
            currentSortColumn = currentSortColumn,
            sortAscending = sortAscending,
            onSearchQueryChange = { viewModel.setSearchQuery(it) },
            onStatusFilterChange = { viewModel.setStatusFilter(it) },
            onToggleSort = { viewModel.toggleSort(it) },
            onAddMember = {
                editingMember = null
                showDialog = true
            },
            onEditMember = { member ->
                editingMember = member
                showDialog = true
            },
            onRetry = { viewModel.loadData(scope) }
        )

        val plans = (uiState as? MembersListUiState.Success)?.plans ?: emptyList()
        val isDialogSuccess = actionState is MembersListActionState.SaveSuccess
        val dialogErrorMessage = (actionState as? MembersListActionState.SaveError)?.message

        if (showDialog) {
            MemberFormDialog(
                member = editingMember,
                plans = plans,
                onDismiss = {
                    showDialog = false
                    editingMember = null
                    viewModel.resetActionState()
                },
                isSuccess = isDialogSuccess,
                onSave = { request ->
                    if (editingMember == null) {
                        viewModel.createMember(request, scope)
                    } else {
                        viewModel.updateMember(editingMember!!.id, request, scope)
                    }
                },
                errorMessage = dialogErrorMessage,
                onClearError = { viewModel.resetActionState() }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
        )
    }
}

@Composable
fun MembersListContent(
    uiState: MembersListUiState,
    searchQuery: String,
    statusFilter: String?,
    currentSortColumn: String,
    sortAscending: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onStatusFilterChange: (String?) -> Unit,
    onToggleSort: (String) -> Unit,
    onAddMember: () -> Unit,
    onEditMember: (Member) -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        MembersListHeader(
            searchQuery = searchQuery,
            statusFilter = statusFilter,
            onSearchQueryChange = onSearchQueryChange,
            onStatusFilterChange = onStatusFilterChange,
            onAddMember = onAddMember
        )

        Spacer(Modifier.height(32.dp))
        MembersStatsSection()

        Spacer(Modifier.height(32.dp))
        MembersTableSection(
            uiState = uiState,
            currentSortColumn = currentSortColumn,
            sortAscending = sortAscending,
            onToggleSort = onToggleSort,
            onEditMember = onEditMember,
            onRetry = onRetry
        )

        Spacer(Modifier.height(32.dp))
        MembersFooterSection()
    }
}

@Composable
fun MembersListHeader(
    searchQuery: String,
    statusFilter: String?,
    onSearchQueryChange: (String) -> Unit,
    onStatusFilterChange: (String?) -> Unit,
    onAddMember: () -> Unit
) {
    val isDark = LocalIsDarkMode.current
    var showFilterMenu by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // Punto de quiebre: si el ancho disponible es menor a 900dp, apilamos verticalmente
        val isNarrow = maxWidth < 900.dp

        if (isNarrow) {
            // Layout vertical para pantallas angostas (720p, tabletas pequeñas, etc.)
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Título y subtítulo (sin ancho fijo)
                Column {
                    Text(
                        "Active Members",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Manage your elite community at the Kinetic Atelier.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                // Controles en fila que se adaptan al ancho disponible
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search Bar — ocupa el espacio restante
                    Surface(
                        modifier = Modifier.weight(1f).height(44.dp),
                        color = if (isDark) Level1Section else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp), tint = OnSurfaceDim)
                            Spacer(Modifier.width(6.dp))
                            androidx.compose.foundation.text.BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(TealPrimary),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) Text("Search...", color = OnSurfaceDim, style = MaterialTheme.typography.bodySmall)
                                    innerTextField()
                                }
                            )
                        }
                    }

                    // Filter Button
                    Box {
                        Surface(
                            modifier = Modifier.clickable { showFilterMenu = true }.height(44.dp),
                            color = if (statusFilter != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else (if (isDark) Level1Section else MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(24.dp),
                            border = if (statusFilter != null) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(statusFilter ?: "Filter", style = MaterialTheme.typography.labelMedium, color = if (statusFilter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false },
                            modifier = Modifier.background(if (isDark) Level1Section else Color.White)
                        ) {
                            DropdownMenuItem(text = { Text("All Members") }, onClick = { onStatusFilterChange(null); showFilterMenu = false })
                            DropdownMenuItem(text = { Text("Active") }, onClick = { onStatusFilterChange("ACTIVE"); showFilterMenu = false })
                            DropdownMenuItem(text = { Text("Inactive") }, onClick = { onStatusFilterChange("INACTIVE"); showFilterMenu = false })
                        }
                    }

                    // Add Button compacto
                    Button(
                        onClick = onAddMember,
                        modifier = Modifier.height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        } else {
            // Layout horizontal original para pantallas anchas (1080p, 1440p)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 24.dp)) {
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
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
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
                                onValueChange = onSearchQueryChange,
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
                            DropdownMenuItem(text = { Text("All Members") }, onClick = { onStatusFilterChange(null); showFilterMenu = false })
                            DropdownMenuItem(text = { Text("Active") }, onClick = { onStatusFilterChange("ACTIVE"); showFilterMenu = false })
                            DropdownMenuItem(text = { Text("Inactive") }, onClick = { onStatusFilterChange("INACTIVE"); showFilterMenu = false })
                        }
                    }

                    KineticButton(
                        onClick = onAddMember,
                        text = "+ Add Member",
                        modifier = Modifier.width(180.dp).height(48.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MembersStatsSection() {
    val isDark = LocalIsDarkMode.current
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
}

@Composable
fun MembersTableSection(
    uiState: MembersListUiState,
    currentSortColumn: String,
    sortAscending: Boolean,
    onToggleSort: (String) -> Unit,
    onEditMember: (Member) -> Unit,
    onRetry: () -> Unit
) {
    val isDark = LocalIsDarkMode.current
    val membersUiModels = (uiState as? MembersListUiState.Success)?.members ?: emptyList()
    val isLoading = uiState is MembersListUiState.Loading
    val errorMessage = (uiState as? MembersListUiState.Error)?.message

    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
        shape = RoundedCornerShape(32.dp),
        color = if (isDark) Level1Section else Color(0xFFF0F2F2).copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            // Table Header
            Row(modifier = Modifier.padding(horizontal = 32.dp, vertical = 20.dp)) {
                TableHeader("MEMBER", Modifier.weight(3f).height(25.dp), sortable = true, isSorted = currentSortColumn == "NAME", ascending = sortAscending, onClick = { onToggleSort("NAME") })
                TableHeader("STATUS", Modifier.weight(1.5f).height(25.dp), sortable = true, isSorted = currentSortColumn == "STATUS", ascending = sortAscending, onClick = { onToggleSort("STATUS") })
                TableHeader("PLAN", Modifier.weight(2f).height(25.dp), sortable = true, isSorted = currentSortColumn == "PLAN", ascending = sortAscending, onClick = { onToggleSort("PLAN") })
                TableHeader("EXPIRES AT", Modifier.weight(2f).height(25.dp))
                TableHeader("JOIN DATE", Modifier.weight(2f).height(25.dp), sortable = true, isSorted = currentSortColumn == "JOIN_DATE", ascending = sortAscending, onClick = { onToggleSort("JOIN_DATE") })
                TableHeader("ACTION", Modifier.width(60.dp))
            }

            when {
                isLoading -> {
                    Column {
                        repeat(3) { MemberSkeleton(isDark) }
                    }
                }
                errorMessage != null -> {
                    ErrorState(errorMessage, onRetry = onRetry)
                }
                membersUiModels.isEmpty() -> {
                    EmptyState(isDark)
                }
                else -> {
                    membersUiModels.forEach { item ->
                        val member = item.member
                        MemberRow(
                            name = member.name,
                            email = member.email,
                            status = member.status.uppercase(),
                            plan = member.currentPlan ?: "Standard Plan",
                            expirationDate = item.expirationDateStr,
                            date = item.joinDateStr,
                            profileImageUrl = member.profileImageUrl,
                            lastCheckin = "Not available",
                            location = "STRENGTH STUDIO",
                            isDark = isDark,
                            onEdit = { onEditMember(member) }
                        )
                    }
                }
            }

            // Pagination
            if (!isLoading && errorMessage == null && membersUiModels.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Showing ${membersUiModels.size} members", style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("‹", fontSize = 20.sp, color = OnSurfaceNeutral)
                        PaginationDot("1", true)
                        Text("›", fontSize = 20.sp, color = OnSurfaceNeutral)
                    }
                }
            }
        }
    }
}

@Composable
fun MembersFooterSection() {
    val isDark = LocalIsDarkMode.current
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
                    Text("Download Quarterly Report", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = if (isDark) Color.White else Color.Black)
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

@Composable
fun MemberRow(
    name: String,
    email: String,
    status: String,
    plan: String,
    expirationDate: String,
    date: String,
    profileImageUrl: String?,
    lastCheckin: String,
    location: String,
    isDark: Boolean,
    onEdit: () -> Unit
) {
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

@Preview
@Composable
fun MembersListPreview() {
    GymTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MembersListContent(
                uiState = MembersListUiState.Success(
                    members = emptyList(),
                    plans = emptyList()
                ),
                searchQuery = "",
                statusFilter = null,
                currentSortColumn = "NAME",
                sortAscending = true,
                onSearchQueryChange = {},
                onStatusFilterChange = {},
                onToggleSort = {},
                onAddMember = {},
                onEditMember = {},
                onRetry = {}
            )
        }
    }
}
