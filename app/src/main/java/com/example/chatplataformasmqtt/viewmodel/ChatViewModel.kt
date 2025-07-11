package com.example.chatplataformasmqtt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatplataformasmqtt.model.Msg
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import android.content.Context
import android.util.Log
import java.util.UUID
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManagerFactory
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatViewModel(private val context: Context) : ViewModel() {

    companion object {

        // private const val BROKER_URL = "wss://g5150556.ala.eu-central-1.emqxsl.com:8084/mqtt"

        private const val BROKER_URL = "ssl://g5150556.ala.eu-central-1.emqxsl.com:8883"

        private val CLIENT_ID = "AndroidClient_${UUID.randomUUID()}"
        private const val TOPIC = "demo/chat/room1"

        private const val USERNAME = "carlo"
        private const val PASSWORD = "123456"

        private const val QOS = 1


        private const val CA_CERTIFICATE_FILE = "emqxsl-ca.crt"
    }

    private val _messages = MutableStateFlow<List<Msg>>(emptyList())
    val messages: StateFlow<List<Msg>> = _messages.asStateFlow()

    private var mqttClient: MqttClient? = null
    private val tag = "ChatViewModel"

    init {
        setupMqttClient()
    }

    private fun setupMqttClient() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val persistence = MemoryPersistence()
                mqttClient = MqttClient(BROKER_URL, CLIENT_ID, persistence)

                mqttClient?.setCallback(object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        Log.d(tag, "Conexión perdida: ${cause?.message}")
                        // Intentar reconectar
                        viewModelScope.launch(Dispatchers.IO) {
                            reconnect()
                        }
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        message?.let {
                            val payload = String(it.payload)
                            Log.d(tag, "Mensaje recibido: $payload")

                            val parts = payload.split("|")
                            if (parts.size >= 3) {
                                val senderId = parts[0]
                                val timestamp = parts[1].toLongOrNull() ?: System.currentTimeMillis()
                                val text = parts.drop(2).joinToString("|")

                                val newMsg = Msg(
                                    texto = text,
                                    epoch = timestamp,
                                    fromMe = (senderId == CLIENT_ID)
                                )

                                viewModelScope.launch {
                                    _messages.value = _messages.value + newMsg
                                }
                            }
                        }
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {
                        Log.d(tag, "Entrega completa")
                    }
                })
            } catch (e: Exception) {
                Log.e(tag, "Error al configurar MQTT: ${e.message}")
            }
        }
    }

    fun connect() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val connectOptions = MqttConnectOptions().apply {
                    isAutomaticReconnect = false
                    isCleanSession = false

                    if (BROKER_URL.startsWith("ssl://") || BROKER_URL.startsWith("wss://")) {
                        try {
                            socketFactory = getSSLSocketFactory()
                        } catch (e: Exception) {
                            Log.e(tag, "Error al configurar SSL: ${e.message}")
                        }
                    }

                    if (USERNAME.isNotEmpty()) {
                        userName = USERNAME
                        password = PASSWORD.toCharArray()
                    }

                    connectionTimeout = 30
                    keepAliveInterval = 20

                    setWill(
                        TOPIC,
                        "$CLIENT_ID|${System.currentTimeMillis()}|Usuario desconectado".toByteArray(),
                        QOS,
                        false
                    )
                }

                mqttClient?.connect(connectOptions)
                Log.d(tag, "Conexión exitosa")

                subscribeToTopic()

            } catch (e: MqttException) {
                Log.e(tag, "Error al conectar: ${e.message}")
            }
        }
    }

    private suspend fun reconnect() {
        withContext(Dispatchers.IO) {
            try {
                if (mqttClient?.isConnected == false) {
                    Thread.sleep(5000)
                    connect()
                }
            } catch (e: Exception) {
                Log.e(tag, "Error al reconectar: ${e.message}")
            }
        }
    }

    private fun subscribeToTopic() {
        try {
            mqttClient?.subscribe(TOPIC, QOS)
            Log.d(tag, "Suscrito al topic: $TOPIC")
        } catch (e: MqttException) {
            Log.e(tag, "Error al suscribirse: ${e.message}")
        }
    }

    fun send(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            val message = "$CLIENT_ID|$timestamp|$text"

            try {
                val mqttMessage = MqttMessage(message.toByteArray()).apply {
                    qos = QOS
                    isRetained = false
                }

                mqttClient?.publish(TOPIC, mqttMessage)
                Log.d(tag, "Mensaje enviado exitosamente")

            } catch (e: MqttException) {
                Log.e(tag, "Error MQTT al publicar: ${e.message}")
            }
        }
    }

    private fun getSSLSocketFactory(): SSLSocketFactory {
        return try {
            val certificateFactory = CertificateFactory.getInstance("X.509")
            val caInputStream: InputStream = context.assets.open(CA_CERTIFICATE_FILE)

            val ca: X509Certificate = caInputStream.use {
                certificateFactory.generateCertificate(it) as X509Certificate
            }

            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                load(null, null)
                setCertificateEntry("ca", ca)
            }

            val trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            ).apply {
                init(keyStore)
            }

            val sslContext = SSLContext.getInstance("TLS").apply {
                init(null, trustManagerFactory.trustManagers, null)
            }

            sslContext.socketFactory
        } catch (e: Exception) {
            Log.e(tag, "Error creando SSLSocketFactory: ${e.message}")
            throw e
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (mqttClient?.isConnected == true) {
                    mqttClient?.disconnect()
                }
            } catch (e: MqttException) {
                Log.e(tag, "Error al desconectar: ${e.message}")
            }
        }
    }
}