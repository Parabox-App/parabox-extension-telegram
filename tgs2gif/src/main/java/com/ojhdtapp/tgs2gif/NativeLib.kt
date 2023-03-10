package com.ojhdtapp.tgs2gif

class NativeLib {

    /**
     * A native method that is implemented by the 'tgs2gif' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    external fun tgs2gif(tgsPath: String, gifPath: String, width: Int, height: Int, fps: Int, quality: Int): Int

    companion object {
        // Used to load the 'tgs2gif' library on application startup.
        init {
            System.loadLibrary("tgs2gif")
        }
    }
}