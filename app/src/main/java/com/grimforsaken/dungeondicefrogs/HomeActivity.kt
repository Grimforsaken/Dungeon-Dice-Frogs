package com.grimforsaken.dungeondicefrogs

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize(), color = Color(0xFF071009)) {
                    DungeonDiceFrogsHome(
                        onPlay = { launchGame(Screen.DUNGEON) },
                        onContinue = { launchGame(null) },
                        onTown = { launchGame(Screen.TOWN) },
                        onShop = {
                            Toast.makeText(this, "Walk to a shop door in Town and tap Enter.", Toast.LENGTH_SHORT).show()
                            launchGame(Screen.TOWN)
                        },
                        onHeroes = { launchGame(Screen.HERO) }
                    )
                }
            }
        }
    }

    private fun launchGame(targetScreen: Screen?) {
        val gameIntent = Intent(this, GameActivity::class.java)
        targetScreen?.let { gameIntent.putExtra(GameActivity.EXTRA_START_SCREEN, it.name) }
        startActivity(gameIntent)
    }
}

@Composable
private fun DungeonDiceFrogsHome(
    onPlay: () -> Unit,
    onContinue: () -> Unit,
    onTown: () -> Unit,
    onShop: () -> Unit,
    onHeroes: () -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }
    val background = Color(0xFF071009)
    val cream = Color(0xFFFFE9B4)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.app_branding),
            contentDescription = "Dungeon Dice Frogs app branding",
            modifier = Modifier.size(82.dp),
            contentScale = ContentScale.Fit
        )

        Image(
            painter = painterResource(R.drawable.home_logo),
            contentDescription = "Dungeon Dice Frogs",
            modifier = Modifier.fillMaxWidth(0.86f).aspectRatio(4f / 3f),
            contentScale = ContentScale.Fit
        )

        Spacer(Modifier.height(4.dp))
        ArtMenuButton(R.drawable.home_play, "Play", onPlay)
        ArtMenuButton(R.drawable.home_continue, "Continue", onContinue)
        ArtMenuButton(R.drawable.home_town, "Town", onTown)
        ArtMenuButton(R.drawable.home_shop, "Shop", onShop)
        ArtMenuButton(R.drawable.home_heroes, "Heroes", onHeroes)
        ArtMenuButton(R.drawable.home_settings, "Settings") { showSettings = true }

        Text(
            "Your main frog is saved until it dies.",
            color = cream,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Settings", fontWeight = FontWeight.Black) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(R.drawable.app_branding),
                        contentDescription = "Dungeon Dice Frogs branding",
                        modifier = Modifier.size(110.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text("Settings options will be expanded as the game systems are finalized.")
                }
            },
            confirmButton = { TextButton(onClick = { showSettings = false }) { Text("Close") } }
        )
    }
}

@Composable
private fun ArtMenuButton(drawable: Int, description: String, onClick: () -> Unit) {
    Image(
        painter = painterResource(drawable),
        contentDescription = description,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).aspectRatio(3f).clickable(onClick = onClick),
        contentScale = ContentScale.Fit
    )
}
