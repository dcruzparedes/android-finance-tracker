package com.example.simplefinancetracker

//TODO: Add a select all button

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.simplefinancetracker.databinding.FragmentHomeBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ExpenseAdapter
    private var observationJob: Job? = null
    private var currentSortType: String = ""
    private var currentCategoryId: Int? = null

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
        setupSortDropdown()
        setupFilterDropdown()
        
        currentSortType = getString(R.string.sort_by_creation_date_selection_text)
        observeExpenses() 
        setupButtons()
    }

    private fun setupSortDropdown() {
        val options = arrayOf(
            getString(R.string.sort_by_creation_date_selection_text),
            getString(R.string.sort_by_name_ascending_selection_text),
            getString(R.string.sort_by_name_descending_selection_text),
            getString(R.string.sort_by_amount_ascending_selection_text),
            getString(R.string.sort_by_amount_descending_selection_text)
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, options)
        binding.sortAutoComplete.setAdapter(adapter)
        binding.sortAutoComplete.setText(options[0], false)

        binding.sortAutoComplete.setOnItemClickListener { _, _, position, _ ->
            currentSortType = options[position]
            observeExpenses()
        }
    }

    private fun setupFilterDropdown() {
        viewLifecycleOwner.lifecycleScope.launch {
            val db = ExpenseDatabase.getDatabase(requireContext())
            db.categoryDao().getAllCategories().collectLatest { categories ->
                val options = mutableListOf(getString(R.string.all_categories_selection_text))
                options.addAll(categories.map { it.name })
                
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, options)
                binding.filterAutoComplete.setAdapter(adapter)
                
                if (binding.filterAutoComplete.text.isEmpty()) {
                    binding.filterAutoComplete.setText(options[0], false)
                }

                binding.filterAutoComplete.setOnItemClickListener { _, _, position, _ ->
                    currentCategoryId = if (position == 0) null else categories[position - 1].id
                    observeExpenses()
                }
            }
        }
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
        // Cancel previous observation if it exists to avoid multiple collectors
        observationJob?.cancel()
        
        observationJob = viewLifecycleOwner.lifecycleScope.launch {
            val dao = ExpenseDatabase.getDatabase(requireContext()).expenseDao()

            val flow = when (currentSortType) {
                getString(R.string.sort_by_amount_descending_selection_text) -> dao.getAllExpensesByAmountDesc()
                getString(R.string.sort_by_amount_ascending_selection_text) -> dao.getAllExpensesByAmountAsc()
                getString(R.string.sort_by_name_ascending_selection_text) -> dao.getAllExpensesByNameAsc()
                getString(R.string.sort_by_name_descending_selection_text) -> dao.getAllExpensesByNameDesc()
                else -> dao.getAllExpensesWithCategories() // Default/Date
            }

            flow.collectLatest { expenses ->
                val filteredList = if (currentCategoryId == null) {
                    expenses
                } else {
                    expenses.filter { item -> 
                        item.categories.any { it.id == currentCategoryId }
                    }
                }
                adapter.submitList(filteredList)
                
                // Show empty state message if list is empty
                binding.tvNoExpenses.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
                binding.rvExpenses.visibility = if (filteredList.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
