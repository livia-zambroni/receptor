package com.example.receptorble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.*

class MainActivity : AppCompatActivity() {

    // --- Constantes ---
    // UUIDs que devem ser IDÊNTICOS aos definidos no código do ESP32
    private val esp32ServiceUuid = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
    private val esp32CharacteristicUuid = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
    // UUID padrão para o descritor de configuração do cliente (para ativar notificações)
    private val clientCharacteristicConfigUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // --- Variáveis de UI ---
    private lateinit var btnEscanear: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvNumeroRecebido: TextView

    // --- Variáveis de Bluetooth ---
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val bleScanner by lazy { bluetoothAdapter.bluetoothLeScanner }
    private var bluetoothGatt: BluetoothGatt? = null

    // --- Lógica de Permissões ---
    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.values.all { it }) {
                // Todas as permissões foram concedidas, iniciar o escaneamento
                startBleScan()
            } else {
                // Pelo menos uma permissão foi negada
                Toast.makeText(this, "Permissões de Bluetooth são necessárias para usar o app.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializa as views
        btnEscanear = findViewById(R.id.btnEscanear)
        tvStatus = findViewById(R.id.tvStatus)
        tvNumeroRecebido = findViewById(R.id.tvNumeroRecebido)

        btnEscanear.setOnClickListener {
            checkForPermissionsAndScan()
        }
    }

    override fun onResume() {
        super.onResume()
        // Garante que o Bluetooth está ligado quando o app é retomado
        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Por favor, ative o Bluetooth.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStop() {
        super.onStop()
        // Para o escaneamento e desconecta quando o app vai para o fundo
        stopBleScan()
        disconnectGatt()
    }

    // --- Funções Principais ---

    private fun checkForPermissionsAndScan() {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val missingPermissions = requiredPermissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            startBleScan()
        } else {
            requestPermissionsLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    @SuppressLint("MissingPermission") // A permissão já foi verificada em checkForPermissionsAndScan
    private fun startBleScan() {
        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Bluetooth não está ativado.", Toast.LENGTH_SHORT).show()
            return
        }

        // Filtro para encontrar nosso ESP32 específico pelo seu Service UUID
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(esp32ServiceUuid))
            .build()

        // Configurações do escaneamento
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        updateStatus("Procurando por 'MeuESP32'...")
        bleScanner.startScan(listOf(scanFilter), scanSettings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    private fun stopBleScan() {
        bleScanner.stopScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        updateStatus("Conectando ao dispositivo...")
        // O `gattCallback` gerenciará os eventos de conexão e descoberta de serviços
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
    }

    @SuppressLint("MissingPermission")
    private fun disconnectGatt() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    // --- Callbacks ---

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            // Dispositivo encontrado!
            Log.d("ScanCallback", "Dispositivo encontrado: ${result.device.address}")
            stopBleScan() // Para de procurar assim que encontrar o primeiro
            connectToDevice(result.device)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e("ScanCallback", "Falha no escaneamento com código de erro: $errorCode")
            updateStatus("Erro ao procurar dispositivos.")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // Conectado com sucesso, agora descubra os serviços
                updateStatus("Conectado! Descobrindo serviços...")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // Desconectado
                updateStatus("Desconectado.")
                disconnectGatt()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(esp32ServiceUuid)
                val characteristic = service?.getCharacteristic(esp32CharacteristicUuid)

                if (characteristic != null) {
                    // Encontrou a característica, agora habilita as notificações
                    gatt.setCharacteristicNotification(characteristic, true)

                    // Escreve no descritor para confirmar o recebimento das notificações
                    val descriptor = characteristic.getDescriptor(clientCharacteristicConfigUuid)
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                    updateStatus("Conectado e pronto para receber dados!")
                }
            }
        }

        // Este método é chamado TODA VEZ que o ESP32 envia um novo valor
        // A CORREÇÃO ESTÁ AQUI: O segundo parâmetro agora é BluetoothGattCharacteristic
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val valorRecebidoBytes = characteristic.value
            val valorRecebidoString = valorRecebidoBytes.toString(Charsets.UTF_8)
            Log.d("GattCallback", "Valor recebido: $valorRecebidoString")
            updateNumeroRecebido(valorRecebidoString)
        }
    }

    // --- Funções de UI ---

    private fun updateStatus(text: String) {
        // A UI só pode ser atualizada na thread principal
        runOnUiThread {
            tvStatus.text = "Status: $text"
        }
    }

    private fun updateNumeroRecebido(numero: String) {
        runOnUiThread {
            tvNumeroRecebido.text = numero
        }
    }
}
