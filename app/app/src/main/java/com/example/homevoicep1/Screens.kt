package com.example.homevoicep1

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =====================================================
// COLORES (según el prototipo del enunciado)
// =====================================================
val Bg = Color(0xFF0B1020)        // fondo de la app
val CardBg = Color(0xFF161C2E)    // fondo de las tarjetas
val Border = Color(0xFF262E45)    // borde normal
val Accent = Color(0xFF4FD1C5)    // borde cuando la tarjeta tiene el foco
val Primary = Color(0xFF7C83FD)   // botones
val TextMain = Color(0xFFF1F3F9)
val TextDim = Color(0xFF9AA3B5)
val OnGreen = Color(0xFF48BB78)
val OffRed = Color(0xFFF56565)

// =====================================================
// PANTALLA PRINCIPAL
// onSpeak se llama cuando tocan el botón "Hablar"
// =====================================================
@Composable
fun SmartHomeApp(s: SmartHomeState, onSpeak: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ScreenTabs(s.screen)
        Spacer(Modifier.height(12.dp))

        StatusChip("● " + s.screen.listening, Accent)
        Spacer(Modifier.height(12.dp))

        // Título y número de pantalla
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                s.screen.title, color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text("Pantalla ${s.screen.number}/4", color = TextDim, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))

        // Contenido de la pantalla actual
        when (s.screen) {
            Screen.DASHBOARD -> DashboardScreen(s)
            Screen.DEVICE -> DeviceScreen(s)
            Screen.ROUTINE -> RoutineScreen(s)
            Screen.CONFIRM -> ConfirmScreen(s)
        }

        Spacer(Modifier.height(12.dp))
        FocusCard(focused = false) {
            Text(s.screen.hint, color = TextDim, fontSize = 12.sp)
        }

        // Botón de voz
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onSpeak,
            enabled = !s.isListening,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)
        ) {
            Text(
                if (s.isListening) "Escuchando..." else "Hablar (1 segundo)",
                fontWeight = FontWeight.Bold, fontSize = 16.sp
            )
        }

        // Voz detectada, último comando y mensaje
        Spacer(Modifier.height(16.dp))
        Text("Voz detectada: " + s.lastDetection, color = TextMain, fontWeight = FontWeight.Bold)
        Text("Último comando: " + s.lastCommand, color = TextMain, fontWeight = FontWeight.Bold)
        Text(s.message, color = TextDim, fontSize = 13.sp)

        // Botones que simulan los comandos (modo de prueba)
        Spacer(Modifier.height(12.dp))
        Text("Comandos simulados (modo de prueba)", color = TextDim, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        CommandPanel(onCommand = { cmd -> s.handle(cmd) })
    }
}

// Las 4 pestañas de arriba; se pinta la de la pantalla actual
@Composable
fun ScreenTabs(current: Screen) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (sc in Screen.entries) {
            val active = (sc == current)
            Text(
                sc.tab,
                color = if (active) Color.White else TextDim,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .background(if (active) Primary else CardBg, RoundedCornerShape(50))
                    .padding(vertical = 6.dp)
            )
        }
    }
}

// Tarjeta base. Si focused es true, el borde se pinta de otro color.
// "content" es lo que va dentro de la tarjeta.
@Composable
fun FocusCard(
    focused: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val borderWidth = if (focused) 2.dp else 1.dp
    val borderColor = if (focused) Accent else Border
    Column(
        modifier
            .fillMaxWidth()
            .background(CardBg, shape)
            .border(borderWidth, borderColor, shape)
            .padding(14.dp),
        content = content
    )
}

// Etiqueta pequeña redondeada (ON, OFF, GO, ACTIVA...)
@Composable
fun StatusChip(text: String, color: Color) {
    Text(
        text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
fun OnOffChip(on: Boolean) {
    if (on) {
        StatusChip("ON", OnGreen)
    } else {
        StatusChip("OFF", OffRed)
    }
}

// ---------- Pantalla 1: Inicio ----------
@Composable
fun DashboardScreen(s: SmartHomeState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DashboardCard(s, 0, Modifier.weight(1f))
        DashboardCard(s, 1, Modifier.weight(1f))
    }
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DashboardCard(s, 2, Modifier.weight(1f))
        DashboardCard(s, 3, Modifier.weight(1f))
    }
}

