package com.ojhdtapp.parabox.extension.telegram.core.util

object IdUtil {
    fun Long.fromChatId(): Long {
        return "${if(this < 0) '1' else '2'}${Math.abs(this)}".toLong()
    }

    fun Long.toChatId(): Long {
        return when("$this".substring(0, 1)) {
            "1" -> "${this}".substring(1).toLong() * -1
            "2" -> "${this}".substring(1).toLong()
            else ->  throw Exception("Invalid chat id")
        }
    }
}