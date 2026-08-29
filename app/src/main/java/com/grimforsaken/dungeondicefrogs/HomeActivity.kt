package com.grimforsaken.dungeondicefrogs

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
                Surface(Modifier.fillMaxSize(), color = Color(0xFF0A120C)) {
                    DungeonDiceFrogsHome(
                        onPlay = { launchGame(false) },
                        onContinue = { launchGame(true) },
                        onTown = { launchGame(true) },
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

@Composable
private fun DungeonDiceFrogsHome(
    onPlay: () -> Unit,
    onContinue: () -> Unit,
    onTown: () -> Unit,
    onShop: () -> Unit,
    onHeroes: () -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }
    val gold = Color(0xFFFFC62D)
    val cream = Color(0xFFFFE9B4)
    val green = Color(0xFF176B32)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A120C))
            .padding(horizontal = 18.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.app_branding),
            contentDescription = "Dungeon Dice Frogs",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(250.dp)
        )

        Text(
            "DUNGEON DICE FROGS",
            color = gold,
            fontWeight = FontWeight.Black,
            fontSize = 25.sp
        )
        Text("Choose an adventure", color = cream, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))

        HomeButtonRow("TOWN", onTown, "PLAY", onPlay, green, gold)
        HomeButtonRow("SHOP", onShop, "HEROES", onHeroes, green, gold)
        HomeButtonRow("SETTINGS", { showSettings = true }, "CONTINUE", onContinue, green, gold)
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Settings") },
            text = { Text("Dungeon Dice Frogs development build. More game settings will be added as their systems are implemented.") },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun HomeButtonRow(
    leftText: String,
    leftAction: () -> Unit,
    rightText: String,
    rightAction: () -> Unit,
    green: Color,
    gold: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = leftAction,
            colors = ButtonDefaults.buttonColors(containerColor = green, contentColor = gold),
            modifier = Modifier.weight(1f).height(62.dp)
        ) {
            Text(leftText, fontWeight = FontWeight.Black)
        }
        Button(
            onClick = rightAction,
            colors = ButtonDefaults.buttonColors(containerColor = green, contentColor = gold),
            modifier = Modifier.weight(1f).height(62.dp)
        ) {
            Text(rightText, fontWeight = FontWeight.Black)
        }
    }
}
