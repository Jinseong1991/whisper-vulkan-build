package com.jinsung.noticlear

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.widget.RemoteViews
import android.widget.Toast

class ClearWidget : AppWidgetProvider() {
    companion object { const val ACTION_CLEAR = "com.jinsung.noticlear.CLEAR" }

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        val intent = Intent(ctx, ClearWidget::class.java).setAction(ACTION_CLEAR)
        val pi = PendingIntent.getBroadcast(ctx, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val views = RemoteViews(ctx.packageName, R.layout.widget).apply {
            setOnClickPendingIntent(R.id.widget_root, pi)
        }
        mgr.updateAppWidget(ids, views)
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action != ACTION_CLEAR) return

        if (!MainActivity.hasAccess(ctx)) {
            Toast.makeText(ctx, "앱을 열어 '알림 접근'을 허용해 주세요", Toast.LENGTH_LONG).show()
            return
        }
        val svc = ClearListenerService.instance
        if (svc == null) {
            // 서비스 연결이 끊긴 경우 재연결 요청
            NotificationListenerService.requestRebind(ComponentName(ctx, ClearListenerService::class.java))
            Toast.makeText(ctx, "연결 중… 한 번 더 눌러주세요", Toast.LENGTH_SHORT).show()
            return
        }
        val n = svc.clearAll()
        Toast.makeText(ctx, if (n > 0) "알림 ${n}개 지움" else "지울 알림 없음", Toast.LENGTH_SHORT).show()
    }
}
