package com.autoreply.messenger

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("autoreply_settings", Context.MODE_PRIVATE)

    // Bật/tắt tự động phản hồi
    fun isEnabled(): Boolean = prefs.getBoolean("enabled", false)
    fun setEnabled(on: Boolean) = prefs.edit().putBoolean("enabled", on).apply()

    // Tin nhắn phản hồi
    fun getReplyMessage(): String = prefs.getString("reply_message", "Xin chào! Tôi hiện không thể trả lời tin nhắn. Sẽ phản hồi sớm nhất có thể.")!!
    fun setReplyMessage(msg: String) = prefs.edit().putString("reply_message", msg).apply()

    // Độ trễ (mili-giây)
    fun getDelayMs(): Long = prefs.getLong("delay_ms", 3000)
    fun setDelayMs(ms: Long) = prefs.edit().putLong("delay_ms", ms).apply()

    // Chế độ lọc liên hệ: "all", "whitelist", "blacklist"
    fun getFilterMode(): String = prefs.getString("filter_mode", "all")!!
    fun setFilterMode(mode: String) = prefs.edit().putString("filter_mode", mode).apply()

    // Danh sách liên hệ lọc
    fun getContactList(): Set<String> {
        val json = prefs.getString("contact_list", "[]")!!
        val set = mutableSetOf<String>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                set.add(arr.getString(i))
            }
        } catch (_: Exception) {
        }
        return set
    }

    fun setContactList(contacts: Set<String>) {
        val arr = JSONArray()
        contacts.forEach { arr.put(it) }
        prefs.edit().putString("contact_list", arr.toString()).apply()
    }

    fun addContact(name: String) {
        val list = getContactList().toMutableSet()
        list.add(name)
        setContactList(list)
    }

    fun removeContact(name: String) {
        val list = getContactList().toMutableSet()
        list.remove(name)
        setContactList(list)
    }

    // Phản hồi tin nhắn nhóm
    fun isGroupReplyEnabled(): Boolean = prefs.getBoolean("group_reply", false)
    fun setGroupReplyEnabled(on: Boolean) = prefs.edit().putBoolean("group_reply", on).apply()

    // Giới hạn thời gian giữa các lần phản hồi cho cùng 1 người (ms)
    fun getRateLimitMs(): Long = prefs.getLong("rate_limit_ms", 60000)
    fun setRateLimitMs(ms: Long) = prefs.edit().putLong("rate_limit_ms", ms).apply()
}