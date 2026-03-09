package id.my.matahati.absensi.worker


import android.content.Context
import androidx.work.*

fun enqueueSyncWorker(context: Context) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED) // hanya jalan kalau ada internet
        .build()

    val work = OneTimeWorkRequestBuilder<SyncWorker>()
        .setConstraints(constraints)
        .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
        "sync_offline_scans",
        ExistingWorkPolicy.KEEP,
        work
    )
}
