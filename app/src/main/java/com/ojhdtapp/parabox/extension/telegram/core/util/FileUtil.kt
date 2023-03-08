package com.ojhdtapp.parabox.extension.telegram.core.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.core.net.toFile
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

    fun getFilenameFromUri(context: Context, uri: Uri): String? {
        try {
            when (uri.scheme) {
                ContentResolver.SCHEME_FILE -> {
                    return uri.toFile().name
                }
                ContentResolver.SCHEME_CONTENT -> {
                    val cursor = context.contentResolver.query(
                        uri,
                        arrayOf(OpenableColumns.DISPLAY_NAME),
                        null,
                        null,
                        null
                    ) ?: throw Exception("Failed to obtain cursor from the content resolver")
                    cursor.moveToFirst()
                    if (cursor.count == 0) {
                        throw Exception("The given Uri doesn't represent any file")
                    }
                    val displayNameColumnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val displayName = cursor.getString(displayNameColumnIndex)
                    cursor.close()
                    return displayName
                }
                ContentResolver.SCHEME_ANDROID_RESOURCE -> {
                    // for uris like [android.resource://com.example.app/1234567890]
                    var resourceId = uri.lastPathSegment?.toIntOrNull()
                    if (resourceId != null) {
                        return context.resources.getResourceName(resourceId)
                    }
                    // for uris like [android.resource://com.example.app/raw/sample]
                    val packageName = uri.authority
                    val resourceType = if (uri.pathSegments.size >= 1) {
                        uri.pathSegments[0]
                    } else {
                        throw Exception("Resource type could not be found")
                    }
                    val resourceEntryName = if (uri.pathSegments.size >= 2) {
                        uri.pathSegments[1]
                    } else {
                        throw Exception("Resource entry name could not be found")
                    }
                    resourceId = context.resources.getIdentifier(
                        resourceEntryName,
                        resourceType,
                        packageName
                    )
                    return context.resources.getResourceName(resourceId)
                }
                else -> {
                    // probably a http uri
                    return toString().substringAfterLast("/")
                }
            }
        } catch (e: Exception) {
            return null
        }

    }
}