package com.example.records

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.medicinereminder.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.FirebaseStorage

data class FileInfo(
    val name: String? = null,
    val url: String? = null,
    val uploadDate: String? = null // Add this line to store the date
)

class FileAdapter(
    private val context: Context,
    private val fileList: MutableList<FileInfo>,

    private val databaseRef: DatabaseReference,
    private val auth: FirebaseAuth // Add this line


) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileName: TextView = itemView.findViewById(R.id.file_name)
        val fileUploadDate: TextView = itemView.findViewById(R.id.file_upload_date)
        val openButton: LinearLayout = itemView.findViewById(R.id.linear)
        val deleteButton: ImageView = itemView.findViewById(R.id.delete_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_file_record, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val fileInfo = fileList[position]
        holder.fileName.text = fileInfo.name
        holder.fileUploadDate.text = fileInfo.uploadDate

        holder.openButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(fileInfo.url)
            context.startActivity(intent)
        }
        holder.deleteButton.setOnClickListener {
            confirmDelete(fileInfo, position)
        }
    }

    override fun getItemCount(): Int = fileList.size

    private fun deleteFile(fileInfo: FileInfo, position: Int) {
        // Get the current user's ID
        val userId = auth.currentUser?.uid ?: return

        // Reference to the file in Firebase Storage
        val fileRef = FirebaseStorage.getInstance().getReferenceFromUrl(fileInfo.url!!)

        // Start by deleting the file from Firebase Storage
        fileRef.delete().addOnSuccessListener {
            // If the file was successfully deleted from Storage, delete it from the Realtime Database
            databaseRef.orderByChild("url").equalTo(fileInfo.url).get()
                .addOnSuccessListener { snapshot ->
                    snapshot.children.forEach { child ->
                        child.ref.removeValue() // Remove the file entry from the database
                    }

                    // Remove the file from the local list and notify the adapter
                    fileList.removeAt(position)
                    notifyItemRemoved(position)

                    Toast.makeText(context, "File deleted successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to delete from database: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to delete from storage: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDelete(fileInfo: FileInfo, position: Int) {
        // Show a confirmation dialog
        AlertDialog.Builder(context)
            .setTitle("Delete File")
            .setMessage("Are you sure you want to delete this file?")
            .setPositiveButton("Yes") { _, _ ->
                deleteFile(fileInfo, position) // Call deleteFile if 'Yes' is pressed
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss() // Dismiss dialog if 'No' is pressed
            }
            .create()
            .show()
    }


}
