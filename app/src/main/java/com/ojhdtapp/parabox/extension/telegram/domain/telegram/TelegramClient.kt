package com.ojhdtapp.parabox.extension.telegram.domain.telegram

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.ojhdtapp.parabox.extension.telegram.core.util.FileUtil
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.Image
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.MessageContent
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.PlainText
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import org.apache.commons.io.FileUtils
import javax.inject.Inject
import org.drinkless.td.libcore.telegram.*
import org.drinkless.td.libcore.telegram.TdApi.Chat
import org.drinkless.td.libcore.telegram.TdApi.StorageStatistics
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalCoroutinesApi::class)
class TelegramClient @Inject constructor(
    private val tdLibParameters: TdApi.TdlibParameters,
    val context: Context
) : Client.ResultHandler {

    private val TAG = TelegramClient::class.java.simpleName

    val client = Client.create(this, null) {
        it.printStackTrace()
        Log.d(TAG, "onError: $it")
    }

    private val _authState = MutableStateFlow(Authentication.UNKNOWN)
    val authState: StateFlow<Authentication> get() = _authState

    private var _resultHandler: ((TdApi.Object) -> Unit)? = null

    init {
        client.send(TdApi.SetLogVerbosityLevel(1), this)
        client.send(TdApi.GetAuthorizationState(), this)
    }

    fun setResultHandler(handler: (TdApi.Object) -> Unit) {
        _resultHandler = handler
    }

    fun open() {
//        client.send(TdApi.GetAuthorizationState(), this)
    }

    fun close() {
        _resultHandler = null
        client.close()
    }

    private val requestScope = CoroutineScope(Dispatchers.IO)

    private fun setAuth(auth: Authentication) {
        _authState.value = auth
    }

    override fun onResult(data: TdApi.Object) {
        Log.d(TAG, "onResult: ${data::class.java.simpleName}")
        when (data.constructor) {
            TdApi.UpdateAuthorizationState.CONSTRUCTOR -> {
                Log.d(TAG, "UpdateAuthorizationState")
                onAuthorizationStateUpdated((data as TdApi.UpdateAuthorizationState).authorizationState)
            }

            TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR -> {
                Log.d(TAG, "AuthorizationStateWaitTdlibParameters")
                client.send(TdApi.SetTdlibParameters(tdLibParameters)) {
                    Log.d(TAG, "SetTdlibParameters result: $it")
                    when (it.constructor) {
                        TdApi.Ok.CONSTRUCTOR -> {
                            //result.postValue(true)
                        }
                        TdApi.Error.CONSTRUCTOR -> {
                            //result.postValue(false)
                        }
                    }
                }
            }
            TdApi.UpdateOption.CONSTRUCTOR -> {

            }

            else -> {
                _resultHandler?.invoke(data)
//                Log.d(TAG, "Unhandled onResult call with data: $data.")
            }
        }
    }

    private fun doAsync(job: () -> Unit) {
        requestScope.launch { job() }
    }

    fun startAuthentication() {
        Log.d(TAG, "startAuthentication called")
        if (_authState.value != Authentication.UNAUTHENTICATED) {
            throw IllegalStateException("Start authentication called but client already authenticated. State: ${_authState.value}.")
        }

        doAsync {
            client.send(TdApi.SetTdlibParameters(tdLibParameters)) {
                Log.d(TAG, "SetTdlibParameters result: $it")
                when (it.constructor) {
                    TdApi.Ok.CONSTRUCTOR -> {
                        //result.postValue(true)
                    }
                    TdApi.Error.CONSTRUCTOR -> {
                        //result.postValue(false)
                    }
                }
            }
        }
    }

    fun loginOut() {
        Log.d(TAG, "loginOut called")
        client.send(TdApi.LogOut()) {
            Log.d(TAG, "loginOut result: $it")
            when (it.constructor) {
                TdApi.Ok.CONSTRUCTOR -> {
                    //result.postValue(true)
                }
                TdApi.Error.CONSTRUCTOR -> {
                    //result.postValue(false)
                }
            }
        }
    }

    suspend fun optimiseStorage(): StorageStatistics {
        return suspendCoroutine { cot ->
            client.send(TdApi.OptimizeStorage()){
                cot.resume(it as StorageStatistics)
            }
        }

    }

    fun insertPhoneNumber(phoneNumber: String) {
        Log.d("TelegramClient", "phoneNumber: $phoneNumber")
        val settings = TdApi.PhoneNumberAuthenticationSettings(
            false,
            false,
            false,
            false,
            null
        )
        client.send(TdApi.SetAuthenticationPhoneNumber(phoneNumber, settings)) {
            Log.d("TelegramClient", "phoneNumber. result: $it")
            when (it.constructor) {
                TdApi.Ok.CONSTRUCTOR -> {
                    Log.d("TelegramClient", "phoneNumber. result: OK")
                }
                TdApi.Error.CONSTRUCTOR -> {
                    Log.d("TelegramClient", "phoneNumber. result: Error")
                }
            }
        }
    }

    fun insertCode(code: String) {
        Log.d("TelegramClient", "code: $code")
        doAsync {
            client.send(TdApi.CheckAuthenticationCode(code)) {
                when (it.constructor) {
                    TdApi.Ok.CONSTRUCTOR -> {

                    }
                    TdApi.Error.CONSTRUCTOR -> {

                    }
                }
            }
        }
    }

    fun insertPassword(password: String) {
        Log.d("TelegramClient", "inserting password")
        doAsync {
            client.send(TdApi.CheckAuthenticationPassword(password)) {
                when (it.constructor) {
                    TdApi.Ok.CONSTRUCTOR -> {

                    }
                    TdApi.Error.CONSTRUCTOR -> {

                    }
                }
            }
        }
    }

    private fun onAuthorizationStateUpdated(authorizationState: TdApi.AuthorizationState) {
        when (authorizationState.constructor) {
            TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR -> {
                Log.d(
                    TAG,
                    "onResult: AuthorizationStateWaitTdlibParameters -> state = UNAUTHENTICATED"
                )
                setAuth(Authentication.UNAUTHENTICATED)
            }
            TdApi.AuthorizationStateWaitEncryptionKey.CONSTRUCTOR -> {
                Log.d(TAG, "onResult: AuthorizationStateWaitEncryptionKey")
                client.send(TdApi.CheckDatabaseEncryptionKey()) {
                    when (it.constructor) {
                        TdApi.Ok.CONSTRUCTOR -> {
                            Log.d(TAG, "CheckDatabaseEncryptionKey: OK")
                        }
                        TdApi.Error.CONSTRUCTOR -> {
                            Log.d(TAG, "CheckDatabaseEncryptionKey: Error")
                        }
                    }
                }
            }
            TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR -> {
                Log.d(TAG, "onResult: AuthorizationStateWaitPhoneNumber -> state = WAIT_FOR_NUMBER")
                setAuth(Authentication.WAIT_FOR_NUMBER)
            }
            TdApi.AuthorizationStateWaitCode.CONSTRUCTOR -> {
                Log.d(TAG, "onResult: AuthorizationStateWaitCode -> state = WAIT_FOR_CODE")
                setAuth(Authentication.WAIT_FOR_CODE)
            }
            TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR -> {
                Log.d(TAG, "onResult: AuthorizationStateWaitPassword")
                setAuth(Authentication.WAIT_FOR_PASSWORD)
            }
            TdApi.AuthorizationStateReady.CONSTRUCTOR -> {
                Log.d(TAG, "onResult: AuthorizationStateReady -> state = AUTHENTICATED")
                setAuth(Authentication.AUTHENTICATED)
            }
            TdApi.AuthorizationStateLoggingOut.CONSTRUCTOR -> {
                Log.d(TAG, "onResult: AuthorizationStateLoggingOut")
                setAuth(Authentication.UNAUTHENTICATED)
            }
            TdApi.AuthorizationStateClosing.CONSTRUCTOR -> {
                Log.d(TAG, "onResult: AuthorizationStateClosing")
            }
            TdApi.AuthorizationStateClosed.CONSTRUCTOR -> {
                Log.d(TAG, "onResult: AuthorizationStateClosed")
            }
            else -> Log.d(TAG, "Unhandled authorizationState with data: $authorizationState.")
        }
    }

    suspend fun getParaboxMessageContents(tdMessageContent: TdApi.MessageContent): List<MessageContent>? {
        return when (tdMessageContent.constructor) {
            TdApi.MessageText.CONSTRUCTOR -> {
                listOf(PlainText(text = (tdMessageContent as TdApi.MessageText).text.text))
            }
            TdApi.MessagePhoto.CONSTRUCTOR -> {
                getDownloadableFileUri((tdMessageContent as TdApi.MessagePhoto).photo.sizes[0].photo)
                    ?.let { uri ->
                        buildList<MessageContent> {
                            add(
                                Image(
                                    url = null,
                                    width = (tdMessageContent as TdApi.MessagePhoto).photo.sizes[0].width,
                                    height = (tdMessageContent as TdApi.MessagePhoto).photo.sizes[0].height,
                                    fileName = (tdMessageContent as TdApi.MessagePhoto).photo.sizes[0].photo.remote.uniqueId,
                                    uri = uri
                                )
                            )
                            if ((this as TdApi.MessagePhoto).caption.text.isNotEmpty()) {
                                add(PlainText(text = (tdMessageContent as TdApi.MessagePhoto).caption.text))
                            }
                        }
                    }
            }
            else -> null
        }
    }

    suspend fun getUserInfo(userId: Long): TdApi.User? {
        return suspendCoroutine<TdApi.User?> { cot ->
            client.send(TdApi.GetUser(userId)) {
                if (it.constructor == TdApi.Error.CONSTRUCTOR) {
                    cot.resume(null)
                } else {
                    cot.resume(it as TdApi.User)
                }
            }
        }
    }

    suspend fun getUserProfilePhotos(userId: Long): TdApi.ChatPhotos? {
        return suspendCoroutine { cot ->
            client.send(TdApi.GetUserProfilePhotos(userId, 0, 1)) {
                if (it.constructor == TdApi.Error.CONSTRUCTOR) {
                    cot.resumeWithException(Exception((it as TdApi.Error).message))
                } else {
                    cot.resume(it as TdApi.ChatPhotos)
                }
            }
        }
    }

    suspend fun getChatInfo(chatId: Long): TdApi.Chat? {
        return suspendCoroutine { cot ->
            client.send(TdApi.GetChat(chatId)) {
                if (it.constructor == TdApi.Error.CONSTRUCTOR) {
                    cot.resumeWithException(Exception((it as TdApi.Error).message))
                } else {
                    cot.resume(it as Chat)
                }
            }
        }

    }

    suspend fun getDownloadableFileUri(file: TdApi.File): Uri? {
        return coroutineScope {
            downloadableFile(file).firstOrNull()?.let {
//                FileUtils.copyFileToDirectory(srcFile = File(it), desFile = FileUtils.getTempDirectory())
                FileUtil.getUriOfFile(context, File(it)).apply {
                    Log.d(TAG, "URI:${this}")
                    context.grantUriPermission(
                        "com.ojhdtapp.parabox",
                        this,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
        }
    }

    fun downloadableFile(file: TdApi.File): Flow<String?> =
        file.takeIf {
            it.local?.isDownloadingCompleted == false
        }?.id?.let { fileId ->
            downloadFile(fileId).map { file.local?.path }
        } ?: flowOf(file.local?.path)

    fun downloadFile(fileId: Int): Flow<Unit> = callbackFlow {
        client.send(TdApi.DownloadFile(fileId, 1, 0, 0, true)) {
            when (it.constructor) {
                TdApi.Ok.CONSTRUCTOR -> {
                    trySend(Unit).isSuccess
                }
                else -> {
                    cancel("", Exception(""))
                }
            }
        }
        awaitClose()
    }

    fun sendAsFlow(query: TdApi.Function): Flow<TdApi.Object> = callbackFlow {
        client.send(query) {
            when (it.constructor) {
                TdApi.Error.CONSTRUCTOR -> {
                    error("")
                }
                else -> {
                    trySend(it).isSuccess
                }
            }
            //close()
        }
        awaitClose { }
    }

    inline fun <reified T : TdApi.Object> send(query: TdApi.Function): Flow<T> =
        sendAsFlow(query).map { it as T }
}