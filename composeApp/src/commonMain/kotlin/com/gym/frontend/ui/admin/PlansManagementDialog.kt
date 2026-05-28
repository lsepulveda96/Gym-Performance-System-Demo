package com.gym.frontend.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gym.frontend.ui.shared.*
import com.gym.frontend.ui.theme.*
import com.gym.shared.domain.GymPlan
import kotlinx.coroutines.launch

@Composable
fun PlansManagementDialog(onDismiss: () -> Unit) {
    val isDark = LocalIsDarkMode.current
    val scope = rememberCoroutineScope()
    val repository = remember { AdminRepository(AdminService()) }
    
    var plans by remember { mutableStateOf<List<GymPlan>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var editingPlan by remember { mutableStateOf<GymPlan?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    fun refreshPlans() {
        isLoading = true
        scope.launch {
            repository.getPlans().onSuccess {
                plans = it
                isLoading = false
            }.onFailure {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshPlans()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.5f).fillMaxHeight(0.85f),
            shape = RoundedCornerShape(32.dp),
            color = if (isDark) MaterialTheme.colorScheme.surface else Color.White
        ) {
            Column(modifier = Modifier.padding(32.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Membership Plans", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Configure prices and benefits for your members", style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KineticButton(
                            onClick = { 
                                editingPlan = null
                                showEditDialog = true
                            },
                            text = "Add New Plan",
                            modifier = Modifier.height(40.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TealPrimary)
                    }
                } else {
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        plans.forEach { plan ->
                            PlanItem(plan, isDark) {
                                editingPlan = plan
                                showEditDialog = true
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        PlanEditDialog(
            plan = editingPlan,
            onDismiss = { showEditDialog = false },
            onSave = { updatedPlan ->
                scope.launch {
                    val result = if (editingPlan == null) {
                        repository.createPlan(updatedPlan)
                    } else {
                        repository.updatePlan(editingPlan!!.id, updatedPlan)
                    }
                    result.onSuccess {
                        showEditDialog = false
                        refreshPlans()
                    }
                }
            }
        )
    }
}

@Composable
fun PlanItem(plan: GymPlan, isDark: Boolean, onEdit: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Level1Section else Color(0xFFF5F7F7),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color.White.copy(0.1f) else Color.Transparent)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(TealPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text("${plan.name.first()}", fontWeight = FontWeight.Bold, color = TealPrimary)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(plan.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(plan.description ?: "No description", style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim)
                Text("${plan.durationDays} days", style = MaterialTheme.typography.labelSmall, color = TealPrimary)
            }
            Text("$${plan.price.toInt()}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(24.dp))
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = OnSurfaceDim)
            }
        }
    }
}

@Composable
fun PlanEditDialog(plan: GymPlan?, onDismiss: () -> Unit, onSave: (GymPlan) -> Unit) {
    var name by remember { mutableStateOf(plan?.name ?: "") }
    var price by remember { mutableStateOf(plan?.price?.toString() ?: "") }
    var duration by remember { mutableStateOf(plan?.durationDays?.toString() ?: "30") }
    var description by remember { mutableStateOf(plan?.description ?: "") }
    val isDark = LocalIsDarkMode.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.35f),
            shape = RoundedCornerShape(24.dp),
            color = if (isDark) MaterialTheme.colorScheme.surface else Color.White
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = if (plan == null) "New Plan" else "Edit Plan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                ModernTextField(value = name, onValueChange = { name = it }, placeholder = "Plan Name (e.g. Platinum)")
                ModernTextField(value = price, onValueChange = { price = it }, placeholder = "Price (e.dp. 35000)")
                ModernTextField(value = duration, onValueChange = { duration = it }, placeholder = "Duration in Days (e.g. 30)")
                ModernTextField(value = description, onValueChange = { description = it }, placeholder = "Description (e.g. Unlimited Access)")
                
                Spacer(Modifier.height(8.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    KineticButton(
                        onClick = {
                            val pValue = price.toDoubleOrNull() ?: 0.0
                            val dValue = duration.toIntOrNull() ?: 30
                            if (name.isNotEmpty() && pValue > 0) {
                                onSave(GymPlan(plan?.id ?: "", name, pValue, dValue, description))
                            }
                        },
                        text = "Save Plan",
                        modifier = Modifier.weight(1f).height(48.dp)
                    )
                }
            }
        }
    }
}
