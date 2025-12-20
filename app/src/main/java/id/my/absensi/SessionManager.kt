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

        // 🔐 ROLE
        private const val KEY_FADMIN = "fadmin"
        private const val KEY_FSUPER = "fsuper"
        private const val KEY_FHRD = "fhrd"

        // 🔹 FACE & SCAN
        private const val KEY_SCAN_STATE = "scan_state"
        private const val KEY_PENDING_SYNC = "pending_sync"
        private const val KEY_LAST_SYNC_ATTEMPT = "last_sync_attempt"
        private const val KEY_FACE_STATUS = "face_status"
    }

    // ================= FLOW =================
    private val _scanStateFlow = MutableStateFlow(getScanState())
    val scanStateFlow = _scanStateFlow.asStateFlow()

    private val _faceStatusFlow = MutableStateFlow(getFaceStatus())
    val faceStatusFlow = _faceStatusFlow.asStateFlow()

    // ================= LOGIN =================
    fun login(
        id: Int,
        name: String,
        email: String,
        rememberMe: Boolean,
        fadmin: Int = 0,
        fsuper: Int = 0,
        fhrd: Int = 0
    ) {
        prefs.edit().apply {
            putInt(KEY_ID, id)
            putString(KEY_NAME, name)
            putString(KEY_EMAIL, email)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putBoolean(KEY_REMEMBER_ME, rememberMe)

            // 🔐 simpan role
            putInt(KEY_FADMIN, fadmin)
            putInt(KEY_FSUPER, fsuper)
            putInt(KEY_FHRD, fhrd)

            apply()
        }

        if (!rememberMe) {
            Handler(Looper.getMainLooper()).postDelayed({
                if (!isRememberMe()) clearSession()
            }, 8 * 60 * 60 * 1000) // 8 jam
        }
    }

    // ================= GET USER =================
    fun getUserId(): Int = prefs.getInt(KEY_ID, -1)

    fun getUser(): Map<String, Any?> = mapOf(
        "id" to prefs.getInt(KEY_ID, -1),
        "name" to prefs.getString(KEY_NAME, null),
        "email" to prefs.getString(KEY_EMAIL, null),
        "fadmin" to prefs.getInt(KEY_FADMIN, 0),
        "fsuper" to prefs.getInt(KEY_FSUPER, 0),
        "fhrd" to prefs.getInt(KEY_FHRD, 0)
    )

    // ================= ROLE CHECK =================
    fun isAdmin(): Boolean = prefs.getInt(KEY_FADMIN, 0) == 1
    fun isCaptain(): Boolean = prefs.getInt(KEY_FSUPER, 0) == 1
    fun isHRD(): Boolean = prefs.getInt(KEY_FHRD, 0) == 1

    /** 🔥 INI YANG KAMU BUTUHKAN */
    fun isCaptainOrAbove(): Boolean =
        isCaptain() || isAdmin() || isHRD()

    // ================= STATUS =================
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    fun isRememberMe(): Boolean = prefs.getBoolean(KEY_REMEMBER_ME, false)

    // ================= LOGOUT =================
    fun clearSession() {
        prefs.edit().clear().apply()
        _scanStateFlow.value = null
        _faceStatusFlow.value = null
    }

    // ================= SCAN =================
    fun setScanState(state: String) {
        prefs.edit().putString(KEY_SCAN_STATE, state).apply()
        _scanStateFlow.value = state
    }

    fun getScanState(): String? =
        prefs.getString(KEY_SCAN_STATE, null)

    // ================= SYNC =================
    fun setPendingSync(pending: Boolean) {
        prefs.edit().putBoolean(KEY_PENDING_SYNC, pending).apply()
    }

    fun hasPendingSync(): Boolean =
        prefs.getBoolean(KEY_PENDING_SYNC, false)

    fun setLastSyncAttempt(time: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC_ATTEMPT, time).apply()
    }

    fun getLastSyncAttempt(): Long =
        prefs.getLong(KEY_LAST_SYNC_ATTEMPT, 0)

    fun resetSyncState() {
        prefs.edit()
            .remove(KEY_PENDING_SYNC)
            .remove(KEY_LAST_SYNC_ATTEMPT)
            .apply()
    }

    // ================= FACE =================
    fun setFaceStatus(status: String?) {
        prefs.edit().apply {
            if (status == null) remove(KEY_FACE_STATUS)
            else putString(KEY_FACE_STATUS, status)
            apply()
        }
        _faceStatusFlow.value = status
    }

    fun getFaceStatus(): String? =
        prefs.getString(KEY_FACE_STATUS, null)
}
