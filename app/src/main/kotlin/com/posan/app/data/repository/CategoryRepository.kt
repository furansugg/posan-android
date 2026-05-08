package com.posan.app.data.repository

import com.posan.app.data.local.dao.CategoryDao
import com.posan.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    fun observeAll(): Flow<List<CategoryEntity>> = categoryDao.observeAll()
    suspend fun findById(id: Long): CategoryEntity? = categoryDao.findById(id)
    suspend fun insert(category: CategoryEntity): Long = categoryDao.insert(category)
    suspend fun update(category: CategoryEntity) = categoryDao.update(category)
    suspend fun delete(category: CategoryEntity) = categoryDao.delete(category)
}
