package com.ojhdtapp.parabox.extension.telegram.core.util

import android.content.Context
import android.graphics.Color
import android.util.Log
import com.aghajari.rlottie.AXrLottie
import com.aghajari.rlottie.AXrLottie2Gif
import com.aghajari.rlottie.AXrLottieDrawable
import com.aghajari.rlottie.extension.GZipFileExtension
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AxrLottieUtil(context: Context) {
    companion object{
        const val TAG = "AxrLottieUtil"
    }
    init {
        AXrLottie.init(context)
        AXrLottie.addFileExtension(GZipFileExtension(".tgs"))
    }

    suspend fun lottie2Gif(tar: String, des: String, gifSize: Int): String?{
        return suspendCoroutine<String?> { cot ->
            val lottieDrawable = AXrLottieDrawable.fromPath(tar).build()
            AXrLottie2Gif.create(lottieDrawable)
                .setListener(object : AXrLottie2Gif.Lottie2GifListener {
                    var start: Long = 0
                    override fun onStarted() {
                        start = System.currentTimeMillis()
                    }

                    override fun onProgress(frame: Int, totalFrame: Int) {
                        Log.d(TAG ,"progress : $frame/$totalFrame")
                    }

                    override fun onFinished() {
                        cot.resume(des)
                        Log.d(TAG,
                            """
                GIF created (${System.currentTimeMillis() - start}ms)
                Resolution : ${gifSize}x$gifSize
                Path : ${des}
                File Size : ${File(des).length() / 1024}kb
                """.trimIndent()
                        )
                    }
                })
                .setBackgroundColor(Color.WHITE)
                .setOutputPath(des)
                .setSize(gifSize, gifSize)
                .setBackgroundTask(true)
                .setDithering(false)
                .setDestroyable(true)
                .build()
        }
    }
}