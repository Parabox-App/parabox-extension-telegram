package com.ojhdtapp.parabox.extension.telegram.domain.service

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Message
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxKey
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxMetadata
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxService
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.PluginConnection
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.Profile
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.ReceiveMessageDto
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendMessageDto
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendTargetType
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.PlainText
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.getContentString
import com.ojhdtapp.parabox.extension.telegram.core.util.DataStoreKeys
import com.ojhdtapp.parabox.extension.telegram.core.util.NotificationUtil
import com.ojhdtapp.parabox.extension.telegram.core.util.dataStore
import com.ojhdtapp.parabox.extension.telegram.domain.telegram.Authentication
import com.ojhdtapp.parabox.extension.telegram.domain.telegram.TelegramClient
import com.ojhdtapp.parabox.extension.telegram.domain.util.CustomKey
import com.ojhdtapp.parabox.extension.telegram.ui.main.LoginState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.drinkless.td.libcore.telegram.TdApi
import org.drinkless.td.libcore.telegram.TdApi.ChatType
import org.drinkless.td.libcore.telegram.TdApi.ChatTypeBasicGroup
import org.drinkless.td.libcore.telegram.TdApi.ChatTypePrivate
import org.drinkless.td.libcore.telegram.TdApi.ChatTypeSecret
import org.drinkless.td.libcore.telegram.TdApi.ChatTypeSupergroup
import org.drinkless.td.libcore.telegram.TdApi.MessageSender
import org.drinkless.td.libcore.telegram.TdApi.MessageSenderChat
import org.drinkless.td.libcore.telegram.TdApi.MessageSenderUser
import javax.inject.Inject

@AndroidEntryPoint
class ConnService : ParaboxService() {
    companion object {
        var connectionType = 0
    }

    @Inject
    lateinit var client: TelegramClient

    override fun customHandleMessage(msg: Message, metadata: ParaboxMetadata) {
        when (msg.what) {
            // TODO 6: Handle custom command
            CustomKey.COMMAND_RECEIVE_TEST_MESSAGE -> {
            }
        }
    }

    override fun onMainAppLaunch() {
        // Auto Login
        if (getServiceState() == ParaboxKey.STATE_STOP) {
            lifecycleScope.launch {
                val isAutoLoginEnabled =
                    dataStore.data.first()[DataStoreKeys.AUTO_LOGIN] ?: false
                if (isAutoLoginEnabled) {
                    onStartParabox()
                }
            }
        }
    }

    override suspend fun onRecallMessage(messageId: Long): Boolean {
        return true
    }

    override fun onRefreshMessage() {

    }

    override suspend fun onSendMessage(dto: SendMessageDto): Boolean {
        return true
    }

    override fun onStartParabox() {
        lifecycleScope.launch {
            // Foreground Service
            val isForegroundServiceEnabled =
                dataStore.data.first()[DataStoreKeys.FOREGROUND_SERVICE] ?: false
            if (isForegroundServiceEnabled) {
                NotificationUtil.startForegroundService(this@ConnService)
            }
            client.authState.onEach {
                Log.d("ConnService", "authState: $it")
                when (it) {
                    Authentication.UNAUTHENTICATED, Authentication.UNKNOWN -> {
                        updateServiceState(ParaboxKey.STATE_LOADING, "正在尝试登录")
                    }
                    Authentication.WAIT_FOR_NUMBER -> {
                        updateServiceState(ParaboxKey.STATE_PAUSE, "请输入手机号码")
                    }
                    Authentication.WAIT_FOR_CODE -> {
                        updateServiceState(ParaboxKey.STATE_PAUSE, "请输入验证码")
                    }
                    Authentication.WAIT_FOR_PASSWORD -> {
                        updateServiceState(ParaboxKey.STATE_PAUSE, "请输入密码")
                    }
                    Authentication.AUTHENTICATED -> {
                        updateServiceState(ParaboxKey.STATE_RUNNING, "TDLib 1.8.0")
                    }
                }
            }.launchIn(lifecycleScope)
            if (client.authState.value == Authentication.UNAUTHENTICATED) {
                client.startAuthentication()
            } else {
                client.open()
                client.setResultHandler {
                    lifecycleScope.launch(Dispatchers.IO) {
                        when (it.constructor) {
                            TdApi.UpdateNewMessage.CONSTRUCTOR -> {
                                (it as TdApi.UpdateNewMessage).also {
                                    handleNewMessage(it)
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    private fun handleNewMessage(obj: TdApi.UpdateNewMessage) {
        Log.d("parabox", obj.toString())
        lifecycleScope.launch(Dispatchers.IO) {
            val contents = client.getParaboxMessageContents(obj.message.content)
            val senderId = when(obj.message.senderId.constructor){
                MessageSenderUser.CONSTRUCTOR -> (obj.message.senderId as MessageSenderUser).userId
                MessageSenderChat.CONSTRUCTOR -> (obj.message.senderId as MessageSenderChat).chatId
                else -> null
            }
            val sender = senderId?.let { client.getUserInfo(it) }
            val chat = client.getChatInfo(obj.message.chatId)
            if (sender == null || chat == null || contents == null) return@launch
            val senderProfile = sender.let {
                Profile(
                    name = it.username.ifBlank { "${it.firstName} ${it.lastName}" }.ifBlank { "${it.id}" },
                    avatarUri = it.profilePhoto?.small?.let { it1 ->
                        client.getDownloadableFileUri(
                            it1
                        )
                    },
                    id = Math.abs(it.id),
                    avatar = null
                )
            }
            val chatProfile = chat.let {
                Profile(
                    name = it.title,
                    avatarUri = it.photo?.small?.let { it1 ->
                        client.getDownloadableFileUri(
                            it1
                        )
                    },
                    id = Math.abs(it.id),
                    avatar = null,
                )
            }
            val type = when (chat.type?.constructor) {
                ChatTypePrivate.CONSTRUCTOR, ChatTypeSecret.CONSTRUCTOR -> SendTargetType.USER
                ChatTypeBasicGroup.CONSTRUCTOR, ChatTypeSupergroup.CONSTRUCTOR -> SendTargetType.GROUP
                else -> SendTargetType.USER
            }
            val pluginConnection = PluginConnection(
                connectionType = connectionType,
                sendTargetType = type,
                Math.abs(chat.id)
            )
            val dto = ReceiveMessageDto(
                contents = contents,
                profile = senderProfile,
                subjectProfile = chatProfile,
                timestamp = obj.message.date.toLong() * 1000,
                messageId = obj.message.id,
                pluginConnection = pluginConnection
            )
            receiveMessage(dto) {

            }
        }
    }

    override fun onStateUpdate(state: Int, message: String?) {

    }

    override fun onStopParabox() {
        NotificationUtil.stopForegroundService(this)
        updateServiceState(ParaboxKey.STATE_STOP)
        client.close()
    }

    override fun onCreate() {
        connectionType = packageManager.getApplicationInfo(
            this@ConnService.packageName,
            PackageManager.GET_META_DATA
        ).metaData.getInt("connection_type")
        super.onCreate()
    }

}