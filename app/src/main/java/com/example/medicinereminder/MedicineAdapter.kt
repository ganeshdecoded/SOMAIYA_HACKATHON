package com.example.medicinereminder

import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.Dialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.example.TextToSpeechManager
import com.example.medicinereminder.Room.Medicine
import com.example.medicinereminder.Room.MedicineDatabase
import com.example.medicinereminder.databinding.ItemReminderBinding
import java.util.Calendar

class MedicineAdapter(private val context: Context) :
    ListAdapter<Medicine, MedicineAdapter.MedicineViewHolder>(MedicineDiffCallback()) {

    private var alarmClockInfo: AlarmManager.AlarmClockInfo? = null

    // Initialize TextToSpeechManager
    private val textToSpeechManager = TextToSpeechManager(context)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        val binding =
            ItemReminderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MedicineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(currentItem)
    }

    // Verilen yeni medicine listesini RecyclerView'a gönderir
    fun submitMedicineList(newList: List<Medicine>) {
        submitList(newList)
    }

    inner class MedicineViewHolder(private val binding: ItemReminderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            setUtteranceProgressListener()
            //onPausedClicked()
        }

        fun bind(medicine: Medicine) {
            binding.textViewMedicine.text = medicine.name
            binding.textViewTime.text = medicine.timeToTake
            //binding.textViewDosage.text = "${medicine.dosage} Dosage"
            binding.textViewDosage.text = medicine.dosage

             //Load image from URI
            if (!medicine.imageUri.isNullOrEmpty()) {
                Glide.with(context)
                    .load(Uri.parse(medicine.imageUri))
                    .circleCrop()  // Apply circular crop
                    .placeholder(R.drawable.sos)  // Fallback image
                    .into(binding.imageViewMedicine)
            } else {
                Glide.with(context)
                    .load(R.drawable.sos)
                    .circleCrop()  // Apply circular crop
                    .into(binding.imageViewMedicine)
            }
            


            updateButtonAndCardColor(medicine)

            // Handle button click
            binding.buttonStatus.setOnClickListener {
                medicine.isTaken = !medicine.isTaken // Toggle the status
                updateButtonAndCardColor(medicine) // Update UI
            }

            // Medicine silinir Alarm silinir.
            binding.imageButtonDelete.setOnClickListener {
                showDeleteDialog(medicine)
            }

            // Dialog açar
            binding.imageButtonEdit.setOnClickListener {
                showEditDialog(medicine)
            }

            // TEXT TO SPEECH
            binding.texttospeech.setOnClickListener{
                val texttospeak = "${medicine.dosage}"
                textToSpeechManager.speak(texttospeak)
            }
        }



        private fun updateButtonAndCardColor(medicine: Medicine) {
            if (medicine.isTaken) {
                binding.buttonStatus.text = "Taken"
                binding.card.setBackgroundColor(ContextCompat.getColor(context, R.color.lightblue)) // Change to green
            } else {
                binding.buttonStatus.text = "Pending"
                binding.card.setBackgroundColor(ContextCompat.getColor(context, R.color.white)) // Change to white
            }
        }
        private fun setUtteranceProgressListener() {
            textToSpeechManager.setUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    // Show pause button while speaking
                    (context as? Activity)?.runOnUiThread {
                        //binding.imageButtonPause.visibility = View.VISIBLE
                    }
                }

                override fun onDone(utteranceId: String?) {
                    // Hide pause button when TTS completes
                    (context as? Activity)?.runOnUiThread {
//                        binding.imageButtonPause.visibility = View.GONE
//                        showToast("Completed")
                    }
                }

                override fun onError(utteranceId: String?) {
                    showToast("Error during speech playback!")
                }
            })
        }

        // Handle pause click event
