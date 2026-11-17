package com.hostel.vocabularyscanner.ui.screens

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hostel.vocabularyscanner.data.local.WordEntity
import com.hostel.vocabularyscanner.data.model.WordDetail
import com.hostel.vocabularyscanner.data.repository.DictionaryRepository
import com.hostel.vocabularyscanner.data.repository.WordRepository
import kotlinx.coroutines.launch

@Composable
fun WordDetailsDialog(
    word: String,
    userId: String,
    wordRepository: WordRepository,
    dictionaryRepository: DictionaryRepository = remember { DictionaryRepository() },
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var wordDetail by remember { mutableStateOf<WordDetail?>(null) }
    var isSaved by remember { mutableStateOf(false) }
    var isPlayingAudio by remember { mutableStateOf(false) }

    // Fetch definition & check if saved on open
    LaunchedEffect(word) {
        isLoading = true
        errorMessage = null
        isSaved = wordRepository.isWordSaved(userId, word)

        val result = dictionaryRepository.fetchWordDetail(word)
        result.onSuccess { detail ->
            wordDetail = detail
            isLoading = false
        }.onFailure { exception ->
            errorMessage = exception.message ?: "Failed to load definition"
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "Error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                } else wordDetail?.let { detail ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        // Phonetic & Audio button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            detail.phonetic?.let { phonetic ->
                                Text(
                                    text = phonetic,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            if (!detail.audioUrl.isNullOrBlank()) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                isPlayingAudio = true
                                                val mediaPlayer = MediaPlayer().apply {
                                                    setAudioAttributes(
                                                        AudioAttributes.Builder()
                                                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                                            .setUsage(AudioAttributes.USAGE_MEDIA)
                                                            .build()
                                                    )
                                                    setDataSource(detail.audioUrl)
                                                    prepareAsync()
                                                    setOnPreparedListener { start() }
                                                    setOnCompletionListener {
                                                        isPlayingAudio = false
                                                        release()
                                                    }
                                                    setOnErrorListener { mp, _, _ ->
                                                        isPlayingAudio = false
                                                        mp.release()
                                                        true
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                isPlayingAudio = false
                                            }
                                        }
                                    },
                                    enabled = !isPlayingAudio
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Pronounce Word",
                                        tint = if (isPlayingAudio) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Part of speech badge
                        detail.partOfSpeech?.let { pos ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text(pos) }
                            )
                        }

                        // Meaning / Definition
                        Text(
                            text = "Definition",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = detail.meaning,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // Example Sentence
                        detail.example?.let { example ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Example:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "\"$example\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontStyle = FontStyle.Italic
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            wordDetail?.let { detail ->
                Button(
                    onClick = {
                        scope.launch {
                            if (!isSaved) {
                                wordRepository.insertWord(
                                    WordEntity(
                                        userId = userId,
                                        word = detail.word,
                                        phonetic = detail.phonetic,
                                        partOfSpeech = detail.partOfSpeech,
                                        meaning = detail.meaning,
                                        example = detail.example,
                                        audioUrl = detail.audioUrl,
                                        isLearned = false
                                    )
                                )
                                isSaved = true
                            }
                        }
                    },
                    enabled = !isSaved && !isLoading && errorMessage == null
                ) {
                    Text(if (isSaved) "Saved to Vocabulary" else "Save Word")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
