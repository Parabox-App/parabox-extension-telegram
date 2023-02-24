package com.parabox.example.domain.service

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
import com.parabox.example.core.util.DataStoreKeys
import com.parabox.example.core.util.NotificationUtil
import com.parabox.example.core.util.dataStore
import com.parabox.example.domain.util.CustomKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ConnService : ParaboxService() {
    companion object {
        var connectionType = 0
    }

    private fun receiveTestMessage(msg: Message, metadata: ParaboxMetadata) {
        // TODO 11 : Receive Message
        val content = (msg.obj as Bundle).getString("content") ?: "No content"
        val profile = Profile(
            name = "anonymous",
            avatar = "https://gravatar.loli.net/avatar/0c13fa7156f734513afeb1d4a965c219?d=mp&v=1.5.1",
            id = 1L,
            avatarUri = null
        )
        receiveMessage(
            ReceiveMessageDto(
                contents = listOf(PlainText(text = content)),
                profile = profile,
                subjectProfile = profile,
                timestamp = System.currentTimeMillis(),
                messageId = null,
                pluginConnection = PluginConnection(
                    connectionType = connectionType,
                    sendTargetType = SendTargetType.USER,
                    id = 1L
                )
            ),
            onResult = {
                // TODO 7 : Call sendCommandResponse when the job is done
                if (it is ParaboxResult.Success) {
                    sendCommandResponse(
                        isSuccess = true,
                        metadata = metadata,
                        extra = Bundle().apply {
                            putString(
                                "message",
                                "Message received at ${System.currentTimeMillis()}"
                            )
                        }
                    )
                } else {
                    sendCommandResponse(
                        isSuccess = false,
                        metadata = metadata,
                        errorCode = (it as ParaboxResult.Fail).errorCode
                    )
                }
            }
        )
    }

    // TODO 9 : Call sendNotification function with NOTIFICATION_SHOW_TEST_MESSAGE_SNACKBAR
    private fun showTestMessageSnackbar(message: String) {
        sendNotification(CustomKey.NOTIFICATION_SHOW_TEST_MESSAGE_SNACKBAR, Bundle().apply {
            putString("message", message)
        })
    }

    override fun customHandleMessage(msg: Message, metadata: ParaboxMetadata) {
        when (msg.what) {
            // TODO 6: Handle custom command
            CustomKey.COMMAND_RECEIVE_TEST_MESSAGE -> {
                receiveTestMessage(msg, metadata)
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
        val contentString = dto.contents.getContentString()
        showTestMessageSnackbar(contentString)
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

//            TODO 3: Delete the code below, and write your own startup process
            updateServiceState(ParaboxKey.STATE_LOADING, "Step A")
            delay(1000)
            updateServiceState(ParaboxKey.STATE_LOADING, "Step B")
            delay(1000)
            updateServiceState(ParaboxKey.STATE_PAUSE, "Step C")
            delay(1000)
            updateServiceState(ParaboxKey.STATE_LOADING, "Step D")
            delay(1000)
            updateServiceState(ParaboxKey.STATE_RUNNING, "Step E")
        }

    }

    override fun onStateUpdate(state: Int, message: String?) {

    }

    override fun onStopParabox() {
        NotificationUtil.stopForegroundService(this)
        updateServiceState(ParaboxKey.STATE_STOP)
    }

    override fun onCreate() {
        connectionType = packageManager.getApplicationInfo(
            this@ConnService.packageName,
            PackageManager.GET_META_DATA
        ).metaData.getInt("connection_type")
        super.onCreate()
    }

}