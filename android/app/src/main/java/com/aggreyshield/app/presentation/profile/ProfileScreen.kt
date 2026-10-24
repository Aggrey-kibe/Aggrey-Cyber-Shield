package com.aggreyshield.app.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aggreyshield.app.core.ui.components.AggreyTextField
import com.aggreyshield.app.core.ui.components.PrimaryButton
import com.aggreyshield.app.core.ui.components.StateContent
import com.aggreyshield.app.core.ui.components.TextLinkButton
import com.aggreyshield.app.domain.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Profile") }) }) { padding ->
        StateContent(
            state = state.profile,
            onRetry = viewModel::onRetry,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) { profile ->
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item { ProfileHeaderCard(profile, state, viewModel) }
                item {
                    MenuRow(icon = Icons.Filled.Security, label = "Account security", onClick = viewModel::onAccountSecurityClicked)
                }
                if (profile.organization != null) {
                    item {
                        MenuRow(icon = Icons.Filled.Business, label = "Organization", onClick = viewModel::onOrganizationClicked)
                    }
                }
                item {
                    MenuRow(icon = Icons.Filled.School, label = "Training center", onClick = viewModel::onTrainingClicked)
                }
                item {
                    MenuRow(icon = Icons.Filled.Settings, label = "Settings", onClick = viewModel::onSettingsClicked)
                }
                item {
                    MenuRow(icon = Icons.Filled.Info, label = "About AGGREY SHIELD", onClick = viewModel::onAboutClicked)
                }
                item {
                    MenuRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        label = "Log out",
                        onClick = viewModel::onLogoutClicked,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileHeaderCard(profile: User, state: ProfileUiState, viewModel: ProfileViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (!state.isEditing) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(text = profile.displayName, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = profile.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = viewModel::onEditClicked) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit name")
                    }
                }
                Text(
                    text = profile.role.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (!profile.emailVerified) {
                    Text(
                        text = "Email not verified",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            } else {
                AggreyTextField(
                    value = state.editedName,
                    onValueChange = viewModel::onNameChanged,
                    label = "Full name",
                    errorText = state.nameError,
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    PrimaryButton(text = "Save", onClick = viewModel::onSaveName, isLoading = state.isSaving, modifier = Modifier.weight(1f))
                    TextLinkButton(text = "Cancel", onClick = viewModel::onCancelEdit, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun MenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(14.dp),
        ) {
            Icon(icon, contentDescription = null, tint = tint)
            Text(text = label, style = MaterialTheme.typography.bodyLarge, color = tint, modifier = Modifier.padding(start = 14.dp).weight(1f))
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
