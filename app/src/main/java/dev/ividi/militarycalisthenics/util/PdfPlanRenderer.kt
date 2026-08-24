package dev.ividi.militarycalisthenics.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import dev.ividi.militarycalisthenics.model.BlockType
import dev.ividi.militarycalisthenics.model.DailyWorkout
import dev.ividi.militarycalisthenics.model.WeeklyPlan
import dev.ividi.militarycalisthenics.ui.Lang
import dev.ividi.militarycalisthenics.ui.t
import java.io.File
import java.io.FileOutputStream

/** Draws a training week to a multi-page PDF, one page per day, in the ividi.dev brand palette. */
object PdfPlanRenderer {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 48f

    private val bgColor = Color.parseColor("#0A0A0F")
    private val accentColor = Color.parseColor("#F99C00")
    private val textColor = Color.parseColor("#E2E8F0")
    private val textDimColor = Color.parseColor("#94A3B8")

    fun render(week: WeeklyPlan, lang: Lang, outFile: File) {
        val document = PdfDocument()
        week.workouts.forEach { workout -> document.startPage(pageInfo(document.pages.size + 1)).also { page ->
            drawDay(page.canvas, workout, week.weekIndex, lang)
            document.finishPage(page)
        } }

        FileOutputStream(outFile).use { document.writeTo(it) }
        document.close()
    }

    private fun pageInfo(pageNumber: Int) =
        PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()

    private fun drawDay(canvas: Canvas, workout: DailyWorkout, weekIndex: Int, lang: Lang) {
        canvas.drawColor(bgColor)

        val titlePaint = Paint().apply { color = accentColor; textSize = 22f; isFakeBoldText = true; isAntiAlias = true }
        val subtitlePaint = Paint().apply { color = textDimColor; textSize = 12f; isAntiAlias = true }
        val sectionPaint = Paint().apply { color = accentColor; textSize = 14f; isFakeBoldText = true; isAntiAlias = true }
        val bodyPaint = Paint().apply { color = textColor; textSize = 12f; isAntiAlias = true }

        var y = MARGIN + 10f
        canvas.drawText("${t("week", lang)} ${weekIndex + 1} · ${t("day", lang)} ${workout.dayIndex + 1}", MARGIN, y, subtitlePaint)
        y += 26f
        canvas.drawText(workout.title, MARGIN, y, titlePaint)
        y += 30f

        workout.blocks.forEach { block ->
            val label = when (block.type) {
                BlockType.WARM_UP -> t("warm_up", lang)
                BlockType.STRENGTH -> t("strength", lang)
                BlockType.CIRCUIT -> t("circuit", lang)
                BlockType.CORE -> t("core", lang)
                BlockType.COOL_DOWN -> t("cool_down", lang)
            }
            canvas.drawText(label.uppercase(), MARGIN, y, sectionPaint)
            y += 18f
            block.exercises.forEach { ex ->
                val amount = when {
                    ex.reps != null -> "${ex.sets}x${ex.reps} ${t("reps", lang)}"
                    ex.seconds != null -> "${ex.sets}x${ex.seconds}${t("seconds", lang)}"
                    else -> "${ex.sets} ${t("sets", lang)}"
                }
                canvas.drawText("• ${ex.name} — $amount", MARGIN + 8f, y, bodyPaint)
                y += 16f
            }
            y += 10f
        }

        val footerPaint = Paint().apply { color = textDimColor; textSize = 10f; isAntiAlias = true }
        canvas.drawText("ividi.dev", MARGIN, PAGE_HEIGHT - MARGIN, footerPaint)
    }
}
