package com.grimforsaken.dungeondicefrogs

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize(), color = Color.Black) {
                    DungeonDiceFrogsHome(
                        onPlay = { launchGame(false) },
                        onContinue = { launchGame(true) },
                        onTown = {
                            launchGame(true)
                        },
                        onShop = {
                            Toast.makeText(this, "Walk to a shop door in Town and tap Enter.", Toast.LENGTH_SHORT).show()
                            launchGame(true)
                        },
                        onHeroes = {
                            Toast.makeText(this, "Open the Hero tab to manage your frog and equipment.", Toast.LENGTH_SHORT).show()
                            launchGame(true)
                        }
                    )
                }
            }
        }
    }

    private fun launchGame(continueExisting: Boolean) {
        val gameIntent = Intent(this, MainActivity::class.java)
        if (continueExisting) gameIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(gameIntent)
    }
}

private data class MenuHitBox(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val action: HomeMenuAction
)

private enum class HomeMenuAction { TOWN, PLAY, SHOP, HEROES, SETTINGS, CONTINUE }

@Composable
private fun DungeonDiceFrogsHome(
    onPlay: () -> Unit,
    onContinue: () -> Unit,
    onTown: () -> Unit,
    onShop: () -> Unit,
    onHeroes: () -> Unit
) {
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1080f / 1600f)
        ) {
            Image(
                painter = painterResource(R.drawable.home_menu),
                contentDescription = "Dungeon Dice Frogs home menu",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )

            val scale = maxWidth.value / 1080f
            val hitBoxes = listOf(
                MenuHitBox(25f, 760f, 500f, 167f, HomeMenuAction.TOWN),
                MenuHitBox(555f, 760f, 500f, 167f, HomeMenuAction.PLAY),
                MenuHitBox(25f, 955f, 500f, 167f, HomeMenuAction.SHOP),
                MenuHitBox(555f, 955f, 500f, 167f, HomeMenuAction.HEROES),
                MenuHitBox(25f, 1150f, 500f, 167f, HomeMenuAction.SETTINGS),
                MenuHitBox(555f, 1150f, 500f, 167f, HomeMenuAction.CONTINUE)
            )

            hitBoxes.forEach { hit ->
                Box(
                    Modifier
                        .offset((hit.x * scale).dp, (hit.y * scale).dp)
                        .size((hit.width * scale).dp, (hit.height * scale).dp)
                        .clickable {
                            when (hit.action) {
                                HomeMenuAction.TOWN -> onTown()
                                HomeMenuAction.PLAY -> onPlay()
                                HomeMenuAction.SHOP -> onShop()
                                HomeMenuAction.HEROES -> onHeroes()
                                HomeMenuAction.SETTINGS -> showSettings = true
                                HomeMenuAction.CONTINUE -> onContinue()
                            }
                        }
                )
            }
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Settings") },
            text = {
                Text("Dungeon Dice Frogs development build. Game-specific settings will be added here as those options are implemented.")
            },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) { Text("Close") }
            }
        )
    }
}
