package com.hostel.vocabularyscanner.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {

    // Save a new vocabulary word
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: WordEntity)

    // Check if word is already saved for this user
    @Query("SELECT EXISTS(SELECT 1 FROM saved_words WHERE userId = :userId AND LOWER(word) = LOWER(:word) LIMIT 1)")
    suspend fun isWordSaved(userId: String, word: String): Boolean

    // Retrieve all words belonging to a user
    @Query("SELECT * FROM saved_words WHERE userId = :userId ORDER BY word ASC")
    fun getAllWords(userId: String): Flow<List<WordEntity>>

    // Retrieve learned words
    @Query("SELECT * FROM saved_words WHERE userId = :userId AND isLearned = 1 ORDER BY word ASC")
    fun getLearnedWords(userId: String): Flow<List<WordEntity>>

    // Retrieve not learned words
    @Query("SELECT * FROM saved_words WHERE userId = :userId AND isLearned = 0 ORDER BY word ASC")
    fun getNotLearnedWords(userId: String): Flow<List<WordEntity>>

    // Update an existing word
    @Update
    suspend fun updateWord(word: WordEntity)

    // Change learning status
    @Query("UPDATE saved_words SET isLearned = :learned WHERE id = :wordId AND userId = :userId")
    suspend fun updateLearningStatus(
        wordId: Int,
        userId: String,
        learned: Boolean
    )

    // Delete a saved word
    @Delete
    suspend fun deleteWord(word: WordEntity)
}