// index 0, 1, 2 = dispositivos; index 3 = Rutinas
@Composable
fun DashboardCard(s: SmartHomeState, index: Int, modifier: Modifier) {
    val title: String
    val description: String
    if (index == 3) {
        title = "Rutinas"
        description = "Estudio, noche y bienvenida"
    } else {
        title = s.devices[index].name
        description = s.devices[index].description
    }

    FocusCard(focused = (s.focus == index), modifier = modifier.height(130.dp)) {
        Text(title, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text(description, color = TextDim, fontSize = 12.sp)
        Spacer(Modifier.weight(1f)) // empuja la etiqueta hacia abajo
        if (index == 3) {
            StatusChip("ABRIR", Primary)
        } else {
            OnOffChip(s.deviceOn[index])
        }
    }
}

// ---------- Pantalla 2: Dispositivo ----------
@Composable
fun DeviceScreen(s: SmartHomeState) {
    val device = s.devices[s.selectedDevice]

    Text(device.name, color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))

    // Estado actual del dispositivo
    FocusCard(focused = false) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Estado actual", color = TextMain, fontWeight = FontWeight.Bold)
                Text(device.description, color = TextDim, fontSize = 12.sp)
            }
            OnOffChip(s.deviceOn[s.selectedDevice])
        }
    }
    Spacer(Modifier.height(12.dp))

    // Opción 0 y 1
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FocusCard(focused = (s.focus == 0), modifier = Modifier.weight(1f)) {
            Text("Encender", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Comando: on", color = TextDim, fontSize = 12.sp)
        }
        FocusCard(focused = (s.focus == 1), modifier = Modifier.weight(1f)) {
            Text("Apagar", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Comando: off", color = TextDim, fontSize = 12.sp)
        }
    }
    Spacer(Modifier.height(12.dp))

    // Opción 2
    FocusCard(focused = (s.focus == 2)) {
        Text("Volver al inicio", color = TextMain, fontWeight = FontWeight.Bold)
        Text("Comando: no", color = TextDim, fontSize = 12.sp)
    }
}

// ---------- Pantalla 3: Rutinas ----------
@Composable
fun RoutineScreen(s: SmartHomeState) {
    for (i in s.routines.indices) {
        val routine = s.routines[i]
        FocusCard(focused = (s.focus == i)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(routine.name, color = TextMain, fontWeight = FontWeight.Bold)
                    Text(routine.description, color = TextDim, fontSize = 12.sp)
                }
                if (s.runningRoutine == i) {
                    StatusChip("ACTIVA", OnGreen)
                } else {
                    StatusChip("GO", Primary)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

// ---------- Pantalla 4: Confirmación ----------
@Composable
fun ConfirmScreen(s: SmartHomeState) {
    FocusCard(focused = false) {
        Text(s.pendingText, color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Esta es una acción sensible. Di yes para aceptar o no para cancelar.",
            color = TextDim, fontSize = 13.sp
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FocusCard(focused = (s.focus == 0), modifier = Modifier.weight(1f)) {
                Text("yes", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Ejecutar", color = TextDim, fontSize = 12.sp)
            }
            FocusCard(focused = (s.focus == 1), modifier = Modifier.weight(1f)) {
                Text("no", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Cancelar y volver", color = TextDim, fontSize = 12.sp)
            }
        }
    }
}

// ---------- Botones de comandos simulados ----------
@Composable
fun CommandPanel(onCommand: (String) -> Unit) {
    val rows = listOf(
        listOf("up", "down", "left", "right"),
        listOf("yes", "no", "on", "off"),
        listOf("go", "stop")
    )
    for (row in rows) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (cmd in row) {
                Button(
                    onClick = { onCommand(cmd) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text(cmd)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}