package id.my.matahati.absensi.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object ApiClient {
    private const val BASE_URL = "https://absensi.matahati.my.id/" // ganti dengan domain kamu

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}