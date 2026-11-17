package com.hostel.vocabularyscanner.data.repository

import com.hostel.vocabularyscanner.data.model.WordDetail
import com.hostel.vocabularyscanner.data.remote.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DictionaryRepository {

    suspend fun fetchWordDetail(word: String): Result<WordDetail> = withContext(Dispatchers.IO) {
        try {
            val cleanWord = word.trim().lowercase()
            val response = RetrofitInstance.api.getWordDefinition(cleanWord)

            if (response.isSuccessful) {
                val items = response.body()
                if (!items.isNullOrEmpty()) {
                    val firstItem = items[0]

                    // Find phonetic text
                    val phoneticText = firstItem.phonetic
                        ?: firstItem.phonetics?.firstOrNull { !it.text.isNullOrBlank() }?.text

                    // Find audio URL
                    val audioUrl = firstItem.phonetics?.firstOrNull { !it.audio.isNullOrBlank() }?.audio

                    // Find meaning, part of speech & example sentence
                    val firstMeaning = firstItem.meanings?.firstOrNull()
                    val partOfSpeech = firstMeaning?.partOfSpeech
                    val firstDef = firstMeaning?.definitions?.firstOrNull()

                    val meaning = firstDef?.definition ?: "No definition found."
                    val example = firstDef?.example

                    val detail = WordDetail(
                        word = cleanWord.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                        phonetic = phoneticText,
                        partOfSpeech = partOfSpeech,
                        meaning = meaning,
                        example = example,
                        audioUrl = audioUrl
                    )
                    Result.success(detail)
                } else {
                    Result.failure(Exception("No definition found for '$word'."))
                }
            } else if (response.code() == 404) {
                Result.failure(Exception("Word '$word' not found in dictionary."))
            } else {
                Result.failure(Exception("Error ${response.code()}: Unable to fetch definition."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
