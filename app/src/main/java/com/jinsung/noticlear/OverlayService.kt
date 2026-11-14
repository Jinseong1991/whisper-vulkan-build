package com.jinsung.noticlear

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import kotlin.math.hypot

class OverlayService : Service() {
    companion object {
        @Volatile var running = false
        const val ACTION_STOP = "com.jinsung.noticlear.STOP_OVERLAY"
    }

    private lateinit var wm: WindowManager
    private var bubble: FrameLayout? = null
    private var closeZone: FrameLayout? = null
    private lateinit var lp: WindowManager.LayoutParams
    private val density get() = resources.displayMetrics.density
    private fun dp(v: Int) = (v * density).toInt()

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "alpha" || key == "size") applyLook()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startAsForeground()
        addBubble()
        Prefs.register(this, prefListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        Prefs.unregister(this, prefListener)
        bubble?.let { runCatching { wm.removeView(it) } }
        closeZone?.let { runCatching { wm.removeView(it) } }
        bubble = null; closeZone = null
        super.onDestroy()
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("overlay", "플로팅 버튼", NotificationManager.IMPORTANCE_MIN))
        val stopPi = PendingIntent.getService(this, 1,
            Intent(this, OverlayService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
        val openPi = PendingIntent.getActivity(this, 2,
            Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, "overlay")
            .setSmallIcon(R.drawable.ic_clear)
            .setContentTitle("플로팅 지우개 실행 중")
            .setContentText("탭하면 설정 열기")
            .setContentIntent(openPi)
            .addAction(Notification.Action.Builder(null, "끄기", stopPi).build())
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34)
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
    }

    private fun overlayParams(w: Int, h: Int, touchable: Boolean) = WindowManager.LayoutParams(
        w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            (if (touchable) 0 else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE),
        PixelFormat.TRANSLUCENT)

    private fun addBubble() {
        val size = dp(Prefs.size(this))
        val b = FrameLayout(this).apply {
            setBackgroundResource(R.drawable.bubble_bg)
            elevation = dp(6).toFloat()
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_clear)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                .apply { val m = (size * 0.22f).toInt(); setMargins(m, m, m, m) })
        }
        lp = overlayParams(size, size, true).apply {
            gravity = Gravity.TOP or Gravity.START
            val (px, py) = Prefs.pos(this@OverlayService)
            x = if (px < 0) resources.displayMetrics.widthPixels - size else px
            y = py
        }
        b.alpha = Prefs.alpha(this) / 100f
        b.setOnTouchListener(DragTouch())
        wm.addView(b, lp)
        bubble = b
    }

    private fun applyLook() {
        val b = bubble ?: return
        b.alpha = Prefs.alpha(this) / 100f
        val size = dp(Prefs.size(this))
        lp.width = size; lp.height = size
        (b.getChildAt(0).layoutParams as FrameLayout.LayoutParams).apply {
            val m = (size * 0.22f).toInt(); setMargins(m, m, m, m)
        }
        clampAndUpdate()
    }

    private fun clampAndUpdate() {
        val dm = resources.displayMetrics
        lp.x = lp.x.coerceIn(0, dm.widthPixels - lp.width)
        lp.y = lp.y.coerceIn(0, dm.heightPixels - lp.height)
        bubble?.let { runCatching { wm.updateViewLayout(it, lp) } }
    }

    // ---------- 닫기 영역(드래그 중 화면 아래 X) ----------
    private fun showClose() {
        if (closeZone != null) return
        val s = dp(64)
        val z = FrameLayout(this).apply {
            setBackgroundResource(R.drawable.close_bg)
            addView(ImageView(context).apply { setImageResource(R.drawable.ic_close) },
                FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER))
            scaleX = 0f; scaleY = 0f
        }
        val p = overlayParams(s, s, false).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dp(56)
        }
        wm.addView(z, p)
        z.animate().scaleX(1f).scaleY(1f).setDuration(180).setInterpolator(OvershootInterpolator()).start()
        closeZone = z
    }

    private fun hideClose() {
        closeZone?.let { runCatching { wm.removeView(it) } }
        closeZone = null
    }

    private fun nearClose(): Boolean {
        val dm = resources.displayMetrics
        val cx = dm.widthPixels / 2f
        val cy = dm.heightPixels - dp(56) - dp(32).toFloat()
        val bx = lp.x + lp.width / 2f
        val by = lp.y + lp.height / 2f
        return hypot(bx - cx, by - cy) < dp(90)
    }

    private fun snapToEdge() {
        val dm = resources.displayMetrics
        val target = if (lp.x + lp.width / 2 < dm.widthPixels / 2) 0 else dm.widthPixels - lp.width
        ValueAnimator.ofInt(lp.x, target).apply {
            duration = 220
            interpolator = OvershootInterpolator(1.2f)
            addUpdateListener { lp.x = it.animatedValue as Int; clampAndUpdate() }
            doOnEndCompat { Prefs.setPos(this@OverlayService, lp.x, lp.y) }
            start()
        }
    }

    private fun ValueAnimator.doOnEndCompat(f: () -> Unit) =
        addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(a: android.animation.Animator) = f()
        })

    private inner class DragTouch : View.OnTouchListener {
        private var downX = 0f; private var downY = 0f
        private var startX = 0; private var startY = 0
        private var dragging = false
        private val slop = ViewConfiguration.get(this@OverlayService).scaledTouchSlop * 1.5f

        override fun onTouch(v: View, e: MotionEvent): Boolean {
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY; startX = lp.x; startY = lp.y; dragging = false
                    v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(80).start()
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX; val dy = e.rawY - downY
                    if (!dragging && hypot(dx, dy) > slop) { dragging = true; showClose() }
                    if (dragging) {
                        lp.x = startX + dx.toInt(); lp.y = startY + dy.toInt()
                        clampAndUpdate()
                        val near = nearClose()
                        closeZone?.animate()?.scaleX(if (near) 1.3f else 1f)?.scaleY(if (near) 1.3f else 1f)
                            ?.setDuration(100)?.start()
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                    if (dragging) {
                        val close = nearClose()
                        hideClose()
                        if (close) { stopSelf(); return true }
                        snapToEdge()
                    } else if (e.action == MotionEvent.ACTION_UP) {
                        v.animate().rotationBy(360f).setDuration(350).start()
                        Clearer.clear(this@OverlayService)
                    }
                }
            }
            return true
        }
    }
}
