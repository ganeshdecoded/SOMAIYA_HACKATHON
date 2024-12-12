package com.example.medicinereminder

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.medicinereminder.databinding.FragmentHomeBinding
import sszj.s.geminiapi.ui.GeminiChatBotActivity

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Access the ImageView using the binding object
        val medicalPromptButton: ImageView = binding.mainchatbotgo
        medicalPromptButton.setOnClickListener {
            // Create an intent to start the GeminiChatBotActivity
            val intent = Intent(requireContext(), GeminiChatBotActivity::class.java)
            // Pass the medical context with the intent
            intent.putExtra("chat_context", "medical")
            // Start the GeminiChatBotActivity
            startActivity(intent)
        }

        // Medicine Fragment'ı Açmak için kullanılır
        binding.addMedicine.setOnClickListener {
            (activity as MainActivity).openFragment(MedicineFragment())
        }

        // Reminder Settings Fragment'ı Açmak için kullanılır
        binding.listMedicine.setOnClickListener {
            (activity as MainActivity).openFragment(ReminderSettingsFragment())
        }

        binding.store.setOnClickListener {
            // Use requireContext() or activity as the Context
            val intent = Intent(requireContext(), MedicineStore::class.java)
            startActivity(intent)
        }
    }
}