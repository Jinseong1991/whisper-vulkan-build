package com.jinsung.noticlear

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

object Updater {
    private const val API =
        "https://api.github.com/repos/Jinseong1991/whisper-vulkan-build/releases?per_page=30"

    data class Release(val code: Int, val url: String)

    fun currentCode(ctx: Context): Long =
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).longVersionCode

    /** 최신 noticlear 릴리스 (네트워크, 백그라운드 스레드에서 호출) */
    fun fetchLatest(): Release? {
        val c = (URL(API).openConnection() as HttpURLConnection).apply {
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "NotiClear")
            connectTimeout = 8000; readTimeout = 8000
        }
        try {
            if (c.responseCode != 200) return null
            val arr = JSONArray(c.inputStream.bufferedReader().readText())
            var best: Release? = null
            for (i in 0 until arr.length()) {
                val r = arr.getJSONObject(i)
                val tag = r.optString("tag_name")
                if (!tag.startsWith("noticlear-v1.")) continue
                val code = tag.substringAfterLast('.').toIntOrNull() ?: continue
                val assets = r.optJSONArray("assets") ?: continue
                for (j in 0 until assets.length()) {
                    val a = assets.getJSONObject(j)
                    if (a.optString("name").endsWith(".apk") && (best == null || code > best.code))
                        best = Release(code, a.getString("browser_download_url"))
                }
            }
            return best
        } finally { c.disconnect() }
    }

    /** APK를 받아 PackageInstaller로 설치 (백그라운드 스레드) */
    fun downloadAndInstall(ctx: Context, url: String, onProgress: (Int) -> Unit) {
        val installer = ctx.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(ctx.packageName)
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            val c = (URL(url).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "NotiClear")
                connectTimeout = 10000; readTimeout = 20000
            }
            val total = c.contentLengthLong
            c.inputStream.use { input ->
                session.openWrite("app.apk", 0, if (total > 0) total else -1).use { out ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L; var last = -1
                    while (true) {
                        val n = input.read(buf); if (n < 0) break
                        out.write(buf, 0, n); done += n
                        if (total > 0) {
                            val p = (done * 100 / total).toInt()
                            if (p != last) { last = p; onProgress(p) }
                        }
                    }
                    session.fsync(out)
                }
            }
            c.disconnect()
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
            val pi = PendingIntent.getBroadcast(ctx, id,
                Intent(ctx, InstallReceiver::class.java), flags)
            session.commit(pi.intentSender)
        }
    }
}
