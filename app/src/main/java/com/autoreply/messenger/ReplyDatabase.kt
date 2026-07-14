package com.autoreply.messenger

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ReplyDatabase(context: Context) :
    SQLiteOpenHelper(context, "autoreply.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sender TEXT NOT NULL,
                received_message TEXT,
                reply_message TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
        db.execSQL("DROP TABLE IF EXISTS history")
        onCreate(db)
    }

    fun insertHistory(sender: String, receivedMsg: String, replyMsg: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("sender", sender)
            put("received_message", receivedMsg)
            put("reply_message", replyMsg)
            put("timestamp", System.currentTimeMillis())
        }
        db.insert("history", null, values)
    }

    fun getAllHistory(): List<ReplyHistory> {
        val list = mutableListOf<ReplyHistory>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM history ORDER BY timestamp DESC", null
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    ReplyHistory(
                        id = it.getLong(0),
                        sender = it.getString(1),
                        receivedMessage = it.getString(2),
                        replyMessage = it.getString(3),
                        timestamp = it.getLong(4)
                    )
                )
            }
        }
        return list
    }

    fun getTodayCount(): Int {
        val db = readableDatabase
        val startOfDay = getStartOfDay()
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM history WHERE timestamp >= ?",
            arrayOf(startOfDay.toString())
        )
        cursor.use {
            if (it.moveToFirst()) return it.getInt(0)
        }
        return 0
    }

    fun getTotalCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM history", null)
        cursor.use {
            if (it.moveToFirst()) return it.getInt(0)
        }
        return 0
    }

    fun clearHistory() {
        writableDatabase.execSQL("DELETE FROM history")
    }

    private fun getStartOfDay(): Long {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}