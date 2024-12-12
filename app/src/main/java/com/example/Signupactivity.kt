package com.example

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.Utils.utils
import com.example.medicinereminder.R
import com.example.medicinereminder.databinding.AccountDialogBinding
import com.example.medicinereminder.databinding.ActivitySignupactivityBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.lang.Exception

class Signupactivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupactivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupactivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button.setOnClickListener{createuser()}
    }

    private fun createuser() {
        utils.showdialog(this)

        val name = binding.username.text.toString()
        val email = binding.email.text.toString()
        val password = binding.password.text.toString()
        val age = binding.age.text.toString()
        val gender = binding.genderSpinner.selectedItem.toString()

        if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && age.isNotEmpty() && gender.isNotEmpty()) {
            saveUserData(name, email, password, age, gender)
        } else {
            utils.hideDialog()
            utils.showtoast(this, "Empty fields are not allowed")
        }
    }

    private fun saveUserData(name: String, email: String, password: String, age: String, gender: String) {

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) return@OnCompleteListener
            val token = task.result
            lifecycleScope.launch {
                val db = FirebaseDatabase.getInstance().getReference("Users")
                try {
                    val firebaseAuth = FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).await()

                    if (firebaseAuth.user != null) {
                        FirebaseAuth.getInstance().currentUser?.sendEmailVerification()?.addOnSuccessListener {
                            val dialog = AccountDialogBinding.inflate(LayoutInflater.from(this@Signupactivity))
                            val alertDialog = AlertDialog.Builder(this@Signupactivity)
                                .setView(dialog.root)
                                .create()
                            utils.hideDialog()
                            alertDialog.show()
                            dialog.btnok.setOnClickListener {
                                alertDialog.dismiss()
                                startActivity(Intent(this@Signupactivity, Signinactivity::class.java))
                                finish()
                            }
                        }

                        val uId = firebaseAuth.user?.uid.toString()
                        val users = Users(userid = uId, username = name, useremail = email, userpassword = password, userage = age, usergender = gender)
                        db.child(uId).setValue(users).await()

                    } else {
                        utils.hideDialog()
                        utils.showtoast(this@Signupactivity, "Sign-Up Failed")
                    }
                } catch (e: Exception) {
                    utils.hideDialog()
                    utils.showtoast(this@Signupactivity, e.message.toString())
                }
            }
        })
    }
}