package com.example.medicinereminder

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.Users
import com.example.Utils.utils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch

class Cart : AppCompatActivity() {
    private var NoofExercise: Int = 0 // Initialize step count

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cart)


        var age: Int = 0 // Variable to store age

        val currentUser = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUser != null) {
            lifecycleScope.launch {
                try {
                    // Reference to the current user's data in Firebase
                    val userRef = FirebaseDatabase.getInstance()
                        .getReference("Users").child(currentUser)

                    // Fetch the data once
                    userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            // Ensure the snapshot exists and parse the "userage" field
                            val userAge = snapshot.child("userage").getValue(String::class.java)
                            if (userAge != null) {
                                age = userAge.toIntOrNull() ?: 0 // Store age or default to 0
                                utils.showtoast(this@Cart, "Age fetched: $age")
                            } else {
                                utils.showtoast(this@Cart, "Age not found")
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            utils.showtoast(this@Cart, "Error: ${error.message}")
                        }
                    })
                } catch (e: Exception) {
                    utils.showtoast(this@Cart, "Error occurred")
                }
            }
        } else {
            utils.showtoast(this@Cart, "User not logged in")
        }




        // Retrieve the total number of exercises from SharedPreferences
        val sharedPreferences = getSharedPreferences("ExercisePrefs", MODE_PRIVATE)
        NoofExercise = sharedPreferences.getInt("NoofExercise", 0)

        val cart = intent.getParcelableArrayListExtra<Product>("cart") ?: emptyList()

        // Initialize RecyclerView
        val recyclerView: RecyclerView = findViewById(R.id.cart_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = CartAdapter(cart)

        // Calculate total
        val totalAmount = cart.sumOf { it.price * it.quantity }
        findViewById<TextView>(R.id.total_amount).text = "Total: ₹$totalAmount"

        findViewById<Button>(R.id.pay_button).setOnClickListener {
            if (NoofExercise == 0){
                Toast.makeText(this, "Payment done", Toast.LENGTH_SHORT).show()
            }
            else{
                val discount = calculateDiscount(NoofExercise,age)
                val finalAmount = totalAmount - (totalAmount * discount)
                showDiscountDialog(discount, finalAmount)
            }
        }
    }

    private fun calculateDiscount(NoofExercise: Int, age: Int): Double {
        return when {
            // Age between 0 and 50
            age in 1..50 -> {
                when {
                    NoofExercise > 80 -> 0.18 // 18% discount
                    NoofExercise > 70 -> 0.12 // 12% discount
                    NoofExercise > 60 -> 0.07 // 7% discount
                    else -> 0.0 // Default 2% discount
                }
            }
            // Age between 51 and 60
            age in 51..60 -> {
                when {
                    NoofExercise > 65 -> 0.18 // 18% discount
                    NoofExercise > 55 -> 0.12 // 12% discount
                    NoofExercise > 45 -> 0.07 // 7% discount
                    else -> 0.0 // Default 2% discount
                }
            }
            // Age above 60
            age > 60 -> {
                when {
                    NoofExercise > 40 -> 0.18 // 18% discount
                    NoofExercise > 30 -> 0.12 // 12% discount
                    NoofExercise > 20 -> 0.07 // 7% discount
                    else -> 0.02 // Default 2% discount
                }
            }
            // Default case if age is 0 or less
            else -> 0.0 // Default 2% discount
        }
    }



    private fun showDiscountDialog(discount: Double, finalAmount: Double) {
        val discountPercentage = (discount * 100).toInt()
        val dialog = AlertDialog.Builder(this)
            .setTitle("Congratulations!")
            .setMessage("Based on your exercise stats, you have received a ${discountPercentage}% discount.")
            .setPositiveButton("Ok") { _, _ ->
                findViewById<TextView>(R.id.total_amount).text = "Total: ₹$finalAmount"
            }
            .create()

        dialog.show()
    }
}
