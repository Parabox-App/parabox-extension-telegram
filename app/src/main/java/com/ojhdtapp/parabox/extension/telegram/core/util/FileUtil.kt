package com.ojhdtapp.parabox.extension.telegram.core.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.ojhdtapp.parabox.extension.telegram.BuildConfig
import java.io.File

object FileUtil {
    fun getUriOfFile(context: Context, file: File): Uri? {
        return try {
            FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID + ".provider", file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}