package com.example.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.a7minutesworkout.MainActivityWorkout
import com.example.medicinereminder.MainActivity
import com.example.medicinereminder.R
import com.example.records.Records_Activity
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager.SENSOR_DELAY_FASTEST
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import kotlin.math.pow
import kotlin.math.sqrt
import android.telephony.SmsManager
import android.widget.LinearLayout
import com.example.insuranceinfo.InsuranceActivity
import com.example.medicinereminder.HomeFragment
import com.example.medicinereminder.ReminderSettingsFragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener
import sszj.s.geminiapi.ui.GeminiChatBotActivity

class Fall_detection : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null

    private var fallDetected = false // Prevent multiple dialogs
    private val handler = Handler(Looper.getMainLooper()) // Handler for delayed actions
    private var emergencyContactNumber: String? = null // Emergency contact number
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var fusedLocationClient: FusedLocationProviderClient // Fused Location Provider
    private lateinit var locationManager: LocationManager // Location Manager
    private var lastKnownLocation: Location? = null // Store last known location
    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_fall_detection)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)


        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Initialize FusedLocationProviderClient and LocationManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Initialize MediaPlayer
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound)

        // Request permissions
        requestPermissions()

        val sosButton = findViewById<ImageView>(R.id.main_sos)
        sosButton.setOnClickListener {
            sendLocationAndCall() // Call the method to send location and call the emergency contact
        }

        val chatBot = findViewById<LinearLayout>(R.id.main_chatbot)
        chatBot.setOnClickListener {
            val intent = Intent(this, GeminiChatBotActivity::class.java)
            startActivity(intent)
        }

        // Get the emergency contact number from SharedPreferences
        emergencyContactNumber = getEmergencyContactNumber()

        // If no contact number, show dialog to enter one
        if (emergencyContactNumber == null) {
            showEmergencyContactDialog()
        }

        startSensor() // Start sensors on activity creation
        setBottomNavigation() // Set up bottom navigation
        setHelplineIcons() // Set onClickListeners for helpline icons
    }

    private fun setBottomNavigation() {
        val bottomNavView = findViewById<BottomNavigationView>(R.id.BottomNavBar)
        bottomNavView.selectedItemId = R.id.nav_home
        bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_workout -> {
                    startActivity(Intent(this, MainActivityWorkout::class.java))
                    finish()
                    true
                }
                R.id.nav_medical -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_records -> {
                    startActivity(Intent(this, Records_Activity::class.java))
                    finish()
                    true
                }
                R.id.nav_insaurance -> {
                    startActivity(Intent(this, InsuranceActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_home -> true
                else -> false
            }
        }
    }

    private fun setHelplineIcons() {
        findViewById<ImageView>(R.id.icon_call1).setOnClickListener { makeCall("1098") } // Emergency
        findViewById<ImageView>(R.id.icon_call2).setOnClickListener { makeCall("100") } // Police
        findViewById<ImageView>(R.id.icon_call3).setOnClickListener { makeCall("101") } // Fire Brigade
        findViewById<ImageView>(R.id.icon_call4).setOnClickListener { makeCall("102") } // Ambulance
        findViewById<ImageView>(R.id.icon_call5).setOnClickListener { makeCall("1091") } // Women Helpline
        findViewById<ImageView>(R.id.icon_call6).setOnClickListener { makeCall("14567") } // Senior Citizen Helpline
    }

    private fun makeCall(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            startActivity(intent)
            enableSpeakerMode()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), 1)
        }
    }

    private fun enableSpeakerMode() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_CALL
        if (!audioManager.isSpeakerphoneOn) {
            audioManager.isSpeakerphoneOn = true
            Toast.makeText(this, "Speaker mode enabled", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Speaker is already enabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CALL_PHONE
        ), 1)
    }

    private fun getEmergencyContactNumber(): String? {
        return sharedPreferences.getString("emergency_contact", null)
    }

    private fun showEmergencyContactDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Emergency Contact")
        builder.setMessage("Please enter your emergency contact number:")

        val input = EditText(this)
        builder.setView(input)

        builder.setPositiveButton("OK") { dialog, _ ->
            val number = input.text.toString()
            if (number.isNotEmpty()) {
                saveEmergencyContactNumber(number)
            } else {
                Toast.makeText(this, "Contact number cannot be empty", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun saveEmergencyContactNumber(number: String) {
        sharedPreferences.edit().putString("emergency_contact", number).apply()
        emergencyContactNumber = number
    }

    private fun startSensor() {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
    }

    private fun stopSensor() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            if (!fallDetected && isFalling(x, y, z)) {
                fallDetected = true
                stopSensor()
                showFallDialog(x, y, z)
            }

            if (!fallDetected && isFalling(x, y, z)) {
                fallDetected = true
                stopSensor()
                showFallDialog(x, y, z)
            }
        }
    }

    private fun isFalling(x: Float, y: Float, z: Float): Boolean {
        val acceleration = sqrt(x.pow(2) + y.pow(2) + z.pow(2))
        return acceleration < 0.5
    }

    private fun showFallDialog(x: Float, y: Float, z: Float) {
        // Play the alarm sound
        mediaPlayer.start()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("ARE YOU SAFE?")
        builder.setMessage("Please confirm your status. \nX: $x \nY: $y \nZ: $z")
        builder.setPositiveButton("YES") { dialog, _ ->
            dialog.dismiss()
            fallDetected = false
            startSensor()
            mediaPlayer.pause() // Pause the sound
            mediaPlayer.seekTo(0) // Reset the MediaPlayer for the next time
        }
        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.show()

        handler.postDelayed({
            if (dialog.isShowing) {
                dialog.dismiss()
                sendLocationAndCall()
                fallDetected = false
                startSensor()
                mediaPlayer.pause() // Pause the sound if the dialog times out
                mediaPlayer.seekTo(0)
            }
        }, 10_000) // Timeout set to 10 seconds
    }

    private fun sendLocationAndCall() {
        emergencyContactNumber?.let { number ->
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                // Use FusedLocationProviderClient to get the last known location
                fusedLocationClient.lastLocation.addOnSuccessListener(this, OnSuccessListener<Location?> { location ->
                    if (location != null) {
                        lastKnownLocation = location
                        // Create a Google Maps link
                        val mapsLink = "https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
                        val message = "Urgent! A fall has been detected. Open the location: $mapsLink"
                        SmsManager.getDefault().sendTextMessage(number, null, message, null, null)
                        makeCall(number)
                    } else {
                        // Fallback: if lastLocation is null, try to request a location update
                        requestCurrentLocation(number)
                    }
                })
            } else {
                Toast.makeText(this, "Permission denied to send SMS or access location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestCurrentLocation(number: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val locationRequest = com.google.android.gms.location.LocationRequest.create().apply {
                interval = 10000 // 10 seconds
                fastestInterval = 5000 // 5 seconds
                priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            fusedLocationClient.requestLocationUpdates(locationRequest, object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                    super.onLocationResult(locationResult)
                    if (locationResult.locations.isNotEmpty()) {
                        val location = locationResult.lastLocation
                        val message = "Urgent! A fall has been detected at location: Lat: ${location?.latitude}, Long: ${location?.longitude}"
                        SmsManager.getDefault().sendTextMessage(number, null, message, null, null)
                        makeCall(number)

                        // Stop location updates after getting a location
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                }
            }, Looper.getMainLooper()) // Ensure updates are handled on the main thread
        } else {
            Toast.makeText(this, "Permission denied to access location", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissions required for app functionality", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        stopSensor()
        mediaPlayer.release() // Release MediaPlayer resources
    }
}