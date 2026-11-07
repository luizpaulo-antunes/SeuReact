package com.seureact.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.ui.PlayerView
import androidx.work.WorkInfo
import com.seureact.app.domain.model.CameraShape
import com.seureact.app.domain.model.CameraFacing
import com.seureact.app.domain.model.CameraTransform
import com.seureact.app.domain.model.OutputAspect
import com.seureact.app.domain.model.OutputResolution
import com.seureact.app.domain.model.ProjectStatus
import com.seureact.app.domain.model.ReactionProject
import com.seureact.app.media.capture.CameraCaptureController
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ReactionApp(viewModel: MainViewModel, requestCapturePermissions: (() -> Unit) -> Unit) {
    val state by viewModel.state.collectAsState()
    val projects by viewModel.projects.collectAsState()
    when (state.screen) {
        AppScreen.HOME -> HomeScreen(projects, state, viewModel::importVideo, viewModel::openProject)
        AppScreen.STUDIO -> StudioScreen(state, viewModel, requestCapturePermissions)
        AppScreen.EXPORT -> ExportScreen(state, viewModel)
    }
    state.message?.takeIf { state.screen != AppScreen.EXPORT }?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearMessage,
            confirmButton = { TextButton(onClick = viewModel::clearMessage) { Text("Entendi") } },
            title = { Text("Atenção") },
            text = { Text(message) },
        )
    }
}

