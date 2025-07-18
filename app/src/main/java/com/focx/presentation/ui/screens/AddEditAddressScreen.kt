package com.focx.presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focx.domain.entity.UserAddress
import com.focx.presentation.viewmodel.ProfileViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAddressScreen(
    addressToEdit: UserAddress? = null,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isEditing = addressToEdit != null

    var label by remember { mutableStateOf(addressToEdit?.label ?: "") }
    var recipientName by remember { mutableStateOf(addressToEdit?.recipientName ?: "") }
    var addressLine1 by remember { mutableStateOf(addressToEdit?.addressLine1 ?: "") }
    var addressLine2 by remember { mutableStateOf(addressToEdit?.addressLine2 ?: "") }
    var city by remember { mutableStateOf(addressToEdit?.city ?: "") }
    var state by remember { mutableStateOf(addressToEdit?.state ?: "") }
    var postalCode by remember { mutableStateOf(addressToEdit?.postalCode ?: "") }
    var country by remember { mutableStateOf(addressToEdit?.country ?: "United States") }
    var phoneNumber by remember { mutableStateOf(addressToEdit?.phoneNumber ?: "") }
    var isDefault by remember { mutableStateOf(addressToEdit?.isDefault ?: false) }

    var isLoading by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Form validation
    val isFormValid = label.isNotBlank() &&
            recipientName.isNotBlank() &&
            addressLine1.isNotBlank() &&
            city.isNotBlank() &&
            state.isNotBlank() &&
            postalCode.isNotBlank() &&
            country.isNotBlank() &&
            phoneNumber.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Edit Address" else "Add Address",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(
                            onClick = { showDeleteDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Address",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Address Label
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Address Label") },
                placeholder = { Text("e.g., Home, Office, etc.") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Label,
                        contentDescription = null
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Recipient Name
            OutlinedTextField(
                value = recipientName,
                onValueChange = { recipientName = it },
                label = { Text("Recipient Name") },
                placeholder = { Text("Full name of recipient") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Address Line 1
            OutlinedTextField(
                value = addressLine1,
                onValueChange = { addressLine1 = it },
                label = { Text("Address Line 1") },
                placeholder = { Text("Street address, P.O. box, etc.") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Address Line 2
            OutlinedTextField(
                value = addressLine2,
                onValueChange = { addressLine2 = it },
                label = { Text("Address Line 2 (Optional)") },
                placeholder = { Text("Apartment, suite, unit, etc.") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // City and State Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City") },
                    placeholder = { Text("City") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = state,
                    onValueChange = { state = it },
                    label = { Text("State") },
                    placeholder = { Text("State") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // Postal Code and Country Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = postalCode,
                    onValueChange = { postalCode = it },
                    label = { Text("Postal Code") },
                    placeholder = { Text("ZIP/Postal Code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text("Country") },
                    placeholder = { Text("Country") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // Phone Number
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number") },
                placeholder = { Text("+1 (555) 123-4567") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Default Address Switch
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Set as Default Address",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Use this address as your default shipping address",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    isLoading = true
                    val address = if (isEditing) {
                        addressToEdit!!.copy(
                            label = label,
                            recipientName = recipientName,
                            addressLine1 = addressLine1,
                            addressLine2 = addressLine2.takeIf { it.isNotBlank() },
                            city = city,
                            state = state,
                            postalCode = postalCode,
                            country = country,
                            phoneNumber = phoneNumber,
                            isDefault = isDefault
                        )
                    } else {
                        UserAddress(
                            id = "addr_${UUID.randomUUID()}",
                            label = label,
                            recipientName = recipientName,
                            addressLine1 = addressLine1,
                            addressLine2 = addressLine2.takeIf { it.isNotBlank() },
                            city = city,
                            state = state,
                            postalCode = postalCode,
                            country = country,
                            phoneNumber = phoneNumber,
                            isDefault = isDefault
                        )
                    }

                    // TODO: Call viewModel to save address
                    // For now, just simulate success
                    isLoading = false
                    onSaveSuccess()
                },
                enabled = isFormValid && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isEditing) "Update Address" else "Save Address")
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text("Delete Address")
            },
            text = {
                Text("Are you sure you want to delete this address? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        isLoading = true
                        // TODO: Call viewModel to delete address
                        // For now, just simulate success
                        isLoading = false
                        onNavigateBack()
                    }
                ) {
                    Text(
                        "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Show error message if any
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // TODO: Show snackbar with error message
        }
    }
}