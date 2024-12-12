package com.example.medicinereminder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.medicinereminder.Room.Medicine
import com.example.medicinereminder.Room.MedicineDatabase
import com.example.medicinereminder.databinding.FragmentReminderSettingsBinding

class ReminderSettingsFragment : Fragment() {

    private lateinit var medicineAdapter: MedicineAdapter
    private lateinit var binding: FragmentReminderSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReminderSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButtonLogo.setOnClickListener {
            requireActivity().onBackPressed()
        }

        // Set up RecyclerView
        binding.recyclerViewReminder.layoutManager = LinearLayoutManager(requireContext())
        medicineAdapter = MedicineAdapter(requireContext())
        binding.recyclerViewReminder.adapter = medicineAdapter

        // Load medicines
        loadMedicines()
    }

    // Load medicines from the database
    private fun loadMedicines() {
        val medicineDao = MedicineDatabase.getDatabase(requireContext()).medicineDao()
        val medicineList: List<Medicine> = medicineDao.getAllMedicines()

        if (medicineList.isEmpty()) {
            // Show the "No medicines found" message
            binding.textViewNoMedicines.visibility = View.VISIBLE
            binding.recyclerViewReminder.visibility = View.GONE
        } else {
            // Hide the "No medicines found" message and show the RecyclerView
            binding.textViewNoMedicines.visibility = View.GONE
            binding.recyclerViewReminder.visibility = View.VISIBLE
        }

        // Update the adapter with the medicine list
        medicineAdapter.submitMedicineList(medicineList)
    }
}
