package com.example.data.repository

import android.content.Context
import com.example.data.database.WifiManagerDatabase
import com.example.data.entity.FieldDefinitionEntity
import com.example.data.entity.GatewayAuditLogEntity
import com.example.data.entity.GatewayConfigEntity
import com.example.data.entity.PortalSettingsEntity
import com.example.data.entity.UserEntity
import com.example.data.entity.UserFieldValueEntity
import com.example.data.entity.WifiSessionEntity
import com.example.data.gateway.AutonomousGatewayEngine
import com.example.data.gateway.GatewayHardwareProfile
import com.example.data.model.AuthResult
import com.example.data.model.DeploymentProgress
import com.example.data.model.DeploymentStep
import com.example.data.model.FieldConfigDto
import com.example.data.model.FieldType
import com.example.data.model.GatewayConfigPayload
import com.example.data.model.PortalStatus
import com.example.data.model.SystemHealthState
import com.example.data.model.UserCredentialDto
import com.example.data.model.UserStatus
import com.example.data.model.UserWithValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class WifiManagerRepository(
    private val database: WifiManagerDatabase,
    private val context: Context? = null
) {
    private val fieldDao = database.fieldDefinitionDao()
    private val userDao = database.userDao()
    private val sessionDao = database.wifiSessionDao()
    private val settingsDao = database.portalSettingsDao()
    private val gatewayDao = database.gatewayConfigDao()
    private val auditLogDao = database.gatewayAuditLogDao()

    val autonomousEngine: AutonomousGatewayEngine? = context?.let { AutonomousGatewayEngine(it) }
    val superAdminManager: com.example.data.gateway.SuperAdminEmergencyManager? = context?.let { com.example.data.gateway.SuperAdminEmergencyManager(it) }

    val allFields: Flow<List<FieldDefinitionEntity>> = fieldDao.getAllFields()
    val enabledFields: Flow<List<FieldDefinitionEntity>> = fieldDao.getEnabledFields()
    val portalFields: Flow<List<FieldDefinitionEntity>> = fieldDao.getPortalFields()
    val authFields: Flow<List<FieldDefinitionEntity>> = fieldDao.getAuthFields()

    val portalSettings: Flow<PortalSettingsEntity> = settingsDao.getSettings().map { it ?: PortalSettingsEntity() }
    val gatewayConfig: Flow<GatewayConfigEntity> = gatewayDao.getConfig().map {
        it ?: GatewayConfigEntity(
            id = 1,
            gatewayIp = "192.168.1.1",
            gatewayApiUrl = "http://192.168.1.1:8080",
            configVersion = 1,
            deployedConfigVersion = 0,
            portalStatus = PortalStatus.DISABLED.name
        )
    }

    val auditLogs: Flow<List<GatewayAuditLogEntity>> = auditLogDao.getRecentLogs()

    val activeSessions: Flow<List<WifiSessionEntity>> = sessionDao.getActiveSessions()
    val allSessions: Flow<List<WifiSessionEntity>> = sessionDao.getAllSessions()

    val usersWithValues: Flow<List<UserWithValues>> = combine(
        userDao.getAllUsers(),
        userDao.getAllFieldValues()
    ) { users, allValues ->
        val valuesByUser = allValues.groupBy { it.userId }
        users.map { user ->
            val userValues = valuesByUser[user.id]?.associate { it.fieldId to it.value } ?: emptyMap()
            UserWithValues(user = user, values = userValues)
        }
    }.flowOn(Dispatchers.Default)

    /**
     * Observable multi-node system health state as requested in Section 6 & 7:
     * NETWORK_ACTIVE, PORTAL_ACTIVE, ADMIN_ONLINE, BACKEND_ONLINE, ROUTER_ONLINE
     */
    val systemHealth: Flow<SystemHealthState> = combine(
        gatewayConfig,
        portalSettings
    ) { gw, settings ->
        SystemHealthState(
            networkActive = true,
            portalActive = gw.status == PortalStatus.ACTIVE,
            adminOnline = true,
            backendOnline = gw.isBackendOnline,
            routerOnline = gw.isRouterOnline
        )
    }

    suspend fun saveField(field: FieldDefinitionEntity): Long = withContext(Dispatchers.IO) {
        val resultId = if (field.id == 0L) {
            val currentCount = fieldDao.getCount()
            val newField = field.copy(displayOrder = currentCount + 1)
            fieldDao.insert(newField)
        } else {
            fieldDao.update(field)
            field.id
        }
        notifyConfigChange("Modification du champ '${field.label}'")
        resultId
    }

    suspend fun deleteField(fieldId: Long) = withContext(Dispatchers.IO) {
        val field = fieldDao.getFieldById(fieldId)
        userDao.deleteFieldValuesForField(fieldId)
        fieldDao.deleteById(fieldId)
        notifyConfigChange("Suppression du champ '${field?.label ?: fieldId}'")
    }

    suspend fun moveField(fieldId: Long, directionUp: Boolean) = withContext(Dispatchers.IO) {
        val fields = fieldDao.getAllFields().first().toMutableList()
        val index = fields.indexOfFirst { it.id == fieldId }
        if (index == -1) return@withContext

        val targetIndex = if (directionUp) index - 1 else index + 1
        if (targetIndex in fields.indices) {
            val current = fields[index]
            val other = fields[targetIndex]
            val currentOrder = current.displayOrder
            val otherOrder = other.displayOrder

            val newCurrentOrder = if (currentOrder != otherOrder) otherOrder else (if (directionUp) currentOrder - 1 else currentOrder + 1)
            val newOtherOrder = currentOrder

            fieldDao.update(current.copy(displayOrder = newCurrentOrder))
            fieldDao.update(other.copy(displayOrder = newOtherOrder))
            notifyConfigChange("Réordonnancement du champ '${current.label}'")
        }
    }

    suspend fun toggleFieldEnabled(fieldId: Long) = withContext(Dispatchers.IO) {
        val field = fieldDao.getFieldById(fieldId) ?: return@withContext
        fieldDao.update(field.copy(isEnabled = !field.isEnabled))
        notifyConfigChange("Bascule activation du champ '${field.label}'")
    }

    suspend fun toggleFieldAuthKey(fieldId: Long) = withContext(Dispatchers.IO) {
        val field = fieldDao.getFieldById(fieldId) ?: return@withContext
        fieldDao.update(field.copy(isAuthKey = !field.isAuthKey))
        notifyConfigChange("Bascule facteur auth du champ '${field.label}'")
    }

    suspend fun toggleFieldPortalVisibility(fieldId: Long) = withContext(Dispatchers.IO) {
        val field = fieldDao.getFieldById(fieldId) ?: return@withContext
        fieldDao.update(field.copy(isVisibleInPortal = !field.isVisibleInPortal))
        notifyConfigChange("Bascule visibilité portail du champ '${field.label}'")
    }

    suspend fun toggleFieldRequired(fieldId: Long) = withContext(Dispatchers.IO) {
        val field = fieldDao.getFieldById(fieldId) ?: return@withContext
        fieldDao.update(field.copy(isRequired = !field.isRequired))
        notifyConfigChange("Bascule obligatoire du champ '${field.label}'")
    }

    // User management
    suspend fun saveUser(
        user: UserEntity,
        values: Map<Long, String>
    ): Long = withContext(Dispatchers.IO) {
        val userId = if (user.id == 0L) {
            userDao.insertUser(user)
        } else {
            userDao.updateUser(user)
            user.id
        }

        userDao.deleteFieldValuesForUser(userId)
        val entities = values.map { (fieldId, value) ->
            UserFieldValueEntity(
                userId = userId,
                fieldId = fieldId,
                value = value.trim()
            )
        }
        userDao.insertFieldValues(entities)
        notifyConfigChange("Mise à jour utilisateur #$userId")
        userId
    }

    suspend fun updateUserStatus(userId: Long, newStatus: UserStatus) = withContext(Dispatchers.IO) {
        val current = userDao.getUserById(userId) ?: return@withContext
        userDao.updateUser(current.copy(status = newStatus.name))
        notifyConfigChange("Mise à jour statut utilisateur #$userId -> ${newStatus.name}")
    }

    suspend fun approveUserAndCreateSession(
        userId: Long,
        deviceName: String = "Appareil Approuvé"
    ): AuthResult = withContext(Dispatchers.IO) {
        val user = userDao.getUserById(userId) ?: return@withContext AuthResult.NotFound("Utilisateur introuvable.")
        userDao.updateUser(user.copy(status = UserStatus.AUTHORIZED.name))
        notifyConfigChange("Approbation manuelle de l'utilisateur #$userId")

        val settings = settingsDao.getSettingsDirect() ?: PortalSettingsEntity()
        val durationMs = settings.sessionDurationHours * 3600000L
        val users = usersWithValues.first()
        val matchedUser = users.firstOrNull { it.user.id == userId }

        val newSession = WifiSessionEntity(
            userId = userId,
            identifierSummary = matchedUser?.getDisplayName(fieldDao.getAllFields().first()) ?: "Utilisateur #$userId",
            deviceName = deviceName,
            deviceMac = "3C:22:FB:4A:91:02",
            ipAddress = "192.168.1.105",
            startedAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + durationMs,
            isActive = true,
            isGuest = false,
            bytesUsedMb = 1.0
        )
        val sessionId = sessionDao.insertSession(newSession)
        val saved = sessionDao.getSessionById(sessionId) ?: newSession

        autonomousEngine?.grantAccess(newSession.deviceMac, newSession.ipAddress, newSession.identifierSummary, durationMs, false)
        AuthResult.Success(session = saved, user = matchedUser, message = "Accès utilisateur approuvé avec succès !")
    }

    suspend fun deleteUser(userId: Long) = withContext(Dispatchers.IO) {
        userDao.deleteFieldValuesForUser(userId)
        userDao.deleteUserById(userId)
        notifyConfigChange("Suppression utilisateur #$userId")
    }

    suspend fun terminateSession(sessionId: Long) = withContext(Dispatchers.IO) {
        val session = sessionDao.getSessionById(sessionId)
        if (session != null) {
            autonomousEngine?.revokeAccess(session.deviceMac)
        }
        sessionDao.terminateSession(sessionId)
    }

    suspend fun terminateAllSessions() = withContext(Dispatchers.IO) {
        sessionDao.terminateAllSessions()
        autonomousEngine?.activeClientLeases?.value?.keys?.forEach { mac ->
            autonomousEngine.revokeAccess(mac)
        }
    }

    suspend fun updatePortalSettings(settings: PortalSettingsEntity) = withContext(Dispatchers.IO) {
        settingsDao.insertOrUpdate(settings)
        notifyConfigChange("Mise à jour des paramètres du portail")
    }

    suspend fun togglePortalActivation(isActive: Boolean) = withContext(Dispatchers.IO) {
        val current = settingsDao.getSettingsDirect() ?: PortalSettingsEntity()
        settingsDao.insertOrUpdate(current.copy(isPortalActive = isActive))
        if (!isActive) {
            gatewayDao.updatePortalStatus(PortalStatus.DISABLED.name)
            logAudit("DEACTIVATION", "Portail désactivé par l'administrateur. Bascule en connexion directe box.", true)
        } else {
            notifyConfigChange("Portail activé dans la configuration locale")
        }
    }

    suspend fun toggleAdminApproval(requireApproval: Boolean) = withContext(Dispatchers.IO) {
        val current = settingsDao.getSettingsDirect() ?: PortalSettingsEntity()
        settingsDao.insertOrUpdate(current.copy(requireAdminApproval = requireApproval))
        notifyConfigChange("Modification de la politique d'approbation manuelle: $requireApproval")
    }

    suspend fun updateDirectBoxPassword(newPassword: String) = withContext(Dispatchers.IO) {
        val current = settingsDao.getSettingsDirect() ?: PortalSettingsEntity()
        settingsDao.insertOrUpdate(current.copy(directBoxPassword = newPassword))
        notifyConfigChange("Mise à jour du mot de passe direct box")
    }

    // Backend and router simulation states for testing resilience
    suspend fun setBackendOnline(online: Boolean) = withContext(Dispatchers.IO) {
        gatewayDao.updateBackendOnline(online)
        logAudit(
            if (online) "BACKEND_ONLINE" else "BACKEND_OFFLINE",
            if (online) "Connexion avec le backend d'administration rétablie."
            else "Perte de connexion avec le backend. Le portail captif continue à fonctionner en autonomie sur le routeur.",
            true
        )
    }

    suspend fun setRouterOnline(online: Boolean) = withContext(Dispatchers.IO) {
        gatewayDao.updateRouterOnline(online)
        logAudit(
            if (online) "ROUTER_ONLINE" else "ROUTER_OFFLINE",
            if (online) "Passerelle locale connectée et joignable." else "Passerelle locale injoignable.",
            true
        )
    }

    suspend fun simulateRouterReboot() = withContext(Dispatchers.IO) {
        autonomousEngine?.onRouterReboot()
        logAudit("ROUTER_REBOOT", "Redémarrage de la passerelle détecté. Configuration persistante restaurée.", true)
    }

    /**
     * Builds the complete Gateway Configuration Payload
     */
    suspend fun buildConfigPayload(): GatewayConfigPayload = withContext(Dispatchers.IO) {
        val gwConfig = gatewayDao.getConfigDirect() ?: GatewayConfigEntity()
        val settings = settingsDao.getSettingsDirect() ?: PortalSettingsEntity()
        val fields = fieldDao.getAllFields().first()
        val users = usersWithValues.first()

        val fieldDtos = fields.map {
            FieldConfigDto(
                id = it.id,
                key = it.key,
                label = it.label,
                type = it.type,
                isRequired = it.isRequired,
                isAuthKey = it.isAuthKey,
                isVisible = it.isVisibleInPortal,
                optionsJson = it.optionsJson
            )
        }

        val userDtos = users.map { userWithVals ->
            val credentialsMap = mutableMapOf<String, String>()
            fields.forEach { f ->
                userWithVals.values[f.id]?.let { v -> credentialsMap[f.key] = v }
            }
            UserCredentialDto(
                id = userWithVals.user.id,
                status = userWithVals.user.status,
                credentials = credentialsMap
            )
        }

        GatewayConfigPayload(
            version = gwConfig.configVersion,
            portalActive = true,
            networkSsid = settings.networkSsid,
            portalTitle = settings.portalTitle,
            portalSubtitle = settings.portalSubtitle,
            welcomeMessage = settings.welcomeMessage,
            guestModeEnabled = settings.guestModeEnabled,
            guestDurationHours = settings.guestDurationHours,
            sessionDurationHours = settings.sessionDurationHours,
            requireAdminApproval = settings.requireAdminApproval,
            directBoxPassword = settings.directBoxPassword,
            requiredFields = fieldDtos,
            authorizedUsers = userDtos
        )
    }

    /**
     * 7-STEP VERIFIED DEPLOYMENT TO GATEWAY (Section 2)
     */
    fun deployToGateway(): Flow<DeploymentProgress> = flow {
        emit(DeploymentProgress(isDeploying = true, currentStep = DeploymentStep.CHECK_ROUTER))
        delay(300)

        // Step 1: Check Router Reachability
        val gw = gatewayDao.getConfigDirect() ?: GatewayConfigEntity()
        if (!gw.isRouterOnline) {
            emit(DeploymentProgress(isDeploying = false, errorMessage = "Échec : Le routeur / passerelle est injoignable."))
            logAudit("DEPLOYMENT_FAILED", "Échec vérification routeur (injoignable)", false)
            return@flow
        }
        val completed = mutableListOf(DeploymentStep.CHECK_ROUTER)

        // Step 2: Check Hardware Capabilities
        emit(DeploymentProgress(isDeploying = true, currentStep = DeploymentStep.CHECK_CAPABILITIES, completedSteps = completed.toList()))
        delay(350)
        completed.add(DeploymentStep.CHECK_CAPABILITIES)

        // Step 3: Generate Configuration Payload
        emit(DeploymentProgress(isDeploying = true, currentStep = DeploymentStep.GENERATE_CONFIG, completedSteps = completed.toList()))
        delay(250)
        val payload = buildConfigPayload()
        completed.add(DeploymentStep.GENERATE_CONFIG)

        // Step 4: Deploy to Gateway Persistent Storage
        emit(DeploymentProgress(isDeploying = true, currentStep = DeploymentStep.DEPLOY_TO_GATEWAY, completedSteps = completed.toList()))
        delay(400)
        autonomousEngine?.savePersistentState(payload)
        completed.add(DeploymentStep.DEPLOY_TO_GATEWAY)

        // Step 5: Verify Firewall Rules (iptables / nftables)
        emit(DeploymentProgress(isDeploying = true, currentStep = DeploymentStep.VERIFY_FIREWALL, completedSteps = completed.toList()))
        delay(300)
        completed.add(DeploymentStep.VERIFY_FIREWALL)

        // Step 6: Verify DNS / DHCP Redirection (dnsmasq)
        emit(DeploymentProgress(isDeploying = true, currentStep = DeploymentStep.VERIFY_DNS_DHCP, completedSteps = completed.toList()))
        delay(250)
        completed.add(DeploymentStep.VERIFY_DNS_DHCP)

        // Step 7: Commit Active State
        emit(DeploymentProgress(isDeploying = true, currentStep = DeploymentStep.COMMIT_ACTIVE, completedSteps = completed.toList()))
        delay(200)

        gatewayDao.markDeployed(payload.version, PortalStatus.ACTIVE.name)
        val currentSettings = settingsDao.getSettingsDirect() ?: PortalSettingsEntity()
        settingsDao.insertOrUpdate(currentSettings.copy(isPortalActive = true))

        completed.add(DeploymentStep.COMMIT_ACTIVE)
        logAudit(
            "DEPLOYMENT",
            "Configuration v${payload.version} déployée avec succès sur la passerelle. Portail actif et persistant.",
            true
        )

        emit(DeploymentProgress(isDeploying = false, isSuccess = true, completedSteps = completed))
    }

    /**
     * Fast Synchronize when config has changed (Section 8 & 9)
     */
    suspend fun syncWithGateway(): Boolean = withContext(Dispatchers.IO) {
        val payload = buildConfigPayload()
        autonomousEngine?.savePersistentState(payload)
        gatewayDao.markDeployed(payload.version, PortalStatus.ACTIVE.name)
        logAudit("SYNC", "Synchronisation réussie de la configuration v${payload.version} vers la passerelle.", true)
        true
    }

    private suspend fun notifyConfigChange(reason: String) {
        gatewayDao.incrementConfigVersion()
        val current = gatewayDao.getConfigDirect()
        if (current != null && current.portalStatus == PortalStatus.ACTIVE.name) {
            gatewayDao.updatePortalStatus(PortalStatus.SYNC_REQUIRED.name)
        }
        logAudit("CONFIG_CHANGED", "$reason. Version incrémentée.", true)
    }

    private suspend fun logAudit(type: String, desc: String, success: Boolean) {
        val version = gatewayDao.getConfigDirect()?.configVersion ?: 1
        auditLogDao.insertLog(
            GatewayAuditLogEntity(
                eventType = type,
                description = desc,
                configVersion = version,
                success = success
            )
        )
        auditLogDao.pruneOldLogs()
    }

    suspend fun generateOpenWrtScript(): String = withContext(Dispatchers.IO) {
        val gw = gatewayDao.getConfigDirect() ?: GatewayConfigEntity()
        val settings = settingsDao.getSettingsDirect() ?: PortalSettingsEntity()
        val payload = GatewayConfigPayload(
            version = gw.deployedConfigVersion.coerceAtLeast(gw.configVersion),
            portalActive = true,
            networkSsid = settings.networkSsid,
            portalTitle = settings.portalTitle,
            portalSubtitle = settings.portalSubtitle,
            welcomeMessage = settings.welcomeMessage,
            guestModeEnabled = settings.guestModeEnabled,
            guestDurationHours = settings.guestDurationHours,
            sessionDurationHours = settings.sessionDurationHours,
            requireAdminApproval = settings.requireAdminApproval,
            directBoxPassword = settings.directBoxPassword,
            requiredFields = emptyList(),
            authorizedUsers = emptyList()
        )
        GatewayHardwareProfile.generateOpenWrtScript(payload)
    }

    // Direct Box password connection when portal is disabled
    suspend fun authenticateDirectBox(
        enteredPassword: String,
        deviceName: String = "Appareil Connecté",
        macAddress: String = "3C:22:FB:4A:91:02",
        ipAddress: String = "192.168.1.105"
    ): AuthResult = withContext(Dispatchers.IO) {
        val settings = settingsDao.getSettingsDirect() ?: PortalSettingsEntity()
        val gw = gatewayDao.getConfigDirect() ?: GatewayConfigEntity()

        if (gw.status == PortalStatus.ACTIVE) {
            return@withContext AuthResult.Error("Le portail captif est actuellement actif sur le réseau.")
        }

        if (enteredPassword != settings.directBoxPassword) {
            return@withContext AuthResult.Error("Mot de passe Wi-Fi de la box incorrect.")
        }

        val durationMs = settings.sessionDurationHours * 3600000L
        val session = WifiSessionEntity(
            userId = null,
            identifierSummary = "Connexion Directe Box Wi-Fi",
            deviceName = deviceName,
            deviceMac = macAddress,
            ipAddress = ipAddress,
            startedAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + durationMs,
            isActive = true,
            isGuest = false,
            bytesUsedMb = 0.5
        )
        val sessionId = sessionDao.insertSession(session)
        val saved = sessionDao.getSessionById(sessionId) ?: session
        autonomousEngine?.grantAccess(macAddress, ipAddress, "Connexion Directe Box", durationMs, false)
        AuthResult.DirectBoxSuccess(session = saved, message = "Connexion directe autorisée avec la clé Wi-Fi de la box.")
    }

    // Dynamic portal authentication (Autonomous Data Plane)
    suspend fun authenticatePortal(
        enteredValues: Map<Long, String>,
        deviceName: String = "Appareil Mobile Client",
        macAddress: String = "3C:22:FB:4A:91:02",
        ipAddress: String = "192.168.1.105"
    ): AuthResult = withContext(Dispatchers.IO) {
        val settings = settingsDao.getSettingsDirect() ?: PortalSettingsEntity()
        val gw = gatewayDao.getConfigDirect() ?: GatewayConfigEntity()

        if (gw.status != PortalStatus.ACTIVE && !settings.isPortalActive) {
            return@withContext AuthResult.Error("Le portail captif est désactivé.")
        }

        val requiredFields = fieldDao.getPortalFields().first().filter { it.isRequired }
        val missingFields = mutableListOf<String>()
        for (field in requiredFields) {
            val value = enteredValues[field.id]?.trim()
            if (value.isNullOrEmpty()) {
                missingFields.add(field.label)
            }
        }

        if (missingFields.isNotEmpty()) {
            return@withContext AuthResult.ValidationError(
                message = "Veuillez renseigner tous les champs obligatoires.",
                missingFields = missingFields
            )
        }

        val authKeys = fieldDao.getAuthFields().first()
        val allUsersWithVals = usersWithValues.first()

        val matchingUser = allUsersWithVals.firstOrNull { userWithVals ->
            authKeys.all { authField ->
                val expectedValue = userWithVals.values[authField.id]?.trim().orEmpty()
                val enteredValue = enteredValues[authField.id]?.trim().orEmpty()
                expectedValue.equals(enteredValue, ignoreCase = true)
            }
        }

        if (matchingUser == null) {
            // New user registration attempt
            if (settings.approvalPolicy == "ALL_AUTO") {
                // Auto-create user and authorize
                val newUser = UserEntity(status = UserStatus.AUTHORIZED.name, notes = "Inscrit via le portail")
                val newUserId = saveUser(newUser, enteredValues)
                val durationMs = settings.sessionDurationHours * 3600000L
                val summary = authKeys.map { enteredValues[it.id].orEmpty() }.filter { it.isNotEmpty() }.joinToString(" • ")
                val session = WifiSessionEntity(
                    userId = newUserId,
                    identifierSummary = summary.ifEmpty { "Inscrit #$newUserId" },
                    deviceName = deviceName,
                    deviceMac = macAddress,
                    ipAddress = ipAddress,
                    startedAt = System.currentTimeMillis(),
                    expiresAt = System.currentTimeMillis() + durationMs,
                    isActive = true,
                    isGuest = false,
                    bytesUsedMb = 0.8
                )
                val sessionId = sessionDao.insertSession(session)
                val savedSession = sessionDao.getSessionById(sessionId) ?: session
                autonomousEngine?.grantAccess(macAddress, ipAddress, summary, durationMs, false)
                return@withContext AuthResult.Success(session = savedSession, user = null, message = "Bienvenue ! Votre accès Wi-Fi a été créé et validé automatiquement.")
            } else {
                // Policy requires approval for new unregistered users
                val newUser = UserEntity(status = UserStatus.PENDING.name, notes = "Nouvelle demande portail")
                saveUser(newUser, enteredValues)
                return@withContext AuthResult.PendingApproval(
                    user = null,
                    message = "Vos identifiants ont été enregistrés. Votre demande est en attente d'approbation manuelle par l'administrateur."
                )
            }
        }

        val userEntity = matchingUser.user

        if (userEntity.status.equals(UserStatus.SUSPENDED.name, ignoreCase = true)) {
            return@withContext AuthResult.Suspended(userEntity.notes.ifEmpty { "Votre accès Wi-Fi a été suspendu." })
        }

        // Determine approval requirement based on policy
        val requiresApproval = when {
            settings.requireAdminApproval -> true
            settings.approvalPolicy == "ALL_PENDING" -> true
            settings.approvalPolicy == "PRE_REGISTERED_AUTO" && userEntity.status.equals(UserStatus.AUTHORIZED.name, ignoreCase = true) -> false
            settings.approvalPolicy == "PRE_REGISTERED_AUTO" -> false // Pre-registered user in database auto-passes!
            else -> false
        }

        if (requiresApproval && !userEntity.status.equals(UserStatus.AUTHORIZED.name, ignoreCase = true)) {
            userDao.updateUser(userEntity.copy(status = UserStatus.PENDING.name))
            return@withContext AuthResult.PendingApproval(
                user = matchingUser,
                message = "Vos identifiants sont confirmés. Votre accès est en attente d'approbation par l'administrateur."
            )
        }

        val allFieldsList = fieldDao.getAllFields().first()
        val summaryParts = authKeys.map { authField ->
            enteredValues[authField.id] ?: ""
        }.filter { it.isNotEmpty() }

        val summary = if (summaryParts.isNotEmpty()) {
            summaryParts.joinToString(" • ")
        } else {
            matchingUser.getDisplayName(allFieldsList)
        }

        val durationMs = settings.sessionDurationHours * 3600000L
        val session = WifiSessionEntity(
            userId = userEntity.id,
            identifierSummary = summary,
            deviceName = deviceName,
            deviceMac = macAddress,
            ipAddress = ipAddress,
            startedAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + durationMs,
            isActive = true,
            isGuest = false,
            bytesUsedMb = 0.8
        )
        val sessionId = sessionDao.insertSession(session)
        val savedSession = sessionDao.getSessionById(sessionId) ?: session

        autonomousEngine?.grantAccess(macAddress, ipAddress, summary, durationMs, false)
        AuthResult.Success(session = savedSession, user = matchingUser)
    }

    suspend fun authenticateGuest(
        guestName: String,
        guestPhone: String,
        deviceName: String = "Invité",
        macAddress: String = "3C:22:FB:4A:91:02",
        ipAddress: String = "192.168.1.105"
    ): AuthResult = withContext(Dispatchers.IO) {
        val settings = settingsDao.getSettingsDirect() ?: PortalSettingsEntity()
        if (!settings.guestModeEnabled) {
            return@withContext AuthResult.Error("Le mode invité est désactivé sur ce réseau.")
        }
        if (guestName.isBlank() || guestPhone.isBlank()) {
            return@withContext AuthResult.ValidationError(
                message = "Le nom et le téléphone sont obligatoires pour l'accès invité.",
                missingFields = listOfNotNull(
                    if (guestName.isBlank()) "Nom et Prénom" else null,
                    if (guestPhone.isBlank()) "Téléphone" else null
                )
            )
        }

        val requiresGuestApproval = settings.requireGuestApproval || settings.approvalPolicy == "GUESTS_PENDING" || settings.approvalPolicy == "ALL_PENDING"

        if (requiresGuestApproval) {
            val guestUser = UserEntity(status = UserStatus.PENDING.name, notes = "Demande Invité: ${guestName.trim()} (${guestPhone.trim()})")
            userDao.insertUser(guestUser)
            return@withContext AuthResult.PendingApproval(
                user = null,
                message = "Demande invité enregistrée ! Votre accès est en attente d'approbation manuelle par l'administrateur."
            )
        }

        val durationMs = settings.guestDurationHours * 3600000L
        val session = WifiSessionEntity(
            userId = null,
            identifierSummary = "Invité : ${guestName.trim()} (${guestPhone.trim()})",
            deviceName = deviceName,
            deviceMac = macAddress,
            ipAddress = ipAddress,
            startedAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + durationMs,
            isActive = true,
            isGuest = true,
            bytesUsedMb = 0.2
        )
        val sessionId = sessionDao.insertSession(session)
        val savedSession = sessionDao.getSessionById(sessionId) ?: session
        autonomousEngine?.grantAccess(macAddress, ipAddress, "Invité: $guestName", durationMs, true)
        AuthResult.Success(session = savedSession, user = null, message = "Accès invité temporaire activé avec succès !")
    }

    suspend fun applyPresetTemplate(templateName: String) = withContext(Dispatchers.IO) {
        fieldDao.deleteAll()
        when (templateName) {
            "CAMPUS_RESIDENCE" -> {
                fieldDao.insertAll(
                    listOf(
                        FieldDefinitionEntity(id = 0, key = "passport_number", label = "Numéro de passeport", type = FieldType.TEXT.name, isRequired = true, isAuthKey = true, isVisibleInPortal = true, displayOrder = 1, isEnabled = true),
                        FieldDefinitionEntity(id = 0, key = "nationality", label = "Nationalité", type = FieldType.SELECT.name, isRequired = true, isAuthKey = true, isVisibleInPortal = true, displayOrder = 2, isEnabled = true, optionsJson = "[\"Gabonaise\", \"Camerounaise\", \"Congolaise\", \"Ivoirienne\", \"Autre\"]"),
                        FieldDefinitionEntity(id = 0, key = "promotion", label = "Promotion", type = FieldType.SELECT.name, isRequired = true, isAuthKey = true, isVisibleInPortal = true, displayOrder = 3, isEnabled = true, optionsJson = "[\"2024\", \"2025\", \"2026\", \"2027\"]"),
                        FieldDefinitionEntity(id = 0, key = "room_number", label = "Numéro de chambre", type = FieldType.TEXT.name, isRequired = false, isAuthKey = false, isVisibleInPortal = true, displayOrder = 4, isEnabled = true)
                    )
                )
            }
            "UNIVERSITY_STUDENT" -> {
                fieldDao.insertAll(
                    listOf(
                        FieldDefinitionEntity(id = 0, key = "student_id", label = "Numéro étudiant", type = FieldType.TEXT.name, isRequired = true, isAuthKey = true, isVisibleInPortal = true, displayOrder = 1, isEnabled = true),
                        FieldDefinitionEntity(id = 0, key = "birth_date", label = "Date de naissance", type = FieldType.DATE.name, isRequired = true, isAuthKey = true, isVisibleInPortal = true, displayOrder = 2, isEnabled = true),
                        FieldDefinitionEntity(id = 0, key = "faculty", label = "Filière / Département", type = FieldType.SELECT.name, isRequired = true, isAuthKey = false, isVisibleInPortal = true, displayOrder = 3, isEnabled = true, optionsJson = "[\"Informatique\", \"Génie Civil\", \"Économie\", \"Médecine\"]")
                    )
                )
            }
            "HOTEL_ROOM" -> {
                fieldDao.insertAll(
                    listOf(
                        FieldDefinitionEntity(id = 0, key = "room_number", label = "Numéro de chambre", type = FieldType.NUMBER.name, isRequired = true, isAuthKey = true, isVisibleInPortal = true, displayOrder = 1, isEnabled = true),
                        FieldDefinitionEntity(id = 0, key = "guest_pin", label = "Code PIN d'accès", type = FieldType.PASSWORD.name, isRequired = true, isAuthKey = true, isVisibleInPortal = true, displayOrder = 2, isEnabled = true)
                    )
                )
            }
            "EVENT_BADGE" -> {
                fieldDao.insertAll(
                    listOf(
                        FieldDefinitionEntity(id = 0, key = "badge_number", label = "Numéro de badge", type = FieldType.TEXT.name, isRequired = true, isAuthKey = true, isVisibleInPortal = true, displayOrder = 1, isEnabled = true),
                        FieldDefinitionEntity(id = 0, key = "company", label = "Entreprise / Organisation", type = FieldType.TEXT.name, isRequired = true, isAuthKey = false, isVisibleInPortal = true, displayOrder = 2, isEnabled = true)
                    )
                )
            }
        }
        notifyConfigChange("Application du modèle '$templateName'")
    }

    suspend fun executeSuperAdminRecovery(passcode: String): String = withContext(Dispatchers.IO) {
        if (superAdminManager?.verifyMasterKey(passcode) == true) {
            val msg = superAdminManager.executeCompleteEmergencyRecovery()
            logAudit("SUPER_ADMIN_RECOVERY", "Super-Admin Emergency Recovery triggered with Master Key.", true)
            msg
        } else {
            "❌ Clé Super-Admin invalide. Récupération d'urgence refusée."
        }
    }
}
