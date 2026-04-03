package com.example.simplefinancetracker

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.simplefinancetracker.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ExpenseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        observeExpenses()

        binding.fab.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        adapter = ExpenseAdapter()
        binding.rvExpenses.layoutManager = LinearLayoutManager(this)
        binding.rvExpenses.adapter = adapter
    }

    private fun observeExpenses() {
        lifecycleScope.launch {
            ExpenseDatabase.getDatabase(this@MainActivity)
                .expenseDao()
                .getAllExpensesWithCategories()
                .collect { expenses ->
                    adapter.submitList(expenses)
                }
        }
    }
}