@Composable
private fun HomeScreen(
    projects: List<ReactionProject>,
    state: MainUiState,
    onImport: (Uri) -> Unit,
    onOpen: (ReactionProject) -> Unit,
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(onImport) }
    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(Modifier.height(24.dp))
                Text("Seu React", style = MaterialTheme.typography.headlineLarge)
                Text("Grave o que você realmente achou.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
                Button(
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !state.importing,
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                ) {
                    if (state.importing) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    else Text("Novo react")
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Headphones, null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(8.dp))
                    Text("Use fones para evitar eco na gravação.", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(24.dp))
                Text("Projetos", style = MaterialTheme.typography.titleLarge)
            }
            if (projects.isEmpty()) item { Text("Seus projetos aparecerão aqui.") }
            items(projects, key = { it.id }) { project -> ProjectCard(project) { onOpen(project) } }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ProjectCard(project: ReactionProject, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(project.metadata.displayName, style = MaterialTheme.typography.titleMedium)
            Text(statusLabel(project.status), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (project.status == ProjectStatus.EXPORTING) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator({ project.exportProgress / 100f }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun StudioScreen(
    state: MainUiState,
    viewModel: MainViewModel,
    requestCapturePermissions: (() -> Unit) -> Unit,
) {
    val project = state.project ?: return
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val cameraController = remember { CameraCaptureController(context) }
    var capturePermissionsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val window = (context as? android.app.Activity)?.window
    LaunchedEffect(Unit) {
        if (!capturePermissionsGranted) {
            requestCapturePermissions { capturePermissionsGranted = true }
        }
    }
    DisposableEffect(cameraController) {
        onDispose { cameraController.release() }
    }
    DisposableEffect(state.isRecording) {
        if (state.isRecording) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    DisposableEffect(owner, state.isRecording) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && state.isRecording) viewModel.interruptRecording()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(state.isRecording) {
        while (state.isRecording) {
            viewModel.tick()
            delay(100)
        }
    }

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    enabled = !state.isRecording && !state.isFinalizing,
                    onClick = viewModel::backHome,
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                Text(
                    when {
                        state.isRecording -> "Gravando reação"
                        state.isFinalizing -> "Finalizando gravação"
                        else -> "Preparar reação"
                    },
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.weight(1f))
                if (state.isRecording) Text("● REC", color = MaterialTheme.colorScheme.primary)
            }
            Box(
                Modifier.fillMaxWidth().weight(1f).background(androidx.compose.ui.graphics.Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { PlayerView(it).apply { player = viewModel.player; useController = false } },
                    update = { it.player = viewModel.player },
                )
                CameraOverlay(
                    project.cameraTransform,
                    project.cameraShape,
                    cameraController,
                    project,
                    capturePermissionsGranted,
                    viewModel::changeTransform,
                )
                if (state.isRecording) {
                    Text(
                        formatTime(state.elapsedNanos / 1_000_000L),
                        modifier = Modifier.align(Alignment.TopCenter).padding(12.dp).background(
                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp),
                        ).padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
            if (state.isRecording) {
                RecordingControls(state, project, viewModel)
            } else if (state.isFinalizing) {
                Row(
                    Modifier.fillMaxWidth().height(90.dp).padding(20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Finalizando câmera e microfone…")
                }
            } else PreparationControls(project, viewModel) {
                if (capturePermissionsGranted) {
                    viewModel.startRecording(cameraController)
                } else requestCapturePermissions {
                    capturePermissionsGranted = true
                    viewModel.startRecording(cameraController)
                }
            }
        }
    }
}

@Composable
private fun CameraOverlay(
    transform: CameraTransform,
    shape: CameraShape,
    controller: CameraCaptureController,
    project: ReactionProject,
    capturePermissionsGranted: Boolean,
    onTransform: (CameraTransform) -> Unit,
) {
    val owner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    var parentWidth by remember { mutableIntStateOf(1) }
    var parentHeight by remember { mutableIntStateOf(1) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    LaunchedEffect(previewView, project.cameraFacing, capturePermissionsGranted) {
        if (capturePermissionsGranted) {
            previewView?.let { controller.bind(owner, it, project.cameraFacing) }
        }
    }
    Box(Modifier.fillMaxSize().onSizeChanged { parentWidth = it.width; parentHeight = it.height }) {
        val width = parentWidth * transform.width
        val height = parentHeight * transform.height
        val widthDp = with(density) { width.toDp() }
        val heightDp = with(density) { height.toDp() }
        val clipShape = when (shape) {
            CameraShape.CIRCLE -> CircleShape
            CameraShape.SQUARE -> RoundedCornerShape(0.dp)
            CameraShape.ROUNDED_RECTANGLE -> RoundedCornerShape(24.dp)
        }
        AndroidView(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (parentWidth * transform.centerX - width / 2).roundToInt(),
                        (parentHeight * transform.centerY - height / 2).roundToInt(),
                    )
                }
                .size(widthDp, heightDp)
                .clip(clipShape)
                .pointerInput(transform) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        onTransform(transform.copy(
                            centerX = transform.centerX + pan.x / parentWidth,
                            centerY = transform.centerY + pan.y / parentHeight,
                            width = transform.width * zoom,
                            height = transform.height * zoom,
                        ))
                    }
                },
            factory = { context ->
                PreviewView(context).also { view ->
                    view.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    previewView = view
                }
            },
        )
        if (!capturePermissionsGranted) {
            Text(
                "Permita câmera e microfone para exibir a câmera frontal.",
                modifier = Modifier.align(Alignment.Center).padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreparationControls(project: ReactionProject, viewModel: MainViewModel, onStart: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().height(300.dp).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { ChoiceRow("Câmera", CameraShape.entries, project.cameraShape, { it.name.lowercase() }) { viewModel.updateSetup(shape = it) } }
        item {
            Button(onClick = {
                viewModel.setCameraFacing(if (project.cameraFacing == CameraFacing.FRONT) CameraFacing.BACK else CameraFacing.FRONT)
            }) { Text(if (project.cameraFacing == CameraFacing.FRONT) "Usar câmera traseira" else "Usar câmera frontal") }
        }
        item { ChoiceRow("Proporção", OutputAspect.entries, project.outputAspect, { aspectLabel(it) }) { viewModel.updateSetup(aspect = it) } }
        item { ChoiceRow("Qualidade", OutputResolution.entries, project.outputResolution, { if (it == OutputResolution.HD_720) "720p" else "1080p" }) { viewModel.updateSetup(resolution = it) } }
        item {
            Text("Volume do vídeo ${percent(project.sourceVolume)}")
            Slider(project.sourceVolume, { viewModel.updateSetup(sourceVolume = it) }, valueRange = 0f..1f)
            Text("Volume do microfone ${percent(project.microphoneVolume)}")
            Slider(project.microphoneVolume, { viewModel.updateSetup(microphoneVolume = it) }, valueRange = 0f..1.5f)
        }
        item { Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(54.dp)) { Icon(Icons.Default.Mic, null); Spacer(Modifier.width(8.dp)); Text("Começar a gravar") } }
    }
}

@Composable
private fun <T> ChoiceRow(title: String, values: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            values.forEach { value -> FilterChip(selected == value, { onSelect(value) }, { Text(label(value)) }) }
        }
    }
}

