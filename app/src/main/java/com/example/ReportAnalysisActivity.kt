package com.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.medicinereminder.R
import com.example.medicinereminder.databinding.ActivityReportAnalysisBinding
import sszj.s.geminiapi.ui.GeminiChatBotActivity

class ReportAnalysisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportAnalysisBinding
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the image picker
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                result.data!!.data?.let { uri ->
                    selectedImageUri = uri
                    // Load the selected image for display
                    Glide.with(this).load(uri).into(binding.selectedImage)

                    // Automatically redirect to the chat bot with just the image URI
                    navigateToChatBot(uri)
                }
            }
        }

        // Trigger image picker on image click
        binding.addImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*" // Limit to image types
            pickImageLauncher.launch(intent)
        }
    }

    private fun navigateToChatBot(imageUri: Uri) {
        // Create the intent to start the GeminiChatBotActivity with only the image URI
        val intent = Intent(this, GeminiChatBotActivity::class.java).apply {
            putExtra("image_uri", imageUri.toString()) // Pass the image URI only
        }
        startActivity(intent)
    }
}