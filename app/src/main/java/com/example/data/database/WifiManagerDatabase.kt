package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.FieldDefinitionDao
import com.example.data.dao.GatewayAuditLogDao
import com.example.data.dao.GatewayConfigDao
import com.example.data.dao.PortalSettingsDao
import com.example.data.dao.UserDao
import com.example.data.dao.WifiSessionDao
import com.example.data.entity.FieldDefinitionEntity
import com.example.data.entity.GatewayAuditLogEntity
import com.example.data.entity.GatewayConfigEntity
import com.example.data.entity.PortalSettingsEntity
import com.example.data.entity.UserEntity
import com.example.data.entity.UserFieldValueEntity
import com.example.data.entity.WifiSessionEntity
import com.example.data.model.FieldType
import com.example.data.model.PortalStatus
import com.example.data.model.UserStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        FieldDefinitionEntity::class,
        UserEntity::class,
        UserFieldValueEntity::class,
        WifiSessionEntity::class,
        PortalSettingsEntity::class,
        GatewayConfigEntity::class,
        GatewayAuditLogEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class WifiManagerDatabase : RoomDatabase() {
    abstract fun fieldDefinitionDao(): FieldDefinitionDao
    abstract fun userDao(): UserDao
    abstract fun wifiSessionDao(): WifiSessionDao
    abstract fun portalSettingsDao(): PortalSettingsDao
    abstract fun gatewayConfigDao(): GatewayConfigDao
    abstract fun gatewayAuditLogDao(): GatewayAuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: WifiManagerDatabase? = null

        fun getInstance(context: Context): WifiManagerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WifiManagerDatabase::class.java,
                    "wifi_manager_database.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedDefaultData(database)
                    }
                }
            }

            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                super.onDestructiveMigration(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedDefaultData(database)
                    }
                }
            }
        }

        suspend fun seedDefaultData(database: WifiManagerDatabase) {
            val fieldDao = database.fieldDefinitionDao()
            val userDao = database.userDao()
            val sessionDao = database.wifiSessionDao()
            val settingsDao = database.portalSettingsDao()
            val gatewayDao = database.gatewayConfigDao()
            val auditLogDao = database.gatewayAuditLogDao()

            if (gatewayDao.getConfigDirect() == null) {
                gatewayDao.insertOrUpdate(
                    GatewayConfigEntity(
                        id = 1,
                        gatewayIp = "192.168.1.1",
                        gatewayApiUrl = "http://192.168.1.1:8080",
                        configVersion = 1,
                        deployedConfigVersion = 0,
                        portalStatus = PortalStatus.DISABLED.name,
                        hardwareType = "OPENWRT",
                        isBackendOnline = true,
                        isRouterOnline = true
                    )
                )
                auditLogDao.insertLog(
                    GatewayAuditLogEntity(
                        eventType = "INITIALIZATION",
                        description = "Module de gestion de passerelle autonome initialisé.",
                        configVersion = 1,
                        success = true
                    )
                )
            }

            if (fieldDao.getCount() == 0) {
                val defaultFields = listOf(
                    FieldDefinitionEntity(
                        id = 1,
                        key = "passport_number",
                        label = "Numéro de passeport",
                        type = FieldType.TEXT.name,
                        isRequired = true,
                        isAuthKey = true,
                        isVisibleInPortal = true,
                        displayOrder = 1,
                        isEnabled = true,
                        placeholder = "Ex: P1234567",
                        helperText = "Votre identifiant officiel de passeport"
                    ),
                    FieldDefinitionEntity(
                        id = 2,
                        key = "nationality",
                        label = "Nationalité",
                        type = FieldType.SELECT.name,
                        isRequired = true,
                        isAuthKey = true,
                        isVisibleInPortal = true,
                        displayOrder = 2,
                        isEnabled = true,
                        optionsJson = "[\"Gabonaise\", \"Camerounaise\", \"Congolaise\", \"Ivoirienne\", \"Sénégalaise\", \"Autre\"]",
                        helperText = "Votre nationalité déclarée"
                    ),
                    FieldDefinitionEntity(
                        id = 3,
                        key = "promotion",
                        label = "Promotion",
                        type = FieldType.SELECT.name,
                        isRequired = true,
                        isAuthKey = true,
                        isVisibleInPortal = true,
                        displayOrder = 3,
                        isEnabled = true,
                        optionsJson = "[\"2024\", \"2025\", \"2026\", \"2027\", \"2028\"]",
                        helperText = "Année de promotion universitaire"
                    ),
                    FieldDefinitionEntity(
                        id = 4,
                        key = "first_name",
                        label = "Prénom",
                        type = FieldType.TEXT.name,
                        isRequired = false,
                        isAuthKey = false,
                        isVisibleInPortal = true,
                        displayOrder = 4,
                        isEnabled = true,
                        placeholder = "Ex: Paul"
                    ),
                    FieldDefinitionEntity(
                        id = 5,
                        key = "last_name",
                        label = "Nom",
                        type = FieldType.TEXT.name,
                        isRequired = false,
                        isAuthKey = false,
                        isVisibleInPortal = true,
                        displayOrder = 5,
                        isEnabled = true,
                        placeholder = "Ex: Jean"
                    ),
                    FieldDefinitionEntity(
                        id = 6,
                        key = "room_number",
                        label = "Numéro de chambre",
                        type = FieldType.TEXT.name,
                        isRequired = false,
                        isAuthKey = false,
                        isVisibleInPortal = true,
                        displayOrder = 6,
                        isEnabled = true,
                        placeholder = "Ex: B204",
                        helperText = "Numéro du logement / chambre"
                    ),
                    FieldDefinitionEntity(
                        id = 7,
                        key = "phone",
                        label = "Téléphone",
                        type = FieldType.PHONE.name,
                        isRequired = false,
                        isAuthKey = false,
                        isVisibleInPortal = false,
                        displayOrder = 7,
                        isEnabled = true,
                        placeholder = "+241 07 00 00 00"
                    )
                )

                fieldDao.insertAll(defaultFields)

                // Add sample users matching the prompt scenario
                val user1Id = userDao.insertUser(
                    UserEntity(
                        id = 1,
                        status = UserStatus.AUTHORIZED.name,
                        createdAt = System.currentTimeMillis() - 86400000L * 5,
                        notes = "Étudiant Résidence B - Chambre 204"
                    )
                )
                userDao.insertFieldValues(
                    listOf(
                        UserFieldValueEntity(userId = user1Id, fieldId = 1, value = "P123456"),
                        UserFieldValueEntity(userId = user1Id, fieldId = 2, value = "Gabonaise"),
                        UserFieldValueEntity(userId = user1Id, fieldId = 3, value = "2026"),
                        UserFieldValueEntity(userId = user1Id, fieldId = 4, value = "Paul"),
                        UserFieldValueEntity(userId = user1Id, fieldId = 5, value = "Jean"),
                        UserFieldValueEntity(userId = user1Id, fieldId = 6, value = "B204"),
                        UserFieldValueEntity(userId = user1Id, fieldId = 7, value = "+241 07 45 67 89")
                    )
                )

                val user2Id = userDao.insertUser(
                    UserEntity(
                        id = 2,
                        status = UserStatus.AUTHORIZED.name,
                        createdAt = System.currentTimeMillis() - 86400000L * 2,
                        notes = "Déléguée Promo 2025"
                    )
                )
                userDao.insertFieldValues(
                    listOf(
                        UserFieldValueEntity(userId = user2Id, fieldId = 1, value = "C987654"),
                        UserFieldValueEntity(userId = user2Id, fieldId = 2, value = "Camerounaise"),
                        UserFieldValueEntity(userId = user2Id, fieldId = 3, value = "2025"),
                        UserFieldValueEntity(userId = user2Id, fieldId = 4, value = "Sarah"),
                        UserFieldValueEntity(userId = user2Id, fieldId = 5, value = "Moukandjo"),
                        UserFieldValueEntity(userId = user2Id, fieldId = 6, value = "A102"),
                        UserFieldValueEntity(userId = user2Id, fieldId = 7, value = "+237 69 12 34 56")
                    )
                )

                val user3Id = userDao.insertUser(
                    UserEntity(
                        id = 3,
                        status = UserStatus.SUSPENDED.name,
                        createdAt = System.currentTimeMillis() - 86400000L * 10,
                        notes = "Compte suspendu pour dépassement de quota"
                    )
                )
                userDao.insertFieldValues(
                    listOf(
                        UserFieldValueEntity(userId = user3Id, fieldId = 1, value = "K456789"),
                        UserFieldValueEntity(userId = user3Id, fieldId = 2, value = "Congolaise"),
                        UserFieldValueEntity(userId = user3Id, fieldId = 3, value = "2026"),
                        UserFieldValueEntity(userId = user3Id, fieldId = 4, value = "Kevin"),
                        UserFieldValueEntity(userId = user3Id, fieldId = 5, value = "Mbemba"),
                        UserFieldValueEntity(userId = user3Id, fieldId = 6, value = "C305")
                    )
                )

                // Seed portal settings
                settingsDao.insertOrUpdate(
                    PortalSettingsEntity(
                        id = 1,
                        isPortalActive = false,
                        requireAdminApproval = false,
                        directBoxPassword = "WiFiPass2026!",
                        networkSsid = "Campus-WiFi-Secure",
                        portalTitle = "WIFI MANAGER",
                        portalSubtitle = "Portail Captif d'Authentification",
                        welcomeMessage = "Veuillez renseigner vos identifiants pour activer votre accès Internet.",
                        guestModeEnabled = true,
                        guestDurationHours = 2,
                        sessionDurationHours = 24
                    )
                )

                // Seed an active session for User 1
                sessionDao.insertSession(
                    WifiSessionEntity(
                        id = 1,
                        userId = user1Id,
                        identifierSummary = "P123456 • Gabonaise • 2026",
                        deviceName = "iPhone 15 Pro de Paul",
                        deviceMac = "3C:22:FB:4A:91:02",
                        ipAddress = "192.168.10.45",
                        startedAt = System.currentTimeMillis() - 3600000L,
                        expiresAt = System.currentTimeMillis() + 82800000L,
                        isActive = true,
                        isGuest = false,
                        bytesUsedMb = 238.4
                    )
                )
            }
        }
    }
}
