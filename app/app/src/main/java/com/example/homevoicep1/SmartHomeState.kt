package com.example.homevoicep1

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// =====================================================
// PANTALLAS
// Cada pantalla guarda sus textos: título, nombre de la pestaña,
// número, qué comandos escucha y una pista de uso.
// =====================================================
enum class Screen(
    val title: String,
    val tab: String,
    val number: Int,
    val listening: String,
    val hint: String
) {
    DASHBOARD(
        "Home Dashboard", "Inicio", 1,
        "Escuchando: up / down / left / right / yes",
        "up/down/left/right mueven el foco y yes abre la tarjeta."
    ),
    DEVICE(
        "Control de dispositivo", "Dispositivo", 2,
        "Escuchando: on / off / yes / no",
        "on/off controlan el dispositivo, yes confirma la opción resaltada y no regresa."
    ),
    ROUTINE(
        "Rutinas", "Rutinas", 3,
        "Escuchando: up / down / go / stop / no",
        "up/down para moverse, go inicia, stop detiene y no regresa."
    ),
    CONFIRM(
        "Confirmar acción", "Confirmar", 4,
        "Escuchando: yes / no",
        "yes ejecuta la acción y no la cancela."
    )
}

// Un dispositivo o una rutina: nombre + descripción
data class Item(val name: String, val description: String)

// =====================================================
// ESTADO Y LÓGICA
// Todo comando entra por handle(cmd), venga de un botón o del modelo.
// =====================================================
class SmartHomeState {

    // En qué pantalla estamos y qué opción está resaltada
    var screen by mutableStateOf(Screen.DASHBOARD)
    var focus by mutableIntStateOf(0)

    // Lo que se muestra abajo de la pantalla
    var lastCommand by mutableStateOf("—")
    var message by mutableStateOf("Listo para recibir comandos")

    // Voz: última palabra detectada por el modelo (con su confianza)
    // y si estamos grabando en este momento
    var lastDetection by mutableStateOf("—")
    var isListening by mutableStateOf(false)

    // Dispositivos (0 Luces, 1 Ventilador, 2 TV)
    val devices = listOf(
        Item("Luces", "Sala y dormitorio"),
        Item("Ventilador", "Circulación y modo confort"),
        Item("TV", "Pantalla de la sala")
    )
    val deviceOn = mutableStateListOf(true, false, false)
    var selectedDevice by mutableIntStateOf(0)

    // Rutinas (-1 significa que ninguna está activa)
    val routines = listOf(
        Item("Modo Estudio", "Luz de escritorio, ventilador bajo, notificaciones en silencio"),
        Item("Modo Noche", "Luces cálidas, TV apagada, brillo reducido"),
        Item("Bienvenida", "Luces encendidas y música ambiental")
    )
    var runningRoutine by mutableIntStateOf(-1)

    // Confirmación:
    // pendingAction dice qué hacer si el usuario dice "yes"
    //   "apagar"  -> apagar el dispositivo seleccionado
    //   "detener" -> detener la rutina activa
    var pendingAction = ""
    var pendingText by mutableStateOf("")

    // A dónde volver después de confirmar o cancelar
    var returnScreen = Screen.DASHBOARD
    var returnFocus = 0

    // ---------- Punto único de entrada ----------
    fun handle(cmd: String) {
        lastCommand = cmd
        when (screen) {
            Screen.DASHBOARD -> handleDashboard(cmd)
            Screen.DEVICE -> handleDevice(cmd)
            Screen.ROUTINE -> handleRoutine(cmd)
            Screen.CONFIRM -> handleConfirm(cmd)
        }
    }

    // ---------- Resultado del modelo ----------
    // Siempre mostramos lo que detectó; solo actuamos si la confianza es suficiente.
    fun onVoiceResult(word: String, confidence: Float) {
        val percent = (confidence * 100).toInt()
        lastDetection = "$word ($percent %)"
        if (confidence >= CONFIDENCE_THRESHOLD) {
            handle(word)
        } else {
            message = "Confianza baja ($percent %), no se ejecutó nada"
        }
    }

    private fun invalid(cmd: String) {
        message = "\"$cmd\" no hace nada en esta pantalla"
    }

