package com.altea.salud

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.util.UUID

private val Red = Color(0xFFD9272E)
private val Ink = Color(0xFF18212B)
private val Muted = Color(0xFF66717D)

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private lateinit var store: LocalStore
    private var gatt: BluetoothGatt? = null
    private var scanCallback: ScanCallback? = null
    private val readings = mutableStateListOf<SensorReading>()
    private var current by mutableStateOf(SensorReading())
    private var connection by mutableStateOf("Sin conectar")
    private val devices = mutableStateListOf<BluetoothDevice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = LocalStore(this)
        readings.addAll(store.latestReadings())
        val permissions = if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.POST_NOTIFICATIONS)
            else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
        setContent {
            var tab by remember { mutableStateOf(0) }
            var profilePhone by remember { mutableStateOf(getPreferences(0).getString("phone", "") ?: "") }
            var emergencyPhone by remember { mutableStateOf(getPreferences(0).getString("emergency", "") ?: "") }
            var age by remember { mutableStateOf(getPreferences(0).getString("age", "") ?: "") }
            var weight by remember { mutableStateOf(getPreferences(0).getString("weight", "") ?: "") }
            var height by remember { mutableStateOf(getPreferences(0).getString("height", "") ?: "") }
            var sex by remember { mutableStateOf(getPreferences(0).getString("sex", "No especificado") ?: "No especificado") }
            var language by remember { mutableStateOf(getPreferences(0).getString("language", "Español") ?: "Español") }
            var historyMinutes by remember { mutableIntStateOf(getPreferences(0).getInt("history", 10)) }
            var sampleSeconds by remember { mutableIntStateOf(getPreferences(0).getInt("sample", 2)) }
            var conditions by remember { mutableStateOf(getPreferences(0).getString("conditions", "") ?: "") }
            var childTheme by remember { mutableStateOf(getPreferences(0).getBoolean("child", false)) }
            var symptom by remember { mutableStateOf("") }
            var symptomLog by remember { mutableStateOf(store.latestSymptoms().joinToString("\n")) }
            val requestPerms = { permissionLauncher.launch(permissions) }

            MaterialTheme(colorScheme = darkColorScheme(primary = Red, background = Ink, surface = Color(0xFF202B36))) {
                Scaffold(containerColor = if (childTheme) Color(0xFFFFF4E5) else Ink,
                    topBar = {
                        TopAppBar(
                            title = { Row(verticalAlignment = Alignment.CenterVertically) { androidx.compose.foundation.Image(painterResource(R.drawable.altea_logo), contentDescription = "Logotipo ALTEA", modifier = Modifier.size(36.dp)); Spacer(Modifier.width(8.dp)); Text("ALTEA Salud", color = if(childTheme) Ink else Color.White) } },
                            navigationIcon = {
                                IconButton(onClick = { tab = 3 }) { Icon(Icons.Default.Settings, "Configuración", tint = if(childTheme) Ink else Color.White) }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = if(childTheme) Color(0xFFFFE0B2) else Color(0xFF111820))
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            listOf("Inicio", "Gráfica", "Síntomas", "Historial").forEachIndexed { i, label ->
                                NavigationBarItem(selected = tab == i, onClick = { tab = i },
                                    icon = { Icon(listOf(Icons.Default.Favorite, Icons.Default.ShowChart, Icons.Default.MedicalServices, Icons.Default.History)[i], label) },
                                    label = { Text(label) })
                            }
                        }
                    }
                ) { padding ->
                    when(tab) {
                        0 -> LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            item {
                                Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(20.dp)) {
                                        Text("FRECUENCIA CARDIACA", color = Muted, fontSize = 12.sp)
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(if(current.bpm > 0) current.bpm.toString() else "--", color = Red, fontSize = 54.sp)
                                            Text(" BPM", color = Ink, modifier = Modifier.padding(bottom = 10.dp))
                                        }
                                        Text("Lectura del sensor; no es un diagnóstico médico.", color = Muted, fontSize = 12.sp)
                                    }
                                }
                            }
                            item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MetricCard("Movimiento", "%.2f".format(current.movement), Modifier.weight(1f))
                                MetricCard("Inclinación", "%.1f°".format(current.tilt), Modifier.weight(1f))
                            } }
                            item {
                                Card(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(16.dp)) {
                                        Text("Pulsera: $connection")
                                        Spacer(Modifier.height(8.dp))
                                        Button(onClick = { requestPerms(); startScan() }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.BluetoothSearching, null); Spacer(Modifier.width(8.dp)); Text("Buscar pulsera ALTEA") }
                                        if (devices.isNotEmpty()) {
                                            devices.forEach { device ->
                                                TextButton(onClick = { connect(device) }) { Text("Conectar a ${safeName(device)}") }
                                            }
                                        }
                                        Text("La pulsera debe estar encendida y cerca.", color = Muted, fontSize = 12.sp)
                                    }
                                }
                            }
                            item {
                                if(current.fall) AlertCard("Posible caída detectada", "Comprueba que la persona esté bien. La detección requiere validación del sensor.")
                                else AlertCard("Estado del sensor", if(current.bpm > 0) "Recibiendo datos de la pulsera." else "Esperando datos. Conecta la pulsera para comenzar.")
                            }
                            item {
                                OutlinedButton(onClick = { shareEmergency(profilePhone, emergencyPhone) }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Default.Sos, null); Spacer(Modifier.width(8.dp)); Text("Enviar aviso de emergencia")
                                }
                            }
                        }
                        1 -> LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                            item { Text("Frecuencia cardiaca", color = Color.White, fontSize = 22.sp); Text("Lecturas recibidas de la pulsera", color = Color.LightGray) }
                            item { Spacer(Modifier.height(16.dp)); BpmChart(readings.map { it.bpm }) }
                            item { Text("Últimas lecturas", color = Color.White, modifier = Modifier.padding(top = 16.dp)) }
                            items(readings.takeLast(30).reversed()) { Text("${it.bpm} BPM  •  movimiento ${"%.2f".format(it.movement)}  •  inclinación ${"%.1f".format(it.tilt)}°", color = Color.White, modifier = Modifier.padding(6.dp)) }
                        }
                        2 -> Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                            Text("¿Qué síntomas presenta?", color = Color.White, fontSize = 22.sp)
                            Text("Este registro ayuda a describir cómo se siente la persona; no determina una enfermedad.", color = Color.LightGray)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(value = symptom, onValueChange = { symptom = it }, label = { Text("Ej. mareo, cansancio, falta de aire...") }, modifier = Modifier.fillMaxWidth())
                            Button(onClick = { if(symptom.isNotBlank()) { store.addSymptom(symptom); symptomLog = store.latestSymptoms().joinToString("\n"); symptom = "" } }, modifier = Modifier.fillMaxWidth()) { Text("Guardar síntoma") }
                            Spacer(Modifier.height(12.dp))
                            Text(symptomLog, color = Color.White)
                        }
                        3 -> LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            item { Text("Configuración", color = Color.White, fontSize = 24.sp) }
                            item { SectionTitle("Datos personales") }
                            item { Field("Teléfono de la persona", profilePhone) { profilePhone = it } }
                            item { Field("Teléfono de emergencia", emergencyPhone) { emergencyPhone = it } }
                            item { Field("Edad", age) { age = it } }
                            item { Field("Peso (kg)", weight) { weight = it } }
                            item { Field("Estatura (cm)", height) { height = it } }
                            item { Field("Sexo (opcional)", sex) { sex = it } }
                            item { SectionTitle("Idioma y región") }
                            item { var expanded by remember { mutableStateOf(false) }; Box {
                                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("Idioma: $language") }
                                DropdownMenu(expanded, { expanded = false }) {
                                    listOf("Español", "English", "Français", "Italiano", "日本語", "中文", "Русский").forEach { DropdownMenuItem(text = { Text(it) }, onClick = { language = it; expanded = false }) }
                                }
                            } }
                            item { SectionTitle("Salud y antecedentes (opcional)") }
                            item { Field("Antecedentes / condiciones", conditions) { conditions = it } }
                            item { SectionTitle("Sensores e historial") }
                            item { Text("Frecuencia de lectura: cada $sampleSeconds segundos", color = Color.White) }
                            item { Slider(sampleSeconds.toFloat(), { sampleSeconds = it.toInt() }, valueRange = 1f..10f, steps = 8) }
                            item { Text("Guardar historial cada $historyMinutes minutos", color = Color.White) }
                            item { Slider(historyMinutes.toFloat(), { historyMinutes = it.toInt() }, valueRange = 5f..60f, steps = 10) }
                            item { Row(verticalAlignment = Alignment.CenterVertically) { Text("Tema infantil", color = Color.White, modifier = Modifier.weight(1f)); Switch(childTheme, { childTheme = it }) } }
                            item { Button(onClick = {
                                getPreferences(0).edit().putString("phone", profilePhone).putString("emergency", emergencyPhone)
                                    .putString("age", age).putString("weight", weight).putString("height", height)
                                    .putString("sex", sex).putString("language", language).putString("conditions", conditions)
                                    .putInt("history", historyMinutes).putInt("sample", sampleSeconds).putBoolean("child", childTheme).apply()
                            }, modifier = Modifier.fillMaxWidth()) { Text("Guardar configuración") } }
                            item { Text("Privacidad: los datos de medición y síntomas se guardan localmente en este teléfono. Esta versión no incluye anuncios ni analítica.", color = Color.LightGray, fontSize = 12.sp) }
                        }
                    }
                }
            }
        }
    }

    private fun startScan() {
        val adapter = (getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager).adapter ?: return
        if (!adapter.isEnabled) { connection = "Activa Bluetooth"; return }
        if (Build.VERSION.SDK_INT >= 31 && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) { connection = "Falta permiso Bluetooth"; return }
        devices.clear()
        connection = "Buscando..."
        val scanner = adapter.bluetoothLeScanner ?: return
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val name = try { result.device.name ?: result.scanRecord?.deviceName } catch (_: SecurityException) { null }
                if (name == DeviceProtocol.DEVICE_NAME && devices.none { it.address == result.device.address }) devices.add(result.device)
            }
            override fun onScanFailed(errorCode: Int) { connection = "Error de búsqueda: $errorCode" }
        }
        try { scanner.startScan(scanCallback) } catch (_: SecurityException) { connection = "Permiso Bluetooth denegado" }
    }

    private fun connect(device: BluetoothDevice) {
        if (Build.VERSION.SDK_INT >= 31 && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) { connection = "Falta permiso Bluetooth"; return }
        try {
            scanCallback?.let { (getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager).adapter.bluetoothLeScanner?.stopScan(it) }
            connection = "Conectando..."
            gatt?.close()
            gatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                    runOnUiThread { connection = if(newState == BluetoothProfile.STATE_CONNECTED) "Conectada" else "Desconectada" }
                    if(newState == BluetoothProfile.STATE_CONNECTED) g.discoverServices()
                }
                override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                    val ch = g.getService(DeviceProtocol.SERVICE_UUID)?.getCharacteristic(DeviceProtocol.DATA_CHARACTERISTIC)
                    if(ch != null) {
                        g.setCharacteristicNotification(ch, true)
                        val descriptor = ch.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                        if(descriptor != null) {
                            if (Build.VERSION.SDK_INT >= 33) g.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                            else { @Suppress("DEPRECATION") descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE; @Suppress("DEPRECATION") g.writeDescriptor(descriptor) }
                        }
                    } else runOnUiThread { connection = "Pulsera sin servicio ALTEA compatible" }
                }
                override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
                    @Suppress("DEPRECATION") val bytes = c.value ?: return
                    receive(bytes)
                }
                override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray) { receive(value) }
            })
        } catch (_: SecurityException) { connection = "Permiso Bluetooth denegado" }
    }

    private fun receive(bytes: ByteArray) {
        try {
            val j = JSONObject(String(bytes, Charsets.UTF_8))
            val r = SensorReading(j.optInt("bpm"), j.optDouble("movement", 0.0).toFloat(), j.optDouble("tilt", 0.0).toFloat(), j.optBoolean("fall"))
            runOnUiThread {
                current = r
                readings.add(r)
                if(readings.size > 300) readings.removeAt(0)
                store.addReading(r)
                if(r.fall) {
                    val v = if(Build.VERSION.SDK_INT >= 31) (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator else @Suppress("DEPRECATION") (getSystemService(VIBRATOR_SERVICE) as Vibrator)
                    if(Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(700, VibrationEffect.DEFAULT_AMPLITUDE)) else @Suppress("DEPRECATION") v.vibrate(700)
                }
            }
        } catch (_: Exception) { }
    }

    private fun safeName(d: BluetoothDevice): String = try { d.name ?: "ALTEA" } catch (_: Exception) { "ALTEA" }

    private fun shareEmergency(phone: String, emergency: String) {
        val number = emergency.ifBlank { phone }
        val msg = "ALERTA ALTEA: necesito ayuda. Mi ubicación debe compartirse desde el teléfono."
        val sms = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(number)}")).putExtra("sms_body", msg)
        val wa = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${number.filter { it.isDigit() }}?text=${Uri.encode(msg)}"))
        val chooser = Intent.createChooser(sms, "Enviar aviso (SMS o WhatsApp)")
        try { startActivity(chooser) } catch (_: Exception) { startActivity(wa) }
    }

    override fun onDestroy() {
        try { gatt?.disconnect(); gatt?.close() } catch (_: Exception) {}
        super.onDestroy()
    }
}

@Composable private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) { Column(Modifier.padding(16.dp)) { Text(title, color = Muted, fontSize = 12.sp); Text(value, color = Red, fontSize = 24.sp) } }
}
@Composable private fun AlertCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) { Text(title, color = Red); Text(body, color = Ink, fontSize = 13.sp) }
    }
}
@Composable private fun SectionTitle(text: String) { Text(text, color = Color(0xFFFF777D), fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp)) }
@Composable private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
}
@Composable private fun BpmChart(values: List<Int>) {
    Card(Modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(190.dp).padding(12.dp)) {
            if(values.size < 2) return@Canvas
            val minV = (values.minOrNull() ?: 40).coerceAtMost(50).toFloat()
            val maxV = (values.maxOrNull() ?: 140).coerceAtLeast(120).toFloat()
            val path = Path()
            values.forEachIndexed { i, v ->
                val x = i.toFloat() / (values.size - 1) * size.width
                val y = size.height - ((v - minV) / (maxV - minV).coerceAtLeast(1f)) * size.height
                if(i == 0) path.moveTo(x,y) else path.lineTo(x,y)
            }
            drawPath(path, Red, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
        }
    }
}
