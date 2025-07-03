# ChatPlataformasMqtt

# Integrantes
- Diaz Portilla, Carlo Rodrigo
- Mamani Cañari, Gabriel Antony

## 📱 Descripción

ChatPlataformasMqtt es una aplicación de mensajería en tiempo real para Android que utiliza el protocolo MQTT para la comunicación. Desarrollada con Kotlin y Jetpack Compose siguiendo la arquitectura MVVM, permite a los usuarios conectarse a un broker MQTT y participar en salas de chat con otros usuarios.

## 🎯 Características

- **Mensajería en tiempo real** mediante protocolo MQTT
- **Conexión segura** con soporte SSL/TLS y certificados CA
- **Arquitectura MVVM** para separación clara de responsabilidades
- **UI moderna** construida con Jetpack Compose y Material Design 3
- **Reconexión automática** en caso de pérdida de conexión
- **Last Will Testament (LWT)** para notificar desconexiones
- **Estado de conexión visual** en tiempo real
- **Persistencia de mensajes** durante la sesión
- **Diseño responsive** con burbujas de chat diferenciadas

## 🚀 Instalación y Configuración

### 1. Clonar el Repositorio

```bash
git clone https://github.com/tu-usuario/ChatPlataformasMqtt.git
cd ChatPlataformasMqtt
```

### 2. Configuración del Broker MQTT

#### Opción A: EMQX Cloud

1. Crear una cuenta en [EMQX Cloud](https://www.emqx.com/en/cloud)
2. Crear un deployment serverless o dedicado
3. Obtener las credenciales de conexión:
   - Address del broker
   - Puerto (8883 para MQTT/SSL o 8084 para WebSocket/SSL)
   - Certificado CA

#### Opción B: Broker Público (Solo para pruebas)

Puedes usar `broker.emqx.io` sin autenticación para pruebas rápidas.

### 3. Configuración del Proyecto

1. **Colocar el certificado CA**:
   ```
   app/src/main/assets/emqxsl-ca.crt
   ```

2. **Actualizar credenciales en `ChatViewModel.kt`**:
   ```kotlin
   companion object {
       // Configuración del broker
       private const val BROKER_URL = "ssl://tu-broker.emqxsl.com:8883"
       // o para WebSocket: "wss://tu-broker.emqxsl.com:8084/mqtt"
       
       // Credenciales
       private const val USERNAME = "tu_usuario"
       private const val PASSWORD = "tu_password"
       
       // Certificado
       private const val CA_CERTIFICATE_FILE = "emqxsl-ca.crt"
   }
   ```

3. **Configurar permisos ACL en EMQX Cloud**:
   - Topic: `demo/chat/room1`
   - Permisos: Publish and Subscribe
   - Usuario: El configurado en USERNAME

## 📁 Estructura del Proyecto

```
com.example.chatplataformasmqtt/
├── model/
│   └── Msg.kt                    # Modelo de datos para mensajes
├── viewmodel/
│   ├── ChatViewModel.kt          # Lógica de negocio y conexión MQTT
│   └── ChatViewModelFactory.kt   # Factory para inyección de contexto
├── ui/
│   ├── screen/
│   │   └── ChatScreen.kt        # UI principal del chat
│   └── theme/
│       └── Theme.kt             # Tema de Material Design 3
└── MainActivity.kt              # Actividad principal
```

## 🔧 Arquitectura

### MVVM Pattern

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│                 │     │                 │     │                 │
│      View       │────▶│   ViewModel     │────▶│      Model      │
│  (ChatScreen)   │     │ (ChatViewModel) │     │      (Msg)      │
│                 │◀────│                 │◀────│                 │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                        │                        │
        └────────────────────────┴────────────────────────┘
                           StateFlow
```

### Flujo de Datos

1. **Conexión**: La app se conecta al broker MQTT al iniciar
2. **Suscripción**: Se suscribe automáticamente al topic configurado
3. **Envío**: Los mensajes se publican con formato `clientId|timestamp|texto`
4. **Recepción**: Los mensajes recibidos se procesan y almacenan en StateFlow
5. **UI**: La interfaz observa el StateFlow y actualiza automáticamente
