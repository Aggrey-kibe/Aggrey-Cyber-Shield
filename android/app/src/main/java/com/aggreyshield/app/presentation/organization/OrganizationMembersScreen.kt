package com.aggreyshield.app.presentation.organization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aggreyshield.app.core.ui.components.BackTopBar
import com.aggreyshield.app.core.ui.components.StateContent
import com.aggreyshield.app.domain.model.OrganizationMember

@Composable
fun OrganizationMembersScreen(viewModel: OrganizationMembersViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { BackTopBar(title = "Members", onBack = onBack) }) { padding ->
        StateContent(
            state = state,
            onRetry = viewModel::onRetry,
            isEmpty = { it.isEmpty() },
            emptyTitle = "No members",
            emptyMessage = "This organization has no other members yet.",
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) { members ->
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(members, key = { it.userId }) { member -> MemberRow(member) }
            }
        }
    }
}

@Composable
private fun MemberRow(member: OrganizationMember) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = member.fullName ?: member.email ?: "Unknown member", style = MaterialTheme.typography.titleSmall)
            if (member.email != null && member.fullName != null) {
                Text(text = member.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = member.role.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
