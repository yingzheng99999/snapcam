package com.snapcam.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.snapcam.presentation.camera.CameraScreen
import com.snapcam.presentation.editor.EditorScreen
import com.snapcam.presentation.gallery.GalleryScreen

object Routes {
    const val CAMERA = "camera"
    const val GALLERY = "gallery"
    const val EDITOR = "editor/{mediaUri}"
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.CAMERA) {
        composable(Routes.CAMERA) {
            CameraScreen(
                onNavigateToGallery = { navController.navigate(Routes.GALLERY) }
            )
        }
        composable(Routes.GALLERY) {
            GalleryScreen(
                onBack = { navController.popBackStack() },
                onItemClick = { item ->
                    navController.navigate("editor/${Uri.encode(item.uri.toString())}")
                }
            )
        }
        composable(
            route = Routes.EDITOR,
            arguments = listOf(navArgument("mediaUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val mediaUri = backStackEntry.arguments?.getString("mediaUri") ?: return@composable
            EditorScreen(
                mediaUri = mediaUri,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
