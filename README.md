# ALTEA Salud — proyecto Android

Aplicación Android nativa en Kotlin/Jetpack Compose, preparada para conectarse por Bluetooth Low Energy (BLE) a la pulsera ESP32 llamada **ALTEA**.

## Qué incluye esta primera base funcional
- Inicio con BPM, movimiento, inclinación y estado de conexión.
- Búsqueda BLE del dispositivo cuyo nombre anunciado sea `ALTEA`.
- Conexión GATT y recepción de notificaciones JSON.
- Gráfica local de BPM e historial local SQLite.
- Registro de síntomas.
- Configuración de perfil, teléfonos, idioma, antecedentes, frecuencia de muestreo e intervalo de historial.
- Vibración local cuando el firmware comunica `fall: true`.
- Acción para preparar un aviso por SMS; WhatsApp se puede abrir mediante su enlace. El usuario confirma el envío.
- No contiene anuncios, SDK publicitarios ni analítica.
- Las mediciones y síntomas se guardan localmente; las pantallas principales funcionan sin Internet.

## Contrato BLE acordado para el firmware final
El firmware que construiremos después deberá:
1. Anunciar el nombre BLE `ALTEA`.
2. Crear el servicio `7c2e0001-8b3a-4f6d-a2c1-9d0e5b7a1000`.
3. Crear una característica de notificación `7c2e0002-8b3a-4f6d-a2c1-9d0e5b7a1000`.
4. Enviar UTF-8 JSON como:
   `{"bpm":78,"movement":0.12,"tilt":4.5,"fall":false,"battery":92}`

Los UUID están centralizados en `DeviceProtocol.kt`. No cambies el contrato en uno de los dos lados sin actualizar el otro.

## Abrir en Android Studio
1. Descomprime el ZIP.
2. Abre Android Studio → **Open** → selecciona la carpeta `ALTEA_Salud_Android`.
3. Espera la sincronización de Gradle.
4. Conecta un teléfono Android, habilita opciones de desarrollador/depuración USB y ejecuta `app`.

Se necesita Android Studio con JDK 17 y Android SDK 35. Android Studio descargará las dependencias de compilación; el uso normal de la app no requiere Internet.

## Importante: límites médicos y de emergencia
Esta aplicación es un proyecto de apoyo, no un dispositivo médico certificado. El MAX30102 puede dar lecturas incorrectas por movimiento, ajuste, temperatura o contacto. No se deben presentar umbrales universales como diagnóstico ni usar esta app para descartar una emergencia. La detección de caídas requiere calibración y pruebas controladas.

El botón de emergencia abre una aplicación de mensajería con un texto preparado: **no envía mensajes silenciosamente ni garantiza entrega**. WhatsApp necesita conexión a Internet; SMS depende de cobertura celular y saldo/plan. Compartir ubicación requiere implementar y conceder permisos de ubicación; esta primera base no adjunta coordenadas todavía.

Android limita el trabajo en segundo plano y puede detener procesos por ahorro de batería. `MonitoringService` es el punto de partida para el servicio persistente; en la siguiente iteración lo conectaremos al cliente BLE para que la conexión continúe con la pantalla bloqueada. No se promete funcionamiento infalible en todos los teléfonos.

## Pendiente para la siguiente iteración
- Integrar el cliente BLE al servicio en primer plano y probar reconexión automática.
- Implementar persistencia de lecturas según el intervalo elegido (5–60 min; el selector ya está en ajustes).
- Traducciones completas reales para cada idioma (el selector está preparado, la interfaz actual aún está principalmente en español).
- Permisos y obtención de ubicación con consentimiento explícito.
- Flujo de emergencia configurable y pruebas de fiabilidad.
- Personalización visual completa (fondos/widgets y temas infantiles/adultos).
- Calibrar detección de caídas y criterios de alertas junto con el firmware.

**No usar como único medio para vigilar la salud de una persona ni para decidir si necesita atención médica.**
