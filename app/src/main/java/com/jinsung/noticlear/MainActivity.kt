package com.jinsung.noticlear

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {
    companion object {
        fun hasAccess(ctx: Context): Boolean {
            val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners") ?: return false
            val me = ComponentName(ctx, ClearListenerService::class.java).flattenToString()
            return flat.split(":").any { it == me }
        }
    }

    private lateinit var swOverlay: Switch
    private lateinit var preview: View
    private var syncing = false
    private var checkedUpdate = false
    private var updating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.btn_clear).setOnClickListener {
            it.animate().rotationBy(360f).setDuration(400).start()
            Clearer.clear(this)
        }
        findViewById<View>(R.id.row_access).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        findViewById<View>(R.id.row_overlay).setOnClickListener { openOverlaySettings() }

        swOverlay = findViewById(R.id.sw_overlay)
        swOverlay.setOnCheckedChangeListener { _, on ->
            if (syncing) return@setOnCheckedChangeListener
            if (on) {
                if (!Settings.canDrawOverlays(this)) {
                    syncing = true; swOverlay.isChecked = false; syncing = false
                    openOverlaySettings(); return@setOnCheckedChangeListener
                }
                startForegroundService(Intent(this, OverlayService::class.java))
            } else stopService(Intent(this, OverlayService::class.java))
        }

        findViewById<Switch>(R.id.sw_persistent).apply {
            isChecked = Prefs.hidePersistent(this@MainActivity)
            setOnCheckedChangeListener { _, on -> Prefs.setHidePersistent(this@MainActivity, on) }
        }

        preview = findViewById(R.id.preview_bubble)
        val alphaVal = findViewById<TextView>(R.id.alpha_val)
        val sizeVal = findViewById<TextView>(R.id.size_val)

        findViewById<SeekBar>(R.id.seek_alpha).apply {
            max = 80; progress = Prefs.alpha(this@MainActivity) - 20
            alphaVal.text = "${progress + 20}%"
            setOnSeekBarChangeListener(simple { p ->
                val v = p + 20; alphaVal.text = "$v%"; Prefs.setAlpha(this@MainActivity, v); updatePreview()
            })
        }
        findViewById<SeekBar>(R.id.seek_size).apply {
            max = 40; progress = Prefs.size(this@MainActivity) - 40
            sizeVal.text = "${progress + 40}dp"
            setOnSeekBarChangeListener(simple { p ->
                val v = p + 40; sizeVal.text = "${v}dp"; Prefs.setSize(this@MainActivity, v); updatePreview()
            })
        }
        updatePreview()
        findViewById<TextView>(R.id.ver_text).text =
            "버전 v1." + Updater.currentCode(this)
    }

    private fun checkUpdate() {
        if (checkedUpdate) return
        checkedUpdate = true
        Thread {
            val rel = runCatching { Updater.fetchLatest() }.getOrNull() ?: return@Thread
            if (rel.code <= Updater.currentCode(this)) return@Thread
            runOnUiThread {
                findViewById<View>(R.id.update_card).visibility = View.VISIBLE
                findViewById<TextView>(R.id.update_text).text = "새 버전 v1.${rel.code} 이 있어요"
                val btn = findViewById<TextView>(R.id.btn_update)
                btn.setOnClickListener { startUpdate(rel.url, btn) }
            }
        }.start()
    }

    private fun startUpdate(url: String, btn: TextView) {
        if (updating) return
        if (!packageManager.canRequestPackageInstalls()) {
            android.widget.Toast.makeText(this, "'이 출처 허용'을 켠 뒤 다시 눌러주세요", android.widget.Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
            return
        }
        updating = true
        btn.text = "받는 중 0%"
        Thread {
            try {
                Updater.downloadAndInstall(this, url) { p -> runOnUiThread { btn.text = "받는 중 $p%" } }
                runOnUiThread { btn.text = "설치 화면 여는 중…" }
            } catch (e: Exception) {
                runOnUiThread { btn.text = "다시 시도"; android.widget.Toast.makeText(this, "다운로드 실패: ${e.message}", android.widget.Toast.LENGTH_LONG).show() }
            } finally { updating = false }
        }.start()
    }

    override fun onResume() {
        super.onResume()
        checkUpdate()
        setStatus(R.id.status_access, hasAccess(this))
        setStatus(R.id.status_overlay, Settings.canDrawOverlays(this))
        syncing = true; swOverlay.isChecked = OverlayService.running; syncing = false
    }

    private fun setStatus(id: Int, ok: Boolean) {
        findViewById<TextView>(id).apply {
            text = if (ok) "허용됨" else "설정하기 ›"
            setBackgroundResource(if (ok) R.drawable.chip_ok else R.drawable.chip_warn)
        }
    }

    private fun openOverlaySettings() = startActivity(
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))

    private fun updatePreview() {
        val d = resources.displayMetrics.density
        val s = (Prefs.size(this) * d).toInt()
        preview.layoutParams = preview.layoutParams.apply { width = s; height = s }
        preview.alpha = Prefs.alpha(this) / 100f
    }

    private fun simple(f: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) { if (fromUser) f(p) }
        override fun onStartTrackingTouch(sb: SeekBar) {}
        override fun onStopTrackingTouch(sb: SeekBar) {}
    }
}
