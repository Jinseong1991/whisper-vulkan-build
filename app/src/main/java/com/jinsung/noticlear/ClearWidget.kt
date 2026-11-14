package com.jinsung.noticlear

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class ClearWidget : AppWidgetProvider() {
    companion object { const val ACTION_CLEAR = "com.jinsung.noticlear.CLEAR" }

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        val pi = PendingIntent.getBroadcast(ctx, 0,
            Intent(ctx, ClearWidget::class.java).setAction(ACTION_CLEAR),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val views = RemoteViews(ctx.packageName, R.layout.widget).apply {
            setOnClickPendingIntent(R.id.widget_root, pi)
        }
        mgr.updateAppWidget(ids, views)
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action == ACTION_CLEAR) Clearer.clear(ctx)
    }
}
