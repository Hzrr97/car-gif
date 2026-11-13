package com.gifapp.car

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gifapp.car.ui.MainViewModel
import com.gifapp.car.ui.detail.DetailScreen
import com.gifapp.car.ui.files.FilesScreen
import com.gifapp.car.ui.local.LocalScreen
import com.gifapp.car.ui.settings.SettingsScreen
import com.gifapp.car.ui.theme.GifCarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()
        setContent {
            GifCarTheme {
                val vm: MainViewModel = viewModel()
                CarApp(vm)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}

@Composable
fun CarApp(vm: MainViewModel) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        vm.toast.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }
    MainScaffold(vm)
}

private enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    Files("files", "云端", Icons.Rounded.Cloud),
    Local("local", "本地", Icons.Rounded.DownloadDone),
    Settings("settings", "设置", Icons.Rounded.Settings)
}

@Composable
fun MainScaffold(vm: MainViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        CarNavRail(
            currentRoute = currentRoute,
            onSelect = { route ->
                navController.navigate(route) {
                    popUpTo("files") { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        NavHost(
            navController = navController,
            startDestination = "files",
            modifier = Modifier.weight(1f)
        ) {
            composable("files") {
                FilesScreen(vm, onOpen = { id -> navController.navigate("detail/$id") })
            }
            composable(
                "detail/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                DetailScreen(
                    vm = vm,
                    fileId = entry.arguments?.getLong("id") ?: 0L,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("local") { LocalScreen(vm) }
            composable("settings") { SettingsScreen(vm) }
        }
    }
}

@Composable
private fun CarNavRail(currentRoute: String?, onSelect: (String) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.width(128.dp).fillMaxHeight()
    ) {
        Column(
            Modifier.fillMaxHeight().padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.DirectionsCar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(24.dp))
            Dest.entries.forEach { dest ->
                val selected = currentRoute == dest.route ||
                    (dest == Dest.Files && currentRoute?.startsWith("detail") == true)
                val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                val fg = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .fillMaxWidth()
                        .height(92.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(bg)
                        .clickable { onSelect(dest.route) }
                ) {
                    Icon(dest.icon, dest.label, tint = fg, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.height(6.dp))
                    Text(dest.label, style = MaterialTheme.typography.labelMedium, color = fg)
                }
            }
        }
    }
}