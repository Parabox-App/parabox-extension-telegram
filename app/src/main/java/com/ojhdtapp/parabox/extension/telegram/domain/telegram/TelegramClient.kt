package com.ojhdtapp.parabox.extension.telegram.domain.telegram

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.ojhdtapp.parabox.extension.telegram.core.util.AxrLottieUtil
import com.ojhdtapp.parabox.extension.telegram.core.util.FileUtil
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.Audio
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.Image
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.MessageContent
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.PlainText
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import org.drinkless.td.libcore.telegram.*
import org.drinkless.td.libcore.telegram.TdApi.Chat
import org.drinkless.td.libcore.telegram.TdApi.MessageAnimatedEmoji
import org.drinkless.td.libcore.telegram.TdApi.MessageSticker
import org.drinkless.td.libcore.telegram.TdApi.StorageStatistics
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalCoroutinesApi::class)
class TelegramClient @Inject constructor(
    private val tdLibParameters: TdApi.TdlibParameters,
    private val axrLottieUtil: AxrLottieUtil,
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
    private var _currentUser: TdApi.User? = null
    val currentUser get() = _currentUser

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

    fun getCurrentUser(){
        client.send(TdApi.GetMe()) { _currentUser = (it as TdApi.User)}
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
                            getCurrentUser()
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

    suspend fun logOut(): Boolean {
        return suspendCoroutine<Boolean> { cot ->
            client.send(TdApi.LogOut()) {
                Log.d(TAG, "logOut result: $it")
                when (it.constructor) {
                    TdApi.Ok.CONSTRUCTOR -> {
                        cot.resume(true)
                    }
                    else -> {
                        cot.resume(false)
                    }
                }
            }
        }

    }

    suspend fun optimiseStorage(): StorageStatistics {
        return suspendCoroutine { cot ->
            client.send(TdApi.OptimizeStorage()) {
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

    suspend fun getParaboxMessageContents(message: TdApi.Message): List<MessageContent>? {
        val tdMessageContent = message.content
        return when (tdMessageContent.constructor) {
            TdApi.MessageText.CONSTRUCTOR -> {
                listOf(PlainText(text = (tdMessageContent as TdApi.MessageText).text.text))
            }
            TdApi.MessagePhoto.CONSTRUCTOR -> {
                downloadableFile((tdMessageContent as TdApi.MessagePhoto).photo.sizes.last().photo)
                    ?.let { path ->
                        val fileName = FileUtil.getFilenameFromPath(path)
                        val extension = FileUtil.getExtensionFromFilename(fileName)
                        val uri = getPermissionGrantedUri(path)
                        buildList<MessageContent> {
                            add(
                                Image(
                                    url = null,
                                    width = (tdMessageContent as TdApi.MessagePhoto).photo.sizes.last().width,
                                    height = (tdMessageContent as TdApi.MessagePhoto).photo.sizes.last().height,
                                    fileName = fileName,
                                    uri = uri
                                )
                            )
                            if ((tdMessageContent as TdApi.MessagePhoto).caption.text.isNotBlank()) {
                                add(PlainText(text = (tdMessageContent as TdApi.MessagePhoto).caption.text))
                            }
                        }
                    }
            }
            TdApi.MessageSticker.CONSTRUCTOR -> {
                downloadableFile((tdMessageContent as MessageSticker).sticker.sticker)
                    ?.let { path ->
                        val fileName = FileUtil.getFilenameFromPath(path)
                        val extension = FileUtil.getExtensionFromFilename(fileName)
                        val imagePath = if(extension == "tgs"){
                            axrLottieUtil.lottie2Gif(path, File(context.externalCacheDir, "$fileName.gif").absolutePath, 200)
                        } else path
                        val uri = imagePath?.let { getPermissionGrantedUri(it) }
                        listOf(
                            Image(
                                url = null,
                                width = (tdMessageContent as MessageSticker).sticker.width,
                                height = (tdMessageContent as MessageSticker).sticker.height,
                                fileName = fileName,
                                uri = uri
                            )
                        )
                    }
            }
            TdApi.MessageAnimatedEmoji.CONSTRUCTOR -> {
                downloadableFile((tdMessageContent as MessageAnimatedEmoji).animatedEmoji.sticker.sticker)
                    ?.let { path ->
                        val fileName = FileUtil.getFilenameFromPath(path)
                        val extension = FileUtil.getExtensionFromFilename(fileName)
                        val imagePath = if(extension == "tgs"){
                            axrLottieUtil.lottie2Gif(path, File(context.externalCacheDir, "$fileName.gif").absolutePath, 200)
                        } else path
                        val uri = imagePath?.let { getPermissionGrantedUri(it) }
                        listOf(
                            Image(
                                url = null,
                                width = (tdMessageContent as MessageAnimatedEmoji).animatedEmoji.sticker.width,
                                height = (tdMessageContent as MessageAnimatedEmoji).animatedEmoji.sticker.height,
                                fileName = fileName,
                                uri = uri
                            )
                        )
                    }
            }
            TdApi.MessageVoiceNote.CONSTRUCTOR -> {
                getDownloadableFileUri((tdMessageContent as TdApi.MessageVoiceNote).voiceNote.voice)?.let{
                    val fileName = FileUtil.getFilenameFromUri(context, it)
                    buildList<MessageContent> {
                        Audio(
                            url = null,
                            length = (tdMessageContent as TdApi.MessageVoiceNote).voiceNote.duration.toLong() * 1000,
                            fileName = fileName,
                            fileSize = (tdMessageContent as TdApi.MessageVoiceNote).voiceNote.voice.size.toLong(),
                            uri = it,
                        )
                        if((tdMessageContent as TdApi.MessageVoiceNote).caption.text.isNotBlank()){
                            add(PlainText(text = (tdMessageContent as TdApi.MessageVoiceNote).caption.text))
                        }
                    }
                }
            }
            TdApi.MessageVideo.CONSTRUCTOR -> {
                getDownloadableFileUri((tdMessageContent as TdApi.MessageVideo).video.video)?.let{
                    val fileName = (tdMessageContent as TdApi.MessageVideo).video.fileName
                    buildList<MessageContent> {
                        com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.File(
                            url = null,
                            name = fileName,
                            extension = FileUtil.getExtensionFromFilename(fileName),
                            size = (tdMessageContent as TdApi.MessageVideo).video.video.size.toLong(),
                            lastModifiedTime = message.date.toLong() * 1000,
                            uri = it,
                        )
                        if((tdMessageContent as TdApi.MessageVideo).caption.text.isNotBlank()){
                            add(PlainText(text = (tdMessageContent as TdApi.MessageVideo).caption.text))
                        }
                    }
                }
            }
            TdApi.MessageDocument.CONSTRUCTOR -> {
                getDownloadableFileUri((tdMessageContent as TdApi.MessageDocument).document.document)?.let{
                    val fileName = (tdMessageContent as TdApi.MessageDocument).document.fileName
                    buildList<MessageContent> {
                        com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.File(
                            url = null,
                            name = fileName,
                            extension = FileUtil.getExtensionFromFilename(fileName),
                            size = (tdMessageContent as TdApi.MessageDocument).document.document.size.toLong(),
                            lastModifiedTime = message.date.toLong() * 1000,
                            uri = it,
                        )
                        if((tdMessageContent as TdApi.MessageDocument).caption.text.isNotBlank()){
                            add(PlainText(text = (tdMessageContent as TdApi.MessageDocument).caption.text))
                        }
                    }
                }
            }
            TdApi.MessageDice.CONSTRUCTOR, TdApi.MessageCall.CONSTRUCTOR, TdApi.MessageUnsupported.CONSTRUCTOR, TdApi.MessageAnimation.CONSTRUCTOR -> {
                listOf(PlainText(text = "Unsupported message type.(${tdMessageContent.constructor}})"))
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

    fun getPermissionGrantedUri(path: String): Uri?{
        return FileUtil.getUriOfFile(context, File(path)).apply {
            Log.d(TAG, "URI:${this}")
            context.grantUriPermission(
                "com.ojhdtapp.parabox",
                this,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }


    suspend fun getDownloadableFileUri(file: TdApi.File): Uri? {
        return downloadableFile(file)?.let {
//                FileUtils.copyFileToDirectory(srcFile = File(it), desFile = FileUtils.getTempDirectory())
            getPermissionGrantedUri(it)
        }?: throw Exception("URI null")
    }

    suspend fun downloadableFile(file: TdApi.File): String? =
        if(file.local?.isDownloadingCompleted == true){
            file.local?.path
        } else {
            file.id?.let { fileId ->
                downloadFile(fileId)?.local?.path
            }
        }

    suspend fun downloadFile(fileId: Int): TdApi.File? =
        suspendCoroutine { cot ->
            try{
                client.send(TdApi.DownloadFile(fileId, 1, 0, 0, true)) {
                    when (it.constructor) {
                        TdApi.File.CONSTRUCTOR -> {
                            cot.resume(it as TdApi.File)
                        }
                        else -> {
                            cot.resume(null)
                        }
                    }
                }
            }catch (e: Exception){
                e.printStackTrace()
                cot.resume(null)
            }
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
        client.send(sendMessage) {
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