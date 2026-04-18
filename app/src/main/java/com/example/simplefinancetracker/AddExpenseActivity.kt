package com.example.simplefinancetracker

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.simplefinancetracker.databinding.ActivityAddExpenseBinding
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddExpenseBinding
    private lateinit var db: ExpenseDatabase
    private var allCategories: List<Category> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        db = ExpenseDatabase.getDatabase(this)

        setupCategoryDropdown()
        setupDatePicker()
        setupCurrency()
        setupButtons()
    }

    private fun setupCurrency() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val currency = prefs.getString("currency", "$") ?: "$"
        binding.tilAmount.prefixText = currency
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
            val adapter = ArrayAdapter(this@AddExpenseActivity, R.layout.list_item, categoryNames)
            binding.actvCategory.setAdapter(adapter)
        }
    }

    private fun setupDatePicker() {
        binding.etDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.date_field_hint_text))
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                binding.etDate.setText(sdf.format(Date(selection)))
            }
            
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val amountText = binding.etAmount.text.toString().trim()
            var date = binding.etDate.text.toString().trim()
            var selectedCategoryName = binding.actvCategory.text.toString().trim()

            // Reset errors
            binding.tilName.error = null
            binding.tilAmount.error = null

            var hasError = false

            if (name.isEmpty()) {
                binding.tilName.error = getString(R.string.error_name_is_required)
                hasError = true
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                binding.tilAmount.error = getString(R.string.error_invalid_amount)
                hasError = true
            }

            if (date.isEmpty()) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                date = sdf.format(Date())
            }

            if (hasError) return@setOnClickListener

            if (selectedCategoryName.isEmpty()) {
                selectedCategoryName = "Other"
            }

            lifecycleScope.launch {
                val expenseId = db.expenseDao().insertExpense(
                    Expense(
                        name = name,
                        amount = amount!!,
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
