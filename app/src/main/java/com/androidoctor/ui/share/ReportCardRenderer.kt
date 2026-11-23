package com.androidoctor.ui.share

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.androidoctor.domain.model.DiagnosisResult
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportCardRenderer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    sealed class RenderResult {
        data class Success(val uri: Uri) : RenderResult()
        data class Failure(val reason: String) : RenderResult()
    }

    suspend fun render(result: DiagnosisResult): RenderResult = withContext(Dispatchers.IO) {
        val (width, height) = tryAllocate() ?: return@withContext RenderResult.Failure("Out of memory")
        val bitmap = try {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        } catch (oom: OutOfMemoryError) {
            Firebase.crashlytics.recordException(RuntimeException("report card bitmap alloc OOM", oom))
            return@withContext RenderResult.Failure("Out of memory")
        }
        try {
            val canvas = Canvas(bitmap)
            drawCard(canvas, width, height, result)
            val uri = saveToMediaStore(bitmap)
                ?: return@withContext RenderResult.Failure("Storage write failed")
            RenderResult.Success(uri)
        } catch (t: Throwable) {
            Log.e("ReportCard", "render failed", t)
            Firebase.crashlytics.recordException(RuntimeException("report card render", t))
            RenderResult.Failure(t.message ?: "Render failed")
        } finally {
            bitmap.recycle()
        }
    }

    private fun tryAllocate(): Pair<Int, Int>? {
        return try {
            val b = Bitmap.createBitmap(HI_W, HI_H, Bitmap.Config.ARGB_8888)
            b.recycle()
            HI_W to HI_H
        } catch (oom: OutOfMemoryError) {
            runCatching {
                val b = Bitmap.createBitmap(LO_W, LO_H, Bitmap.Config.ARGB_8888)
                b.recycle()
                LO_W to LO_H
            }.getOrNull()
        }
    }

    private fun drawCard(canvas: Canvas, w: Int, h: Int, result: DiagnosisResult) {
        // Background: Zinc950 #09090B
        canvas.drawColor(Color.parseColor("#09090B"))

        val scale = w.toFloat() / HI_W
        val paintWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F4F4F5")
            typeface = Typeface.DEFAULT_BOLD
        }
        val paintCopper = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C2703E")
            typeface = Typeface.DEFAULT_BOLD
        }
        val paintZinc400 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A1A1AA")
            typeface = Typeface.DEFAULT
        }
        val paintZinc700 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#3F3F46")
            typeface = Typeface.DEFAULT
        }

        val marginX = 96f * scale
        var y = 220f * scale

        // Title
        paintWhite.textSize = 64f * scale
        canvas.drawText("android", marginX, y, paintWhite)
        y += 78f * scale
        paintCopper.textSize = 64f * scale
        canvas.drawText("doctor", marginX, y, paintCopper)

        y += 60f * scale
        paintZinc400.textSize = 28f * scale
        canvas.drawText(result.device.model, marginX, y, paintZinc400)
        y += 36f * scale
        canvas.drawText("Android ${result.device.androidVersion}", marginX, y, paintZinc400)

        // Big score
        y += 180f * scale
        paintCopper.textSize = 220f * scale
        canvas.drawText("${result.verdict.overallScore}", marginX, y, paintCopper)

        paintZinc400.textSize = 40f * scale
        canvas.drawText("/100", marginX + 340f * scale, y, paintZinc400)

        y += 56f * scale
        paintZinc400.textSize = 26f * scale
        canvas.drawText("OVERALL SCORE", marginX, y, paintZinc400)

        // Sub-scores
        y += 100f * scale
        drawSubScore(canvas, "Battery", result.battery.score, marginX, y, scale, paintWhite, paintZinc400)
        y += 60f * scale
        drawSubScore(canvas, "Storage", result.storage.score, marginX, y, scale, paintWhite, paintZinc400)
        y += 60f * scale
        drawSubScore(canvas, "Memory", result.memory.score, marginX, y, scale, paintWhite, paintZinc400)
        y += 60f * scale
        drawSubScore(canvas, "CPU", result.cpu.score, marginX, y, scale, paintWhite, paintZinc400)
        y += 60f * scale
        drawSubScore(canvas, "Bloatware", result.bloatware.score, marginX, y, scale, paintWhite, paintZinc400)

        // Footer
        paintZinc700.textSize = 22f * scale
        canvas.drawText("Android Doctor · knowamit.github.io/android-doctor-app", marginX, h - 60f * scale, paintZinc700)
    }

    private fun drawSubScore(
        canvas: Canvas, label: String, score: Int, x: Float, y: Float, scale: Float,
        white: Paint, zinc400: Paint,
    ) {
        zinc400.textSize = 24f * scale
        canvas.drawText(label.uppercase(), x, y, zinc400)
        white.textSize = 36f * scale
        canvas.drawText("$score", x + 400f * scale, y, white)
    }

    private fun saveToMediaStore(bitmap: Bitmap): Uri? {
        val resolver = context.contentResolver
        val filename = "android-doctor-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AndroidDoctor")
            }
        }
        return try {
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri == null) {
                Log.w("ReportCard", "MediaStore insert returned null")
                return null
            }
            resolver.openOutputStream(uri)?.use { out ->
                val ok = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                if (!ok) {
                    Log.w("ReportCard", "bitmap compress returned false")
                }
            } ?: run {
                Log.w("ReportCard", "openOutputStream returned null for $uri")
                return null
            }
            uri
        } catch (t: Throwable) {
            Log.e("ReportCard", "saveToMediaStore failed", t)
            Firebase.crashlytics.recordException(RuntimeException("report card save", t))
            null
        }
    }

    companion object {
        const val HI_W = 1080
        const val HI_H = 1920
        const val LO_W = 540
        const val LO_H = 960
    }
}
