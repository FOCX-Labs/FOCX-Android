package com.focx.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focx.presentation.ui.theme.Spacing
import com.focx.presentation.viewmodel.SellerRegistrationViewModel
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerRegistrationScreen(
    activityResultSender: ActivityResultSender,
    onRegistrationSuccess: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SellerRegistrationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle error messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    // Handle registration success
    LaunchedEffect(uiState.registrationSuccess) {
        if (uiState.registrationSuccess) {
            onRegistrationSuccess()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                color = Color(0xFF1A1B2E)
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(Spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Title
        Text(
            text = "Become a Seller",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Security Deposit Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Security Deposit Required",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "1,000 USDC",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 36.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "This security deposit protects buyers and will\nbe refunded when you stop selling",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Store Name Input
        Column {
            Text(
                text = "Store/Brand Name",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = uiState.storeName,
                onValueChange = viewModel::updateStoreName,
                placeholder = {
                    Text(
                        text = "Enter your store name",
                        color = Color.White.copy(alpha = 0.5f)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF4A90E2),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    cursorColor = Color(0xFF4A90E2)
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Store Description Input
        Column {
            Text(
                text = "Store Description",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = uiState.storeDescription,
                onValueChange = viewModel::updateStoreDescription,
                placeholder = {
                    Text(
                        text = "Describe your business",
                        color = Color.White.copy(alpha = 0.5f)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF4A90E2),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    cursorColor = Color(0xFF4A90E2)
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 4
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Submit Button
        Button(
            onClick = { viewModel.registerAsSeller(activityResultSender) },
            enabled = !uiState.isRegistrationInProgress && !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(top = 10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4A90E2)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (uiState.isRegistrationInProgress) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.height(24.dp)
                )
            } else {
                Text(
                    text = "Deposit & Become Seller",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Terms Text
        Text(
            text = "By becoming a seller, you agree to our terms of\nservice and seller policies.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Snackbar for error messages
    Box(modifier = Modifier.fillMaxSize()) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}