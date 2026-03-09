package id.my.matahati.absensi.data

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ApprovalViewModel : ViewModel() {

    private val repo = ApprovalRepository()

    var listData by mutableStateOf<List<ApprovalItem>>(emptyList())
        private set

    var loading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    fun load(type: String, userId: Int) {
        viewModelScope.launch {
            loading = true
            error = null
            try {
                val res = repo.getList(type, userId)
                if (res.isSuccessful && res.body()?.success == true) {
                    listData = res.body()?.data ?: emptyList()
                } else {
                    error = "Gagal memuat data"
                }
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    fun action(
        userId: Int,
        id: Int,
        type: String,
        action: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                repo.action(userId, id, type, action)
                onDone()
            } catch (e: Exception) {
                error = e.message
            }
        }
    }
}
