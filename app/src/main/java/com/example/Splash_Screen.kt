package com.example.medicinereminder

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.Signinactivity
import com.example.Utils.utils
import com.example.home.Fall_detection
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import java.util.*


class Splash_Screen : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.activity_splash_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Delay for the splash screen
        view.postDelayed({
            checkUserAuthentication()
        }, 1500) // Show splash screen for 1500 milliseconds
    }

    private fun checkUserAuthentication() {
        // Check for user authentication
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            // If no user is authenticated, redirect to the sign-in activity
            navigateToSignIn()
        } else {
            // Proceed with user data check
            checkUserData(currentUser.uid)
        }
    }

    private fun checkUserData(uid: String) {
        FirebaseDatabase.getInstance().getReference("Users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (isAdded) { // Check if the fragment is still added
                        if (snapshot.exists()) {
                            // User data exists, launch the main activity
                            navigateToFallDetection()
                        } else {
                            // No user data found, go to sign-in activity
                            navigateToSignIn()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    if (isAdded) { // Check if the fragment is still added
                        utils.showtoast(requireContext(), error.message)
                    }
                }
            })
    }

    private fun navigateToFallDetection() {
        if (isAdded) { // Check if the fragment is still added
            startActivity(Intent(requireContext(), Fall_detection::class.java))
            requireActivity().finish()
        }
    }

    private fun navigateToSignIn() {
        if (isAdded) { // Check if the fragment is still added
            startActivity(Intent(requireContext(), Signinactivity::class.java))
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up any resources if necessary
    }
}