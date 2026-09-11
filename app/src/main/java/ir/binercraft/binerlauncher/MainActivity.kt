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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.binercraft.binerlauncher.core.MinecraftVersion
import ir.binercraft.binerlauncher.game.LaunchGameActivity
import ir.binercraft.binerlauncher.minecraft.MinecraftPaths
import ir.binercraft.binerlauncher.minecraft.MinecraftVersionRepository
import ir.binercraft.binerlauncher.minecraft.ModrinthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BinerLauncherApp() }
    }
}

private val Background = Color(0xFF090B12)
private val SurfaceDark = Color(0xFF111522)
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
                    2 -> ModsScreen(selectedVersion, Modifier.padding(padding))
                    else -> SettingsScreen(Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(version: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
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
                    Text("نسخه $version", color = Accent2, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(18.dp))
                    Button(onClick = {
                        context.startActivity(Intent(context, LaunchGameActivity::class.java).apply {
                            putExtra(LaunchGameActivity.EXTRA_VERSION, version)
                            putExtra(LaunchGameActivity.EXTRA_USERNAME, "BinerPlayer")
                            putExtra(LaunchGameActivity.EXTRA_UUID, "00000000-0000-0000-0000-000000000000")
                        })
                    }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("PLAY")
                    }
                }
            }
        }
        item { StatusCard("Release versions", "1.7.10 → 26.2", Accent2) }
        item { StatusCard("Modrinth", "فعال", Accent2) }
        item { StatusCard("Install → Prepare → Launch", "فعال", Accent2) }
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
        item { Text("نسخه‌های Release", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold); Text("1.7.10 تا 26.2", color = Accent2) }
        if (loading) item { Text("در حال دریافت…", color = Accent2) }
        error?.let { item { Text("خطا: $it", color = Color(0xFFFF6B6B)) } }
        items(versions, key = { it.id }) { version ->
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text(version.id, color = Color.White, fontWeight = FontWeight.Bold); Text("release", color = Color(0xFF9BA3B5)) }
                    Button(onClick = { onSelect(version.id) }, shape = RoundedCornerShape(12.dp)) { Text(if (version.id == selectedVersion) "انتخاب شد" else "انتخاب") }
                }
            }
        }
    }
}

@Composable
private fun ModsScreen(version: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var loader by remember { mutableStateOf("fabric") }
    var results by remember { mutableStateOf<List<ModrinthService.ModResult>>(emptyList()) }
    var message by remember { mutableStateOf("") }
    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text("Mod Manager", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Text("Modrinth • $version", color = Accent2)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text("نام مود") })
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(loader == "fabric", { loader = "fabric" }, label = { Text("Fabric") })
            FilterChip(loader == "forge", { loader = "forge" }, label = { Text("Forge") })
            FilterChip(loader == "neoforge", { loader = "neoforge" }, label = { Text("NeoForge") })
        }
        Button(onClick = {
            if (query.isBlank()) return@Button
            scope.launch(Dispatchers.IO) {
                try {
                    val found = ModrinthService(MinecraftPaths(context)).search(query, version, loader)
                    withContext(Dispatchers.Main) { results = found; message = "${found.size} نتیجه" }
                } catch (t: Throwable) {
                    withContext(Dispatchers.Main) { message = t.message ?: "خطا" }
                }
            }
        }, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) { Text("جستجو در Modrinth") }
        Text(message, color = Color(0xFF9BA3B5))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(results) { mod ->
                Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(mod.title, color = Color.White, fontWeight = FontWeight.Bold); Text(mod.description, color = Color(0xFF9BA3B5), maxLines = 2) }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    ModrinthService(MinecraftPaths(context)).installLatest(mod.projectId, version, loader)
                                    withContext(Dispatchers.Main) { message = "${mod.title} نصب شد" }
                                } catch (t: Throwable) {
                                    withContext(Dispatchers.Main) { message = t.message ?: "خطای نصب" }
                                }
                            }
                        }) { Text("نصب") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("launcher", 0) }
    var memory by remember { mutableIntStateOf(prefs.getInt("memory", 2048)) }
    var width by remember { mutableIntStateOf(prefs.getInt("width", 1280)) }
    var height by remember { mutableIntStateOf(prefs.getInt("height", 720)) }
    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text("تنظیمات لانچر", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Text("تنظیمات ذخیره می‌شوند و در اجرای بعدی استفاده خواهند شد.", color = Color(0xFF9BA3B5))
        Spacer(Modifier.height(18.dp))
        Text("RAM: ${memory}MB", color = Color.White)
        Slider(value = memory.toFloat(), onValueChange = { memory = (it / 256).toInt() * 256 }, valueRange = 1024f..8192f, steps = 27, onValueChangeFinished = { prefs.edit().putInt("memory", memory).apply() })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(width.toString(), { width = it.toIntOrNull() ?: width; prefs.edit().putInt("width", width).apply() }, Modifier.weight(1f), label = { Text("Width") })
            OutlinedTextField(height.toString(), { height = it.toIntOrNull() ?: height; prefs.edit().putInt("height", height).apply() }, Modifier.weight(1f), label = { Text("Height") })
        }
        Spacer(Modifier.height(14.dp))
        Text("Java و تنظیمات JVM بر اساس نسخه Minecraft مدیریت می‌شوند.", color = Color(0xFF9BA3B5))
    }
}

@Composable
private fun StatusCard(title: String, state: String, color: Color) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(title, color = Color(0xFFDDE2EF)); Text(state, color = color, fontWeight = FontWeight.Bold) }
    }
}
