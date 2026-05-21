package com.snapcam.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.snapcam.data.camera.CameraManager
import com.snapcam.data.local.MediaDatabase
import com.snapcam.data.repository.MediaRepositoryImpl
import com.snapcam.domain.repository.MediaRepository
import com.snapcam.navigation.NavGraph
import com.snapcam.presentation.camera.CameraViewModel
import com.snapcam.presentation.gallery.GalleryViewModel

class MainActivity : ComponentActivity() {
    private lateinit var cameraManager: CameraManager
    private lateinit var mediaRepository: MediaRepository
    private lateinit var database: MediaDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applicationContext.let { ctx ->
            database = Room.databaseBuilder(ctx, MediaDatabase::class.java, "snapcam_db").build()
            cameraManager = CameraManager(ctx)
            mediaRepository = MediaRepositoryImpl(database.mediaDao(), ctx.contentResolver)
        }
        setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                val navController = rememberNavController()
                NavGraph(
                    navController = navController,
                    cameraManager = cameraManager,
                    mediaRepository = mediaRepository
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.release()
    }
}

@Composable
fun cameraViewModel(cameraManager: CameraManager, mediaRepository: MediaRepository): CameraViewModel {
    return viewModel(factory = CameraViewModel.Factory(cameraManager, mediaRepository))
}

@Composable
fun galleryViewModel(mediaRepository: MediaRepository): GalleryViewModel {
    return viewModel(factory = GalleryViewModel.Factory(mediaRepository))
}
