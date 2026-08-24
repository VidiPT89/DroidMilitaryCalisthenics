package dev.ividi.militarycalisthenics.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dev.ividi.militarycalisthenics.model.BlockType
import dev.ividi.militarycalisthenics.model.DailyWorkout
import dev.ividi.militarycalisthenics.model.WeeklyPlan
import dev.ividi.militarycalisthenics.ui.Lang
import dev.ividi.militarycalisthenics.ui.t
import java.io.File

/** Builds a plain-text rendering of a single day's workout, suitable for sharing. */
fun DailyWorkout.toShareText(lang: Lang): String {
    val builder = StringBuilder()
    builder.appendLine("${t("day", lang)} ${dayIndex + 1} — ${t(title, lang)}")
    blocks.forEach { block ->
        val label = when (block.type) {
            BlockType.WARM_UP -> t("warm_up", lang)
            BlockType.STRENGTH -> t("strength", lang)
            BlockType.CIRCUIT -> t("circuit", lang)
            BlockType.CORE -> t("core", lang)
            BlockType.COOL_DOWN -> t("cool_down", lang)
        }
        builder.appendLine()
        builder.appendLine(label.uppercase())
        block.exercises.forEach { ex ->
            val amount = when {
                ex.reps != null -> "${ex.sets}x${ex.reps} ${t("reps", lang)}"
                ex.seconds != null -> "${ex.sets}x${ex.seconds}${t("seconds", lang)}"
                else -> "${ex.sets} ${t("sets", lang)}"
            }
            builder.appendLine("- ${t(ex.name, lang)}: $amount")
        }
    }
    return builder.toString()
}

/** Builds a plain-text rendering of a full training week, suitable for sharing. */
fun WeeklyPlan.toShareText(lang: Lang): String {
    val builder = StringBuilder()
    builder.appendLine("${t("app_title", lang)} — ${t("week", lang)} ${weekIndex + 1}")
    workouts.forEach { workout ->
        builder.appendLine()
        builder.append(workout.toShareText(lang))
    }
    builder.appendLine()
    builder.append("ividi.dev")
    return builder.toString()
}

/** Shares plain text through the system chooser (SMS, WhatsApp, notes, email, etc). */
fun shareAsText(context: Context, subject: String, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, subject))
}

/** Renders a training week as a simple one-page-per-day PDF and shares it via a content URI. */
fun sharePlanAsPdf(context: Context, week: WeeklyPlan, lang: Lang) {
    val file = File(context.cacheDir, "plan_week_${week.weekIndex + 1}.pdf")
    PdfPlanRenderer.render(week, lang, file)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, t("your_plan", lang)))
}
