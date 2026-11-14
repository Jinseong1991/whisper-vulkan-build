package com.jinsung.noticlear

import android.content.ComponentName
import android.content.Context
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
        val n = svc.clearAll()
        toast(ctx, if (n > 0) "🧹 알림 ${n}개 지웠어요" else "지울 알림이 없어요")
    }
    private fun toast(c: Context, m: String) = Toast.makeText(c.applicationContext, m, Toast.LENGTH_SHORT).show()
}
