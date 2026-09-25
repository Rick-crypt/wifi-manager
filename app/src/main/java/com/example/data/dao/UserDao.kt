package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.UserEntity
import com.example.data.entity.UserFieldValueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    suspend fun getAllUsersDirect(): List<UserEntity>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE status = :status ORDER BY createdAt DESC")
    fun getUsersByStatus(status: String): Flow<List<UserEntity>>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: Long)

    // User Field Values
    @Query("SELECT * FROM user_field_values")
    fun getAllFieldValues(): Flow<List<UserFieldValueEntity>>

    @Query("SELECT * FROM user_field_values")
    suspend fun getAllFieldValuesDirect(): List<UserFieldValueEntity>

    @Query("SELECT * FROM user_field_values WHERE userId = :userId")
    suspend fun getFieldValuesForUser(userId: Long): List<UserFieldValueEntity>

    @Query("SELECT * FROM user_field_values WHERE userId = :userId")
    fun observeFieldValuesForUser(userId: Long): Flow<List<UserFieldValueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFieldValue(fieldValue: UserFieldValueEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFieldValues(fieldValues: List<UserFieldValueEntity>)

    @Query("DELETE FROM user_field_values WHERE userId = :userId")
    suspend fun deleteFieldValuesForUser(userId: Long)

    @Query("DELETE FROM user_field_values WHERE fieldId = :fieldId")
    suspend fun deleteFieldValuesForField(fieldId: Long)
}
