package com.hostel.vocabularyscanner.ui.screens

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hostel.vocabularyscanner.data.local.WordEntity
import com.hostel.vocabularyscanner.data.repository.WordRepository
import kotlinx.coroutines.launch

enum class WordFilter {
    ALL, TO_LEARN, LEARNED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedWordsScreen(
    userId: String,
    wordRepository: WordRepository,
    onBackClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedFilter by remember { mutableStateOf(WordFilter.ALL) }

    val savedWordsFlow = remember(selectedFilter, userId) {
        when (selectedFilter) {
            WordFilter.ALL -> wordRepository.getAllWords(userId)
            WordFilter.TO_LEARN -> wordRepository.getNotLearnedWords(userId)
            WordFilter.LEARNED -> wordRepository.getLearnedWords(userId)
        }
    }

    val savedWords by savedWordsFlow.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Vocabulary") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Filter Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WordFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text(
                                when (filter) {
                                    WordFilter.ALL -> "All"
                                    WordFilter.TO_LEARN -> "To Learn"
                                    WordFilter.LEARNED -> "Learned"
                                }
                            )
                        }
                    )
                }
            }

            if (savedWords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (selectedFilter) {
                            WordFilter.ALL -> "No saved words yet. Scan an image to add words!"
                            WordFilter.TO_LEARN -> "No words to learn! All saved words are marked as learned."
                            WordFilter.LEARNED -> "No learned words yet. Keep studying!"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(savedWords, key = { it.id }) { wordEntity ->
                        SavedWordItemCard(
                            wordEntity = wordEntity,
                            onToggleLearned = { learned ->
                                scope.launch {
                                    wordRepository.updateLearningStatus(wordEntity.id, userId, learned)
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    wordRepository.deleteWord(wordEntity)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SavedWordItemCard(
    wordEntity: WordEntity,
    onToggleLearned: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isPlayingAudio by remember { mutableStateOf(false) }

    val containerColor by animateColorAsState(
        if (wordEntity.isLearned) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "cardColor"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = wordEntity.word,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    wordEntity.partOfSpeech?.let { pos ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(pos, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Audio pronunciation button
                    if (!wordEntity.audioUrl.isNullOrBlank()) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        isPlayingAudio = true
                                        MediaPlayer().apply {
                                            setAudioAttributes(
                                                AudioAttributes.Builder()
                                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                                    .build()
                                            )
                                            setDataSource(wordEntity.audioUrl)
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
                                    } catch (_: Exception) {
                                        isPlayingAudio = false
                                    }
                                }
                            },
                            enabled = !isPlayingAudio
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Play Pronunciation",
                                tint = if (isPlayingAudio) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Delete button
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Word",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            wordEntity.phonetic?.let { phonetic ->
                Text(
                    text = phonetic,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = wordEntity.meaning,
                style = MaterialTheme.typography.bodyMedium
            )

            wordEntity.example?.let { example ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"$example\"",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onToggleLearned(!wordEntity.isLearned) }) {
                    Icon(
                        imageVector = if (wordEntity.isLearned) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Toggle Learned Status",
                        tint = if (wordEntity.isLearned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (wordEntity.isLearned) "Learned" else "Mark as Learned",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (wordEntity.isLearned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
