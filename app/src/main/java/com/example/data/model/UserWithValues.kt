package com.example.data.model

import com.example.data.entity.FieldDefinitionEntity
import com.example.data.entity.UserEntity
import com.example.data.entity.WifiSessionEntity

data class UserWithValues(
    val user: UserEntity,
    val values: Map<Long, String> // fieldId -> value
) {
    fun getDisplayName(fields: List<FieldDefinitionEntity>): String {
        val fnField = fields.firstOrNull { it.key.contains("prenom", true) || it.key.contains("first", true) }
        val lnField = fields.firstOrNull { it.key.contains("nom", true) && !it.key.contains("prenom", true) || it.key.contains("last", true) }
        val passField = fields.firstOrNull { it.key.contains("passport", true) || it.key.contains("passeport", true) }
        val studentField = fields.firstOrNull { it.key.contains("etudiant", true) || it.key.contains("student", true) }
        val roomField = fields.firstOrNull { it.key.contains("chambre", true) || it.key.contains("room", true) }

        val names = listOfNotNull(values[fnField?.id], values[lnField?.id]).filter { it.isNotBlank() }
        if (names.isNotEmpty()) return names.joinToString(" ")
        values[passField?.id]?.takeIf { it.isNotBlank() }?.let { return "Passeport: $it" }
        values[studentField?.id]?.takeIf { it.isNotBlank() }?.let { return "Étudiant: $it" }
        values[roomField?.id]?.takeIf { it.isNotBlank() }?.let { return "Chambre: $it" }
        return "Utilisateur #${user.id}"
    }

    fun getAuthSummary(authFields: List<FieldDefinitionEntity>): String {
        val parts = authFields.mapNotNull { field ->
            val v = values[field.id]
            if (!v.isNullOrBlank()) "${field.label}: $v" else null
        }
        return if (parts.isNotEmpty()) parts.joinToString(" | ") else "ID: #${user.id}"
    }

    fun getSummaryDisplay(fields: List<FieldDefinitionEntity>): String = getAuthSummary(fields)
}

sealed interface AuthResult {
    data class Success(
        val session: WifiSessionEntity? = null,
        val user: UserWithValues? = null,
        val message: String = "Accès Wi-Fi accordé !"
    ) : AuthResult

    data class DirectBoxSuccess(
        val session: WifiSessionEntity? = null,
        val message: String = "Portail désactivé : Connexion directe par mot de passe de la box réussie !"
    ) : AuthResult

    data class PendingApproval(
        val user: UserWithValues? = null,
        val message: String = "Demande envoyée ! En attente d'approbation par l'administrateur réseau."
    ) : AuthResult

    data class Suspended(
        val reason: String = "Accès refusé : Ce compte est actuellement suspendu."
    ) : AuthResult

    data class NotFound(
        val message: String = "Aucun utilisateur ne correspond à cette combinaison d'identifiants."
    ) : AuthResult

    data class ValidationError(
        val message: String,
        val missingFields: List<String> = emptyList()
    ) : AuthResult

    data class Error(
        val message: String
    ) : AuthResult
}
