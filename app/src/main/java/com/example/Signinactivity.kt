package com.example

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.Utils.utils
import com.example.medicinereminder.MainActivity
import com.example.medicinereminder.databinding.ActivitySigninactivityBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.lang.Exception

class Signinactivity : AppCompatActivity() {

        // Declare binding object
        private lateinit var binding: ActivitySigninactivityBinding

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // Initialize binding
            binding = ActivitySigninactivityBinding.inflate(layoutInflater)
            setContentView(binding.root)

            binding.button.setOnClickListener{
                val email = binding.email.text.toString()
                val password = binding.password.text.toString()
                loginUser(email,password)
            }
            binding.textView.setOnClickListener{
                startActivity(Intent(this,Signupactivity::class.java))
                finish()
            }
    }

    private fun loginUser(email: String, password: String) {
        utils.showdialog(this)
        val firebaseAuth= FirebaseAuth.getInstance()
        lifecycleScope.launch {
            try {
                val authResult = firebaseAuth.signInWithEmailAndPassword(email,password).await()
                val currentUser = authResult.user?.uid


                val verifyEmail = firebaseAuth.currentUser?.isEmailVerified
                if (verifyEmail==true){
                    if (currentUser != null){
                        FirebaseDatabase.getInstance().getReference("Users").child(currentUser).addListenerForSingleValueEvent(object   :
                            ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val currentUserData = snapshot.getValue(Users::class.java)
                                startActivity(Intent(this@Signinactivity, MainActivity::class.java))
                                finish()

//
                            }

                            override fun onCancelled(error: DatabaseError) {
                                utils.hideDialog()
                                utils.showtoast(this@Signinactivity,error.message)
                            }


                        })
                    }
                    else{
                        utils.hideDialog()
                        utils.showtoast(this@Signinactivity,"User not found \n Please Sign up first")

                    }
                }
                else{
                    utils.hideDialog()
                    utils.showtoast(this@Signinactivity,"Email not verified")

                }
            }
            catch (e: Exception){
                utils.hideDialog()
                utils.showtoast(this@Signinactivity,e.message!!)
            }
        }
    }
}