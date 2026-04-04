package com.example.simplefinancetracker

import android.content.Intent
import android.os.Bundle
import android.view.View
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
        setupButtons()
    }

    private fun setupRecyclerView() {
        adapter = ExpenseAdapter { count ->
            if (count > 0) {
                binding.btnDeleteSelected.visibility = View.VISIBLE
                supportActionBar?.title = "$count selected"
            } else {
                binding.btnDeleteSelected.visibility = View.GONE
                supportActionBar?.title = getString(R.string.app_name)
            }
        }
        
        binding.rvExpenses.layoutManager = LinearLayoutManager(this)
        binding.rvExpenses.adapter = adapter
    }

    private fun setupButtons() {
        binding.fab.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        binding.btnDeleteSelected.setOnClickListener {
            val selectedIds = adapter.getSelectedIds()
            lifecycleScope.launch {
                val db = ExpenseDatabase.getDatabase(this@MainActivity)
                selectedIds.forEach { id ->
                    db.expenseDao().deleteExpense(id)
                }
                adapter.clearSelection()
            }
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.nav_home -> { /* Navigate to Home */ true }
                R.id.nav_stats -> { /* Show Stats */ true }
                R.id.nav_settings -> { /* Show Settings */ true }
                else -> false
            }
        }
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
