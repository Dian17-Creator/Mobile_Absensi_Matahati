package id.my.matahati.absensi.data

import android.util.Log
import androidx.camera.core.impl.utils.ContextUtil.getApplicationContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.my.matahati.absensi.SessionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class CompanyViewModel : ViewModel() {

    private val _updateSuccess = MutableSharedFlow<Boolean>()
    val updateSuccess = _updateSuccess.asSharedFlow()

    var companyName by mutableStateOf("")
        private set

    var companyEmail by mutableStateOf("")
        private set

    var checking by mutableStateOf(false)
        private set

    var saving by mutableStateOf(false)
        private set

    var nameExists by mutableStateOf(false)
        private set

    var domainExists by mutableStateOf(false)
        private set

    var message by mutableStateOf("")
        private set

    fun onCompanyNameChange(value: String) {
        companyName = value
    }

    fun onCompanyEmailChange(value: String) {
        companyEmail = value
    }

    fun checkCompany(userId: Int) {

        viewModelScope.launch {

            checking = true

            try {

                val response = RetrofitClientLaravel.instance.checkCompany(
                    CompanyRequest(
                        user_id = userId,
                        cname = companyName,
                        cemail = companyEmail
                    )
                )

                if (response.isSuccessful) {

                    response.body()?.data?.let {
                        nameExists = it.name_exists
                        domainExists = it.domain_exists
                    }

                }

            } catch (e: Exception) {

                message = e.message ?: "Terjadi kesalahan."

            } finally {

                checking = false

            }

        }

    }

    fun updateCompany(userId: Int) {

        viewModelScope.launch {

            saving = true

            try {

                val response = RetrofitClientLaravel.instance.updateCompany(
                    CompanyRequest(
                        user_id = userId,
                        cname = companyName,
                        cemail = companyEmail
                    )
                )

                Log.d("COMPANY", "Update Code = ${response.code()}")
                Log.d("COMPANY", "Update Body = ${response.body()}")
                Log.d("COMPANY", "Update Error = ${response.errorBody()?.string()}")

                if (response.isSuccessful) {

                    message = response.body()?.message ?: "Berhasil disimpan."
                    _updateSuccess.emit(true)

                } else {

                    message = response.errorBody()?.string()
                        ?: "Gagal menyimpan."

                }

            } catch (e: Exception) {

                Log.e("COMPANY", "Update Exception", e)

                message = e.message ?: "Terjadi kesalahan."

            } finally {

                saving = false

            }

        }

    }

    fun loadCompany(userId: Int) {

        Log.d("COMPANY", "User ID = $userId")

        viewModelScope.launch {

            val response = RetrofitClientLaravel.instance.getCompany(userId)

            Log.d("COMPANY", "Code = ${response.code()}")
            Log.d("COMPANY", "Body = ${response.body()}")

            if (response.isSuccessful) {

                response.body()?.data?.let {

                    companyName = it.cname
                    companyEmail = it.cemail

                }

            } else {

                message = "Error ${response.code()}"

            }

        }

    }

}