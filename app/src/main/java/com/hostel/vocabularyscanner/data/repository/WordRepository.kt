package com.hostel.vocabularyscanner.data.repository

import com.hostel.vocabularyscanner.data.local.WordDao
import com.hostel.vocabularyscanner.data.local.WordEntity
import kotlinx.coroutines.flow.Flow

class WordRepository(
    private val wordDao: WordDao
) {

    // Save a vocabulary word
    suspend fun insertWord(word: WordEntity) {
        wordDao.insertWord(word)
    }

    // Check if word is already saved
    suspend fun isWordSaved(userId: String, word: String): Boolean {
        return wordDao.isWordSaved(userId, word)
    }

    // Get all saved words
    fun getAllWords(userId: String): Flow<List<WordEntity>> {
        return wordDao.getAllWords(userId)
    }

    // Get learned words
    fun getLearnedWords(userId: String): Flow<List<WordEntity>> {
        return wordDao.getLearnedWords(userId)
    }

    // Get not learned words
    fun getNotLearnedWords(userId: String): Flow<List<WordEntity>> {
        return wordDao.getNotLearnedWords(userId)
    }

    // Update word details
    suspend fun updateWord(word: WordEntity) {
        wordDao.updateWord(word)
    }

    // Change learning status
    suspend fun updateLearningStatus(
        wordId: Int,
        userId: String,
        learned: Boolean
    ) {
        wordDao.updateLearningStatus(
            wordId,
            userId,
            learned
        )
    }

    // Delete a word
    suspend fun deleteWord(word: WordEntity) {
        wordDao.deleteWord(word)
    }
}
