package dev.ividi.militarycalisthenics.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.ividi.militarycalisthenics.ui.Lang
import java.util.Calendar
import java.util.concurrent.TimeUnit

private const val WORK_NAME = "workout_reminder"

/** Schedules or cancels the recurring daily workout-reminder notification. */
object ReminderScheduler {

    fun schedule(context: Context, hour: Int, minute: Int = 0, lang: Lang) {
        val initialDelay = millisUntilNextOccurrence(hour, minute)
        val data = Data.Builder().putString(ReminderWorker.KEY_LANG, lang.name).build()

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private fun millisUntilNextOccurrence(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
