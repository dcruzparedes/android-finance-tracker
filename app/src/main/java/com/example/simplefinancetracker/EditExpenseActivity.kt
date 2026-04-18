package com.example.simplefinancetracker

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.simplefinancetracker.databinding.ActivityViewAndEditExpenseBinding
import androidx.room.withTransaction
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class EditExpenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewAndEditExpenseBinding
    private lateinit var db: ExpenseDatabase
    private var expenseId: Int = -1
    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewAndEditExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = ExpenseDatabase.getDatabase(this)
        expenseId = intent.getIntExtra("EXPENSE_ID", -1)

        if (expenseId == -1) {
            Toast.makeText(this, "@string/error_expense_not_found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        loadExpenseData()
        setupCurrency()
        setupListeners()
    }

    private fun setupCurrency() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val currency = prefs.getString("currency", "$") ?: "$"
        binding.amountInputLayout.prefixText = "$currency "
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadExpenseData() {
        lifecycleScope.launch {
            // Fetch the expense with its categories
            val expensesWithCategories = db.expenseDao().getAllExpensesWithCategories().first()
            val item = expensesWithCategories.find { it.expense.id == expenseId }

            item?.let {
                binding.nameEditText.setText(it.expense.name)
                binding.amountEditText.setText(it.expense.amount.toString())
                binding.dateEditText.setText(it.expense.date)
                
                // For date picker initialization
                val dateParts = it.expense.date.split("-")
                if (dateParts.size == 3) {
                    selectedDate.set(dateParts[0].toInt(), dateParts[1].toInt() - 1, dateParts[2].toInt())
                }

                loadCategories(it.categories)
            }
        }
    }

    private fun loadCategories(selectedCategories: List<Category>) {
        lifecycleScope.launch {
            // Use first() instead of collect to prevent UI refresh while editing
            val allCategories = db.categoryDao().getAllCategories().first()
            binding.categoryChipGroup.removeAllViews()
            allCategories.forEach { category ->
                val chip = Chip(this@EditExpenseActivity).apply {
                    text = category.name
                    isCheckable = true
                    isChecked = selectedCategories.any { it.id == category.id }
                    tag = category.id
                }
                binding.categoryChipGroup.addView(chip)
            }
        }
    }

    private fun setupListeners() {
        binding.dateEditText.setOnClickListener {
            showDatePicker()
        }

        binding.saveButton.setOnClickListener {
            saveExpense()
        }

        binding.deleteButton.setOnClickListener {
            deleteExpense()
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate.set(year, month, dayOfMonth)
                val dateString = "%d-%02d-%02d".format(year, month + 1, dayOfMonth)
                binding.dateEditText.setText(dateString)
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveExpense() {
        val name = binding.nameEditText.text.toString()
        val amount = binding.amountEditText.text.toString().toDoubleOrNull() ?: 0.0
        val date = binding.dateEditText.text.toString()

        if (name.isBlank()) {
            binding.nameInputLayout.error = getString(R.string.error_name_is_required)
            return
        }

        // Gather selected category IDs from the ChipGroup
        val selectedCategoryIds = mutableListOf<Int>()
        for (i in 0 until binding.categoryChipGroup.childCount) {
            val chip = binding.categoryChipGroup.getChildAt(i) as? Chip
            if (chip?.isChecked == true) {
                selectedCategoryIds.add(chip.tag as Int)
            }
        }

        lifecycleScope.launch {
            try {
                // Fetch the original expense to preserve its createdAt timestamp
                val expensesWithCategories = db.expenseDao().getAllExpensesWithCategories().first()
                val originalExpense = expensesWithCategories.find { it.expense.id == expenseId }?.expense
                
                val createdAt = originalExpense?.createdAt ?: System.currentTimeMillis()
                
                val updatedExpense = Expense(
                    id = expenseId, 
                    name = name, 
                    amount = amount, 
                    date = date,
                    createdAt = createdAt
                )
                
                // Use withTransaction to ensure atomic update of expense and its category links
                db.withTransaction {
                    db.expenseDao().updateExpense(updatedExpense)
                    
                    // Remove old category links and insert new ones
                    db.categoryDao().deleteRefsByExpenseId(expenseId)
                    selectedCategoryIds.forEach { categoryId ->
                        db.categoryDao().insertExpenseCategoryRef(
                            ExpenseCategoryCrossRef(expenseId, categoryId)
                        )
                    }
                }
                
                Toast.makeText(this@EditExpenseActivity, getString(R.string.expense_updated_success), Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@EditExpenseActivity, getString(R.string.error_saving_expense), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun deleteExpense() {
        lifecycleScope.launch {
            db.expenseDao().deleteExpense(expenseId)
            Toast.makeText(this@EditExpenseActivity, getString(R.string.expense_deleted_success), Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
