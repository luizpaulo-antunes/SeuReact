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
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.ui.PlayerView
import androidx.work.WorkInfo
import com.seureact.app.domain.model.CameraShape
import com.seureact.app.domain.model.CameraFacing
import com.seureact.app.domain.model.CameraOverlayGeometry
import com.seureact.app.domain.model.CameraTransform
import com.seureact.app.domain.model.OutputAspect
import com.seureact.app.domain.model.OutputResolution
import com.seureact.app.domain.model.PointerMarker
import com.seureact.app.domain.model.PointerMarkerLayout
import com.seureact.app.domain.model.PointerStyle
import com.seureact.app.domain.model.ProjectStatus
import com.seureact.app.domain.model.ReactionProject
import com.seureact.app.domain.model.SourceTransform
import com.seureact.app.media.capture.CameraCaptureController
import com.seureact.app.media.PointerMarkerVisuals
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
    val resumableProject = projects.firstOrNull {
        it.status == ProjectStatus.PREPARING ||
            it.status == ProjectStatus.INTERRUPTED ||
            it.status == ProjectStatus.READY_TO_EXPORT ||
            it.status == ProjectStatus.FAILED
    }
    val launchPicker = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
    }
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(MaterialTheme.colorScheme.background, Color(0xFF11111A)),
            ),
        ),
    ) {
        Scaffold(containerColor = Color.Transparent) { padding ->
            LazyVerticalGrid(
                columns = GridCells.Adaptive(148.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    HomeHero(importing = state.importing, onNewReaction = launchPicker)
                }
                resumableProject?.let { project ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            Spacer(Modifier.height(10.dp))
                            SectionEyebrow("CONTINUE CRIANDO")
                            Spacer(Modifier.height(8.dp))
                            ResumeProjectCard(project = project, onClick = { onOpen(project) })
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    GallerySectionHeader(projectCount = projects.size)
                }
                if (projects.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyGallery()
                    }
                } else {
                    items(projects, key = { it.id }) { project ->
                        ProjectGalleryCard(project = project, onClick = { onOpen(project) })
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHero(importing: Boolean, onNewReaction: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(
            Brush.linearGradient(
                listOf(Color(0xFFFF5C7A), Color(0xFFB73A87), Color(0xFF5344A2)),
            ),
        ),
    ) {
        Box(
            Modifier.align(Alignment.TopEnd).offset(x = 48.dp, y = (-54).dp).size(180.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape),
        )
        Box(
            Modifier.align(Alignment.BottomEnd).offset(x = 32.dp, y = 52.dp).size(120.dp)
                .background(Color.Black.copy(alpha = 0.08f), CircleShape),
        )
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color.White.copy(alpha = 0.16f),
                    contentColor = Color.White,
                    shape = CircleShape,
                ) {
                    Icon(Icons.Default.Videocam, null, Modifier.padding(8.dp).size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "SEUREACT",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                "Sua reação,\ndo seu jeito.",
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Escolha um vídeo, comente e transforme tudo em um único react.",
                color = Color.White.copy(alpha = 0.84f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(0.88f),
            )
            Spacer(Modifier.height(22.dp))
            Button(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !importing,
                onClick = onNewReaction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF35101F),
                    disabledContainerColor = Color.White.copy(alpha = 0.7f),
                    disabledContentColor = Color(0xFF35101F).copy(alpha = 0.7f),
                ),
            ) {
                if (importing) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = Color(0xFF35101F), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Importando vídeo…")
                } else {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Criar novo react")
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Headphones, null, tint = Color.White.copy(alpha = 0.78f), modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    "Fones de ouvido evitam eco na gravação",
                    color = Color.White.copy(alpha = 0.76f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun SectionEyebrow(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun ResumeProjectCard(project: ReactionProject, onClick: () -> Unit) {
    val accent = projectStatusAccent(project.status)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(48.dp),
                color = accent.copy(alpha = 0.16f),
                contentColor = accent,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(projectStatusIcon(project.status), null, Modifier.padding(12.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    project.metadata.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    statusLabel(project.status),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                "Abrir projeto",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GallerySectionHeader(projectCount: Int) {
    Row(
        Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Sua galeria", style = MaterialTheme.typography.titleLarge)
            Text(
                if (projectCount == 0) "Seus reacts vão aparecer aqui" else "Projetos recentes e exportados",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (projectCount > 0) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = CircleShape,
            ) {
                Text(
                    projectCount.toString(),
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun EmptyGallery() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape,
            ) {
                Icon(Icons.Default.VideoLibrary, null, Modifier.padding(17.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Sua primeira reação começa aqui", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(5.dp))
            Text(
                "Use o botão acima para escolher um vídeo do celular.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProjectGalleryCard(project: ReactionProject, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            ProjectArtwork(project)
            Column(Modifier.padding(12.dp)) {
                Text(
                    project.metadata.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccessTime,
                        null,
                        Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${formatTime(project.metadata.durationMs)}  •  ${sourceOrientationLabel(project)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectArtwork(project: ReactionProject) {
    val palettes = listOf(
        listOf(Color(0xFF64304C), Color(0xFF2D274E)),
        listOf(Color(0xFF1D5B57), Color(0xFF17334E)),
        listOf(Color(0xFF5B3D25), Color(0xFF3E254C)),
        listOf(Color(0xFF3D466E), Color(0xFF39263F)),
    )
    val palette = palettes[Math.floorMod(project.id.hashCode(), palettes.size)]
    val isPortrait = project.metadata.height > project.metadata.width
    Box(
        Modifier.fillMaxWidth().aspectRatio(1.35f).background(Brush.linearGradient(palette)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.align(Alignment.TopEnd).offset(x = 24.dp, y = (-22).dp).size(90.dp)
                .background(Color.White.copy(alpha = 0.06f), CircleShape),
        )
        Box(
            Modifier
                .size(width = if (isPortrait) 42.dp else 72.dp, height = if (isPortrait) 68.dp else 44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.22f))
                .border(1.dp, Color.White.copy(alpha = 0.34f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Box(Modifier.align(Alignment.TopStart).padding(9.dp)) {
            ProjectStatusBadge(project.status)
        }
        Surface(
            modifier = Modifier.align(Alignment.BottomEnd).padding(9.dp),
            color = Color.Black.copy(alpha = 0.52f),
            contentColor = Color.White,
            shape = CircleShape,
        ) {
            Text(
                formatTime(project.metadata.durationMs),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (project.status == ProjectStatus.EXPORTING) {
            LinearProgressIndicator(
                progress = { project.exportProgress / 100f },
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent,
            )
        }
    }
}

@Composable
private fun ProjectStatusBadge(status: ProjectStatus) {
    val accent = projectStatusAccent(status)
    Surface(
        color = Color.Black.copy(alpha = 0.42f),
        contentColor = Color.White,
        shape = CircleShape,
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(projectStatusIcon(status), null, Modifier.size(12.dp), tint = accent)
            Spacer(Modifier.width(4.dp))
            Text(shortStatusLabel(status), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun projectStatusAccent(status: ProjectStatus): Color = when (status) {
    ProjectStatus.RECORDING -> MaterialTheme.colorScheme.primary
    ProjectStatus.READY_TO_EXPORT, ProjectStatus.COMPLETED -> MaterialTheme.colorScheme.secondary
    ProjectStatus.FAILED -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.tertiary
}

private fun projectStatusIcon(status: ProjectStatus) = when (status) {
    ProjectStatus.PREPARING -> Icons.Default.Tune
    ProjectStatus.RECORDING -> Icons.Default.FiberManualRecord
    ProjectStatus.INTERRUPTED -> Icons.Default.Restore
    ProjectStatus.READY_TO_EXPORT -> Icons.Default.Movie
    ProjectStatus.EXPORTING -> Icons.Default.Refresh
    ProjectStatus.COMPLETED -> Icons.Default.CheckCircle
    ProjectStatus.FAILED -> Icons.Default.ErrorOutline
}

private fun shortStatusLabel(status: ProjectStatus) = when (status) {
    ProjectStatus.PREPARING -> "Preparando"
    ProjectStatus.RECORDING -> "Gravando"
    ProjectStatus.INTERRUPTED -> "Pausado"
    ProjectStatus.READY_TO_EXPORT -> "Pronto"
    ProjectStatus.EXPORTING -> "Exportando"
    ProjectStatus.COMPLETED -> "Concluído"
    ProjectStatus.FAILED -> "Falhou"
}

private fun sourceOrientationLabel(project: ReactionProject) = when {
    project.metadata.width > project.metadata.height -> "Horizontal"
    project.metadata.height > project.metadata.width -> "Vertical"
    else -> "Quadrado"
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
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                Row(
                    Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        enabled = !state.isRecording && !state.isFinalizing,
                        onClick = viewModel::backHome,
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                    Column(Modifier.weight(1f)) {
                        Text(
                            when {
                                state.isRecording -> "Gravando reação"
                                state.isDiscarding -> "Descartando gravação"
                                state.isFinalizing -> "Finalizando gravação"
                                else -> "Preparar reação"
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            project.metadata.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (state.isRecording) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            contentColor = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                        ) {
                            Row(
                                Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.FiberManualRecord, null, Modifier.size(13.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("REC", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            Box(
                Modifier.fillMaxWidth().weight(1f).background(androidx.compose.ui.graphics.Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        scaleX = state.sourceTransform.scale
                        scaleY = state.sourceTransform.scale
                        transformOrigin = TransformOrigin(state.sourceTransform.focusX, state.sourceTransform.focusY)
                    },
                    factory = { PlayerView(it).apply { player = viewModel.player; useController = false } },
                    update = { it.player = viewModel.player },
                )
                if (state.isRecording) {
                    SourceInteractionLayer(
                        transform = state.sourceTransform,
                        marker = state.activePointer,
                        onTransform = viewModel::changeSourceTransform,
                        onTap = viewModel::placePointer,
                    )
                }
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
                    Text(if (state.isDiscarding) "Descartando gravação…" else "Finalizando câmera e microfone…")
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
private fun SourceInteractionLayer(
    transform: SourceTransform,
    marker: PointerMarker?,
    onTransform: (SourceTransform) -> Unit,
    onTap: (Float, Float) -> Unit,
) {
    val density = LocalDensity.current
    var width by remember { mutableIntStateOf(1) }
    var height by remember { mutableIntStateOf(1) }
    Box(
        Modifier.fillMaxSize()
            .onSizeChanged { width = it.width; height = it.height }
            .pointerInput(transform) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    onTransform(
                        transform.copy(
                            scale = transform.scale * zoom,
                            focusX = centroid.x / width - pan.x / width,
                            focusY = centroid.y / height - pan.y / height,
                        ),
                    )
                }
            }
            .pointerInput(transform) {
                detectTapGestures { position ->
                    val screenX = position.x / width
                    val screenY = position.y / height
                    onTap(
                        transform.focusX + (screenX - transform.focusX) / transform.scale,
                        transform.focusY + (screenY - transform.focusY) / transform.scale,
                    )
                }
            },
    ) {
        marker?.let {
            val screenX = transform.focusX + (it.x - transform.focusX) * transform.scale
            val screenY = transform.focusY + (it.y - transform.focusY) * transform.scale
            val screenMarker = it.copy(x = screenX, y = screenY)
            val markerSizePx = with(density) { 72.dp.toPx() }
            val placement = PointerMarkerLayout.place(screenMarker, width.toFloat(), height.toFloat(), markerSizePx)
            Box(
                Modifier.offset {
                    IntOffset(
                        (screenX * width).roundToInt() - 6.dp.roundToPx(),
                        (screenY * height).roundToInt() - 6.dp.roundToPx(),
                    )
                }.size(12.dp).background(androidx.compose.ui.graphics.Color(0x552563EB), CircleShape),
            )
            PointerMarkerIcon(
                marker = screenMarker,
                placement = placement,
                modifier = Modifier.offset {
                    IntOffset(
                        placement.left.roundToInt(),
                        placement.top.roundToInt(),
                    )
                },
            )
        }
    }
}

@Composable
private fun PointerMarkerIcon(
    marker: PointerMarker,
    placement: com.seureact.app.domain.model.PointerDrawablePlacement,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    Image(
        painter = painterResource(PointerMarkerVisuals.drawable(marker.style)),
        contentDescription = pointerStyleLabel(marker.style),
        modifier = modifier
            .size(
                with(density) { (placement.right - placement.left).toDp() },
                with(density) { (placement.bottom - placement.top).toDp() },
            )
            .graphicsLayer {
                scaleX = placement.scaleX
                scaleY = placement.scaleY
                transformOrigin = TransformOrigin(placement.pivotFractionX, placement.pivotFractionY)
            },
    )
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
        val previewSize = CameraOverlayGeometry.normalizedSize(
            transform,
            shape,
            parentWidth.toFloat() / parentHeight.coerceAtLeast(1),
        )
        val width = parentWidth * previewSize.width
        val height = parentHeight * previewSize.height
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
        modifier = Modifier.fillMaxWidth().height(320.dp).clip(
            RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        ).background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Monte seu enquadramento", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Ajuste a câmera antes de começar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = CircleShape,
                ) {
                    Icon(Icons.Default.Videocam, null, Modifier.padding(9.dp).size(18.dp))
                }
            }
        }
        item { ChoiceRow("Formato da câmera", CameraShape.entries, project.cameraShape, ::cameraShapeLabel) { viewModel.updateSetup(shape = it) } }
        item {
            OutlinedButton(
                onClick = {
                    viewModel.setCameraFacing(if (project.cameraFacing == CameraFacing.FRONT) CameraFacing.BACK else CameraFacing.FRONT)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Videocam, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (project.cameraFacing == CameraFacing.FRONT) "Trocar para câmera traseira" else "Trocar para câmera frontal")
            }
        }
        item { ChoiceRow("Proporção", OutputAspect.entries, project.outputAspect, { aspectLabel(it) }) { viewModel.updateSetup(aspect = it) } }
        item { ChoiceRow("Qualidade", OutputResolution.entries, project.outputResolution, { if (it == OutputResolution.HD_720) "720p" else "1080p" }) { viewModel.updateSetup(resolution = it) } }
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text("Volume do vídeo ${percent(project.sourceVolume)}", style = MaterialTheme.typography.labelLarge)
                    Slider(project.sourceVolume, { viewModel.updateSetup(sourceVolume = it) }, valueRange = 0f..1f)
                    Text("Volume do microfone ${percent(project.microphoneVolume)}", style = MaterialTheme.typography.labelLarge)
                    Slider(project.microphoneVolume, { viewModel.updateSetup(microphoneVolume = it) }, valueRange = 0f..1.5f)
                }
            }
        }
        item {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Icon(Icons.Default.Mic, null)
                Spacer(Modifier.width(8.dp))
                Text("Começar a gravar")
            }
        }
    }
}

@Composable
private fun <T> ChoiceRow(title: String, values: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(3.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            values.forEach { value -> FilterChip(selected == value, { onSelect(value) }, { Text(label(value)) }) }
        }
    }
}

@Composable
private fun RecordingControls(state: MainUiState, project: ReactionProject, viewModel: MainViewModel) {
    var confirmDiscard by remember { mutableStateOf(false) }
    var toolsExpanded by remember { mutableStateOf(false) }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Descartar esta gravação?") },
            text = { Text("A câmera, o áudio e os eventos desta sessão serão apagados. Nenhum vídeo será exportado.") },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Continuar gravando") } },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    viewModel.discardRecording()
                }) { Text("Descartar", color = MaterialTheme.colorScheme.error) }
            },
        )
    }
    Column(
        Modifier.fillMaxWidth().background(
            MaterialTheme.colorScheme.surfaceContainer,
            RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ).navigationBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Vídeo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${formatTime(state.sourcePositionMs)} / ${formatTime(project.metadata.durationMs)}",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            TextButton(onClick = { toolsExpanded = !toolsExpanded }) {
                Icon(Icons.Default.Tune, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (toolsExpanded) "Fechar ferramentas" else "Destaques e ajustes")
            }
        }
        Slider(
            value = state.sourcePositionMs.toFloat().coerceIn(0f, project.metadata.durationMs.toFloat()),
            onValueChange = { viewModel.seekTo(it.toLong()) },
            valueRange = 0f..project.metadata.durationMs.toFloat().coerceAtLeast(1f),
            modifier = Modifier.fillMaxWidth().height(32.dp),
        )
        Row(Modifier.fillMaxWidth().height(56.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.seekTo(state.sourcePositionMs - 5_000) }) {
                Icon(Icons.Default.FastRewind, "Voltar 5 segundos")
            }
            IconButton(onClick = viewModel::togglePlayback, modifier = Modifier.size(52.dp).background(MaterialTheme.colorScheme.primary, CircleShape)) {
                Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play ou pause")
            }
            IconButton(onClick = { viewModel.seekTo(state.sourcePositionMs + 5_000) }) {
                Icon(Icons.Default.FastForward, "Avançar 5 segundos")
            }
            IconButton(onClick = viewModel::toggleMute) {
                Icon(
                    if (state.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    "Áudio original",
                )
            }
        }
        if (toolsExpanded) {
            Column(
                Modifier.fillMaxWidth().background(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    RoundedCornerShape(16.dp),
                ).padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text("Marcador ao tocar no vídeo", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PointerStyle.entries.forEach { style ->
                        FilterChip(
                            selected = state.pointerStyle == style,
                            onClick = { viewModel.setPointerStyle(style) },
                            label = { Text(pointerStyleLabel(style)) },
                            leadingIcon = {
                                Image(
                                    painterResource(PointerMarkerVisuals.drawable(style)),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                        )
                    }
                }
                Row(Modifier.height(42.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ZoomOutMap, "Zoom", Modifier.size(18.dp))
                    Slider(
                        value = state.sourceTransform.scale,
                        onValueChange = { viewModel.changeSourceTransform(state.sourceTransform.copy(scale = it)) },
                        valueRange = 1f..4f,
                        modifier = Modifier.weight(1f),
                    )
                    Text(String.format(java.util.Locale.getDefault(), "%.1fx", state.sourceTransform.scale))
                    TextButton(enabled = state.sourceTransform.scale > 1f, onClick = viewModel::resetSourceZoom) {
                        Text("Resetar")
                    }
                }
                Row(Modifier.height(42.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (state.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        "Volume do vídeo",
                        Modifier.size(18.dp),
                    )
                    Slider(
                        value = project.sourceVolume,
                        onValueChange = { viewModel.updateSetup(sourceVolume = it) },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { confirmDiscard = true },
                modifier = Modifier.weight(1f).height(48.dp),
            ) {
                Icon(Icons.Default.DeleteOutline, null)
                Spacer(Modifier.width(6.dp))
                Text("Descartar")
            }
            Button(
                onClick = viewModel::finishRecording,
                modifier = Modifier.weight(1f).height(48.dp),
            ) {
                Icon(Icons.Default.Stop, null)
                Spacer(Modifier.width(6.dp))
                Text("Finalizar")
            }
        }
    }
}

private fun pointerStyleLabel(style: PointerStyle) = when (style) {
    PointerStyle.FINGER -> "Toque"
    PointerStyle.ARROW -> "Seta"
    PointerStyle.MOUSE -> "Clique"
}

private fun cameraShapeLabel(shape: CameraShape) = when (shape) {
    CameraShape.CIRCLE -> "Círculo"
    CameraShape.SQUARE -> "Quadrado"
    CameraShape.ROUNDED_RECTANGLE -> "Arredondado"
}

@Composable
private fun ExportScreen(state: MainUiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    val project = state.project
    val completed = state.exportState == WorkInfo.State.SUCCEEDED || project?.status == ProjectStatus.COMPLETED
    val exporting = state.exportState == WorkInfo.State.ENQUEUED || state.exportState == WorkInfo.State.RUNNING ||
        state.exportState == WorkInfo.State.BLOCKED || project?.status == ProjectStatus.EXPORTING
    val accent = when {
        completed -> MaterialTheme.colorScheme.secondary
        exporting -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }
    val title = when {
        completed -> "Sua reação está pronta"
        exporting -> "Criando seu vídeo"
        else -> "Não foi possível exportar"
    }
    val subtitle = when {
        completed -> "O MP4 foi salvo em Filmes/SeuReact."
        exporting -> "Você pode sair desta tela enquanto finalizamos tudo."
        else -> "Nada foi perdido. Os arquivos brutos continuam salvos."
    }
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surfaceContainerLow),
            ),
        ).statusBarsPadding().navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.size(78.dp),
                color = accent.copy(alpha = 0.14f),
                contentColor = accent,
                shape = CircleShape,
            ) {
                Icon(
                    when {
                        completed -> Icons.Default.CheckCircle
                        exporting -> Icons.Default.Refresh
                        else -> Icons.Default.ErrorOutline
                    },
                    null,
                    Modifier.padding(20.dp),
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(26.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(26.dp),
                tonalElevation = 2.dp,
            ) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (exporting) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Exportando", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text(
                                "${state.exportProgress}%",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = { state.exportProgress / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        )
                        Spacer(Modifier.height(16.dp))
                        TextButton(onClick = viewModel::cancelExport) { Text("Cancelar exportação") }
                    } else if (completed) {
                        Button(
                            onClick = {
                                project?.outputUri?.let { uri ->
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW).setDataAndType(uri.toUri(), "video/mp4")
                                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        ) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Assistir agora")
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                project?.outputUri?.let { uri ->
                                    context.startActivity(
                                        Intent.createChooser(
                                            Intent(Intent.ACTION_SEND).setType("video/mp4")
                                                .putExtra(Intent.EXTRA_STREAM, uri.toUri())
                                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                                            "Compartilhar reação",
                                        ),
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        ) {
                            Icon(Icons.Default.Share, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Compartilhar")
                        }
                    } else {
                        Text(
                            "Você pode tentar novamente sem regravar sua reação.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = viewModel::retryExport,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        ) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Tentar novamente")
                        }
                    }
                }
            }
            state.message?.let {
                Spacer(Modifier.height(14.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.42f),
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(it, modifier = Modifier.fillMaxWidth().padding(14.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = viewModel::backHome) { Text("Voltar para a galeria") }
        }
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
