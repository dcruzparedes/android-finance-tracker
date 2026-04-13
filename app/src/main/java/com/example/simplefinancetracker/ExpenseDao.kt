package com.example.simplefinancetracker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

// Flow: Self updating data stream
import kotlinx.coroutines.flow.Flow

// Dao: Data Access Object - Interface for database operations
@Dao
interface ExpenseDao {

    @Insert
    suspend fun insertExpense(expense: Expense): Long

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    fun getExpenseById(id: Int): Flow<Expense>

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: Int)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Transaction
    @Query("SELECT * FROM expenses ORDER BY createdAt DESC")
    fun getAllExpensesWithCategories(): Flow<List<ExpenseWithCategories>>

    @Transaction
    @Query("SELECT * FROM expenses ORDER BY amount DESC")
    fun getAllExpensesByAmountDesc(): Flow<List<ExpenseWithCategories>>

    @Transaction
    @Query("SELECT * FROM expenses ORDER BY amount ASC")
    fun getAllExpensesByAmountAsc(): Flow<List<ExpenseWithCategories>>

    @Transaction
    @Query("SELECT * FROM expenses ORDER BY name COLLATE NOCASE ASC")
    fun getAllExpensesByNameAsc(): Flow<List<ExpenseWithCategories>>

    @Transaction
    @Query("SELECT * FROM expenses ORDER BY name COLLATE NOCASE DESC")
    fun getAllExpensesByNameDesc(): Flow<List<ExpenseWithCategories>>

}

// !! Suspend means the function runs in a coroutine