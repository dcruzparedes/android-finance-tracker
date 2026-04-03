package com.example.simplefinancetracker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.simplefinancetracker.databinding.ItemExpenseBinding

class ExpenseAdapter : ListAdapter<ExpenseWithCategories, ExpenseAdapter.ExpenseViewHolder>(DiffCallback) {

    companion object DiffCallback : DiffUtil.ItemCallback<ExpenseWithCategories>() {
        override fun areItemsTheSame(old: ExpenseWithCategories, new: ExpenseWithCategories) =
            old.expense.id == new.expense.id
        override fun areContentsTheSame(old: ExpenseWithCategories, new: ExpenseWithCategories) =
            old == new
    }

    inner class ExpenseViewHolder(private val binding: ItemExpenseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ExpenseWithCategories) {
            binding.tvName.text = item.expense.name
            binding.tvDate.text = item.expense.date
            binding.tvAmount.text = "L. %.2f".format(item.expense.amount) // TODO: Make currency customizable
            binding.tvCategory.text = item.categories.joinToString(", ") { it.name }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}