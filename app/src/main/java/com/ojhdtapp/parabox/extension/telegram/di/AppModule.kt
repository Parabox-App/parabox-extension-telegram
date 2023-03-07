package com.ojhdtapp.parabox.extension.telegram.di

import android.content.Context
import android.os.Build
import com.ojhdtapp.parabox.extension.telegram.R
import com.ojhdtapp.parabox.extension.telegram.domain.repository.ChatRepository
import com.ojhdtapp.parabox.extension.telegram.domain.repository.MessageRepository
import com.ojhdtapp.parabox.extension.telegram.domain.telegram.TelegramClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.drinkless.td.libcore.telegram.TdApi
import java.util.*
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideTdlibParameters(@ApplicationContext context: Context): TdApi.TdlibParameters {
        return TdApi.TdlibParameters().apply {
            // Obtain application identifier hash for Telegram API access at https://my.telegram.org
            apiId = context.resources.getInteger(R.integer.telegram_api_id)
            apiHash = context.getString(R.string.telegram_api_hash)
            useMessageDatabase = true
            useSecretChats = true
            systemLanguageCode = Locale.getDefault().language
            databaseDirectory = context.filesDir.absolutePath
            deviceModel = Build.MODEL
            systemVersion = Build.VERSION.RELEASE
            applicationVersion = "0.1"
            enableStorageOptimizer = true
        }
    }

    @Provides
    @Singleton
    fun provideTelegramClient(parameters: TdApi.TdlibParameters) = TelegramClient(parameters)

    @Provides
    @Singleton
    fun provideMessageRepository(
        @ApplicationContext context: Context,
        telegramClient: TelegramClient
    ) = MessageRepository(context, telegramClient)

    @Provides
    @Singleton
    fun provideChatsRepository(
        @ApplicationContext context: Context,
        client: TelegramClient
    ) = ChatRepository(context, client)
}