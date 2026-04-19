package com.example.extra

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
import com.example.extra.databinding.FragmentSettingsBinding
import com.example.extra.databinding.ItemCategoryConfigBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.edit

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
        setupCategoryManagement()
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
            prefs.edit { putString("currency", selected) }
        }
    }

    private fun setupCategoryManagement() {
        val db = ExpenseDatabase.getDatabase(requireContext())
        
        // Observe categories and update list
        viewLifecycleOwner.lifecycleScope.launch {
            db.categoryDao().getAllCategories().collectLatest { categories ->
                updateCategoryList(categories)
            }
        }

        binding.btnAddCategory.setOnClickListener {
            showAddEditCategoryDialog(null)
        }
    }

    private fun updateCategoryList(categories: List<Category>) {
        if (_binding == null) return
        binding.categoryListContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        
        categories.forEach { category ->
            val itemBinding = ItemCategoryConfigBinding.inflate(inflater, binding.categoryListContainer, false)
            itemBinding.tvCategoryName.text = category.name
            
            itemBinding.btnEditCategory.setOnClickListener {
                showAddEditCategoryDialog(category)
            }
            
            itemBinding.btnDeleteCategory.setOnClickListener {
                checkAndDeleteCategory(category)
            }
            
            binding.categoryListContainer.addView(itemBinding.root)
        }
    }

    private fun showAddEditCategoryDialog(category: Category?) {
        val builder = AlertDialog.Builder(requireContext())
        val isEdit = category != null
        builder.setTitle(if (isEdit) R.string.edit_category_title else R.string.add_category_hint)

        val input = com.google.android.material.textfield.TextInputEditText(requireContext())
        input.setText(category?.name ?: "")
        
        val container = android.widget.FrameLayout(requireContext())
        val params = android.widget.FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(48, 24, 48, 24)
        input.layoutParams = params
        container.addView(input)
        builder.setView(container)

        builder.setPositiveButton(R.string.save_button_text) { _, _ ->
            val name = input.text.toString().trim()
            if (name.isNotEmpty()) {
                saveCategory(category?.id ?: 0, name)
            }
        }
        builder.setNegativeButton(R.string.cancel_button_text, null)
        builder.show()
    }

    private fun saveCategory(id: Int, name: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val db = ExpenseDatabase.getDatabase(requireContext())
            val existingCategory = withContext(Dispatchers.IO) {
                db.categoryDao().getCategoryByName(name)
            }

            if (existingCategory != null && existingCategory.id != id) {
                android.widget.Toast.makeText(requireContext(), R.string.category_exists_error, android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }

            withContext(Dispatchers.IO) {
                if (id == 0) {
                    db.categoryDao().insertCategory(Category(name = name))
                } else {
                    db.categoryDao().updateCategory(Category(id = id, name = name))
                }
            }
            
            val messageRes = if (id == 0) R.string.save_button_text else R.string.category_updated_success
            if (id != 0) {
                android.widget.Toast.makeText(requireContext(), messageRes, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkAndDeleteCategory(category: Category) {
        viewLifecycleOwner.lifecycleScope.launch {
            val db = ExpenseDatabase.getDatabase(requireContext())
            val expenses = withContext(Dispatchers.IO) {
                db.expenseDao().getExpensesForCategory(category.id).firstOrNull()
            }
            
            if (expenses != null && expenses.isNotEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.category_delete_error_title)
                    .setMessage(R.string.category_delete_error_message)
                    .setPositiveButton(R.string.ok_button, null)
                    .show()
            } else {
                withContext(Dispatchers.IO) {
                    db.categoryDao().deleteCategory(category)
                }
                android.widget.Toast.makeText(requireContext(), R.string.category_deleted_success, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupThemeSetting() {
        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)

        binding.switchDarkMode.isChecked = isDarkMode

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit {putBoolean("dark_mode", isChecked) }

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
