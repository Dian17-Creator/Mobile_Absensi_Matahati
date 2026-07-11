package id.my.matahati.absensi.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class FaceApprovalViewModel : ViewModel() {

    var users by mutableStateOf<List<FacePendingUser>>(emptyList())
    var loading by mutableStateOf(false)

    fun loadPending(approverId: Int) {

        viewModelScope.launch {

            loading = true

            try {

                val response =
                    RetrofitClientLaravel.instance
                        .getPendingFaceList(approverId)

                android.util.Log.d(
                    "FACE_PENDING",
                    "approverId = $approverId"
                )

                android.util.Log.d(
                    "FACE_PENDING",
                    "url = ${response.raw().request.url}"
                )

                android.util.Log.d(
                    "FACE_PENDING",
                    "code = ${response.code()}"
                )

                android.util.Log.d(
                    "FACE_PENDING",
                    "body = ${response.body()}"
                )

                users = response.body()?.data ?: emptyList()

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