package id.my.matahati.absensi.data

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserContractViewModel : ViewModel() {

    private val _contract = MutableStateFlow<UserContract?>(null)
    val contract: StateFlow<UserContract?> = _contract

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadUserContract(userId: Int) {
        if (userId <= 0) return

        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val url =
                    "https://absensi.matahati.my.id/user_contract_mobile.php?userid=$userId"

                val response = ApiClient.apiService.getUserContract(url)

                if (response.isSuccessful) {
                    val body = response.body()

                    Log.d("CONTRACT_API", body.toString())

                    if (body?.success == true && body.data != null) {
                        _contract.value = body.data
                    } else {
                        _contract.value = null
                        _error.value = body?.message ?: "Tidak ada kontrak aktif"
                    }
                } else {
                    _error.value = "HTTP ${response.code()}"
                    Log.e("CONTRACT_API", "HTTP Error ${response.code()}")
                }
            } catch (e: Exception) {
                _error.value = e.message
                Log.e("CONTRACT_API", "Exception", e)
            } finally {
                _loading.value = false
            }
        }
    }
}
