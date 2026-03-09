package id.my.matahati.absensi.data

data class ShiftDay(
    val date: String,
    val shiftName: String,
    val sessions: List<ShiftSession>
)

