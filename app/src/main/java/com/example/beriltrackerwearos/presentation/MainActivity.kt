package com.example.beriltrackerwearos.presentation

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

// Modelimizi optimize ettik: Artık saat verisi UI'a gelmeden önce "String" formatına çevrilmiş oluyor.
data class BerilLog(
    val id: String,
    val type: String,
    val timeString: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                BerilTrackerWearApp { type ->
                    saveLogToFirebase(type)
                }
            }
        }
    }

    private fun saveLogToFirebase(type: String) {
        val db = Firebase.firestore
        val now = System.currentTimeMillis()

        val endTime = if (type == "BEZ") now else now + (15 * 60 * 1000)
        val side = if (type == "EMZIRME") "Sol" else null
        val amount = if (type == "SUT") 30 else null
        val status = if (type == "BEZ") listOf("Çok Çişli") else null

        val logData = hashMapOf(
            "type" to type,
            "startTime" to now,
            "endTime" to endTime,
            "side" to side,
            "amount" to amount,
            "status" to status,
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("logs").add(logData)
            .addOnSuccessListener {
                Toast.makeText(this, "$type eklendi", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Hata!", Toast.LENGTH_SHORT).show()
            }
    }
}

@Composable
fun BerilTrackerWearApp(onLogClick: (String) -> Unit) {
    val listState = rememberScalingLazyListState()
    val recentLogs = remember { mutableStateListOf<BerilLog>() }

    LaunchedEffect(Unit) {
        val db = Firebase.firestore
        // OPTİMİZASYON 1: Formatter sadece 1 kez yaratılır.
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())

        db.collection("logs")
            .orderBy("startTime", Query.Direction.DESCENDING)
            .limit(5)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                recentLogs.clear()
                for (doc in snapshot.documents) {
                    val type = doc.getString("type") ?: "BİLİNMİYOR"
                    val startTime = doc.getLong("startTime") ?: 0L

                    // UI tarafını yormamak için tarihi veritabanından çekerken formatlıyoruz.
                    val timeString = formatter.format(Date(startTime))

                    recentLogs.add(BerilLog(doc.id, type, timeString))
                }
            }
    }

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        state = listState,
        autoCentering = AutoCenteringParams(itemIndex = 0)
    ) {
        item {
            ListHeader { Text("Beril Tracker 🍼", color = Color.White) }
        }

        item {
            Chip(
                modifier = Modifier.fillMaxWidth(0.9f),
                onClick = { onLogClick("EMZIRME") },
                label = { Text("🤱 Emzirme (Sol)", color = Color.Black) },
                colors = ChipDefaults.primaryChipColors(backgroundColor = Color(0xFFF48FB1))
            )
        }

        item {
            Chip(
                modifier = Modifier.fillMaxWidth(0.9f),
                onClick = { onLogClick("SUT") },
                label = { Text("🍼 Süt", color = Color.Black) },
                colors = ChipDefaults.primaryChipColors(backgroundColor = Color(0xFF90CAF9))
            )
        }

        item {
            Chip(
                modifier = Modifier.fillMaxWidth(0.9f),
                onClick = { onLogClick("BEZ") },
                label = { Text("💩 Bez", color = Color.Black) },
                colors = ChipDefaults.primaryChipColors(backgroundColor = Color(0xFFA5D6A7))
            )
        }

        item {
            Chip(
                modifier = Modifier.fillMaxWidth(0.9f),
                onClick = { onLogClick("UYKU") },
                label = { Text("😴 Uyku", color = Color.Black) },
                colors = ChipDefaults.primaryChipColors(backgroundColor = Color(0xFFCE93D8))
            )
        }

        if (recentLogs.isNotEmpty()) {
            item {
                ListHeader {
                    Text("Son Kayıtlar", color = Color.LightGray, fontSize = 12.sp)
                }
            }

            // OPTİMİZASYON 2: "key = { it.id }" eklendi. Artık sadece değişen satırlar çizilecek.
            items(items = recentLogs, key = { it.id }) { log ->
                val (icon, bgColor) = when(log.type) {
                    "EMZIRME" -> "🤱" to Color(0xFFF48FB1)
                    "SUT" -> "🍼" to Color(0xFF90CAF9)
                    "BEZ" -> "💩" to Color(0xFFA5D6A7)
                    "UYKU" -> "😴" to Color(0xFFCE93D8)
                    else -> "📝" to Color.DarkGray
                }

                Chip(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    onClick = { },
                    label = { Text("$icon ${log.type} - ${log.timeString}", color = Color.Black) }, // Önceden hesaplanmış String'i kullandık
                    colors = ChipDefaults.primaryChipColors(backgroundColor = bgColor)
                )
            }
        }
    }
}