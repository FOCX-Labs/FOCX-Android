package com.focx.presentation.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.focx.presentation.ui.theme.FocxTheme
import com.focx.presentation.ui.theme.Spacing

data class ProductFormData(
    val name: String = "",
    val description: String = "",
    val price: String = "",
    val category: String = "",
    val stock: String = "",
    val images: List<String> = emptyList(),
    val shippingOptions: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    productId: String? = null, // null for add, non-null for edit
    onBackClick: () -> Unit,
    onSaveClick: (ProductFormData, com.solana.mobilewalletadapter.clientlib.ActivityResultSender) -> Unit,
    activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender
) {
    val isEditMode = productId != null
    var formData by remember {
        mutableStateOf(
            if (isEditMode) {
                // In real app, load existing product data
                ProductFormData(
                    name = "iPhone 15 Pro Max  Apple",
                    description = "Latest iPhone with titanium design and advanced camera system",
                    price = "1199.99",
                    category = "Electronics",
                    stock = "25",
                    images = listOf("image1.jpg", "image2.jpg"),
                    shippingOptions = listOf("Standard", "Express")
                )
            } else {
                ProductFormData()
            }
        )
    }

    var showCategoryDropdown by remember { mutableStateOf(false) }
    var newShippingOption by remember { mutableStateOf("") }
    var showAddShippingDialog by remember { mutableStateOf(false) }

    val categories = listOf(
        "Electronics", "Clothing", "Home & Garden", "Sports",
        "Books", "Toys", "Beauty", "Automotive", "Other"
    )

    val isFormValid = formData.name.isNotBlank() &&
            formData.description.isNotBlank() &&
            formData.price.isNotBlank() &&
            formData.category.isNotBlank() &&
            formData.stock.isNotBlank()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isEditMode) "Edit Product" else "Add Product")
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onSaveClick(formData, activityResultSender) },
                        enabled = isFormValid
                    ) {
                        Text(if (isEditMode) "Update" else "Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            item {
                Spacer(modifier = Modifier.height(Spacing.small))
            }

            // Product Images Section
            item {
                Text(
                    text = "Product Images",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(Spacing.small))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    items(formData.images) { image ->
                        ProductImageCard(
                            imageUrl = image,
                            onRemove = {
                                formData = formData.copy(
                                    images = formData.images - image
                                )
                            }
                        )
                    }

                    item {
                        AddImageCard(
                            onClick = {
                                // In real app, open image picker
                                formData = formData.copy(
                                    images = formData.images + "new_image_${formData.images.size + 1}.jpg"
                                )
                            }
                        )
                    }
                }
            }

            // Basic Information
            item {
                Text(
                    text = "Basic Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                OutlinedTextField(
                    value = formData.name,
                    onValueChange = { formData = formData.copy(name = it) },
                    label = { Text("Product Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = formData.description,
                    onValueChange = { formData = formData.copy(description = it) },
                    label = { Text("Description *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    OutlinedTextField(
                        value = formData.price,
                        onValueChange = { formData = formData.copy(price = it) },
                        label = { Text("Price ($) *") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = formData.stock,
                        onValueChange = { formData = formData.copy(stock = it) },
                        label = { Text("Stock *") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = showCategoryDropdown,
                    onExpandedChange = { showCategoryDropdown = it }
                ) {
                    OutlinedTextField(
                        value = formData.category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = showCategoryDropdown
                            )
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    formData = formData.copy(category = category)
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // Shipping Options
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Shipping Options",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    TextButton(
                        onClick = { showAddShippingDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            }

            item {
                if (formData.shippingOptions.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.large),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No shipping options added",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        formData.shippingOptions.forEach { option ->
                            ShippingOptionCard(
                                option = option,
                                onRemove = {
                                    formData = formData.copy(
                                        shippingOptions = formData.shippingOptions - option
                                    )
                                }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(Spacing.medium))
            }
        }
    }

    // Add Shipping Option Dialog
    if (showAddShippingDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddShippingDialog = false
                newShippingOption = ""
            },
            title = { Text("Add Shipping Option") },
            text = {
                OutlinedTextField(
                    value = newShippingOption,
                    onValueChange = { newShippingOption = it },
                    label = { Text("Shipping Option") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newShippingOption.isNotBlank()) {
                            formData = formData.copy(
                                shippingOptions = formData.shippingOptions + newShippingOption
                            )
                        }
                        showAddShippingDialog = false
                        newShippingOption = ""
                    },
                    enabled = newShippingOption.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddShippingDialog = false
                        newShippingOption = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProductImageCard(
    imageUrl: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(80.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "IMG",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
    }
}

@Composable
fun AddImageCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .size(80.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Add Image",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Add",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ShippingOptionCard(
    option: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = option,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// Note: Preview disabled due to ActivityResultSender dependency
// @Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
// @Composable
// fun AddEditProductScreenPreview() {
//     FocxTheme {
//         AddEditProductScreen(
//             productId = null,
//             onBackClick = { },
//             onSaveClick = { _, _ -> },
//             activityResultSender = // Cannot mock in preview
//         )
//     }
// }

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
fun ProductImageCardPreview() {
    FocxTheme {
        ProductImageCard(
            imageUrl = "sample_image.jpg",
            onRemove = { }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
fun AddImageCardPreview() {
    FocxTheme {
        AddImageCard(
            onClick = { }
        )
    }
}