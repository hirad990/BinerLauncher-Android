package ir.binercraft.binerlauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.binercraft.binerlauncher.core.MinecraftVersion
import ir.binercraft.binerlauncher.game.LaunchGameActivity
import ir.binercraft.binerlauncher.minecraft.MinecraftVersionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BinerLauncherApp() }
    }
}

private val Background = Color(0xFF090B12)
private val SurfaceDark = Color(0xFF111522)
private val Accent = Color(0xFF6C63FF)
private val Accent2 = Color(0xFF22D3EE)

@Composable
fun BinerLauncherApp() {
    var selected by remember { mutableIntStateOf(0) }
    var selectedVersion by remember { mutableStateOf("1.21.11") }
    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = Background) {
            Scaffold(containerColor = Background, bottomBar = {
                NavigationBar(containerColor = SurfaceDark) {
                    NavigationBarItem(selected == 0, { selected = 0 }, { Icon(Icons.Default.Gamepad, null) }, label = { Text("خانه") })
                    NavigationBarItem(selected == 1, { selected = 1 }, { Icon(Icons.Default.Download, null) }, label = { Text("نسخه‌ها") })
                    NavigationBarItem(selected == 2, { selected = 2 }, { Icon(Icons.Default.Extension, null) }, label = { Text("مودها") })
                    NavigationBarItem(selected == 3, { selected = 3 }, { Icon(Icons.Default.Settings, null) }, label = { Text("تنظیمات") })
                }
            }) { padding ->
                when (selected) {
                    0 -> HomeScreen(selectedVersion, Modifier.padding(padding))
                    1 -> VersionsScreen(selectedVersion, { selectedVersion = it }, Modifier.padding(padding))
                    2 -> PlaceholderScreen("مدیریت مودها", Icons.Default.Extension, Modifier.padding(padding))
                    else -> PlaceholderScreen("تنظیمات لانچر", Icons.Default.Tune, Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(version: String, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Spacer(Modifier.height(18.dp))
            Text("BINER", color = Accent2, fontWeight = FontWeight.Bold)
            Text("Launcher", style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
            Text("Minecraft Java Edition روی Android", color = Color(0xFF9BA3B5))
        }
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF171C2B))) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Gamepad, null, tint = Accent2, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.width(14.dp))
                        Column { Text("Minecraft Java", color = Color.White, fontWeight = FontWeight.Bold); Text("نسخه $version", color = Accent2) }
                    }
                    Spacer(Modifier.height(18.dp))
                    Button(onClick = {
                        startActivity(Intent(this@MainActivity, LaunchGameActivity::class.java).apply {
                            putExtra(LaunchGameActivity.EXTRA_VERSION, version)
                            putExtra(LaunchGameActivity.EXTRA_USERNAME, "BinerPlayer")
                            putExtra(LaunchGameActivity.EXTRA_UUID, "00000000-0000-0000-0000-000000000000")
                        })
                    }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text("PLAY")
                    }
                }
            }
        }
        item {
            Text("هسته‌های آماده", color = Color.White, fontWeight = FontWeight.Bold)
            StatusCard("Version Manifest", "فعال", Accent2)
            StatusCard("Client / Libraries / Assets", "فعال", Accent2)
            StatusCard("Java Runtime Manager", "فعال", Accent2)
            StatusCard("Launch Planner / Executor", "فعال", Accent2)
            StatusCard("Android Native Surface", "فعال", Accent2)
        }
    }
}

@Composable
private fun VersionsScreen(selectedVersion: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var versions by remember { mutableStateOf<List<MinecraftVersion>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try { versions = withContext(Dispatchers.IO) { MinecraftVersionRepository().fetchVersions() } }
        catch (t: Throwable) { error = t.message ?: "خطای ناشناخته" }
        finally { loading = false }
    }
    LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("نسخه‌های Minecraft", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Text("نسخه انتخابی: $selectedVersion", color = Accent2)
        }
        if (loading) item { Text("در حال دریافت لیست نسخه‌ها…", color = Accent2) }
        error?.let { item { Text("خطا: $it", color = Color(0xFFFF6B6B)) } }
        items(versions.take(60), key = { it.id }) { version ->
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text(version.id, color = Color.White, fontWeight = FontWeight.Bold); Text(version.type, color = Color(0xFF9BA3B5)) }
                    Button(onClick = { onSelect(version.id) }, shape = RoundedCornerShape(12.dp)) { Text(if (version.id == selectedVersion) "انتخاب شد" else "انتخاب") }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(title: String, state: String, color: Color) {
    Card(Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(title, color = Color(0xFFDDE2EF)); Text(state, color = color, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun PlaceholderScreen(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, null, tint = Accent2, modifier = Modifier.size(54.dp))
        Spacer(Modifier.height(18.dp))
        Text(title, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("این بخش در معماری اصلی لانچر قرار گرفته و بعد از هسته اجرا کامل می‌شود.", color = Color(0xFF9BA3B5))
    }
}
