package com.example.amimetadataproxy

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var lastTrackText: TextView
    private lateinit var debugText: TextView
    private lateinit var spotifyOnlyBox: CheckBox
    private lateinit var enabledBox: CheckBox

    private val btPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        refreshUi()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 36, 36, 36)
        }

        val title = TextView(this).apply {
            textSize = 22f
            text = "AMI Metadata Proxy v2"
        }

        val info = TextView(this).apply {
            text = "Deze app probeert voor oudere Audi AMI/Bluetooth adapters het album-veld te vervangen door de artiest. De kans op succes is het grootst als alleen Spotify actief is en jouw telefoon de proxy-media-session kiest."
        }

        enabledBox = CheckBox(this).apply {
            text = "Proxy actief"
            setOnCheckedChangeListener { _, checked ->
                AppPrefs.setEnabled(this@MainActivity, checked)
                DebugLog.add(this@MainActivity, if (checked) "Proxy ingeschakeld" else "Proxy uitgeschakeld")
                refreshUi()
            }
        }

        spotifyOnlyBox = CheckBox(this).apply {
            text = "Alleen Spotify als bron gebruiken"
            setOnCheckedChangeListener { _, checked ->
                AppPrefs.setSpotifyOnly(this@MainActivity, checked)
                DebugLog.add(this@MainActivity, "Spotify-only = $checked")
                refreshUi()
            }
        }

        statusText = TextView(this)
        lastTrackText = TextView(this)
        debugText = TextView(this).apply {
            textSize = 12f
        }

        val notifButton = Button(this).apply {
            text = "1. Open meldingstoegang"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        val btButton = Button(this).apply {
            text = "2. Open Bluetooth-instellingen"
            setOnClickListener {
                maybeRequestBluetoothPermission()
                startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            }
        }

        val devButton = Button(this).apply {
            text = "3. Open Developer options (voor AVRCP-test)"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
            }
        }

        val refreshButton = Button(this).apply {
            text = "Ververs status"
            setOnClickListener { refreshUi() }
        }

        val clearLogButton = Button(this).apply {
            text = "Wis debuglog"
            setOnClickListener {
                DebugLog.clear(this@MainActivity)
                refreshUi()
            }
        }

        root.addView(title)
        root.addView(info)
        root.addView(enabledBox)
        root.addView(spotifyOnlyBox)
        root.addView(statusText)
        root.addView(lastTrackText)
        root.addView(notifButton)
        root.addView(btButton)
        root.addView(devButton)
        root.addView(refreshButton)
        root.addView(clearLogButton)
        root.addView(debugText)

        val scroll = ScrollView(this).apply {
            addView(root)
        }
        setContentView(scroll)

        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun refreshUi() {
        enabledBox.setOnCheckedChangeListener(null)
        spotifyOnlyBox.setOnCheckedChangeListener(null)
        enabledBox.isChecked = AppPrefs.isEnabled(this)
        spotifyOnlyBox.isChecked = AppPrefs.isSpotifyOnly(this)
        enabledBox.setOnCheckedChangeListener { _, checked ->
            AppPrefs.setEnabled(this@MainActivity, checked)
            DebugLog.add(this@MainActivity, if (checked) "Proxy ingeschakeld" else "Proxy uitgeschakeld")
            refreshUi()
        }
        spotifyOnlyBox.setOnCheckedChangeListener { _, checked ->
            AppPrefs.setSpotifyOnly(this@MainActivity, checked)
            DebugLog.add(this@MainActivity, "Spotify-only = $checked")
            refreshUi()
        }

        val notificationAccess = isNotificationAccessEnabled()
        val bluetoothState = BluetoothStateHelper.getConnectedAudioSummary(this)
        AppPrefs.saveLastAudioDevice(this, bluetoothState)

        statusText.text = buildString {
            append("Status: ")
            append(if (AppPrefs.isEnabled(this@MainActivity)) "proxy AAN" else "proxy UIT")
            append("\nMeldingstoegang: ")
            append(if (notificationAccess) "aan" else "uit")
            append("\nBronfilter: ")
            append(if (AppPrefs.isSpotifyOnly(this@MainActivity)) "alleen Spotify" else "elke mediaspeler")
            append("\nBluetooth: ")
            append(bluetoothState)
            append("\n\nAanpak voor testen:\n")
            append("• Zet Spotify aan en sluit andere players af\n")
            append("• Verbind met je Audi/AMI Bluetooth module\n")
            append("• Test eventueel AVRCP 1.4, 1.5 en 1.6 in Developer options")
        }

        val lastTitle = AppPrefs.getLastTitle(this) ?: "-"
        val lastArtist = AppPrefs.getLastArtist(this) ?: "-"
        val lastSource = AppPrefs.getLastSource(this) ?: "-"
        lastTrackText.text = "Laatste track: $lastTitle\nLaatste artiest: $lastArtist\nLaatste bron: $lastSource"
        debugText.text = "Debuglog\n\n${DebugLog.read(this)}"
    }

    private fun maybeRequestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
    }

    private fun isNotificationAccessEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        val component = ComponentName(this, MetadataProxyNotificationListener::class.java)
        return flat.split(":").any {
            ComponentName.unflattenFromString(it) == component
        }
    }
}
