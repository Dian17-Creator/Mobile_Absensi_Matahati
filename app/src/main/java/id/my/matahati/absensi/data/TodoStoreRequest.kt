package id.my.matahati.absensi.data

data class TodoStoreItem(
    val ndep_tujuan: Int,
    val cpermintaan: String
)

data class TodoStoreRequest(
    val user_id: Int,
    val items: List<TodoStoreItem>
)
