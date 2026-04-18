package id.my.matahati.absensi.utils

import java.text.NumberFormat
import java.util.Locale

fun Number?.toRupiah(): String {
    val number = this?.toDouble() ?: 0.0

    val formatter = NumberFormat.getNumberInstance(Locale("id", "ID")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    return formatter.format(number)
}