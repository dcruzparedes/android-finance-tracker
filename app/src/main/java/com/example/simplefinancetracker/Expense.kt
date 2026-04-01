package com.example.simplefinancetracker

import androidx.room.Entity
import androidx.room.PrimaryKey

// Expenses entity, representing a single expense
@Entity(tableName = "expenses")
data class Expense (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val amount: Double,
    val date: String,
    val category: String
)