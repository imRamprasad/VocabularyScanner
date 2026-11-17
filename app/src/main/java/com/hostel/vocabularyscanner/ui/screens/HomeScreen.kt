package com.hostel.vocabularyscanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    userEmail: String? = null,
    onScanClick: () -> Unit,
    onSavedWordsClick: () -> Unit,
    onSignOutClick: (() -> Unit)? = null
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Vocabulary Scanner",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (!userEmail.isNullOrBlank()) {
                Text(
                    text = "Logged in as $userEmail",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = "Learn new words from everyday reading.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onScanClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Scan Image")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onSavedWordsClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("My Saved Words")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Your Vocabulary Journey",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Scan printed text, discover new meanings, " +
                                "and build your personal vocabulary."
                    )
                }
            }

            if (onSignOutClick != null) {
                Spacer(modifier = Modifier.height(24.dp))

                TextButton(
                    onClick = onSignOutClick
                ) {
                    Text(
                        text = "Sign Out",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
