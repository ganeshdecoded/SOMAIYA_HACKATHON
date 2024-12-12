package com.example.a7minutesworkout

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.medicinereminder.Cart
import com.example.medicinereminder.R
import com.example.medicinereminder.databinding.ActivityHistoryBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate

class HistoryActivity : AppCompatActivity() {

    private var binding: ActivityHistoryBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setSupportActionBar(binding?.toolbarHistory)
        if(supportActionBar != null) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "History"
        }
        binding?.toolbarHistory?.setNavigationOnClickListener {
            onBackPressed()
        }

//        val dao = (application as WorkoutApp).db.historyDao()
//        getAllHistory(dao)
        val db = HistoryDatabase.getInstance(this)
        val dao = db.historyDao()
        // Insert records only once (to avoid multiple insertions)
        lifecycleScope.launch {
            if ((dao.fetchAllHistory().firstOrNull() ?: emptyList()).isEmpty()) {
                insertPredefinedRecords(dao)
                getAllHistory(dao) // Refresh the list after insertion
            } else {
                getAllHistory(dao) // Load existing records
            }
        }


        binding?.ibDeleteAll?.setOnClickListener {
            deleteAllRecordAlertDialog(dao)
        }

    }

    private suspend fun insertPredefinedRecords(historyDao: HistoryDao) {
        val sampleData = listOf(
            HistoryEntity(date = "2024-10-01", totalExerciseDone = 7),
            HistoryEntity(date = "2024-10-02", totalExerciseDone = 12),
            HistoryEntity(date = "2024-10-03", totalExerciseDone = 10),
            HistoryEntity(date = "2024-10-05", totalExerciseDone = 12),
            HistoryEntity(date = "2024-10-07", totalExerciseDone = 12),
            HistoryEntity(date = "2024-10-09", totalExerciseDone = 7),
            HistoryEntity(date = "2024-10-19", totalExerciseDone = 7),
        )

        // Insert each record into the database
        sampleData.forEach { historyDao.insert(it) }

        Toast.makeText(this, "Sample records added to history", Toast.LENGTH_SHORT).show()
    }


    private fun getAllHistory(historyDao: HistoryDao) {
        lifecycleScope.launch {
            historyDao.fetchAllHistory().collect { allCompletedExercisesList ->
                Log.d("HistoryActivity", "Fetched History: $allCompletedExercisesList")  // Add this line
                 if (allCompletedExercisesList.isNotEmpty()) {
                    binding?.rvHistory?.visibility = View.VISIBLE
                    binding?.ivNoDataAvailable?.visibility = View.GONE
                    binding?.tvNoDataAvailable?.visibility = View.GONE


                     // Calculate weekly exercise
                     calculateWeeklyExercise(allCompletedExercisesList)

                    val dataToAdapter = ArrayList(allCompletedExercisesList)

                    val historyAdapter = HistoryAdapter(dataToAdapter,
                        { deleteId ->
                            deleteRecord(deleteId, historyDao) } )

                    binding?.rvHistory?.layoutManager = LinearLayoutManager(this@HistoryActivity)
                    binding?.rvHistory?.adapter = historyAdapter

                } else {
                    binding?.rvHistory?.visibility = View.GONE
                    binding?.ivNoDataAvailable?.visibility = View.VISIBLE
                    binding?.tvNoDataAvailable?.visibility = View.VISIBLE
                }

            }
        }
    }

//    private fun calculateWeeklyExercise(allHistory: List<HistoryEntity>) {
//        val dateExerciseMap = mutableMapOf<String, Int>() // To hold date and total exercises for that date
//
//        allHistory.forEach { history ->
//            // Assuming history.date is a string in a specific format (e.g., "yyyy-MM-dd")
//            val date = history.date
//            dateExerciseMap[date] = (dateExerciseMap[date] ?: 0) + history.totalExerciseDone
//        }
//        // Calculate total exercises done
//        val totalExercises = dateExerciseMap.values.sum()
//
//        // Display total exercises in a TextView (assuming you have a TextView with id tvTotalExercises)
//        binding?.tvTotalExercises?.text = "Total Exercises in this Week: $totalExercises"
//
//        val int = 0;
//        val sharedPreferences = getSharedPreferences("ExercisePrefs", MODE_PRIVATE)
//        val editor = sharedPreferences.edit()
//        // Check if there are 7 different dates
//
//        val uniqueDates = dateExerciseMap.keys.size
//        if (uniqueDates >= 7) {
//            editor.putInt("NoofExercise", totalExercises)
//            editor.apply() // Save the value asynchronously
//        }else{
//            editor.putInt("NoofExercise", int)
//            editor.apply() // Save the value asynchronously
//        }
//    }

    private fun calculateWeeklyExercise(allHistory: List<HistoryEntity>) {
        // Use a Set to collect unique dates (since a Set automatically removes duplicates)
        val uniqueDates = allHistory.map { it.date }.toSet()

        // Create a map to sum up exercises by unique date
        val dateExerciseMap = mutableMapOf<String, Int>()

        allHistory.forEach { history ->
            val date = history.date
            dateExerciseMap[date] = (dateExerciseMap[date] ?: 0) + history.totalExerciseDone
        }

        // Calculate total exercises done
        val totalExercises = dateExerciseMap.values.sum()

        // Display total exercises in a TextView
        binding?.tvTotalExercises?.text = "Total Exercises in this Week: $totalExercises"

        val sharedPreferences = getSharedPreferences("ExercisePrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Check if there are more than 6 unique dates
        if (uniqueDates.size > 6) {
            editor.putInt("NoofExercise", totalExercises)
        } else {
            editor.putInt("NoofExercise", 0)
        }
        editor.apply() // Save the value asynchronously
    }


    private fun deleteRecord(id: Int, historyDao: HistoryDao) {
        lifecycleScope.launch {
            historyDao.delete(HistoryEntity(id))
        }
    }

    private fun deleteAllRecordAlertDialog(historyDao: HistoryDao){
        // Dialog Builder "Delete All History"
        val sureDialog = Dialog(this)

        sureDialog.setContentView(R.layout.want_to_delete_dialog)
        sureDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        sureDialog.setCancelable(false)
        sureDialog.setTitle("Delete History")

        // on yes it will delete all the histories
        sureDialog.findViewById<Button>(R.id.yes_button).setOnClickListener {
            lifecycleScope.launch {
                historyDao.deleteAllHistory()

                Toast.makeText(applicationContext,
                    "All Record Deleted",
                    Toast.LENGTH_LONG)
                    .show()
            }
            sureDialog.dismiss()
        }
        // on no it will do nothing
        sureDialog.findViewById<Button>(R.id.no_button).setOnClickListener {
            sureDialog.dismiss()
        }

        sureDialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}