package com.example.transferdatabluetooth


import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.OutputStream
import java.util.*

class MainActivity : ComponentActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private var connectedDevice: BluetoothDevice? = null

    private var isBluetoothEnabled by mutableStateOf(false)
    private var isBluetoothConnecting by mutableStateOf(false)
    private var isTransferInProgress by mutableStateOf(false)

    private var messageToSend by mutableStateOf("")

    private val bluetoothDeviceSelectedLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val device: BluetoothDevice? =
                    result.data?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                if (device != null) {
                    connectedDevice = device
                    connectToDevice(device)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BluetoothTransferScreen(
                isBluetoothEnabled = isBluetoothEnabled,
                isBluetoothConnecting = isBluetoothConnecting,
                isTransferInProgress = isTransferInProgress,
                messageToSend = messageToSend,
                onBluetoothToggle = { toggleBluetooth() },
                onDeviceSelected = { selectBluetoothDevice() },
                onSendMessage = { sendMessage(it) },
                onDisconnect = { disconnectBluetooth() }
            )
        }

        // Initialize Bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        isBluetoothEnabled = bluetoothAdapter?.isEnabled == true
    }

    private fun toggleBluetooth() {
        isBluetoothEnabled = if (isBluetoothEnabled) {
            // Turn off Bluetooth
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            bluetoothAdapter?.disable()
            false
        } else {
            // Turn on Bluetooth
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            bluetoothAdapter?.enable()
            true
        }
    }

    private fun selectBluetoothDevice() {
        if (!isBluetoothEnabled) {
            return
        }

        val intent = Intent(this, DeviceListActivity::class.java)
        bluetoothDeviceSelectedLauncher.launch(intent)
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        isBluetoothConnecting = true
        GlobalScope.launch {
            try {
                val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                bluetoothSocket?.connect()
                outputStream = bluetoothSocket?.outputStream
                isBluetoothConnecting = false
            } catch (e: IOException) {
                isBluetoothConnecting = false
                e.printStackTrace()
            }
        }
    }

    private fun sendMessage(message: String) {
        if (isBluetoothEnabled && !isBluetoothConnecting) {
            isTransferInProgress = true
            GlobalScope.launch {
                try {
                    outputStream?.write(message.toByteArray())
                    Handler(Looper.getMainLooper()).post {
                        isTransferInProgress = false
                    }
                } catch (e: IOException) {
                    Handler(Looper.getMainLooper()).post {
                        isTransferInProgress = false
                    }
                    e.printStackTrace()
                }
            }
        }
    }

    private fun disconnectBluetooth() {
        try {
            bluetoothSocket?.close()
            isTransferInProgress = false
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothTransferScreen(
    isBluetoothEnabled: Boolean,
    isBluetoothConnecting: Boolean,
    isTransferInProgress: Boolean,
    messageToSend: String,
    onBluetoothToggle: () -> Unit,
    onDeviceSelected: () -> Unit,
    onSendMessage: (String) -> Unit,
    onDisconnect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Send,
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onBluetoothToggle() },
            enabled = !isTransferInProgress
        ) {
            Text(if (isBluetoothEnabled) "Disable Bluetooth" else "Enable Bluetooth")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isBluetoothEnabled) {
            Button(
                onClick = { onDeviceSelected() },
                enabled = !isBluetoothConnecting && !isTransferInProgress
            ) {
                Text("Select Device")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isBluetoothConnecting) {
            CircularProgressIndicator()
        }

        if (isTransferInProgress) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Transfer in progress...")
        }

        if (!isTransferInProgress) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = messageToSend,
                onValueChange = { onSendMessage(it) },
                label = { Text("Message to send") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onSendMessage(messageToSend) },
                enabled = !messageToSend.isBlank() && !isBluetoothConnecting
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Send Message")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onDisconnect() },
                enabled = !isBluetoothConnecting
            ) {
                Text("Disconnect")
            }
        }
    }
}