@Composable
private fun RecordingControls(state: MainUiState, project: ReactionProject, viewModel: MainViewModel) {
    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(14.dp)) {
        Slider(
            value = state.sourcePositionMs.toFloat().coerceIn(0f, project.metadata.durationMs.toFloat()),
            onValueChange = { viewModel.seekTo(it.toLong()) },
            valueRange = 0f..project.metadata.durationMs.toFloat().coerceAtLeast(1f),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(formatTime(state.sourcePositionMs))
            Text("Reação ${formatTime(state.elapsedNanos / 1_000_000L)}")
            Text(formatTime(project.metadata.durationMs))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (state.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp, null)
            Slider(
                value = project.sourceVolume,
                onValueChange = { viewModel.updateSetup(sourceVolume = it) },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.seekTo(state.sourcePositionMs - 5_000) }) { Icon(Icons.Default.FastRewind, "Voltar 5 segundos") }
            IconButton(onClick = viewModel::togglePlayback, modifier = Modifier.size(58.dp).background(MaterialTheme.colorScheme.primary, CircleShape)) {
                Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play ou pause")
            }
            IconButton(onClick = { viewModel.seekTo(state.sourcePositionMs + 5_000) }) { Icon(Icons.Default.FastForward, "Avançar 5 segundos") }
            IconButton(onClick = viewModel::toggleMute) { Icon(if (state.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp, "Áudio original") }
            IconButton(onClick = viewModel::finishRecording, modifier = Modifier.background(MaterialTheme.colorScheme.error, CircleShape)) { Icon(Icons.Default.Stop, "Finalizar") }
        }
    }
}

@Composable
private fun ExportScreen(state: MainUiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    val project = state.project
    val completed = state.exportState == WorkInfo.State.SUCCEEDED || project?.status == ProjectStatus.COMPLETED
    val exporting = state.exportState == WorkInfo.State.ENQUEUED || state.exportState == WorkInfo.State.RUNNING ||
        state.exportState == WorkInfo.State.BLOCKED || project?.status == ProjectStatus.EXPORTING
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(if (completed) "Reação pronta" else "Exportando reação", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        if (exporting) {
            LinearProgressIndicator({ state.exportProgress / 100f }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text("${state.exportProgress}% — você pode sair desta tela.")
            Spacer(Modifier.height(24.dp))
            TextButton(onClick = viewModel::cancelExport) { Text("Cancelar exportação") }
        } else if (completed) {
            Text("O MP4 foi salvo em Filmes/SeuReact.")
            Spacer(Modifier.height(20.dp))
            Button(onClick = {
                project?.outputUri?.let { uri ->
                    context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri), "video/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                }
            }) { Icon(Icons.Default.PlayArrow, null); Text(" Assistir") }
            TextButton(onClick = {
                project?.outputUri?.let { uri ->
                    context.startActivity(Intent.createChooser(
                        Intent(Intent.ACTION_SEND).setType("video/mp4").putExtra(Intent.EXTRA_STREAM, Uri.parse(uri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                        "Compartilhar reação",
                    ))
                }
            }) { Icon(Icons.Default.Share, null); Text(" Compartilhar") }
        } else {
            Text("Os arquivos brutos estão preservados e prontos para uma nova tentativa.")
            Spacer(Modifier.height(16.dp))
            Button(onClick = viewModel::retryExport) { Text("Exportar agora") }
        }
        state.message?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = viewModel::retryExport) { Text("Tentar novamente") }
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = viewModel::backHome) { Text("Voltar aos projetos") }
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0) / 1_000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private fun percent(value: Float) = "${(value * 100).roundToInt()}%"
private fun aspectLabel(aspect: OutputAspect) = when (aspect) {
    OutputAspect.ORIGINAL -> "Original"
    OutputAspect.PORTRAIT_9_16 -> "9:16"
    OutputAspect.LANDSCAPE_16_9 -> "16:9"
    OutputAspect.SQUARE -> "1:1"
}
private fun statusLabel(status: ProjectStatus) = when (status) {
    ProjectStatus.PREPARING -> "Em preparação"
    ProjectStatus.RECORDING -> "Gravando"
    ProjectStatus.INTERRUPTED -> "Interrompido — recuperável"
    ProjectStatus.READY_TO_EXPORT -> "Pronto para exportar"
    ProjectStatus.EXPORTING -> "Exportando"
    ProjectStatus.COMPLETED -> "Concluído"
    ProjectStatus.FAILED -> "Falha — arquivos preservados"
}
