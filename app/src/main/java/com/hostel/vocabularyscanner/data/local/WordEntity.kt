
package com.hostel.vocabularyscanner.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_words")
data class WordEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Firebase user ID
    val userId: String,

    // Dictionary information
    val word: String,
    val phonetic: String? = null,
    val partOfSpeech: String? = null,
    val meaning: String,
    val example: String? = null,
    val audioUrl: String? = null,

    // Learning status
    val isLearned: Boolean = false
)