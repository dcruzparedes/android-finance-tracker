package com.example.simplefinancetracker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
// Flow: Self updating data stream
import kotlinx.coroutines.flow.Flow

// Dao: Data Access Object - Interface for database operations
@Dao
interface ExpenseDao {

    @Insert
    suspend fun insertExpense(expense: Expense): Long

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: Int)
}

// !! Suspend means the function runs in a coroutine