//        private fun onPausedClicked() {
//            binding.imageButtonPause.setOnClickListener {
//                binding.imageButtonPause.visibility = View.GONE // Hide pause button
//                textToSpeechManager.stop(false) // Stop TTS without shutdown
//            }
//        }
    }

    // Medicine silme işlemini gerçekleştirir
    private fun deleteMedicine(deletedItem: Medicine) {
        val dao = MedicineDatabase.getDatabase(context).medicineDao()
        dao.delete(deletedItem)
        submitMedicineList(dao.getAllMedicines())
        showToast("Medicine Reminder Deleted!")
    }

    // Alarmı iptal eder
    private fun cancelAlarm(medicine: Medicine) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MedicineReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicine.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    // Toast mesajını gösterir
    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // Düzenleme dialogunu gösterir
    private fun showEditDialog(medicine: Medicine) {
        // Inflate the custom dialog layout
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_medicine, null)

        // Create the Dialog and set the custom layout
        val dialog = Dialog(context)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Get references to the views in the custom layout
        val editTextMedicine = dialogView.findViewById<EditText>(R.id.editTextMedicine)
        val editTextDosage = dialogView.findViewById<EditText>(R.id.editTextDosage)
        val editTextHour = dialogView.findViewById<EditText>(R.id.editTextHour)
        val editTextMinute = dialogView.findViewById<EditText>(R.id.editTextMinute)
        val buttonSave = dialogView.findViewById<Button>(R.id.buttonSave)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)

        // Populate the fields with the existing medicine data
        editTextMedicine.setText(medicine.name)
        editTextDosage.setText(medicine.dosage)
        editTextHour.setText(medicine.timeToTake.split(":")[0])  // Hour part of time
        editTextMinute.setText(medicine.timeToTake.split(":")[1])  // Minute part of time

        // Set up button listeners
        buttonSave.setOnClickListener {
            val updatedName = editTextMedicine.text.toString()
            val updatedDosage = editTextDosage.text.toString()
            var updatedHour = editTextHour.text.toString()
            var updatedMinute = editTextMinute.text.toString()

            // Validate inputs
            if (updatedName.isBlank() || updatedDosage.isBlank() || updatedHour.isBlank() || updatedMinute.isBlank()) {
                showToast("Please fill in all fields!")
                return@setOnClickListener
            } else if (updatedHour.toInt() !in 0..23) {
                showToast("Please enter a valid hour (0-23)")
                return@setOnClickListener
            } else if (updatedMinute.toInt() !in 0..59) {
                showToast("Please enter a valid minute (0-59)")
                return@setOnClickListener
            } else {
                // Format the time with leading zeros if needed
                if (updatedHour.length == 1) updatedHour = "0$updatedHour"
                if (updatedMinute.length == 1) updatedMinute = "0$updatedMinute"

                val updatedTime = "$updatedHour:$updatedMinute"

                // Create an updated medicine object
                val updatedMedicine = medicine.copy(
                    name = updatedName,
                    dosage = updatedDosage,
                    timeToTake = updatedTime
                )

                cancelAlarm(medicine)
                updateMedicine(updatedMedicine)
                scheduleMedicineReminder(context, updatedMedicine)

                showToast("Medicine Reminder Updated!")
                dialog.dismiss()
            }
        }

        // Set up the cancel button to dismiss the dialog
        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        // Show the dialog
        dialog.show()
    }

    // Medicine silme dialogunu gösterir
    private fun showDeleteDialog(medicine: Medicine) {
        // Inflate the custom dialog layout
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_delete_medicine, null)

        // Create the Dialog and set the custom layout
        val dialog = Dialog(context)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Get references to the views in the custom layout
        val buttonDelete = dialogView.findViewById<Button>(R.id.buttonDelete)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)

        // Set up the button listeners
        buttonDelete.setOnClickListener {
            deleteMedicine(medicine)
            cancelAlarm(medicine)
            showToast("Medicine Reminder Deleted!")
            dialog.dismiss()
        }

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        // Show the dialog
        dialog.show()
    }


    // Medicine Reminder'i planlar
    private fun scheduleMedicineReminder(context: Context, medicine: Medicine) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MedicineReminderReceiver::class.java).apply {
            putExtra("medicineName", medicine.name)
            putExtra("medicineDosage", medicine.dosage)
            putExtra("medicineId", medicine.id)
            putExtra("medicineTime", medicine.timeToTake)
        }
        val pendingIntent = medicine.id.let {
            PendingIntent.getBroadcast(
                context,
                it,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, medicine.timeToTake.split(":")[0].toInt())
            set(Calendar.MINUTE, medicine.timeToTake.split(":")[1].toInt())
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Alarm zamanı bugün için geçmişse bir sonraki gün için ayarlar
        alarmClockInfo = if (calendar.timeInMillis < System.currentTimeMillis()) {
            AlarmManager.AlarmClockInfo(calendar.timeInMillis + 86400000, pendingIntent)
        } else {
            AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent)
        }

        alarmManager.setAlarmClock(alarmClockInfo!!, pendingIntent)
    }

    // Medicine verisini günceller
    private fun updateMedicine(updatedMedicine: Medicine) {
        val dao = MedicineDatabase.getDatabase(context).medicineDao()
        dao.update(updatedMedicine)
        submitMedicineList(dao.getAllMedicines())
    }

    // Medicine listesi için fark algoritması
    private class MedicineDiffCallback : DiffUtil.ItemCallback<Medicine>() {
        override fun areItemsTheSame(oldItem: Medicine, newItem: Medicine): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Medicine, newItem: Medicine): Boolean {
            return oldItem == newItem
        }
    }
}