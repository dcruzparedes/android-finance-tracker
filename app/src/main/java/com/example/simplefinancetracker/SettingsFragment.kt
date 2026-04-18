package com.example.simplefinancetracker

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.simplefinancetracker.databinding.FragmentSettingsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCurrencySetting()
        setupThemeSetting()
        setupClearData()
    }

    private fun setupCurrencySetting() {
        val currencies = arrayOf("$", "L", "€", "£", "¥")
        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val currentCurrency = prefs.getString("currency", "$") ?: "$"

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, currencies)
        binding.currencyAutoComplete.setAdapter(adapter)
        binding.currencyAutoComplete.setText(currentCurrency, false)

        binding.currencyAutoComplete.setOnItemClickListener { _, _, position, _ ->
            val selected = currencies[position]
            prefs.edit().putString("currency", selected).apply()
        }
    }

    private fun setupThemeSetting() {
        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)

        binding.switchDarkMode.isChecked = isDarkMode

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }

    private fun setupClearData() {
        binding.btnClearData.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.clear_data_dialog_title))
                .setMessage(getString(R.string.clear_data_dialog_message))
                .setPositiveButton(getString(R.string.clear_everything_button)) { _, _ ->
                    clearDatabase()
                }
                .setNegativeButton(getString(R.string.cancel_button_text), null)
                .show()
        }
    }

    private fun clearDatabase() {
        viewLifecycleOwner.lifecycleScope.launch {
            val db = ExpenseDatabase.getDatabase(requireContext())
            withContext(Dispatchers.IO) {
                db.clearAllTables()
            }
            android.widget.Toast.makeText(requireContext(), "All data cleared", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
