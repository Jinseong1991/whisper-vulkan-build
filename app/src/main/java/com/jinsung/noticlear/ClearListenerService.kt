package com.jinsung.noticlear

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class ClearListenerService : NotificationListenerService() {
    companion object {
        @Volatile var instance: ClearListenerService? = null
        const val SNOOZE_MS = 12L * 60 * 60 * 1000   // 12시간
    }
    override fun onListenerConnected() { instance = this }
    override fun onListenerDisconnected() { instance = null }

    data class Result(val cleared: Int, val hidden: Int)

    /** '다른 앱 위에 표시 중' 시스템 알림인지 */
    private fun isOverlayNotice(s: StatusBarNotification) =
        s.packageName == "android" &&
            ((s.notification.channelId ?: "").contains("AlertWindow", true) ||
             (s.tag ?: "").contains("AlertWindow", true))

    /** 숨김 대상 고정 알림인지 (일반 시스템 알림은 건드리지 않음) */
    private fun hideTarget(s: StatusBarNotification): Boolean {
        if (s.isClearable) return false
        if (s.packageName == "com.android.systemui") return false
        if (s.packageName == "android") return isOverlayNotice(s)
        return true   // 다른 앱 + 알림지우개 자체 실행중 알림 포함
    }

    private fun active() = try { activeNotifications?.toList() ?: emptyList() } catch (e: Exception) { emptyList() }

    fun clearAll(hidePersistent: Boolean): Result {
        val list = active()
        val cleared = list.count { it.isClearable }
        cancelAllNotifications()

        var hidden = 0
        if (hidePersistent) {
            val keys = mutableSetOf<String>()
            for (sbn in list) {
                if (!hideTarget(sbn)) continue
                runCatching { cancelNotification(sbn.key) }
                if (runCatching { snoozeNotification(sbn.key, SNOOZE_MS) }.isSuccess) {
                    hidden++; keys += sbn.key
                }
            }
            Prefs.addSnoozed(this, keys)
        }
        return Result(cleared, hidden)
    }

    /** 숨긴 고정 알림 되돌리기: 아주 짧게 다시 스누즈 → 곧바로 다시 나타남 */
    fun restore(): Int {
        val keys = Prefs.snoozed(this)
        for (k in keys) runCatching { snoozeNotification(k, 500) }
        Prefs.clearSnoozed(this)
        return keys.size
    }

    fun isShowing(keys: Set<String>) = active().count { it.key in keys }

    fun remainingPersistentApps(): List<String> {
        val pm = packageManager
        return active().filter { hideTarget(it) }.map { sbn ->
            if (isOverlayNotice(sbn)) "앱 위에 표시 중"
            else runCatching { pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString() }
                .getOrDefault(sbn.packageName)
        }.distinct()
    }
}
