package com.snapcam.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.snapcam.data.camera.CameraManager
import com.snapcam.domain.repository.MediaRepository
import com.snapcam.presentation.camera.CameraScreen
import com.snapcam.presentation.editor.EditorScreen
import com.snapcam.presentation.gallery.GalleryScreen
import com.snapcam.app.cameraViewModel
import com.snapcam.app.galleryViewModel

object Routes {
    const val CAMERA = "camera"
    const val GALLERY = "gallery"
    const val EDITOR = "editor/{mediaUri}"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    cameraManager: CameraManager,
    mediaRepository: MediaRepository
) {
    NavHost(navController = navController, startDestination = Routes.CAMERA) {
        composable(Routes.CAMERA) {
            CameraScreen(
                onNavigateToGallery = { navController.navigate(Routes.GALLERY) },
                viewModel = cameraViewModel(cameraManager, mediaRepository)
            )
        }
        composable(Routes.GALLERY) {
            GalleryScreen(
                onBack = { navController.popBackStack() },
                onItemClick = { item ->
                    navController.navigate("editor/${Uri.encode(item.uri)}")
                },
                viewModel = galleryViewModel(mediaRepository)
            )
        }
        composable(
            route = Routes.EDITOR,
            arguments = listOf(navArgument("mediaUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val mediaUri = backStackEntry.arguments?.getString("mediaUri") ?: return@composable
            EditorScreen(
                mediaUri = mediaUri,
                onBack = { navController.popBackStack() },
                mediaRepository = mediaRepository
            )
        }
    }
}
