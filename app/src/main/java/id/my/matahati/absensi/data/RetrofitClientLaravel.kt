package id.my.matahati.absensi.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClientLaravel {

    private const val BASE_URL =
        "https://absensi.matahati.my.id/laravel/public/"

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL) // ⚠️ wajib slash di akhir
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
