package com.ojhdtapp.parabox.extension.telegram.domain.service

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Message
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
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
                        updateServiceState(ParaboxKey.STATE_RUNNING, "服务正在运行")
                    }
                }
            }.launchIn(lifecycleScope)
            if (client.authState.value == Authentication.UNAUTHENTICATED) {
                client.startAuthentication()
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