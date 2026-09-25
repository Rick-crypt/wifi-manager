package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_field_values",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["fieldId"]),
        Index(value = ["userId", "fieldId"], unique = true)
    ]
)
data class UserFieldValueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val fieldId: Long,
    val value: String
)
