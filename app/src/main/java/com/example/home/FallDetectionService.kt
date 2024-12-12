package com.example.home
import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.medicinereminder.MainActivity
import com.example.medicinereminder.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener
import kotlin.math.pow
import kotlin.math.sqrt

class FallDetectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var fallDetected = false
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationString: String = "Location unavailable"
    private val fallThreshold = 2.0 // Acceleration threshold to detect a fall

    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound)

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestLocationUpdate()

        // Initialize sensor manager and accelerometer
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Register sensor listener for fall detection
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        // Start the service in the foreground
        startForegroundService()
    }

    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)


        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "FallDetectionChannel"
            val channel = NotificationChannel(channelId, "Fall Detection", NotificationManager.IMPORTANCE_HIGH)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)

            NotificationCompat.Builder(this, channelId)
                .setContentTitle("Fall Detection Active")
                .setContentText("Monitoring for falls in the background.")
                .setSmallIcon(R.drawable.logo)
                .setContentIntent(pendingIntent)
                .build()
        } else {
            NotificationCompat.Builder(this)
                .setContentTitle("Fall Detection Active")
                .setContentText("Monitoring for falls in the background.")
                .setSmallIcon(R.drawable.play)
                .setContentIntent(pendingIntent)
                .build()
        }

        startForeground(1, notification)
    }

    private fun requestLocationUpdate() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener(OnSuccessListener<Location?> { location ->
                location?.let {
                    locationString = "https://maps.google.com/?q=${it.latitude},${it.longitude}"
                } ?: run {
                    locationString = "Unable to retrieve location."
                }
            })
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val accelerationMagnitude = sqrt(x * x + y * y + z * z)

            // Detect fall if acceleration is below the threshold
            if (accelerationMagnitude < fallThreshold && !fallDetected) {
                fallDetected = true
                mediaPlayer.start()
                showFallDetectedDialog()
            }
        }
    }

    private fun showFallDetectedDialog() {
        Handler(mainLooper).postDelayed({
            if (fallDetected) { // If no response after 10 seconds
                sendEmergencySMS()
                stopAlarm()
            }
        }, 10_000) // 10-second delay for response

        Toast.makeText(this, "Fall detected! Checking on you...", Toast.LENGTH_SHORT).show()
    }

    private fun sendEmergencySMS() {
        val sharedPreferences = getSharedPreferences("EmergencyContacts", MODE_PRIVATE)
        val emergencyContact = sharedPreferences.getString("EmergencyContact", null)
        val smsManager = SmsManager.getDefault()

        if (emergencyContact != null && ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            val message = "Fall detected! Check on me at $locationString"
            smsManager.sendTextMessage(emergencyContact, null, message, null, null)
            Toast.makeText(this, "Emergency SMS sent", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Unable to send SMS. Check permissions or contact details.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopAlarm() {
        mediaPlayer.stop()
        fallDetected = false
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        mediaPlayer.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}