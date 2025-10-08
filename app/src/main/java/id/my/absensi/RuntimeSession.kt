package id.my.matahati.absensi

object RuntimeSession {
    var isLoggedIn: Boolean = false
    var userId: Int = -1
    var userName: String? = null
    var userEmail: String? = null

    fun set(userId: Int, name: String?, email: String?) {
        this.isLoggedIn = true
        this.userId = userId
        this.userName = name
        this.userEmail = email
    }

    fun clear() {
        isLoggedIn = false
        userId = -1
        userName = null
        userEmail = null
    }
}
