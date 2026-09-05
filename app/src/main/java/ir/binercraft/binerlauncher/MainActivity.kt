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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
        Surface(modifier = Modifier.fillMaxSize(), color = Background) {
            Scaffold(
                containerColor = Background,
                bottomBar = {
                    NavigationBar(containerColor = SurfaceDark) {
                        NavigationBarItem(
                            selected = selected == 0,
                            onClick = { selected = 0 },
                            icon = { Icon(Icons.Default.Gamepad, null) },
                            label = { Text("خانه") }
                        )
                        NavigationBarItem(
                            selected = selected == 1,
                            onClick = { selected = 1 },
                            icon = { Icon(Icons.Default.Download, null) },
                            label = { Text("نسخه‌ها") }
                        )
                        NavigationBarItem(
                            selected = selected == 2,
                            onClick = { selected = 2 },
                            icon = { Icon(Icons.Default.Extension, null) },
                            label = { Text("مودها") }
                        )
                        NavigationBarItem(
                            selected = selected == 3,
                            onClick = { selected = 3 },
                            icon = { Icon(Icons.Default.Settings, null) },
                            label = { Text("تنظیمات") }
                        )
                    }
                }
            ) { padding ->
                when (selected) {
                    0 -> HomeScreen(Modifier.padding(padding))
                    1 -> PlaceholderScreen("نسخه‌های Minecraft", Icons.Default.Download, Modifier.padding(padding))
                    2 -> PlaceholderScreen("مدیریت مودها", Icons.Default.Extension, Modifier.padding(padding))
                    else -> PlaceholderScreen("تنظیمات لانچر", Icons.Default.Tune, Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(Modifier.height(18.dp))
            Text(
                text = "BINER",
                style = MaterialTheme.typography.labelLarge,
                color = Accent2,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Launcher",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Minecraft Java Edition روی Android",
                color = Color(0xFF9BA3B5)
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceSoft)
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(54.dp).background(Accent, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Gamepad, null, tint = Color.White)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Minecraft Java", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Runtime آماده نشده", color = Color(0xFFFFC857))
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(disabledContainerColor = Color(0xFF292E3E))
                    ) {
                        Text("به‌زودی: اجرای Minecraft")
                    }
                }
            }
        }

        item {
            Text("وضعیت پروژه", color = Color.White, fontWeight = FontWeight.Bold)
            StatusCard("UI و هسته لانچر", "فعال", Accent2)
            StatusCard("مدیریت نسخه‌ها", "در حال ساخت", Accent)
            StatusCard("Java Runtime + Native", "مرحله بعد", Color(0xFFFFC857))
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun StatusCard(title: String, state: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Color(0xFFDDE2EF))
            Text(state, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = Accent2, modifier = Modifier.size(54.dp))
        Spacer(Modifier.height(18.dp))
        Text(title, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("این بخش در معماری اصلی لانچر قرار گرفته و در مرحله بعد کامل می‌شود.", color = Color(0xFF9BA3B5))
    }
}
