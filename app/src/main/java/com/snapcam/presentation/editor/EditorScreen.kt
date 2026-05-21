package com.snapcam.presentation.editor

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun EditorScreen(
    mediaUri: String,
    onBack: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(mediaUri) {
        viewModel.loadImage(Uri.parse(mediaUri))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1B2D))
            .systemBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
            }
            Text("编辑图片", color = Color.White, fontSize = 18.sp)
            IconButton(onClick = { viewModel.save() }) {
                if (state.isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Check, "Save", tint = Color.White)
                }
            }
        }

        // Image preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A1A2E)),
            contentAlignment = Alignment.Center
        ) {
            state.bitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentScale = ContentScale.Fit
                )
            } ?: Text("Loading...", color = Color.Gray)
        }

        // Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Adjustment sliders
            AdjustSlider(
                icon = Icons.Default.BrightnessHigh,
                label = "亮度",
                value = state.brightness,
                range = -1f..1f,
                onValueChange = { viewModel.setBrightness(it) }
            )
            AdjustSlider(
                icon = Icons.Default.Contrast,
                label = "对比度",
                value = state.contrast,
                range = 0f..3f,
                onValueChange = { viewModel.setContrast(it) }
            )
            AdjustSlider(
                icon = Icons.Default.Palette,
                label = "饱和度",
                value = state.saturation,
                range = 0f..3f,
                onValueChange = { viewModel.setSaturation(it) }
            )

            Spacer(Modifier.height(8.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton(icon = Icons.Default.RotateRight, label = "旋转") {
                    viewModel.rotateRight()
                }
                ActionButton(icon = Icons.Default.Crop, label = "裁切") {
                    viewModel.toggleCrop()
                }
                ActionButton(icon = Icons.Default.CompareArrows, label = "对比") {
                    viewModel.setBrightness(0f)
                    viewModel.setContrast(1f)
                    viewModel.setSaturation(1f)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Save & Reset buttons
            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("保存到相册", fontSize = 16.sp, color = Color.White)
            }
        }
    }

    // Error snackbar
    state.error?.let { err ->
        androidx.compose.material3.Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                androidx.compose.material3.TextButton(onClick = { viewModel.clearError() }) {
                    Text("关闭", color = Color.White)
                }
            }
        ) {
            Text(err, color = Color.White)
        }
    }
}

@Composable
fun AdjustSlider(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.width(56.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF2563EB),
                activeTrackColor = Color(0xFF2563EB),
                inactiveTrackColor = Color(0x44FFFFFF)
            )
        )
        Text(
            String.format("%.1f", value),
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.width(36.dp)
        )
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(icon, label, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Text(label, color = Color.Gray, fontSize = 11.sp)
    }
}
