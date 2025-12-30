package id.my.absensi.data

data class ShiftDay(
    val date: String,
    val shiftName: String,
    val sessions: List<ShiftSession>
)

