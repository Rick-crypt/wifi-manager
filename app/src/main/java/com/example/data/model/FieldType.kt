package com.example.data.model

enum class FieldType(val label: String, val description: String) {
    TEXT("Texte", "Texte libre (ex: Nom, Ville, CNI)"),
    NUMBER("Nombre", "Valeur numérique (ex: Numéro de chambre)"),
    DATE("Date", "Format de date (ex: Date de naissance)"),
    PHONE("Téléphone", "Numéro d'appel (ex: +33 6...)"),
    SELECT("Liste déroulante (Choix unique)", "Sélection parmi des options prédéfinies"),
    MULTI_SELECT("Choix multiples", "Plusieurs options sélectionnables"),
    BOOLEAN("Oui / Non (Bascule)", "Interrupteur binaire (ex: CGU acceptées)"),
    PASSWORD("Mot de passe / Code PIN", "Saisie masquée (ex: Code secret)");

    companion object {
        fun fromString(value: String): FieldType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: TEXT
        }
    }
}

enum class UserStatus(val label: String) {
    AUTHORIZED("Autorisé"),
    SUSPENDED("Suspendu"),
    PENDING("En attente");

    companion object {
        fun fromString(value: String): UserStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: AUTHORIZED
        }
    }
}
