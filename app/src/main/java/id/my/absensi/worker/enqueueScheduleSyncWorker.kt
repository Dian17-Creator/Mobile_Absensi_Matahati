package id.my.matahati.absensi.worker

import android.content.Context
import androidx.work.*

fun enqueueScheduleSyncWorker(context: Context, userId: Int) {
    val workManager = WorkManager.getInstance(context)

    val inputData = workDataOf("USER_ID" to userId)

    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val workRequest = OneTimeWorkRequestBuilder<ScheduleSyncWorker>()
        .setConstraints(constraints)
        .setInputData(inputData)
        .addTag("sync_schedule_worker")
        .build()

    workManager.enqueueUniqueWork(
        "sync_schedule_$userId",
        ExistingWorkPolicy.REPLACE,
        workRequest
    )
}
