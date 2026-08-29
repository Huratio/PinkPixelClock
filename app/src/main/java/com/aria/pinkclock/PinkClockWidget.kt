\
package com.aria.pinkclock

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.widget.RemoteViews
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

class PinkClockWidget : AppWidgetProvider() {

    companion object {
        private const val ACTION_UPDATE = "com.aria.pinkclock.UPDATE_CLOCK"
        private const val REQUEST = 4242

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, PinkClockWidget::class.java)
            val ids = manager.getAppWidgetIds(component)
            ids.forEach { id -> updateOne(context, manager, id) }
        }

        private fun updateOne(context: Context, manager: AppWidgetManager, id: Int) {
            val bitmap = ClockRenderer.render(context)
            val views = RemoteViews(context.packageName, R.layout.widget_clock)
            views.setImageViewBitmap(R.id.widget_image, bitmap)
            manager.updateAppWidget(id, views)
        }

        fun schedule(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, PinkClockWidget::class.java).setAction(ACTION_UPDATE)
            val pending = PendingIntent.getBroadcast(
                context, REQUEST, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pending)

            val now = System.currentTimeMillis()
            val nextMinute = now - (now % 60_000L) + 60_000L

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, nextMinute, pending
                )
            } else {
                // Fallback for devices where exact alarms are not allowed.
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP, nextMinute, 60_000L, pending
                )
            }
        }

        fun cancel(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, PinkClockWidget::class.java).setAction(ACTION_UPDATE)
            val pending = PendingIntent.getBroadcast(
                context, REQUEST, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pending)
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateOne(context, manager, it) }
        schedule(context)
    }

    override fun onEnabled(context: Context) {
        updateAll(context)
        schedule(context)
    }

    override fun onDisabled(context: Context) {
        cancel(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_UPDATE -> {
                updateAll(context)
                schedule(context)
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                updateAll(context)
                schedule(context)
            }
        }
    }
}

object ClockRenderer {
    private const val W = 768
    private const val H = 768
    private const val CX = 316f
    private const val CY = 326f

    fun render(context: Context): Bitmap {
        val face = BitmapFactory.decodeResource(context.resources, R.drawable.clock_face)
        val out = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Keep the original pixel-art face crisp when it is scaled into the widget.
        paint.isFilterBitmap = false
        canvas.drawBitmap(face, null, Rect(0, 0, W, H), paint)

        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR)
        val minute = now.get(Calendar.MINUTE)
        val second = now.get(Calendar.SECOND)

        val hourAngle = Math.toRadians(((hour + minute / 60f) * 30f - 90f).toDouble())
        val minuteAngle = Math.toRadians((minute * 6f - 90f).toDouble())
        val secondAngle = Math.toRadians((second * 6f - 90f).toDouble())

        // Pixel-art pink hands. The lengths match the proportions of the supplied artwork.
        drawHand(canvas, hourAngle, 190f, 13f, Color.rgb(239, 112, 157), Color.rgb(190, 52, 105))
        drawHand(canvas, minuteAngle, 255f, 10f, Color.rgb(246, 126, 169), Color.rgb(198, 56, 109))
        drawHand(canvas, secondAngle, 285f, 4f, Color.rgb(245, 143, 181), Color.rgb(202, 62, 115))

        // Center heart/pivot.
        drawHeart(canvas, CX, CY, 20f)

        // Tiny center highlight.
        paint.color = Color.rgb(255, 168, 196)
        paint.style = Paint.Style.FILL
        canvas.drawRect(CX - 3, CY - 3, CX + 3, CY + 3, paint)

        face.recycle()
        return out
    }

    private fun drawHand(
        canvas: Canvas,
        angle: Double,
        length: Float,
        width: Float,
        main: Int,
        shadow: Int
    ) {
        val ex = CX + cos(angle).toFloat() * length
        val ey = CY + sin(angle).toFloat() * length
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.strokeCap = Paint.Cap.SQUARE
        p.style = Paint.Style.STROKE

        p.color = shadow
        p.strokeWidth = width + 7f
        canvas.drawLine(CX, CY, ex, ey, p)

        p.color = main
        p.strokeWidth = width
        canvas.drawLine(CX, CY, ex, ey, p)

        // A short light pixel highlight along the hand.
        p.color = Color.rgb(255, 171, 198)
        p.strokeWidth = maxOf(2f, width / 3f)
        val hx = CX + cos(angle).toFloat() * (length * 0.72f)
        val hy = CY + sin(angle).toFloat() * (length * 0.72f)
        canvas.drawLine(CX, CY, hx, hy, p)
    }

    private fun drawHeart(canvas: Canvas, x: Float, y: Float, s: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.color = Color.rgb(230, 67, 123)

        val path = Path()
        path.moveTo(x, y + s)
        path.cubicTo(x - s * 1.35f, y - s * 0.05f, x - s * 0.95f, y - s, x - s * 0.42f, y - s * 0.58f)
        path.cubicTo(x - s * 0.10f, y - s * 0.98f, x, y - s * 0.62f, x, y - s * 0.25f)
        path.cubicTo(x, y - s * 0.62f, x + s * 0.10f, y - s * 0.98f, x + s * 0.42f, y - s * 0.58f)
        path.cubicTo(x + s * 0.95f, y - s, x + s * 1.35f, y - s * 0.05f, x, y + s)
        canvas.drawPath(path, p)
    }
}
