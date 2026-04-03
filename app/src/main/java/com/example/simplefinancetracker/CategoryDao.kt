package com.example.simplefinancetracker

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
// Flow: Self updating data stream
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert
    suspend fun insertCategory(category: Category)

    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    fun getCategoryById(id: Int): Flow<Category>

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    // Linking the expense to a category
    @Insert
    suspend fun insertExpenseCategoryRef(crossRef: ExpenseCategoryCrossRef)

}