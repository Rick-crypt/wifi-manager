package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.FieldType

@Entity(tableName = "field_definitions")
data class FieldDefinitionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String,
    val label: String,
    val type: String, // FieldType.name
    val isRequired: Boolean = false,
    val isAuthKey: Boolean = false, // Utilisé pour identifier/authentifier l'accès Wi-Fi
    val isVisibleInPortal: Boolean = true, // Affiché sur le portail captif
    val displayOrder: Int = 0,
    val isEnabled: Boolean = true, // Activé ou temporairement désactivé
    val optionsJson: String = "[]", // Tableau JSON pour SELECT et MULTI_SELECT (ex: ["Gabonaise","Camerounaise"])
    val placeholder: String = "",
    val helperText: String = ""
) {
    val fieldType: FieldType
        get() = FieldType.fromString(type)

    fun getOptionsList(): List<String> {
        return try {
            val clean = optionsJson.trim()
            if (clean.startsWith("[") && clean.endsWith("]")) {
                val inner = clean.substring(1, clean.length - 1).trim()
                if (inner.isEmpty()) emptyList()
                else {
                    inner.split(",")
                        .map { it.trim().trim('"', '\'') }
                        .filter { it.isNotEmpty() }
                }
            } else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
