package id.my.matahati.absensi.data

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class TodoViewModel : ViewModel() {
    var myTasks by mutableStateOf<List<TodoItem>>(emptyList())
    var incomingTasks by mutableStateOf<List<TodoItem>>(emptyList())
    var loading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var updateSuccess by mutableStateOf(false)

    // Store states
    var departments by mutableStateOf<List<DepartmentItem>>(emptyList())
    var saving by mutableStateOf(false)

    fun loadDepartments() {
        if (departments.isNotEmpty()) return
        
        viewModelScope.launch {
            try {
                val response = RetrofitClientLaravel.instance.getDepartments()
                if (response.isSuccessful) {
                    departments = response.body()?.data ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("TodoViewModel", "Load Dept Error: ${e.message}")
            }
        }
    }

    fun storeTodo(request: TodoStoreRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            saving = true
            errorMessage = null
            try {
                val response = RetrofitClientLaravel.instance.storeTodo(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        onSuccess()
                        loadTodo(request.user_id)
                    } else {
                        errorMessage = body?.message ?: "Gagal menyimpan task"
                    }
                } else {
                    errorMessage = "Gagal menyimpan task (${response.code()})"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Terjadi kesalahan koneksi"
                Log.e("TodoViewModel", "Store Error: ${e.message}")
            } finally {
                saving = false
            }
        }
    }

    fun loadTodo(userId: Int) {
        viewModelScope.launch {
            loading = true
            errorMessage = null
            try {
                val response = RetrofitClientLaravel.instance.getTodoList(userId)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        myTasks = body.my_task
                        incomingTasks = body.incoming_task
                    } else {
                        errorMessage = "Gagal memuat data task"
                    }
                } else {
                    errorMessage = "Gagal memuat data (${response.code()})"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Terjadi kesalahan koneksi"
                Log.e("TodoViewModel", "Load Error: ${e.message}")
            } finally {
                loading = false
            }
        }
    }

    fun completeTodo(taskId: Int, userId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            loading = true
            errorMessage = null
            updateSuccess = false
            try {
                val response = RetrofitClientLaravel.instance.completeTodo(taskId, userId)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        updateSuccess = true
                        onSuccess()
                        loadTodo(userId)
                    } else {
                        errorMessage = body?.message ?: "Gagal menyelesaikan task"
                    }
                } else {
                    errorMessage = "Gagal menyelesaikan task (${response.code()})"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Terjadi kesalahan koneksi"
                Log.e("TodoViewModel", "Complete Error: ${e.message}")
            } finally {
                loading = false
            }
        }
    }

    fun clearError() {
        errorMessage = null
    }
}
