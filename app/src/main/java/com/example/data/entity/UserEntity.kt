package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.UserStatus

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val status: String = UserStatus.AUTHORIZED.name,
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String = ""
) {
    val userStatus: UserStatus
        get() = UserStatus.fromString(status)
}
