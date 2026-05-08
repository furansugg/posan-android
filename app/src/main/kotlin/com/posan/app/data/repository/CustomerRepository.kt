package com.posan.app.data.repository

import com.posan.app.data.local.dao.CustomerDao
import com.posan.app.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerRepository @Inject constructor(
    private val customerDao: CustomerDao
) {
    fun observeAll(): Flow<List<CustomerEntity>> = customerDao.observeAll()
    fun search(query: String): Flow<List<CustomerEntity>> = customerDao.search(query)
    suspend fun findById(id: Long): CustomerEntity? = customerDao.findById(id)
    suspend fun insert(customer: CustomerEntity): Long = customerDao.insert(customer)
    suspend fun update(customer: CustomerEntity) = customerDao.update(customer)
    suspend fun delete(customer: CustomerEntity) = customerDao.delete(customer)
}
