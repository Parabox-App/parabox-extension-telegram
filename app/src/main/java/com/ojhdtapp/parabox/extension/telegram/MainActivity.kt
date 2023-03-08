package com.ojhdtapp.parabox.extension.telegram

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxActivity
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxKey
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxMetadata
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxResult
import com.ojhdtapp.parabox.extension.telegram.domain.service.ConnService
import com.ojhdtapp.parabox.extension.telegram.domain.util.CustomKey
import com.ojhdtapp.parabox.extension.telegram.domain.util.ServiceStatus
import com.ojhdtapp.parabox.extension.telegram.ui.main.MainScreen
import com.ojhdtapp.parabox.extension.telegram.ui.main.MainViewModel
import com.ojhdtapp.parabox.extension.telegram.ui.main.UiEvent
import com.ojhdtapp.parabox.extension.telegram.ui.theme.ParaboxExtensionExampleTheme
import com.ojhdtapp.parabox.extension.telegram.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ParaboxActivity<ConnService>(ConnService::class.java) {
    private val viewModel: MainViewModel by viewModels<MainViewModel>()

    private fun checkMainAppInstallation() {
        val pkg = "com.ojhdtapp.parabox"
        var res = false
        try {
            packageManager.getPackageInfo(pkg, PackageManager.GET_META_DATA)
            res = true
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        viewModel.setMainAppInstalled(res)
    }

    fun launchMainApp() {
        val pkg = "com.ojhdtapp.parabox"
        packageManager.getLaunchIntentForPackage(pkg)?.let {
            startActivity(it.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }

    override fun customHandleMessage(msg: Message, metadata: ParaboxMetadata) {
        when(msg.what){

        }
    }

    override fun onParaboxServiceConnected() {
        getState()
    }

    override fun onParaboxServiceDisconnected() {

    }

    override fun onParaboxServiceStateChanged(state: Int, message: String?) {
        val serviceState = when (state) {
            ParaboxKey.STATE_ERROR -> ServiceStatus.Error(
                message ?: getString(R.string.status_error)
            )

            ParaboxKey.STATE_LOADING -> ServiceStatus.Loading(
                message ?: getString(R.string.status_loading)
            )

            ParaboxKey.STATE_PAUSE -> ServiceStatus.Pause(
                message ?: getString(R.string.status_pause)
            )

            ParaboxKey.STATE_STOP -> ServiceStatus.Stop
            ParaboxKey.STATE_RUNNING -> ServiceStatus.Running(
                message ?: getString(R.string.status_running)
            )

            else -> ServiceStatus.Stop
        }
        Log.d("Parabox", "onParaboxServiceStateChanged: $serviceState")
        viewModel.updateServiceStatusStateFlow(serviceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Immersive Navigation Bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Check MainApp Installation
        checkMainAppInstallation()
        setContent {
            ParaboxExtensionExampleTheme {
                // Set Icons Color on Immersive Navigation Bar
                val systemUiController = rememberSystemUiController()
                val useDarkIcons = !isSystemInDarkTheme()
                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = Color.Transparent,
                        darkIcons = useDarkIcons
                    )
                }
                MainScreen(
                    viewModel = viewModel
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        bindParaboxService()
    }

    override fun onStop() {
        super.onStop()
        unbindParaboxService()
    }
}