package com.jinsung.noticlear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.widget.Toast

class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT) ?: return
                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(confirm)
            }
            PackageInstaller.STATUS_SUCCESS -> {}
            else -> Toast.makeText(ctx,
                "설치 실패: " + (intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: ""),
                Toast.LENGTH_LONG).show()
        }
    }
}
