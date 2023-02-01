package com.animalswithcoolhats.gradekeeper.android.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers

interface DefaultApi {
    @Headers("Accept: application/json")
    @GET("users/me")
    abstract fun getMe(): Call<MeResponseModel?>?
}