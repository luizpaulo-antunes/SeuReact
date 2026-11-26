package com.seureact.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.seureact.app.domain.model.DualCameraGeometry
import com.seureact.app.domain.model.DualCameraPipTransform
import com.seureact.app.media.capture.DualCameraCaptureController
import com.seureact.app.media.capture.DualCameraUnavailableException
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private enum class DualCameraPhase {
    PERMISSION_REQUIRED,
    PERMISSION_DENIED,
    PREPARING,
    READY,
    STARTING,
    RECORDING,
    FINALIZING,
    SAVED,
    UNSUPPORTED,
    ERROR,
}

@Composable
fun DualCameraScreen(
    onBack: () -> Unit,
    requestCapturePermissions: ((Boolean) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val view = LocalView.current
    val activity = context as? android.app.Activity
    val controller = remember { DualCameraCaptureController(context.applicationContext) }
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }
    fun hasPermissions(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    var permissionsGranted by remember { mutableStateOf(hasPermissions()) }
    var phase by remember {
        mutableStateOf(if (permissionsGranted) DualCameraPhase.PREPARING else DualCameraPhase.PERMISSION_REQUIRED)
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var nonBlockingMessage by remember { mutableStateOf<String?>(null) }
    var savedUri by remember { mutableStateOf<Uri?>(null) }
    var retryKey by remember { mutableIntStateOf(0) }
    var pipTransform by remember { mutableStateOf(DualCameraPipTransform()) }
    var frontIsPip by remember { mutableStateOf(true) }
    var canvasWidthPx by remember { mutableIntStateOf(1) }
    var canvasHeightPx by remember { mutableIntStateOf(1) }
    var recordingStartedAt by remember { mutableLongStateOf(0L) }
    var elapsedMillis by remember { mutableLongStateOf(0L) }
    var confirmStop by remember { mutableStateOf(false) }
    var leaveAfterFinalize by remember { mutableStateOf(false) }
    var chromeVisible by remember { mutableStateOf(true) }

    fun applyPip(proposed: DualCameraPipTransform) {
        runCatching { controller.updatePip(proposed) }
            .onSuccess { pipTransform = it }
            .onFailure {
                val message = it.message ?: "Não foi possível reposicionar a câmera."
                if (phase == DualCameraPhase.RECORDING || phase == DualCameraPhase.STARTING) {
                    nonBlockingMessage = "$message A gravação continua com a última posição válida."
                } else {
                    errorMessage = message
                    phase = DualCameraPhase.ERROR
                }
            }
    }

    fun stopRecording(exitAfter: Boolean = false) {
        if (phase == DualCameraPhase.RECORDING || phase == DualCameraPhase.STARTING) {
            confirmStop = false
            leaveAfterFinalize = exitAfter
            phase = DualCameraPhase.FINALIZING
            chromeVisible = true
            controller.stop()
        }
    }

    fun startRecording() {
        if (phase != DualCameraPhase.READY) return
        phase = DualCameraPhase.STARTING
        errorMessage = null
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        runCatching {
            controller.start(
                onStarted = {
                    if (phase == DualCameraPhase.STARTING) {
                        recordingStartedAt = SystemClock.elapsedRealtime()
                        elapsedMillis = 0L
                        phase = DualCameraPhase.RECORDING
                    }
                },
                onFinalized = { result ->
                    result.onSuccess { uri ->
                        savedUri = uri
                        phase = DualCameraPhase.SAVED
                        chromeVisible = true
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        if (leaveAfterFinalize) onBack()
                    }.onFailure { error ->
                        errorMessage = error.message ?: "Não foi possível salvar a gravação DualCam."
                        phase = DualCameraPhase.ERROR
                    }
                },
            )
        }.onFailure { error ->
            errorMessage = error.message ?: "Não foi possível iniciar a gravação DualCam."
            phase = DualCameraPhase.ERROR
        }
    }

    LaunchedEffect(permissionsGranted, retryKey, owner, previewView) {
        if (!permissionsGranted) return@LaunchedEffect
        phase = DualCameraPhase.PREPARING
        errorMessage = null
        runCatching {
            controller.bind(
                owner = owner,
                view = previewView,
                initialTransform = pipTransform,
                initialFrontIsPip = frontIsPip,
            )
        }.onSuccess {
            phase = DualCameraPhase.READY
        }.onFailure { error ->
            errorMessage = error.message ?: "Não foi possível preparar as duas câmeras."
            phase = if (error is DualCameraUnavailableException) {
                DualCameraPhase.UNSUPPORTED
            } else {
                DualCameraPhase.ERROR
            }
        }
    }

    LaunchedEffect(phase, recordingStartedAt) {
        while (phase == DualCameraPhase.RECORDING) {
            elapsedMillis = SystemClock.elapsedRealtime() - recordingStartedAt
            delay(100)
        }
    }

    DisposableEffect(controller) {
        onDispose { controller.release() }
    }
    DisposableEffect(phase) {
        if (
            phase == DualCameraPhase.RECORDING ||
            phase == DualCameraPhase.STARTING ||
            phase == DualCameraPhase.FINALIZING
        ) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    DisposableEffect(owner, phase) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    val granted = hasPermissions()
                    if (granted && !permissionsGranted) {
                        permissionsGranted = true
                        retryKey++
                    }
                }
                Lifecycle.Event.ON_STOP -> if (
                    phase == DualCameraPhase.RECORDING || phase == DualCameraPhase.STARTING
                ) {
                    stopRecording()
                }
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    BackHandler {
        when (phase) {
            DualCameraPhase.RECORDING, DualCameraPhase.STARTING -> confirmStop = true
            DualCameraPhase.FINALIZING -> Unit
            else -> onBack()
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        val outputAspect = DualCameraGeometry.PORTRAIT_OUTPUT_ASPECT_RATIO
        val canvasWidth = minOf(maxWidth, maxHeight * outputAspect)
        val canvasHeight = canvasWidth / outputAspect

        Box(
            Modifier.size(canvasWidth, canvasHeight)
                .background(Color.Black)
                .onSizeChanged {
                    canvasWidthPx = it.width.coerceAtLeast(1)
                    canvasHeightPx = it.height.coerceAtLeast(1)
                },
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize(),
            )
            if (!chromeVisible) {
                Box(
                    Modifier.fillMaxSize()
                        .semantics {
                            contentDescription = "Prévia DualCam em tela limpa"
                            onClick("Mostrar controles") {
                                chromeVisible = true
                                true
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { chromeVisible = true }
                        },
                )
            }

            if (phase in setOf(
                    DualCameraPhase.READY,
                    DualCameraPhase.STARTING,
                    DualCameraPhase.RECORDING,
                )
            ) {
                val pipWidth = canvasWidth * pipTransform.width
                val pipHeight = canvasHeight * DualCameraGeometry.pipHeight(pipTransform)
                val left = canvasWidth * pipTransform.centerX - pipWidth / 2f
                val top = canvasHeight * pipTransform.centerY - pipHeight / 2f
                Box(
                    Modifier.offset { IntOffset(left.roundToPx(), top.roundToPx()) }
                        .size(pipWidth, pipHeight)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.38f), CircleShape)
                        .semantics {
                            contentDescription = "Janela da câmera menor"
                            val horizontal = if (pipTransform.centerX < 0.5f) "esquerda" else "direita"
                            val vertical = if (pipTransform.centerY < 0.5f) "superior" else "inferior"
                            stateDescription = buildString {
                                append(if (frontIsPip) "Câmera frontal menor" else "Câmera traseira menor")
                                append(", posição $vertical $horizontal")
                                append(", tamanho ${(pipTransform.width * 100f).roundToInt()} por cento")
                            }
                            customActions = listOf(
                                CustomAccessibilityAction("Mover para a esquerda") {
                                    applyPip(pipTransform.copy(centerX = pipTransform.centerX - 0.06f)); true
                                },
                                CustomAccessibilityAction("Mover para a direita") {
                                    applyPip(pipTransform.copy(centerX = pipTransform.centerX + 0.06f)); true
                                },
                                CustomAccessibilityAction("Mover para cima") {
                                    applyPip(pipTransform.copy(centerY = pipTransform.centerY - 0.06f)); true
                                },
                                CustomAccessibilityAction("Mover para baixo") {
                                    applyPip(pipTransform.copy(centerY = pipTransform.centerY + 0.06f)); true
                                },
                                CustomAccessibilityAction("Aumentar") {
                                    applyPip(pipTransform.copy(width = pipTransform.width + 0.04f)); true
                                },
                                CustomAccessibilityAction("Diminuir") {
                                    applyPip(pipTransform.copy(width = pipTransform.width - 0.04f)); true
                                },
                            )
                        }
                        .pointerInput(canvasWidthPx, canvasHeightPx, phase) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                applyPip(
                                    pipTransform.copy(
                                        centerX = pipTransform.centerX + pan.x / canvasWidthPx,
                                        centerY = pipTransform.centerY + pan.y / canvasHeightPx,
                                        width = pipTransform.width * zoom,
                                    ),
                                )
                            }
                        },
                )
            }
        }

        if (chromeVisible || phase !in setOf(DualCameraPhase.READY, DualCameraPhase.RECORDING)) {
            DualCameraTopBar(
                phase = phase,
                elapsedMillis = elapsedMillis,
                onBack = {
                    if (phase == DualCameraPhase.RECORDING || phase == DualCameraPhase.STARTING) {
                        confirmStop = true
                    } else if (phase != DualCameraPhase.FINALIZING) {
                        onBack()
                    }
                },
                onHideControls = if (
                    phase == DualCameraPhase.READY || phase == DualCameraPhase.RECORDING
                ) {
                    { chromeVisible = false }
                } else {
                    null
                },
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }

        when (phase) {
            DualCameraPhase.PERMISSION_REQUIRED,
            DualCameraPhase.PERMISSION_DENIED,
            DualCameraPhase.PREPARING,
            DualCameraPhase.UNSUPPORTED,
            DualCameraPhase.ERROR,
            -> DualCameraStateCard(
                phase = phase,
                message = errorMessage,
                onPrimary = {
                    when (phase) {
                        DualCameraPhase.PERMISSION_REQUIRED,
                        DualCameraPhase.PERMISSION_DENIED,
                        -> {
                            phase = DualCameraPhase.PREPARING
                            requestCapturePermissions { granted ->
                                permissionsGranted = granted
                                phase = if (granted) DualCameraPhase.PREPARING else DualCameraPhase.PERMISSION_DENIED
                            }
                        }
                        DualCameraPhase.ERROR -> retryKey++
                        else -> Unit
                    }
                },
                onSettings = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            "package:${context.packageName}".toUri(),
                        ),
                    )
                },
                onBack = onBack,
                modifier = Modifier.align(Alignment.Center),
            )
            else -> Unit
        }

        if (chromeVisible && phase in setOf(
                DualCameraPhase.READY,
                DualCameraPhase.STARTING,
                DualCameraPhase.RECORDING,
                DualCameraPhase.FINALIZING,
            )
        ) {
            DualCameraControls(
                phase = phase,
                frontIsPip = frontIsPip,
                onSwap = {
                    runCatching { controller.swapCameras() }
                        .onSuccess {
                            frontIsPip = it
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                        .onFailure {
                            val message = it.message ?: "Não foi possível trocar as câmeras."
                            if (phase == DualCameraPhase.RECORDING || phase == DualCameraPhase.STARTING) {
                                nonBlockingMessage = "$message A gravação continua com o último enquadramento."
                            } else {
                                errorMessage = message
                                phase = DualCameraPhase.ERROR
                            }
                        }
                },
                onRecord = {
                    if (phase == DualCameraPhase.RECORDING || phase == DualCameraPhase.STARTING) {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        stopRecording()
                    } else {
                        startRecording()
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        if (phase == DualCameraPhase.SAVED) {
            DualCameraSavedCard(
                onWatch = {
                    savedUri?.let { uri ->
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "video/mp4")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                },
                            )
                        }.onFailure {
                            nonBlockingMessage =
                                "O vídeo continua salvo em Filmes/SeuReact, mas nenhum player pôde ser aberto."
                        }
                    }
                },
                onRecordAnother = {
                    savedUri = null
                    elapsedMillis = 0L
                    phase = DualCameraPhase.READY
                },
                onDone = onBack,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            icon = { Icon(Icons.Default.Stop, null) },
            title = { Text("Parar e salvar a gravação?") },
            text = { Text("O vídeo será finalizado antes de você sair do DualCam.") },
            confirmButton = {
                Button(onClick = {
                    confirmStop = false
                    stopRecording(exitAfter = true)
                }) { Text("Parar e salvar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmStop = false }) { Text("Continuar gravando") }
            },
        )
    }
    nonBlockingMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { nonBlockingMessage = null },
            icon = { Icon(Icons.Default.ErrorOutline, null) },
            title = {
                Text(
                    if (message.contains("player", ignoreCase = true)) {
                        "Não foi possível abrir o vídeo"
                    } else {
                        "Ajuste não aplicado"
                    },
                )
            },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { nonBlockingMessage = null }) { Text("Entendi") }
            },
        )
    }
}

