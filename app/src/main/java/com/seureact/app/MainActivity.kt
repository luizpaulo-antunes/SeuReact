package com.seureact.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.seureact.app.ui.MainViewModel
import com.seureact.app.ui.MainViewModelFactory
import com.seureact.app.ui.ReactionApp
import com.seureact.app.ui.theme.SeuReactTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel> {
        val app = application as ReactionApplication
        MainViewModelFactory(app, app.container)
    }
    private var permissionGrantedAction: (() -> Unit)? = null
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants[Manifest.permission.CAMERA] == true && grants[Manifest.permission.RECORD_AUDIO] == true) {
            permissionGrantedAction?.invoke()
        }
        permissionGrantedAction = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SeuReactTheme {
                ReactionApp(
                    viewModel = viewModel,
                    requestCapturePermissions = { action ->
                        permissionGrantedAction = action
                        val permissions = buildList {
                            add(Manifest.permission.CAMERA)
                            add(Manifest.permission.RECORD_AUDIO)
                            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        permissionLauncher.launch(permissions.toTypedArray())
                    },
                )
            }
        }
    }
}
