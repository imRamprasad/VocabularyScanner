package com.hostel.vocabularyscanner.data.model

import com.google.gson.annotations.SerializedName

data class DictionaryResponseItem(
    val word: String? = null,
    val phonetic: String? = null,
    val phonetics: List<PhoneticDto>? = null,
    val meanings: List<MeaningDto>? = null
)

data class PhoneticDto(
    val text: String? = null,
    val audio: String? = null
)

data class MeaningDto(
    val partOfSpeech: String? = null,
    val definitions: List<DefinitionDto>? = null
)

data class DefinitionDto(
    val definition: String? = null,
    val example: String? = null
)

data class WordDetail(
    val word: String,
    val phonetic: String? = null,
    val partOfSpeech: String? = null,
    val meaning: String,
    val example: String? = null,
    val audioUrl: String? = null
)