@Composable
private fun DualCameraTopBar(
    phase: DualCameraPhase,
    elapsedMillis: Long,
    onBack: () -> Unit,
    onHideControls: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.74f))
            .statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, enabled = phase != DualCameraPhase.FINALIZING) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                "Voltar",
                tint = if (phase == DualCameraPhase.FINALIZING) Color.White.copy(alpha = 0.38f) else Color.White,
            )
        }
        Column(Modifier.weight(1f)) {
            Text("DualCam", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(
                when (phase) {
                    DualCameraPhase.STARTING -> "Iniciando gravação"
                    DualCameraPhase.RECORDING -> "Duas câmeras gravando"
                    DualCameraPhase.FINALIZING -> "Finalizando gravação"
                    DualCameraPhase.SAVED -> "Vídeo salvo"
                    else -> "Traseira + frontal"
                },
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        onHideControls?.let {
            IconButton(onClick = it) {
                Icon(Icons.Default.Fullscreen, "Ocultar controles e visualizar o quadro inteiro", tint = Color.White)
            }
        }
        if (phase == DualCameraPhase.RECORDING || phase == DualCameraPhase.FINALIZING) {
            Surface(
                color = if (phase == DualCameraPhase.RECORDING) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.16f),
                contentColor = if (phase == DualCameraPhase.RECORDING) MaterialTheme.colorScheme.onPrimary else Color.White,
                shape = CircleShape,
                modifier = Modifier.semantics {
                    stateDescription = if (phase == DualCameraPhase.RECORDING) "Gravando" else "Finalizando"
                },
            ) {
                Text(
                    if (phase == DualCameraPhase.RECORDING) formatDualCameraTime(elapsedMillis) else "Salvando…",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun DualCameraControls(
    phase: DualCameraPhase,
    frontIsPip: Boolean,
    onSwap: () -> Unit,
    onRecord: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val recording = phase == DualCameraPhase.RECORDING || phase == DualCameraPhase.STARTING
    val busy = phase == DualCameraPhase.STARTING || phase == DualCameraPhase.FINALIZING
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.68f),
        contentColor = Color.White,
    ) {
        Column(
            Modifier.navigationBarsPadding().padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (phase == DualCameraPhase.READY) {
                Text(
                    "Arraste o círculo para posicionar • pince para redimensionar",
                    color = Color.White.copy(alpha = 0.74f),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(88.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.12f),
                    ) {
                        IconButton(onClick = onSwap, enabled = !busy, modifier = Modifier.size(52.dp)) {
                            Icon(Icons.Default.Cameraswitch, "Trocar câmera principal")
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (frontIsPip) "Frontal menor" else "Traseira menor",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.74f),
                        maxLines = 1,
                    )
                }
                Surface(
                    modifier = Modifier.size(82.dp).semantics {
                        contentDescription = when (phase) {
                            DualCameraPhase.READY -> "Iniciar gravação"
                            DualCameraPhase.STARTING -> "Iniciando gravação"
                            DualCameraPhase.RECORDING -> "Parar e salvar"
                            DualCameraPhase.FINALIZING -> "Finalizando gravação"
                            else -> "Controle de gravação"
                        }
                        stateDescription = when (phase) {
                            DualCameraPhase.STARTING, DualCameraPhase.FINALIZING -> "Aguarde"
                            DualCameraPhase.RECORDING -> "Gravando"
                            else -> "Pronto"
                        }
                    },
                    shape = CircleShape,
                    color = Color.White,
                    onClick = onRecord,
                    enabled = !busy,
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (busy) {
                            CircularProgressIndicator(Modifier.size(30.dp), strokeWidth = 3.dp)
                        } else {
                            Surface(
                                modifier = Modifier.size(if (recording) 36.dp else 62.dp),
                                shape = if (recording) RoundedCornerShape(10.dp) else CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                            ) {
                                Icon(
                                    if (recording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                    null,
                                    tint = if (recording) MaterialTheme.colorScheme.onPrimary else Color.Transparent,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(88.dp))
            }
        }
    }
}

@Composable
private fun DualCameraStateCard(
    phase: DualCameraPhase,
    message: String?,
    onPrimary: () -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preparing = phase == DualCameraPhase.PREPARING
    val permission = phase == DualCameraPhase.PERMISSION_REQUIRED || phase == DualCameraPhase.PERMISSION_DENIED
    val unsupported = phase == DualCameraPhase.UNSUPPORTED
    Surface(
        modifier = modifier.padding(24.dp).widthIn(max = 440.dp).fillMaxWidth().heightIn(min = 220.dp)
            .semantics {
                liveRegion = if (phase == DualCameraPhase.ERROR) LiveRegionMode.Assertive else LiveRegionMode.Polite
            },
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 8.dp,
    ) {
        Column(
            Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (preparing) {
                CircularProgressIndicator(Modifier.size(42.dp))
            } else {
                Icon(
                    when {
                        permission -> Icons.Default.Videocam
                        unsupported -> Icons.Default.Cameraswitch
                        else -> Icons.Default.ErrorOutline
                    },
                    null,
                    Modifier.size(42.dp),
                    tint = if (phase == DualCameraPhase.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                when {
                    preparing -> "Preparando as duas câmeras"
                    phase == DualCameraPhase.PERMISSION_REQUIRED -> "Câmera e microfone"
                    phase == DualCameraPhase.PERMISSION_DENIED -> "Permissão necessária"
                    unsupported -> "DualCam indisponível"
                    else -> "Não foi possível abrir o DualCam"
                },
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message ?: when {
                    preparing -> "Isso pode levar alguns segundos."
                    phase == DualCameraPhase.PERMISSION_REQUIRED ->
                        "O DualCam usa a câmera traseira, a frontal e o microfone para criar um único vídeo."
                    phase == DualCameraPhase.PERMISSION_DENIED ->
                        "Permita o acesso para gravar com as duas câmeras. Você também pode alterar isso nos ajustes do app."
                    unsupported ->
                        "Este aparelho não expôs ao Android um par frontal e traseiro compatível com gravação simultânea."
                    else -> "Tente preparar as câmeras novamente."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            when {
                preparing -> Unit
                permission -> {
                    Button(onClick = onPrimary, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Icon(Icons.Default.Videocam, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Permitir câmera e microfone")
                    }
                    if (phase == DualCameraPhase.PERMISSION_DENIED) {
                        TextButton(onClick = onSettings) {
                            Icon(Icons.Default.Settings, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Abrir configurações")
                        }
                    }
                }
                phase == DualCameraPhase.ERROR -> {
                    Button(onClick = onPrimary, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Tentar novamente")
                    }
                    TextButton(onClick = onBack) { Text("Voltar ao início") }
                }
                unsupported -> OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) { Text("Voltar ao início") }
            }
        }
    }
}

@Composable
private fun DualCameraSavedCard(
    onWatch: () -> Unit,
    onRecordAnother: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 8.dp,
    ) {
        Column(
            Modifier.navigationBarsPadding().padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.CheckCircle, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(10.dp))
            Text("Vídeo salvo", style = MaterialTheme.typography.titleLarge)
            Text(
                "A gravação está em Filmes/SeuReact e já aparece na sua galeria.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Button(onClick = onWatch, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, null)
                Spacer(Modifier.width(8.dp))
                Text("Assistir vídeo")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onRecordAnother, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Icon(Icons.Default.Videocam, null)
                Spacer(Modifier.width(8.dp))
                Text("Gravar outro")
            }
            TextButton(onClick = onDone) { Text("Concluir") }
        }
    }
}

private fun formatDualCameraTime(elapsedMillis: Long): String {
    val totalSeconds = elapsedMillis.coerceAtLeast(0L) / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}
