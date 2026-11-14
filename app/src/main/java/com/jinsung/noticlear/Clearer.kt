package com.jinsung.noticlear

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.widget.Toast

object Clearer {
    fun clear(ctx: Context) {
        if (!MainActivity.hasAccess(ctx)) {
            toast(ctx, "알림지우개 앱에서 '알림 접근'을 허용해 주세요"); return
        }
        val svc = ClearListenerService.instance
        if (svc == null) {
            NotificationListenerService.requestRebind(ComponentName(ctx, ClearListenerService::class.java))
            toast(ctx, "연결 중… 한 번 더 눌러주세요"); return
        }
        val hide = Prefs.hidePersistent(ctx)
        val r = svc.clearAll(hide)
        val total = r.cleared + r.hidden
        toast(ctx, if (total > 0) "🧹 알림 ${total}개 정리했어요" else "지울 알림이 없어요")

        if (hide && r.hidden > 0) {
            // 잠시 뒤 실제로 사라졌는지 확인
            Handler(Looper.getMainLooper()).postDelayed({
                val left = ClearListenerService.instance?.remainingPersistentApps().orEmpty()
                if (left.isNotEmpty())
                    Toast.makeText(ctx.applicationContext,
                        "시스템이 막아서 못 숨긴 알림: ${left.joinToString(", ")}",
                        Toast.LENGTH_LONG).show()
            }, 700)
        }
    }
    fun restore(ctx: Context) {
        val svc = ClearListenerService.instance
        if (svc == null) {
            if (MainActivity.hasAccess(ctx))
                NotificationListenerService.requestRebind(ComponentName(ctx, ClearListenerService::class.java))
            toast(ctx, "연결 중… 한 번 더 눌러주세요"); return
        }
        val keys = Prefs.snoozed(ctx)
        if (keys.isEmpty()) { toast(ctx, "숨긴 고정 알림이 없어요"); return }
        svc.restore()
        toast(ctx, "고정 알림 ${keys.size}개 되돌리는 중…")
        Handler(Looper.getMainLooper()).postDelayed({
            val back = ClearListenerService.instance?.isShowing(keys) ?: 0
            Toast.makeText(ctx.applicationContext,
                if (back > 0) "✅ 고정 알림 ${back}개 되돌렸어요"
                else "바로 안 돌아온 알림은 최대 12시간 뒤 자동으로 돌아와요",
                Toast.LENGTH_LONG).show()
        }, 2500)
    }

    private fun toast(c: Context, m: String) = Toast.makeText(c.applicationContext, m, Toast.LENGTH_SHORT).show()
}
