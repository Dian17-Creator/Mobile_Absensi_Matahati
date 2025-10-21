package id.my.matahati.absensi.data

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "https://absensi.matahati.my.id/laravel/public/" // atau tambahkan /api/ kalau API kamu langsung di bawah itu

    // 🔹 Client dengan interceptor logging mentah
    private val client by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request()
                val res = chain.proceed(req)
                val bodyString = res.body?.string()

                // Log respons mentah
                Log.e("RAW_RESPONSE", "URL=${req.url} CODE=${res.code} BODY=${bodyString?.take(500)}")

                // Rebuild body agar Retrofit tetap bisa membacanya
                val newBody = bodyString?.toResponseBody(res.body?.contentType())
                res.newBuilder().body(newBody).build()
            }
            .build()
    }

    // 🔹 Retrofit + client di atas
    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client) // pakai client yang ada interceptor-nya
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