    // ---------- Pantalla 1: Inicio ----------
    // Las tarjetas están en una cuadrícula 2x2:
    //   0 Luces    1 Ventilador
    //   2 TV       3 Rutinas
    private fun handleDashboard(cmd: String) {
        when (cmd) {
            "left" -> {
                if (focus == 1 || focus == 3) focus = focus - 1
                message = "Foco en " + dashboardCardName()
            }
            "right" -> {
                if (focus == 0 || focus == 2) focus = focus + 1
                message = "Foco en " + dashboardCardName()
            }
            "up" -> {
                if (focus == 2 || focus == 3) focus = focus - 2
                message = "Foco en " + dashboardCardName()
            }
            "down" -> {
                if (focus == 0 || focus == 1) focus = focus + 2
                message = "Foco en " + dashboardCardName()
            }
            "yes" -> openFocusedCard()
            else -> invalid(cmd)
        }
    }

    private fun dashboardCardName(): String {
        if (focus == 3) return "Rutinas"
        return devices[focus].name
    }

    private fun openFocusedCard() {
        if (focus == 3) {
            screen = Screen.ROUTINE
            focus = 0
            message = "Abriste Rutinas"
        } else {
            selectedDevice = focus
            screen = Screen.DEVICE
            focus = 0
            message = "Abriste " + devices[selectedDevice].name
        }
    }

    // ---------- Pantalla 2: Dispositivo ----------
    // Opciones: 0 Encender, 1 Apagar, 2 Volver
    private fun handleDevice(cmd: String) {
        when (cmd) {
            "up", "left" -> {
                if (focus > 0) focus = focus - 1
                message = "Foco en " + deviceOptionName()
            }
            "down", "right" -> {
                if (focus < 2) focus = focus + 1
                message = "Foco en " + deviceOptionName()
            }
            "on" -> turnOn()
            "off" -> turnOff()
            "yes" -> {
                if (focus == 0) turnOn()
                else if (focus == 1) turnOff()
                else goBackToDashboard()
            }
            "no" -> goBackToDashboard()
            else -> invalid(cmd)
        }
    }

    private fun deviceOptionName(): String {
        return when (focus) {
            0 -> "Encender"
            1 -> "Apagar"
            else -> "Volver"
        }
    }

    private fun turnOn() {
        deviceOn[selectedDevice] = true
        message = devices[selectedDevice].name + ": encendido"
    }

    // Apagar es una acción sensible, por eso pide confirmación
    private fun turnOff() {
        val name = devices[selectedDevice].name
        if (!deviceOn[selectedDevice]) {
            message = "$name ya está apagado"
        } else {
            askConfirm("apagar", "¿Apagar $name?")
        }
    }

    private fun goBackToDashboard() {
        // Al volver, dejamos el foco en la tarjeta de donde veníamos
        if (screen == Screen.ROUTINE) {
            focus = 3
        } else {
            focus = selectedDevice
        }
        screen = Screen.DASHBOARD
        message = "De vuelta al inicio"
    }

    // ---------- Pantalla 3: Rutinas ----------
    private fun handleRoutine(cmd: String) {
        when (cmd) {
            "up" -> {
                if (focus > 0) focus = focus - 1
                message = "Foco en " + routines[focus].name
            }
            "down" -> {
                if (focus < routines.size - 1) focus = focus + 1
                message = "Foco en " + routines[focus].name
            }
            "go", "yes" -> {
                runningRoutine = focus
                message = "Iniciada: " + routines[focus].name
            }
            "stop" -> {
                if (runningRoutine == -1) {
                    message = "No hay ninguna rutina activa"
                } else {
                    askConfirm("detener", "¿Detener " + routines[runningRoutine].name + "?")
                }
            }
            "no" -> goBackToDashboard()
            else -> invalid(cmd)
        }
    }

    // ---------- Pantalla 4: Confirmación ----------
    private fun askConfirm(action: String, text: String) {
        pendingAction = action
        pendingText = text
        // Guardamos dónde estábamos para regresar después
        returnScreen = screen
        returnFocus = focus
        screen = Screen.CONFIRM
        focus = 0
        message = "Esperando confirmación"
    }

    private fun handleConfirm(cmd: String) {
        when (cmd) {
            "left" -> focus = 0
            "right" -> focus = 1
            "yes" -> {
                if (pendingAction == "apagar") deviceOn[selectedDevice] = false
                if (pendingAction == "detener") runningRoutine = -1
                pendingAction = ""
                screen = returnScreen
                focus = returnFocus
                message = "Acción ejecutada"
            }
            "no" -> {
                pendingAction = ""
                screen = returnScreen
                focus = returnFocus
                message = "Acción cancelada"
            }
            else -> invalid(cmd)
        }
    }
}