package com.jinsung.noticlear

import android.service.notification.NotificationListenerService

class ClearListenerService : NotificationListenerService() {
    companion object {
        @Volatile var instance: ClearListenerService? = null
        private val SYSTEM_PKGS = setOf("android", "com.android.systemui")
        const val SNOOZE_MS = 12L * 60 * 60 * 1000   // 12시간
    }
    override fun onListenerConnected() { instance = this }
    override fun onListenerDisconnected() { instance = null }

    data class Result(val cleared: Int, val hidden: Int)

    fun clearAll(hidePersistent: Boolean): Result {
        val list = try { activeNotifications?.toList() ?: emptyList() } catch (e: Exception) { emptyList() }
        val cleared = list.count { it.isClearable }
        cancelAllNotifications()

        var hidden = 0
        if (hidePersistent) {
            for (sbn in list) {
                if (sbn.isClearable) continue
                if (sbn.packageName == packageName || sbn.packageName in SYSTEM_PKGS) continue
                runCatching { cancelNotification(sbn.key) }
                if (runCatching { snoozeNotification(sbn.key, SNOOZE_MS) }.isSuccess) hidden++
            }
        }
        return Result(cleared, hidden)
    }

    /** 아직 화면에 남아있는(숨기기 실패한) 다른 앱 고정 알림 앱 이름들 */
    fun remainingPersistentApps(): List<String> {
        val list = try { activeNotifications?.toList() ?: emptyList() } catch (e: Exception) { emptyList() }
        val pm = packageManager
        return list.filter { !it.isClearable && it.packageName != packageName && it.packageName !in SYSTEM_PKGS }
            .map { sbn ->
                runCatching { pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString() }
                    .getOrDefault(sbn.packageName)
            }.distinct()
    }
}
