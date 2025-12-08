package id.my.matahati.absensi

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_EMAIL = "email"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_REMEMBER_ME = "rememberMe"
        private const val KEY_SCAN_STATE = "scan_state"
        private const val KEY_PENDING_SYNC = "pending_sync"
        private const val KEY_LAST_SYNC_ATTEMPT = "last_sync_attempt"
        private const val KEY_FACE_STATUS = "face_status"
    }

    // 🔹 Flow untuk status scan — ini yang akan dipantau oleh Compose
    private val _scanStateFlow = MutableStateFlow(getScanState())
    val scanStateFlow = _scanStateFlow.asStateFlow()
    private val _faceStatusFlow = MutableStateFlow(getFaceStatus())
    val faceStatusFlow = _faceStatusFlow.asStateFlow()

    // ✅ Simpan data login (tergantung checkbox "ingatkan saya")
    fun login(id: Int, name: String, email: String, rememberMe: Boolean) {
        val editor = prefs.edit()
        editor.putInt(KEY_ID, id)
        editor.putString(KEY_NAME, name)
        editor.putString(KEY_EMAIL, email)
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putBoolean(KEY_REMEMBER_ME, rememberMe)
        editor.apply()

        if (!rememberMe) {
            Handler(Looper.getMainLooper()).postDelayed({
                if (!isRememberMe()) clearSession()
            }, 8 * 60 * 60 * 1000)
        }
    }

    // ✅ Ambil user ID langsung
    fun getUserId(): Int = prefs.getInt(KEY_ID, -1)

    // ✅ Ambil semua data user
    fun getUser(): Map<String, Any?> = mapOf(
        "id" to prefs.getInt(KEY_ID, -1),
        "name" to prefs.getString(KEY_NAME, null),
        "email" to prefs.getString(KEY_EMAIL, null)
    )

    // ✅ Status login & ingatkan saya
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    fun isRememberMe(): Boolean = prefs.getBoolean(KEY_REMEMBER_ME, false)

    // ✅ Hapus session (logout)
    fun clearSession() {
        prefs.edit().clear().apply()
        _scanStateFlow.value = null
        _faceStatusFlow.value = "NONE"   // ✅ reset face status juga
    }

    // ✅ Simpan status scan
    fun setScanState(state: String) {
        prefs.edit().putString(KEY_SCAN_STATE, state).apply()
        _scanStateFlow.value = state // 🔹 update Flow agar Compose tahu
    }

    // ✅ Ambil status scan terakhir
    fun getScanState(): String? = prefs.getString(KEY_SCAN_STATE, null)

    // 🔹 Simpan status pending sync
    fun setPendingSync(pending: Boolean) {
        prefs.edit().putBoolean(KEY_PENDING_SYNC, pending).apply()
    }

    fun hasPendingSync(): Boolean = prefs.getBoolean(KEY_PENDING_SYNC, false)

    // 🔹 Simpan waktu terakhir sync attempt
    fun setLastSyncAttempt(time: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC_ATTEMPT, time).apply()
    }

    fun getLastSyncAttempt(): Long = prefs.getLong(KEY_LAST_SYNC_ATTEMPT, 0)

    // 🔹 Reset sync state
    fun resetSyncState() {
        prefs.edit().remove(KEY_PENDING_SYNC).remove(KEY_LAST_SYNC_ATTEMPT).apply()
    }

    fun setFaceStatus(status: String?) {
        val editor = prefs.edit()
        if (status == null) {
            editor.remove(KEY_FACE_STATUS)
        } else {
            editor.putString(KEY_FACE_STATUS, status)
        }
        editor.apply()
    }

    // 🔹 Ambil status face
    fun getFaceStatus(): String? = prefs.getString(KEY_FACE_STATUS, null)
}
