package com.example.records

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.a7minutesworkout.MainActivityWorkout
import com.example.medicinereminder.MainActivity
import com.example.medicinereminder.R
import com.example.medicinereminder.databinding.ActivityRecordsBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import androidx.recyclerview.widget.RecyclerView
import com.example.ReportAnalysisActivity
import com.example.Utils.utils
import com.example.home.Fall_detection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Records_Activity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference
    private lateinit var storageRef: StorageReference
    private lateinit var recyclerView: RecyclerView
    private lateinit var fileAdapter: FileAdapter

    // Use the same filteredList across the adapter and logic
    private val fileList = mutableListOf<FileInfo>()
    private val filteredList = mutableListOf<FileInfo>()

    companion object {
        const val PICK_FILE_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_records)


        val gotoChatBot: ImageView = findViewById(R.id.gotochatbot)
        gotoChatBot.setOnClickListener {
            // Intent to navigate to ReportAnalysisActivity
            val intent = Intent(this, ReportAnalysisActivity::class.java)
            startActivity(intent)
        }

        val bottomNavView = findViewById<BottomNavigationView>(R.id.BottomNavBar)
        bottomNavView.selectedItemId = R.id.nav_records

        bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_workout -> {
                    val intent = Intent(this, MainActivityWorkout::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_medical -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_records -> {
                    // Stay on the same activity
                    true
                }
                R.id.nav_home -> {
                    val intent = Intent(this, Fall_detection::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }

        auth = FirebaseAuth.getInstance()

        // Check if the user is logged in
        if (auth.currentUser != null) {
            databaseRef = FirebaseDatabase.getInstance().getReference("Users")
            storageRef = FirebaseStorage.getInstance().reference

            recyclerView = findViewById(R.id.files_recycler_view)
            recyclerView.layoutManager = LinearLayoutManager(this)

            // Initialize the adapter with filteredList
            fileAdapter = FileAdapter(this, filteredList, databaseRef.child(auth.currentUser!!.uid).child("files"), auth)
            recyclerView.adapter = fileAdapter

            findViewById<ImageView>(R.id.button_add_file).setOnClickListener {
                openFileChooser()
            }

            val searchView = findViewById<SearchView>(R.id.search_view)
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    filterFiles(query)
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    filterFiles(newText)
                    return true
                }
            })

            // Load files on startup
            loadFiles()
        } else {
            // If user is not logged in, show a message and close the activity
            Toast.makeText(this, "User is not authenticated", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun filterFiles(query: String?) {
        val searchText = query?.lowercase() ?: ""

        filteredList.clear()
        filteredList.addAll(
            if (searchText.isNotEmpty()) {
                fileList.filter { it.name?.lowercase()?.contains(searchText) == true }
            } else {
                fileList
            }
        )
        fileAdapter.notifyDataSetChanged()
    }

    private fun openFileChooser() {
        val intent = Intent()
        intent.type = "*/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select a file"), PICK_FILE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data?.data != null) {
            uploadFile(data.data!!)
        }
    }

    private fun uploadFile(uri: Uri) {
        if (auth.currentUser == null) {
            Toast.makeText(this, "User is not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        utils.showdialog(this)
        val userId = auth.currentUser?.uid ?: return
        val fileRef = storageRef.child("user_files/$userId/${getFileName(uri)}")

        fileRef.putFile(uri).addOnSuccessListener {
            utils.hideDialog()
            Toast.makeText(this, "File uploaded successfully", Toast.LENGTH_SHORT).show()
            saveFileInfoToDatabase(fileRef, uri)
        }.addOnFailureListener {
            utils.hideDialog()
            Toast.makeText(this, "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveFileInfoToDatabase(fileRef: StorageReference, uri: Uri) {
        if (auth.currentUser == null) {
            Toast.makeText(this, "User is not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        fileRef.downloadUrl.addOnSuccessListener { downloadUrl ->
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            val currentDate = dateFormat.format(Date())

            val fileInfo = FileInfo(getFileName(uri), downloadUrl.toString(), currentDate)
            databaseRef.child(auth.currentUser!!.uid).child("files").push().setValue(fileInfo)
            loadFiles() // Refresh the list
        }
    }

    private fun loadFiles() {
        if (auth.currentUser == null) {
            Toast.makeText(this, "User is not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        databaseRef.child(auth.currentUser!!.uid).child("files").get().addOnSuccessListener { snapshot ->
            fileList.clear()

            snapshot.children.forEach { childSnapshot ->
                val fileInfo = childSnapshot.getValue(FileInfo::class.java)
                fileInfo?.let { fileList.add(it) }
            }

            // Update filteredList initially to show all files
            filteredList.clear()
            filteredList.addAll(fileList)

            fileAdapter.notifyDataSetChanged()
        }
    }

    private fun getFileName(uri: Uri): String {
        var result = ""
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst()) {
                    result = it.getString(nameIndex)
                }
            }
        }
        if (result.isEmpty() && uri.path != null) {
            result = uri.path!!.substring(uri.path!!.lastIndexOf('/') + 1)
        }
        return result
    }
}