package id.my.matahati.absensi.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class FaceApprovalViewModel : ViewModel() {

    var users by mutableStateOf<List<PendingFaceUser>>(emptyList())
    var loading by mutableStateOf(false)

    fun loadPending(approverId: Int) {

        viewModelScope.launch {

            loading = true

            try {

                val response =
                    RetrofitClientLaravel.instance
                        .getPendingFaces(approverId)

                users = response.data

            } catch (e: Exception) {
                e.printStackTrace()
            }

            loading = false
        }
    }

    fun approve(
        userId: Int,
        approverId: Int,
        onDone: () -> Unit
    ) {

        viewModelScope.launch {

            try {

                RetrofitClientLaravel.instance
                    .approveFace(userId, approverId)

                onDone()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun reject(
        userId: Int,
        approverId: Int,
        onDone: () -> Unit
    ) {

        viewModelScope.launch {

            try {

                RetrofitClientLaravel.instance
                    .rejectFace(userId, approverId)

                onDone()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}