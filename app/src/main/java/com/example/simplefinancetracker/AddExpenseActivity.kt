package com.example.simplefinancetracker

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.simplefinancetracker.databinding.ActivityAddExpenseBinding
import kotlinx.coroutines.launch
import java.util.Calendar

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddExpenseBinding
    private lateinit var db: ExpenseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = ExpenseDatabase.getDatabase(this)

        setupCategoryDropdown()
        setupDatePicker()
        setupButtons()
    }

    private fun setupCategoryDropdown() {
        val categories = listOf("Food", "Transport", "Health", "Entertainment", "Home", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.actvCategory.setAdapter(adapter)
    }

    private fun setupDatePicker() {
        binding.etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    binding.etDate.setText("%02d/%02d/%d".format(day, month + 1, year))
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
            var category = binding.actvCategory.text.toString().trim()

            if (name.isEmpty() || amountText.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, "Please complete all the fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // If no category is selected set it to "No category"
            if (category.isEmpty()) {
                category = "No category"
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Ingresá un monto válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                db.expenseDao().insertExpense(
                    Expense(
                        name = name,
                        amount = amount,
                        date = date,
                        category = category
                    )
                )
                finish()
            }
        }
    }
}