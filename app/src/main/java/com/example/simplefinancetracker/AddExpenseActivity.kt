package com.example.simplefinancetracker

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.simplefinancetracker.databinding.ActivityAddExpenseBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddExpenseBinding
    private lateinit var db: ExpenseDatabase
    private var allCategories: List<Category> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = ExpenseDatabase.getDatabase(this)

        setupCategoryDropdown()
        setupDatePicker()
        setupCurrency()
        setupButtons()
    }

    private fun setupCurrency() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val currency = prefs.getString("currency", "$") ?: "$"
        binding.etAmount.hint = "Amount ($currency)"
    }

    private fun setupCategoryDropdown() {
        lifecycleScope.launch {
            // Load categories from database
            allCategories = db.categoryDao().getAllCategories().first()

            // If database is empty, send some default categories
            if (allCategories.isEmpty()) {
                val defaultNames = listOf("Food", "Work", "University", "Other")
                defaultNames.forEach { name ->
                    db.categoryDao().insertCategory(Category(name = name))
                }
                allCategories = db.categoryDao().getAllCategories().first()
            }

            val categoryNames = allCategories.map { it.name }
            val adapter = ArrayAdapter(this@AddExpenseActivity, android.R.layout.simple_dropdown_item_1line, categoryNames)
            binding.actvCategory.setAdapter(adapter)
            binding.actvCategory.setOnClickListener {
                binding.actvCategory.showDropDown()
            }
            // Make it so the keyboard doesn't pop up when the dropdown is clicked
            binding.actvCategory.inputType = android.text.InputType.TYPE_NULL
        }
    }

    private fun setupDatePicker() {
        binding.etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    binding.etDate.setText("%d-%02d-%02d".format(year, month + 1, day))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val amountText = binding.etAmount.text.toString().trim()
            val date = binding.etDate.text.toString().trim()
            var selectedCategoryName = binding.actvCategory.text.toString().trim()

            if (name.isEmpty() || amountText.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, R.string.error_complete_fields, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedCategoryName.isEmpty()) {
                selectedCategoryName = "Other"
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, R.string.error_invalid_amount, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val expenseId = db.expenseDao().insertExpense(
                    Expense(
                        name = name,
                        amount = amount,
                        date = date
                    )
                ).toInt()

                val category = allCategories.find { it.name == selectedCategoryName }

                // Link category and expense in the Join Table
                if (category != null) {
                    db.categoryDao().insertExpenseCategoryRef(
                        ExpenseCategoryCrossRef(
                            expenseId = expenseId,
                            categoryId = category.id
                        )
                    )
                }

                finish()
            }
        }
    }
}
