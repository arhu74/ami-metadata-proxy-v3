package com.example.amimetadataproxy

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object BluetoothStateHelper {
    fun getConnectedAudioSummary(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return "Bluetooth-permissie nog niet verleend"
        }

        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return "Bluetooth-manager niet gevonden"
        val adapter: BluetoothAdapter = manager.adapter ?: return "Geen Bluetooth-adapter"
        if (!adapter.isEnabled) return "Bluetooth staat uit"

        val devices = adapter.bondedDevices
            ?.filter { it.bluetoothClass?.majorDeviceClass != null }
            ?.sortedBy { it.name ?: it.address }
            .orEmpty()

        val active = devices.firstOrNull { it.name?.contains("audi", ignoreCase = true) == true }
            ?: devices.firstOrNull()

        return if (active != null) {
            "Gekoppeld/bekend: ${active.name ?: "onbekend apparaat"}"
        } else {
            "Geen gekoppelde Bluetooth-audioapparaten gevonden"
        }
    }
}
