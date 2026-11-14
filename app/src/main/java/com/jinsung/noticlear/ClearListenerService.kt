package com.jinsung.noticlear

import android.service.notification.NotificationListenerService

class ClearListenerService : NotificationListenerService() {
    companion object {
        @Volatile var instance: ClearListenerService? = null
    }
    override fun onListenerConnected() { instance = this }
    override fun onListenerDisconnected() { instance = null }

    /** 지울 수 있는 알림 전부 삭제. 지운 개수 반환 */
    fun clearAll(): Int {
        val before = try { activeNotifications?.count { it.isClearable } ?: 0 } catch (e: Exception) { 0 }
        cancelAllNotifications()
        return before
    }
}
