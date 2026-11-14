package com.jinsung.noticlear

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {
    companion object {
        fun hasAccess(ctx: Context): Boolean {
            val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners") ?: return false
            val me = ComponentName(ctx, ClearListenerService::class.java).flattenToString()
            return flat.split(":").any { it == me }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.btn_access).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        findViewById<Button>(R.id.btn_test).setOnClickListener {
            sendBroadcast(Intent(this, ClearWidget::class.java).setAction(ClearWidget.ACTION_CLEAR))
        }
    }

    override fun onResume() {
        super.onResume()
        findViewById<TextView>(R.id.status).text =
            if (hasAccess(this)) "✅ 알림 접근 허용됨\n홈화면에 '알림 지우기' 위젯을 추가하세요"
            else "❌ 알림 접근이 꺼져 있어요\n아래 버튼 → 알림지우개 켜기"
    }
}
