package id.my.matahati.absensi.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

fun enqueueManualSyncWorker(context: Context) {
    Log.d("ManualWorkerHelper", "enqueueManualSyncWorker() dipanggil")

    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val workRequest = OneTimeWorkRequestBuilder<SyncManualWorker>()
        .setConstraints(constraints)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
        .addTag("sync_manual_absen")
        .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
        "sync_manual_absen",
        ExistingWorkPolicy.KEEP,
        workRequest
    )

    Log.d("ManualWorkerHelper", "✅ Worker sinkronisasi absen manual dijadwalkan.")
}
