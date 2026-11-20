package com.altea.salud

import java.util.UUID

/**
 * Contrato BLE de ALTEA. El firmware final deberá anunciar este servicio
 * y enviar JSON por la característica DATA_CHARACTERISTIC.
 *
 * Ejemplo de notificación:
 * {"bpm":78,"movement":0.12,"tilt":4.5,"fall":false,"battery":92}
 */
object DeviceProtocol {
    const val DEVICE_NAME = "ALTEA"
    val SERVICE_UUID: UUID = UUID.fromString("7c2e0001-8b3a-4f6d-a2c1-9d0e5b7a1000")
    val DATA_CHARACTERISTIC: UUID = UUID.fromString("7c2e0002-8b3a-4f6d-a2c1-9d0e5b7a1000")
    val COMMAND_CHARACTERISTIC: UUID = UUID.fromString("7c2e0003-8b3a-4f6d-a2c1-9d0e5b7a1000")
}
