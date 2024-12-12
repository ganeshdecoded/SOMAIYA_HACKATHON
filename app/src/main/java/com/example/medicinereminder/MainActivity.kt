package com.example.medicinereminder


import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.a7minutesworkout.MainActivityWorkout
import com.example.home.FallDetectionService
import com.example.home.Fall_detection
import com.example.insuranceinfo.InsuranceActivity
import com.example.records.Records_Activity
import com.google.android.material.bottomnavigation.BottomNavigationView
import sszj.s.geminiapi.ui.GeminiChatBotActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val serviceIntent = Intent(this, FallDetectionService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)


        // Add the functionality for Medical_Prompt button

        // Set up BottomNavigationView with listener for the Workout item
        val bottomNavView = findViewById<BottomNavigationView>(R.id.BottomNavBar)
        bottomNavView.selectedItemId = R.id.nav_medical

        bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_workout -> {
                    // Navigate to MainActivityWorkout
                    val intent = Intent(this, MainActivityWorkout::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_medical -> {
                    // Navigate to MainActivity
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_records -> {
                    // Navigate to Records_Activity
                    val intent = Intent(this, Records_Activity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_home -> {
                    // Navigate to Fall_detection
                    val intent = Intent(this, Fall_detection::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_insaurance -> {
                    val intent = Intent(this, InsuranceActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }

                else -> {
                    false // Returning false if no valid item is selected
                }
            }
        }

        permissionNotification()
        permissionAlarms()
        createNotificationChannel()
    }

    // Function to open fragments
    fun openFragment(fragment: Fragment) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.containerView, fragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }



    // Create Notification Channel
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("1", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Permission for exact alarms (Android 12+)
    private fun permissionAlarms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
    }

    // Permission for notifications (Android 13+)
    private val appPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    private fun permissionNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appPermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        }
    }
}