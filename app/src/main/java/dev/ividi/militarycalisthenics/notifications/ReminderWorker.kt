package dev.ividi.militarycalisthenics.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import dev.ividi.militarycalisthenics.MainActivity
import dev.ividi.militarycalisthenics.R
import dev.ividi.militarycalisthenics.ui.Lang
import dev.ividi.militarycalisthenics.ui.t

private const val CHANNEL_ID = "workout_reminders"
private const val NOTIFICATION_ID = 1001

/** Fires the daily workout-reminder notification. Scheduled by [ReminderScheduler]. */
class ReminderWorker(appContext: Context, params: WorkerParameters) : Worker(appContext, params) {

    override fun doWork(): Result {
        postReminderNotification(applicationContext, langFromInputData())
        return Result.success()
    }

    private fun langFromInputData(): Lang =
        if (inputData.getString(KEY_LANG) == "EN") Lang.EN else Lang.PT

    companion object {
        const val KEY_LANG = "lang"
    }
}

fun ensureReminderChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java)
    val channel = NotificationChannel(
        CHANNEL_ID,
        "Workout reminders",
        NotificationManager.IMPORTANCE_DEFAULT
    )
    manager.createNotificationChannel(channel)
}

fun postReminderNotification(context: Context, lang: Lang) {
    ensureReminderChannel(context)

    val openIntent = android.content.Intent(context, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(
        context, 0, openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(t("reminder_notification_title", lang))
        .setContentText(t("reminder_notification_body", lang))
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    androidx.core.app.NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
}
