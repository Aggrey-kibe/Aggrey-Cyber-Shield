package com.aggreyshield.app.presentation.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.aggreyshield.app.BuildConfig
import com.aggreyshield.app.core.ui.components.BackTopBar

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    Scaffold(topBar = { BackTopBar(title = "About", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )
            Text(text = "AGGREY SHIELD", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 12.dp))
            Text(
                text = "AGGREY CYBER SHIELD \u2014 version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Card(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AGGREY SHIELD is a cybersecurity, fraud-detection, and digital-safety platform for individuals, schools, NGOs, startups, and organizations in Kenya and beyond.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
                    Text(text = "Developer", style = MaterialTheme.typography.titleSmall)
                    Text(text = "Aggrey Kibe Kwamboka", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                    Text(text = "Kenya", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    LinkRow(label = "Email", value = "aggreykwamboka62@protonmail.com") {
                        uriHandler.openUri("mailto:aggreykwamboka62@protonmail.com")
                    }
                    LinkRow(label = "GitHub", value = "github.com/Aggrey-kibe") {
                        uriHandler.openUri("https://github.com/Aggrey-kibe")
                    }
                    LinkRow(label = "Website", value = "aggreykibe.blogspot.com") {
                        uriHandler.openUri("https://aggreykibe.blogspot.com/")
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkRow(label: String, value: String, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clickable(onClick = onClick)) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}
