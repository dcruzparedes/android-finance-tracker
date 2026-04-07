package com.example.simplefinancetracker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.simplefinancetracker.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ExpenseAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeExpenses()
        setupButtons()
    }

    private fun setupRecyclerView() {
        adapter = ExpenseAdapter(
            onItemClick = { item ->
                val intent = Intent(requireContext(), EditExpenseActivity::class.java).apply {
                    putExtra("EXPENSE_ID", item.expense.id)
                }
                startActivity(intent)
            },
            onSelectionChanged = { count ->
                if (count > 0) {
                    binding.btnDeleteSelected.visibility = View.VISIBLE
                } else {
                    binding.btnDeleteSelected.visibility = View.GONE
                }
            }
        )
        
        binding.rvExpenses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvExpenses.adapter = adapter
    }

    private fun setupButtons() {
        binding.fab.setOnClickListener {
            startActivity(Intent(requireContext(), AddExpenseActivity::class.java))
        }

        binding.btnDeleteSelected.setOnClickListener {
            val selectedIds = adapter.getSelectedIds()
            viewLifecycleOwner.lifecycleScope.launch {
                val db = ExpenseDatabase.getDatabase(requireContext())
                selectedIds.forEach { id ->
                    db.expenseDao().deleteExpense(id)
                }
                adapter.clearSelection()
            }
        }
    }

    private fun observeExpenses() {
        viewLifecycleOwner.lifecycleScope.launch {
            ExpenseDatabase.getDatabase(requireContext())
                .expenseDao()
                .getAllExpensesWithCategories()
                .collect { expenses ->
                    adapter.submitList(expenses)
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
