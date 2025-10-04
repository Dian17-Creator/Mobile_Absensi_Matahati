package id.my.matahati.absensi.data

sealed class ScanResult {
    data class Message(val text: String) : ScanResult()
    object SuccessImage : ScanResult()
    object WaitingImage : ScanResult()
}
