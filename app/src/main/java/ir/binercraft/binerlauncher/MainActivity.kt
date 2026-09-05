package ir.binercraft.binerlauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.binercraft.binerlauncher.core.MinecraftVersion
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
private val SurfaceSoft = Color(0xFF171C2B)
private val Accent = Color(0xFF6C63FF)
private val Accent2 = Color(0xFF22D3EE)

@Composable
fun BinerLauncherApp() {
    var selected by remember { mutableIntStateOf(0) }
    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = Background) {
            Scaffold(
                containerColor = Background,
                bottomBar = {
                    NavigationBar(containerColor = SurfaceDark) {
                        NavigationBarItem(selected == 0, { selected = 0 }, { Icon(Icons.Default.Gamepad, null) }, label = { Text("خانه") })
                        NavigationBarItem(selected == 1, { selected = 1 }, { Icon(Icons.Default.Download, null) }, label = { Text("نسخه‌ها") })
                        NavigationBarItem(selected == 2, { selected = 2 }, { Icon(Icons.Default.Extension, null) }, label = { Text("مودها") })
                        NavigationBarItem(selected == 3, { selected = 3 }, { Icon(Icons.Default.Settings, null) }, label = { Text("تنظیمات") })
                    }
                }
            ) { padding ->
                when (selected) {
                    0 -> HomeScreen(Modifier.padding(padding))
                    1 -> VersionsScreen(Modifier.padding(padding))
                    2 -> PlaceholderScreen("مدیریت مودها", Icons.Default.Extension, Modifier.padding(padding))
                    else -> PlaceholderScreen("تنظیمات لانچر", Icons.Default.Tune, Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Spacer(Modifier.height(18.dp))
            Text("BINER", color = Accent2, fontWeight = FontWeight.Bold)
            Text("Launcher", style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
            Text("Minecraft Java Edition روی Android", color = Color(0xFF9BA3B5))
        }
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = SurfaceSoft)) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(54.dp).background(Accent, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Gamepad, null, tint = Color.White)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Minecraft Java", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("هسته دانلود فعال شد", color = Accent2)
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(disabledContainerColor = Color(0xFF292E3E))) {
                        Text("اجرای Minecraft — مرحله Native")
                    }
                }
            }
        }
        item {
            Text("هسته‌های آماده", color = Color.White, fontWeight = FontWeight.Bold)
            StatusCard("Mojang Version Manifest", "فعال", Accent2)
            StatusCard("Client / Version Downloader", "فعال", Accent2)
            StatusCard("Java Runtime Manager", "فعال", Accent2)
            StatusCard("NDK / Native Bridge", "فعال", Accent2)
            StatusCard("LWJGL / GLFW", "مرحله بعد", Color(0xFFFFC857))
        }
    }
}

@Composable
private fun VersionsScreen(modifier: Modifier = Modifier) {
    var versions by remember { mutableStateOf<List<MinecraftVersion>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            versions = withContext(Dispatchers.IO) { MinecraftVersionRepository().fetchVersions() }
        } catch (t: Throwable) {
            error = t.message ?: "خطای ناشناخته"
        } finally {
            loading = false
        }
    }

    LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("نسخه‌های Minecraft", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Text("لیست از Version Manifest رسمی Minecraft دریافت می‌شود.", color = Color(0xFF9BA3B5))
            Spacer(Modifier.height(10.dp))
        }
        if (loading) item { Text("در حال دریافت لیست نسخه‌ها…", color = Accent2) }
        error?.let { message -> item { Text("خطا: $message", color = Color(0xFFFF6B6B)) } }
        items(versions.take(60), key = { it.id }) { version ->
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(version.id, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(version.type, color = Color(0xFF9BA3B5))
                    }
                    Button(onClick = {}, shape = RoundedCornerShape(12.dp)) { Text("انتخاب") }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(title: String, state: String, color: Color) {
    Card(Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color(0xFFDDE2EF))
            Text(state, color = color, fontWeight = FontWeight.Bold)
        }
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
