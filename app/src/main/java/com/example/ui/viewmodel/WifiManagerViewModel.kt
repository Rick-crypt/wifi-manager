package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.WifiManagerDatabase
import com.example.data.entity.FieldDefinitionEntity
import com.example.data.entity.GatewayAuditLogEntity
import com.example.data.entity.GatewayConfigEntity
import com.example.data.entity.PortalSettingsEntity
import com.example.data.entity.UserEntity
import com.example.data.entity.WifiSessionEntity
import com.example.data.model.AuthResult
import com.example.data.model.DeploymentProgress
import com.example.data.model.PortalStatus
import com.example.data.model.SystemHealthState
import com.example.data.model.UserStatus
import com.example.data.model.UserWithValues
import com.example.data.repository.WifiManagerRepository
import com.example.data.wifi.WifiConnectionInfo
import com.example.data.wifi.WifiDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WifiManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val database = WifiManagerDatabase.getInstance(application)
    val repository = WifiManagerRepository(database, application)
    private val wifiDetector = WifiDetector(application)

    // Real Wi-Fi detection
    val wifiConnectionInfo: StateFlow<WifiConnectionInfo> = wifiDetector.observeWifiConnection()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            wifiDetector.getCurrentWifiInfo()
        )

    val allFields: StateFlow<List<FieldDefinitionEntity>> = repository.allFields
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val portalFields: StateFlow<List<FieldDefinitionEntity>> = repository.portalFields
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val authFields: StateFlow<List<FieldDefinitionEntity>> = repository.authFields
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val portalSettings: StateFlow<PortalSettingsEntity> = repository.portalSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PortalSettingsEntity())

    val gatewayConfig: StateFlow<GatewayConfigEntity> = repository.gatewayConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GatewayConfigEntity())

    val systemHealth: StateFlow<SystemHealthState> = repository.systemHealth
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SystemHealthState())

    val auditLogs: StateFlow<List<GatewayAuditLogEntity>> = repository.auditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSessions: StateFlow<List<WifiSessionEntity>> = repository.activeSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<WifiSessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Deployment Progress
    private val _deployProgress = MutableStateFlow<DeploymentProgress?>(null)
    val deployProgress: StateFlow<DeploymentProgress?> = _deployProgress.asStateFlow()

    private val _isDeploying = MutableStateFlow(false)
    val isDeploying: StateFlow<Boolean> = _isDeploying.asStateFlow()

    // Search and filters for users
    val searchQuery = MutableStateFlow("")
    val statusFilter = MutableStateFlow<UserStatus?>(null)

    val filteredUsers: StateFlow<List<UserWithValues>> = combine(
        repository.usersWithValues,
        searchQuery,
        statusFilter
    ) { users, query, filter ->
        users.filter { userWithValues ->
            val matchesFilter = filter == null || userWithValues.user.userStatus == filter
            val matchesQuery = query.isBlank() || userWithValues.values.values.any {
                it.contains(query, ignoreCase = true)
            } || userWithValues.user.notes.contains(query, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Users waiting for admin manual approval
    val pendingUsers: StateFlow<List<UserWithValues>> = repository.usersWithValues.combine(searchQuery) { users, _ ->
        users.filter { it.user.userStatus == UserStatus.PENDING }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamic portal inputs: fieldId -> value
    private val _portalValues = MutableStateFlow<Map<Long, String>>(emptyMap())
    val portalValues: StateFlow<Map<Long, String>> = _portalValues.asStateFlow()

    // Direct box password input when portal is disabled
    val directBoxPasswordInput = MutableStateFlow("")

    private val _isGuestModeActive = MutableStateFlow(false)
    val isGuestModeActive: StateFlow<Boolean> = _isGuestModeActive.asStateFlow()

    val guestName = MutableStateFlow("")
    val guestPhone = MutableStateFlow("")

    private val _authResult = MutableStateFlow<AuthResult?>(null)
    val authResult: StateFlow<AuthResult?> = _authResult.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _currentClientSession = MutableStateFlow<WifiSessionEntity?>(null)
    val currentClientSession: StateFlow<WifiSessionEntity?> = _currentClientSession.asStateFlow()

    private val _openWrtScript = MutableStateFlow("")
    val openWrtScript: StateFlow<String> = _openWrtScript.asStateFlow()

    // Server State
    val serverStatus = com.example.server.PortalServerService.serverInstance?.serverStatus
        ?: MutableStateFlow(com.example.server.EmbeddedPortalServer.ServerStatus()).asStateFlow()

    val serverLogs = com.example.server.PortalServerService.serverInstance?.serverLogs
        ?: MutableStateFlow<List<String>>(emptyList()).asStateFlow()

    init {
        viewModelScope.launch {
            WifiManagerDatabase.seedDefaultData(database)
            _openWrtScript.value = repository.generateOpenWrtScript()
            // Auto start embedded HTTP server if portal is set to active
            val currentSettings = repository.portalSettings.first()
            if (currentSettings.isPortalActive) {
                val ip = wifiDetector.getCurrentWifiInfo().ipAddress.ifEmpty { "192.168.1.105" }
                com.example.server.PortalServerService.startService(application, ip)
            }
        }
    }

    fun refreshOpenWrtScript() {
        viewModelScope.launch {
            _openWrtScript.value = repository.generateOpenWrtScript()
        }
    }

    fun getOpenWrtScript(): String {
        if (_openWrtScript.value.isBlank()) {
            refreshOpenWrtScript()
        }
        return _openWrtScript.value
    }

    fun refreshWifiInfo() {
        val latest = wifiDetector.getCurrentWifiInfo()
        if (latest.isConnected && latest.ssid.isNotEmpty() && latest.ssid != "Wi-Fi Connecté") {
            viewModelScope.launch {
                val current = repository.portalSettings.stateIn(viewModelScope).value
                if (current.networkSsid != latest.ssid) {
                    repository.updatePortalSettings(current.copy(networkSsid = latest.ssid))
                }
            }
        }
    }

    fun deployToGateway() {
        if (_isDeploying.value) return
        _isDeploying.value = true
        viewModelScope.launch {
            repository.deployToGateway().collect { progress ->
                _deployProgress.value = progress
                if (!progress.isDeploying) {
                    _isDeploying.value = false
                    if (progress.isSuccess) {
                        val ip = wifiDetector.getCurrentWifiInfo().ipAddress.ifEmpty { "192.168.1.105" }
                        com.example.server.PortalServerService.startService(getApplication(), ip)
                    }
                }
            }
        }
    }

    fun dismissDeployProgress() {
        _deployProgress.value = null
    }

    fun syncWithGateway() {
        viewModelScope.launch {
            repository.syncWithGateway()
            val ip = wifiDetector.getCurrentWifiInfo().ipAddress.ifEmpty { "192.168.1.105" }
            com.example.server.PortalServerService.startService(getApplication(), ip)
        }
    }

    fun setPortalActive(active: Boolean) {
        if (active) {
            deployToGateway()
        } else {
            viewModelScope.launch {
                repository.togglePortalActivation(false)
                com.example.server.PortalServerService.stopService(getApplication())
            }
        }
    }

    fun executeEmergencyRecovery(passcode: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.executeSuperAdminRecovery(passcode)
            com.example.server.PortalServerService.stopService(getApplication())
            onResult(result)
        }
    }

    fun setRequireAdminApproval(requireApproval: Boolean) {
        viewModelScope.launch {
            repository.toggleAdminApproval(requireApproval)
        }
    }

    fun setDirectBoxPassword(password: String) {
        viewModelScope.launch {
            repository.updateDirectBoxPassword(password)
        }
    }

    fun setBackendOnline(online: Boolean) {
        viewModelScope.launch {
            repository.setBackendOnline(online)
        }
    }

    fun setRouterOnline(online: Boolean) {
        viewModelScope.launch {
            repository.setRouterOnline(online)
        }
    }

    fun simulateRouterReboot() {
        viewModelScope.launch {
            repository.simulateRouterReboot()
        }
    }

    fun updatePortalValue(fieldId: Long, value: String) {
        val current = _portalValues.value.toMutableMap()
        current[fieldId] = value
        _portalValues.value = current
    }

    fun toggleGuestMode(active: Boolean) {
        _isGuestModeActive.value = active
        _authResult.value = null
    }

    fun authenticatePortal() {
        viewModelScope.launch {
            _isConnecting.value = true
            val result = if (_isGuestModeActive.value) {
                repository.authenticateGuest(guestName.value, guestPhone.value)
            } else {
                repository.authenticatePortal(_portalValues.value)
            }
            _authResult.value = result
            _isConnecting.value = false
            if (result is AuthResult.Success) {
                _currentClientSession.value = result.session
            }
        }
    }

    fun authenticateDirectBox() {
        viewModelScope.launch {
            _isConnecting.value = true
            val result = repository.authenticateDirectBox(directBoxPasswordInput.value.trim())
            _authResult.value = result
            _isConnecting.value = false
            if (result is AuthResult.DirectBoxSuccess) {
                _currentClientSession.value = result.session
            }
        }
    }

    fun approveUser(userId: Long) {
        viewModelScope.launch {
            val result = repository.approveUserAndCreateSession(userId)
            if (result is AuthResult.Success) {
                _authResult.value = result
            }
        }
    }

    fun disconnectClientSession() {
        val session = _currentClientSession.value
        if (session != null) {
            viewModelScope.launch {
                repository.terminateSession(session.id)
                _currentClientSession.value = null
                _authResult.value = null
            }
        } else {
            _currentClientSession.value = null
            _authResult.value = null
        }
    }

    fun dismissAuthResult() {
        _authResult.value = null
    }

    // Field Definition Management
    fun saveField(field: FieldDefinitionEntity) {
        viewModelScope.launch {
            repository.saveField(field)
        }
    }

    fun deleteField(fieldId: Long) {
        viewModelScope.launch {
            repository.deleteField(fieldId)
        }
    }

    fun moveField(fieldId: Long, directionUp: Boolean) {
        viewModelScope.launch {
            repository.moveField(fieldId, directionUp)
        }
    }

    fun toggleFieldEnabled(fieldId: Long) {
        viewModelScope.launch {
            repository.toggleFieldEnabled(fieldId)
        }
    }

    fun toggleFieldAuthKey(fieldId: Long) {
        viewModelScope.launch {
            repository.toggleFieldAuthKey(fieldId)
        }
    }

    fun toggleFieldPortalVisible(fieldId: Long) {
        viewModelScope.launch {
            repository.toggleFieldPortalVisibility(fieldId)
        }
    }

    fun toggleFieldRequired(fieldId: Long) {
        viewModelScope.launch {
            repository.toggleFieldRequired(fieldId)
        }
    }

    // User Management
    fun saveUser(
        userId: Long = 0L,
        status: UserStatus = UserStatus.AUTHORIZED,
        notes: String = "",
        fieldValues: Map<Long, String>
    ) {
        viewModelScope.launch {
            val user = UserEntity(
                id = userId,
                status = status.name,
                notes = notes,
                createdAt = System.currentTimeMillis()
            )
            repository.saveUser(user, fieldValues)
        }
    }

    fun deleteUser(userId: Long) {
        viewModelScope.launch {
            repository.deleteUser(userId)
        }
    }

    fun updateUserStatus(userId: Long, newStatus: UserStatus) {
        viewModelScope.launch {
            repository.updateUserStatus(userId, newStatus)
        }
    }

    // Session Management
    fun terminateSession(sessionId: Long) {
        viewModelScope.launch {
            repository.terminateSession(sessionId)
            if (_currentClientSession.value?.id == sessionId) {
                _currentClientSession.value = null
            }
        }
    }

    fun terminateAllSessions() {
        viewModelScope.launch {
            repository.terminateAllSessions()
            _currentClientSession.value = null
        }
    }

    // Portal Settings
    fun updatePortalSettings(settings: PortalSettingsEntity) {
        viewModelScope.launch {
            repository.updatePortalSettings(settings)
        }
    }

    fun applyPresetTemplate(templateId: String) {
        viewModelScope.launch {
            repository.applyPresetTemplate(templateId)
            _portalValues.value = emptyMap()
            _authResult.value = null
        }
    }
}
