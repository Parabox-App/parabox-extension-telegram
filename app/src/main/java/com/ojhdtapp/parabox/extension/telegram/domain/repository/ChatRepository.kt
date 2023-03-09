package com.ojhdtapp.parabox.extension.telegram.domain.repository

import android.content.Context
import com.ojhdtapp.parabox.extension.telegram.domain.telegram.TelegramClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import org.drinkless.td.libcore.telegram.TdApi
import javax.inject.Inject

class ChatRepository @Inject constructor(
    val context: Context,
    val client: TelegramClient
){
    private fun getChatIds(offsetOrder: Long = Long.MAX_VALUE, limit: Int): Flow<LongArray> =
        callbackFlow {
            client.client.send(TdApi.GetChats(TdApi.ChatListMain(), limit)) {
                when (it.constructor) {
                    TdApi.Chats.CONSTRUCTOR -> {
                        trySend((it as TdApi.Chats).chatIds).isSuccess
                    }
                    TdApi.Error.CONSTRUCTOR -> {
                        error("")
                    }
                    else -> {
                        error("")
                    }
                }
                //close()
            }
            awaitClose { }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getChats(offsetOrder: Long = Long.MAX_VALUE, limit: Int): Flow<List<TdApi.Chat>> =
        getChatIds(offsetOrder, limit)
            .map { ids -> ids.map { getChat(it) } }
            .flatMapLatest { chatsFlow ->
                combine(chatsFlow) { chats ->
                    chats.toList()
                }
            }

    fun getChat(chatId: Long): Flow<TdApi.Chat> = callbackFlow {
        client.client.send(TdApi.GetChat(chatId)) {
            when (it.constructor) {
                TdApi.Chat.CONSTRUCTOR -> {
                    trySend(it as TdApi.Chat).isSuccess
                }
                TdApi.Error.CONSTRUCTOR -> {
                    error("Something went wrong")
                }
                else -> {
                    error("Something went wrong")
                }
            }
            //close()
        }
        awaitClose { }
    }

//    fun chatImage(chat: TdApi.Chat): Flow<String?> =
//        chat.photo?.small?.takeIf {
//            it.local?.isDownloadingCompleted == false
//        }?.id?.let { fileId ->
//            client.downloadFile(fileId).map { chat.photo?.small?.local?.path }
//        } ?: flowOf(chat.photo?.small?.local?.path)
}