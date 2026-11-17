package com.hostel.vocabularyscanner.data.remote

import com.hostel.vocabularyscanner.data.model.DictionaryResponseItem
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface DictionaryApiService {

    @GET("api/v2/entries/en/{word}")
    suspend fun getWordDefinition(
        @Path("word") word: String
    ): Response<List<DictionaryResponseItem>>
}
