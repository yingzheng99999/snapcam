package com.snapcam.presentation.camera

import android.Manifest
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.LifecycleOwner
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.snapcam.domain.model.CameraMode
import com.snapcam.domain.model.Filter

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onNavigateToGallery: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val audioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    if (!cameraPermission.status.isGranted) {
        PermissionGate(
            text = "需要相机权限才能拍照和录像",
            onRequest = { cameraPermission.launchPermissionRequest() }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F1B2D))) {
            CameraPreview(modifier = Modifier.fillMaxSize(), viewModel = viewModel)
            CameraTopBar(state, viewModel)
            CameraBottomBar(state, viewModel, onNavigateToGallery)
            ZoomSlider(state, viewModel)

            AnimatedVisibility(visible = state.showFilterCarousel) {
                FilterCarousel(
                    selected = state.selectedFilter,
                    onSelect = { viewModel.selectFilter(it) },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 140.dp)
                )
            }
        }
    }
}

@Composable
fun CameraPreview(modifier: Modifier, viewModel: CameraViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsState()

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    LaunchedEffect(Unit) {
        // Camera will be initialized via lifecycle in CameraPreview
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )

    LaunchedEffect(lifecycleOwner) {
        viewModel.setMode(CameraMode.PHOTO)
    }
}

@Composable
fun CameraTopBar(state: CameraUiState, viewModel: CameraViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .statusBarsPadding(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { viewModel.toggleFlash() }) {
            Icon(
                if (state.flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = "Flash",
                tint = Color.White
            )
        }
        Text(
            if (state.mode == CameraMode.PHOTO) "拍照" else "录像",
            color = Color.White, fontSize = 16.sp
        )
        IconButton(onClick = { viewModel.switchLens() }) {
            Icon(Icons.Default.Cameraswitch, "Switch lens", tint = Color.White)
        }
    }
}

@Composable
fun CameraBottomBar(
    state: CameraUiState,
    viewModel: CameraViewModel,
    onNavigateToGallery: () -> Unit
) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateToGallery) {
                Icon(Icons.Default.PhotoLibrary, "Gallery", tint = Color.White, modifier = Modifier.size(28.dp))
            }

            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .clickable {
                            if (state.mode == CameraMode.PHOTO) viewModel.capturePhoto()
                            else viewModel.toggleVideo()
                        }
                ) {
                    if (state.isRecording) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color.Red, RoundedCornerShape(4.dp))
                                .align(Alignment.Center)
                        )
                    }
                }
            }

            IconButton(onClick = { viewModel.toggleFilterCarousel() }) {
                Icon(Icons.Default.FilterCenterFocus, "Filters", tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModeButton("拍照", state.mode == CameraMode.PHOTO) { viewModel.setMode(CameraMode.PHOTO) }
            ModeButton("录像", state.mode == CameraMode.VIDEO) { viewModel.setMode(CameraMode.VIDEO) }
        }
    }
}

@Composable
fun ModeButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        color = if (selected) Color.White else Color.Gray,
        fontSize = 14.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) Color(0x332563EB) else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 6.dp)
    )
}

@Composable
fun ZoomSlider(state: CameraUiState, viewModel: CameraViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("0.5x", "1x", "2x", "5x").forEach { label ->
            val zoom = label.removeSuffix("x").toFloatOrNull() ?: 1f
            Text(
                text = label,
                color = if (kotlin.math.abs(state.zoomLevel - zoom) < 0.1f) Color.White else Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .clickable { viewModel.setZoom(zoom) }
            )
        }
    }
}

@Composable
fun FilterCarousel(selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier.fillMaxWidth().background(Color(0x99000000)).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(Filter.PRESETS) { filter ->
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (filter.name == selected) Color(0xFF2563EB) else Color(0x44FFFFFF))
                    .clickable { onSelect(filter.name) }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(filter.name, color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun PermissionGate(text: String, onRequest: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0F1B2D)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text, color = Color.White, fontSize = 18.sp)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRequest) { Text("授予权限") }
        }
    }
}
