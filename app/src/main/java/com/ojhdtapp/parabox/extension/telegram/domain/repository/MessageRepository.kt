package com.ojhdtapp.parabox.extension.telegram.domain.repository

import android.content.Context
import com.ojhdtapp.parabox.extension.telegram.domain.telegram.TelegramClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.drinkless.td.libcore.telegram.TdApi
import javax.inject.Inject

class MessageRepository @Inject constructor(
    val context: Context,
    val client: TelegramClient
    ) {
    fun getMessages(chatId: Long, fromMessageId: Long, limit: Int): Flow<List<TdApi.Message>> =
        callbackFlow {
            client.client.send(TdApi.GetChatHistory(chatId, fromMessageId, 0, limit, false)) {
                when (it.constructor) {
                    TdApi.Messages.CONSTRUCTOR -> {
                        trySend((it as TdApi.Messages).messages.toList()).isSuccess
                    }
                    TdApi.Error.CONSTRUCTOR -> {
                        error("")
                    }
                    else -> {
                        error("")
                    }
                }
            }
            awaitClose { }
        }

//    fun getMessagesPaged(chatId: Long): PagingSource<Long, TdApi.Message> =
//        MessagesPagingSource(chatId, this)

    fun getMessage(chatId: Long, messageId: Long): Flow<TdApi.Message> = callbackFlow {
        client.client.send(TdApi.GetMessage(chatId, messageId)) {
            when (it.constructor) {
                TdApi.Message.CONSTRUCTOR -> {
                    trySend(it as TdApi.Message).isSuccess
                }
                TdApi.Error.CONSTRUCTOR -> {
                    error("Something went wrong")
                }
                else -> {
                    error("Something went wrong")
                }
            }
        }
        awaitClose { }
    }

    fun sendMessage(
        chatId: Long,
        messageThreadId: Long = 0,
        replyToMessageId: Long = 0,
        options: TdApi.MessageSendOptions = TdApi.MessageSendOptions(),
        inputMessageContent: TdApi.InputMessageContent
    ): Deferred<TdApi.Message> = sendMessage(
        TdApi.SendMessage(
            chatId,
            messageThreadId,
            replyToMessageId,
            options,
            null,
            inputMessageContent
        )
    )

    fun sendMessage(sendMessage: TdApi.SendMessage): Deferred<TdApi.Message> {
        val result = CompletableDeferred<TdApi.Message>()
        client.client.send(sendMessage) {
            when (it.constructor) {
                TdApi.Message.CONSTRUCTOR -> {
                    result.complete(it as TdApi.Message)
                }
                else -> {
                    result.completeExceptionally(error("Something went wrong"))
                }
            }
        }
        return result
    }
}