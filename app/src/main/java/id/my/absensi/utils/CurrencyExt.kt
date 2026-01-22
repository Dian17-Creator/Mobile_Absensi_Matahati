package id.my.matahati.absensi.utils

import java.text.NumberFormat
import java.util.Locale

fun String?.toRupiah(): String {
    val number = this?.toDoubleOrNull() ?: 0.0

    val formatter = NumberFormat.getNumberInstance(Locale("in", "ID")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    return "Rp ${formatter.format(number)}"
}
