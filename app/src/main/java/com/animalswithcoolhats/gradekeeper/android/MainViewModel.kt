package com.animalswithcoolhats.gradekeeper.android

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.animalswithcoolhats.gradekeeper.android.api.DefaultApi
import com.animalswithcoolhats.gradekeeper.android.api.MeResponseModel
import com.animalswithcoolhats.gradekeeper.android.api.StudyBlock
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.SignInClient
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class MainViewModel: ViewModel() {
    var userToken by mutableStateOf<String?>(null)
    val userIsAuthenticated get() = userToken != null
    var studyBlocks by mutableStateOf<List<StudyBlock>?>(null)

    val tag = MainViewModel::class.simpleName

    var oneTapClient: SignInClient? = null
    var signUpRequest: BeginSignInRequest? = null
    var signInRequest: BeginSignInRequest? = null

    fun redownload() {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val newRequest: Request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $userToken")
                .build()
            chain.proceed(newRequest)
        }.build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://app.gradekeeper.xyz/api/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()

        val api = retrofit.create(DefaultApi::class.java);

        val call = api.getMe()
        call!!.enqueue(object: Callback<MeResponseModel?> {
            override fun onResponse(
                call: Call<MeResponseModel?>,
                response: Response<MeResponseModel?>
            ) {
                if (response.isSuccessful) {
                    Log.i(tag, "Successfully downloaded ${response.body()?.studyBlocks?.count()} study blocks")
                    studyBlocks = response.body()?.studyBlocks!!
                }
                else {
                    Log.e(tag, "Failed to download: ${response.code()} ${response.message()}")
                }
            }

            override fun onFailure(call: Call<MeResponseModel?>, t: Throwable) {
                Log.e(tag, "Failed to request: ${t.message.toString()}")
            }
        })
    }

    fun acceptToken(token: String, activity: Activity) {
        userToken = token

        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE) ?: return
        with (sharedPref.edit()) {
            putString("TOKEN", userToken)
            apply()
        }

        redownload()
    }

    fun logout() {
        userToken = null
        studyBlocks = null

    }
}