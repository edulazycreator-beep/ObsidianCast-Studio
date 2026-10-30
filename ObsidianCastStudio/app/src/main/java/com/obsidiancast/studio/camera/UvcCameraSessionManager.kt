package com.obsidiancast.studio.camera

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DetectedUvcDevice(
    val deviceId: Int,
    val vendorId: Int,
    val productId: Int,
    val productName: String,
    val hasPermission: Boolean
)

/**
 * Manages USB Video Class (UVC Class 0x0E / 14) discovery, runtime USB permission requests,
 * and hot-plug attach/detach events for external HDMI capture cards and USB cameras.
 */
class UvcCameraSessionManager(private val context: Context) {

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private val _connectedUvcDevices = MutableStateFlow<List<DetectedUvcDevice>>(emptyList())
    val connectedUvcDevices: StateFlow<List<DetectedUvcDevice>> = _connectedUvcDevices.asStateFlow()

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED,
                UsbManager.ACTION_USB_DEVICE_DETACHED,
                ACTION_USB_PERMISSION -> refreshConnectedUvcDevices()
            }
        }
    }

    fun startMonitoring() {
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(ACTION_USB_PERMISSION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(usbReceiver, filter)
        }
        refreshConnectedUvcDevices()
    }

    fun stopMonitoring() {
        runCatching { context.unregisterReceiver(usbReceiver) }
    }

    fun refreshConnectedUvcDevices() {
        val uvcList = usbManager.deviceList.values
            .filter { isUvcVideoDevice(it) }
            .map { dev ->
                DetectedUvcDevice(
                    deviceId = dev.deviceId,
                    vendorId = dev.vendorId,
                    productId = dev.productId,
                    productName = dev.productName ?: "USB UVC Video Capture (${dev.vendorId}:${dev.productId})",
                    hasPermission = usbManager.hasPermission(dev)
                )
            }
        _connectedUvcDevices.value = uvcList
    }

    fun requestUsbPermissionIfNeeded(device: UsbDevice) {
        if (usbManager.hasPermission(device)) return
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION),
            flags
        )
        usbManager.requestPermission(device, permissionIntent)
    }

    private fun isUvcVideoDevice(device: UsbDevice): Boolean {
        if (device.deviceClass == UsbConstants.USB_CLASS_VIDEO) return true
        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            if (intf.interfaceClass == UsbConstants.USB_CLASS_VIDEO) return true
        }
        return false
    }

    companion object {
        private const val ACTION_USB_PERMISSION = "com.obsidiancast.studio.USB_PERMISSION"
    }
}
