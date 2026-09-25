package com.example.server

import android.content.Context
import android.util.Log
import com.example.data.database.WifiManagerDatabase
import com.example.data.entity.FieldDefinitionEntity
import com.example.data.entity.PortalSettingsEntity
import com.example.data.entity.UserEntity
import com.example.data.entity.UserFieldValueEntity
import com.example.data.entity.WifiSessionEntity
import com.example.data.model.UserStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Concrete Native Embedded HTTP Server.
 * Runs on Android device on port 8080 (or 80/8088).
 * Serves real Captive Portal HTML/CSS web page to any connected device or browser.
 * Handles real HTTP requests, POST submissions, and Super-Admin emergency recovery!
 */
class EmbeddedPortalServer(
    private val context: Context,
    val port: Int = 8080
) {
    private val database = WifiManagerDatabase.getInstance(context)
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _serverStatus = MutableStateFlow(ServerStatus())
    val serverStatus = _serverStatus.asStateFlow()

    private val _serverLogs = MutableStateFlow<List<String>>(emptyList())
    val serverLogs = _serverLogs.asStateFlow()

    data class ServerStatus(
        val isRunning: Boolean = false,
        val port: Int = 8080,
        val localIp: String = "127.0.0.1",
        val totalRequests: Long = 0,
        val activeConnections: Int = 0,
        val lastRequestTime: Long = 0
    )

    fun start(localIp: String = "192.168.1.105") {
        if (isRunning) return
        isRunning = true

        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                logEvent("🟢 Serveur Web du Portail Captif démarré sur http://$localIp:$port")
                _serverStatus.value = ServerStatus(
                    isRunning = true,
                    port = port,
                    localIp = localIp
                )

                while (isRunning && serverSocket?.isClosed == false) {
                    try {
                        val socket = serverSocket!!.accept()
                        scope.launch {
                            handleClientSocket(socket, localIp)
                        }
                    } catch (e: Exception) {
                        if (isRunning) {
                            Log.e("EmbeddedPortalServer", "Socket accept error", e)
                        }
                    }
                }
            } catch (e: Exception) {
                logEvent("🔴 Erreur démarrage serveur HTTP: ${e.localizedMessage}")
                isRunning = false
                _serverStatus.value = _serverStatus.value.copy(isRunning = false)
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
            serverJob?.cancel()
            logEvent("⚪ Serveur Web du Portail Captif arrêté")
        } catch (e: Exception) {
            Log.e("EmbeddedPortalServer", "Error stopping server", e)
        }
        _serverStatus.value = _serverStatus.value.copy(isRunning = false)
    }

    private suspend fun handleClientSocket(socket: Socket, localIp: String) {
        val clientIp = socket.inetAddress.hostAddress ?: "inconnu"
        _serverStatus.value = _serverStatus.value.copy(
            totalRequests = _serverStatus.value.totalRequests + 1,
            lastRequestTime = System.currentTimeMillis()
        )

        try {
            socket.soTimeout = 5000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)

            val requestLine = reader.readLine() ?: return
            logEvent("📥 REQ from $clientIp: $requestLine")

            val parts = requestLine.split(" ")
            if (parts.size < 2) return
            val method = parts[0].uppercase()
            val fullPath = parts[1]

            // Read headers
            var contentLength = 0
            var headerLine = reader.readLine()
            while (!headerLine.isNullOrEmpty()) {
                if (headerLine.lowercase().startsWith("content-length:")) {
                    contentLength = headerLine.substring(15).trim().toIntOrNull() ?: 0
                }
                headerLine = reader.readLine()
            }

            // Read POST body if present
            var body = ""
            if (method == "POST" && contentLength > 0) {
                val charArray = CharArray(contentLength)
                var read = 0
                while (read < contentLength) {
                    val r = reader.read(charArray, read, contentLength - read)
                    if (r == -1) break
                    read += r
                }
                body = String(charArray, 0, read)
            }

            val path = fullPath.substringBefore("?")
            val queryString = if (fullPath.contains("?")) fullPath.substringAfter("?") else ""

            when {
                // Emergency Super-Admin Reset
                path == "/emergency_reset" -> {
                    val params = parseQuery(queryString)
                    val key = params["key"] ?: ""
                    if (key == "SUPERADMIN_2026_MASTER" || key == "ADMIN_EMERGENCY_RESET") {
                        executeEmergencyReset()
                        sendHtmlResponse(writer, 200, buildEmergencySuccessHtml())
                        logEvent("🚨 RÉCUPÉRATION SUPER-ADMIN EXÉCUTÉE avec succès depuis $clientIp !")
                    } else {
                        sendHtmlResponse(writer, 403, buildEmergencyForbiddenHtml())
                        logEvent("⚠️ tentative de reset super-admin échouée (clé invalide) depuis $clientIp")
                    }
                }

                // API Status JSON
                path == "/api/status" -> {
                    val settings = withContext(Dispatchers.IO) { database.portalSettingsDao().getSettingsDirect() } ?: PortalSettingsEntity()
                    val json = """
                        {
                            "server_online": true,
                            "portal_active": ${settings.isPortalActive},
                            "network_ssid": "${settings.networkSsid}",
                            "portal_title": "${settings.portalTitle}",
                            "local_ip": "$localIp",
                            "port": $port
                        }
                    """.trimIndent()
                    sendJsonResponse(writer, 200, json)
                }

                // Live polling endpoint for clients waiting in PENDING state
                path == "/api/session_status" -> {
                    val activeSession: WifiSessionEntity? = withContext(Dispatchers.IO) {
                        database.wifiSessionDao().getActiveSessionByIpDirect(clientIp)
                    }
                    val status = when {
                        activeSession != null && activeSession.isActive -> "AUTHORIZED"
                        else -> {
                            val users: List<UserEntity> = withContext(Dispatchers.IO) { database.userDao().getAllUsersDirect() }
                            // find user matching this clientIp or pending
                            val pendingUser = users.firstOrNull { it.status.equals("PENDING", ignoreCase = true) }
                            if (pendingUser != null) "PENDING" else "UNAUTHORIZED"
                        }
                    }
                    val json = """{"status": "$status", "client_ip": "$clientIp"}"""
                    sendJsonResponse(writer, 200, json)
                }

                // Authentication POST from captive browser
                method == "POST" && path == "/api/authenticate" -> {
                    val formData = parseQuery(body)
                    val authResponseHtml = processWebAuthentication(formData, clientIp)
                    sendHtmlResponse(writer, 200, authResponseHtml)
                }

                // OS Captive Portal Probes (Android generate_204, iOS hotspot-detect, Windows msftconnecttest)
                path.contains("generate_204") || path.contains("gen_204") || path.contains("hotspot-detect") ||
                path.contains("connecttest") || path.contains("noredirect") || path.contains("check_network_status") -> {
                    val activeSession: WifiSessionEntity? = withContext(Dispatchers.IO) { database.wifiSessionDao().getActiveSessionByIpDirect(clientIp) }
                    if (activeSession != null && activeSession.isActive) {
                        // User ALREADY authorized -> return probe success
                        when {
                            path.contains("generate_204") || path.contains("gen_204") -> sendEmpty204Response(writer)
                            path.contains("connecttest") -> sendTextResponse(writer, 200, "Microsoft Connect Test")
                            else -> sendHtmlResponse(writer, 200, "<HTML><HEAD><TITLE>Success</TITLE></HEAD><BODY>Success</BODY></HTML>")
                        }
                    } else {
                        // User NOT authorized -> Serve Captive Portal Page immediately
                        val settings = withContext(Dispatchers.IO) { database.portalSettingsDao().getSettingsDirect() } ?: PortalSettingsEntity()
                        val fields = withContext(Dispatchers.IO) { database.fieldDefinitionDao().getPortalFieldsDirect() }
                        val html = buildCaptivePortalPageHtml(settings, fields, localIp, port)
                        sendHtmlResponse(writer, 200, html)
                    }
                }

                // Catch-all: Captive portal default page for any URL / page
                else -> {
                    val settings = withContext(Dispatchers.IO) { database.portalSettingsDao().getSettingsDirect() } ?: PortalSettingsEntity()
                    val fields = withContext(Dispatchers.IO) { database.fieldDefinitionDao().getPortalFieldsDirect() }
                    val html = buildCaptivePortalPageHtml(settings, fields, localIp, port)
                    sendHtmlResponse(writer, 200, html)
                }
            }

            writer.flush()
            socket.close()
        } catch (e: Exception) {
            Log.e("EmbeddedPortalServer", "Client handling error", e)
        }
    }

    private suspend fun processWebAuthentication(params: Map<String, String>, clientIp: String): String = withContext(Dispatchers.IO) {
        val settings = database.portalSettingsDao().getSettingsDirect() ?: PortalSettingsEntity()
        val fields: List<FieldDefinitionEntity> = database.fieldDefinitionDao().getPortalFieldsDirect()
        val authFields = fields.filter { it.isAuthKey }
        val requiredFields = fields.filter { it.isRequired }

        // Check required fields
        val missing = requiredFields.filter { f -> params[f.key].isNullOrBlank() }
        if (missing.isNotEmpty()) {
            return@withContext buildResultPageHtml(
                title = "Champs Requis Manquants",
                message = "Veuillez renseigner les champs obligatoires: ${missing.joinToString(", ") { it.label }}",
                isSuccess = false
            )
        }

        // Match user credentials
        val allUsers: List<UserEntity> = database.userDao().getAllUsersDirect()
        val allValues = database.userDao().getAllFieldValuesDirect()
        val valuesByUser = allValues.groupBy { it.userId }

        val matchedUser = allUsers.firstOrNull { user ->
            val uVals = valuesByUser[user.id]?.associate { it.fieldId to it.value } ?: emptyMap()
            authFields.all { field ->
                val expected = uVals[field.id].orEmpty().trim()
                val entered = params[field.key].orEmpty().trim()
                expected.equals(entered, ignoreCase = true)
            }
        }

        val policy = settings.approvalPolicy.uppercase(Locale.ROOT)

        when {
            // POLICY: ALL_AUTO -> Auto-authorize everyone immediately
            policy == "ALL_AUTO" -> {
                val userToAuthorize = matchedUser ?: createNewUserFromParams(params, authFields, UserStatus.AUTHORIZED)
                if (userToAuthorize.status.equals(UserStatus.SUSPENDED.name, ignoreCase = true)) {
                    logEvent("⛔ Connexion refusée (Compte suspendu) pour $clientIp")
                    return@withContext buildResultPageHtml("Accès Suspendu", "Votre compte a été suspendu par l'administrateur.", false)
                }
                createSessionAndAuthorize(userToAuthorize.id, params, authFields, clientIp, settings)
            }

            // POLICY: ALL_PENDING -> Everyone must be approved manually by admin
            policy == "ALL_PENDING" -> {
                val userToQueue = matchedUser ?: createNewUserFromParams(params, authFields, UserStatus.PENDING)
                database.userDao().updateUser(userToQueue.copy(status = UserStatus.PENDING.name))
                logEvent("⏳ Demande mise en attente d'approbation manuelle pour $clientIp (${userToQueue.id})")
                buildResultPageHtml(
                    title = "En Attente d'Approbation",
                    message = "Vos identifiants ont bien été reçus. L'administrateur du réseau doit approuver votre demande pour débloquer votre accès Internet.",
                    isSuccess = true
                )
            }

            // POLICY: GUESTS_PENDING -> Pre-registered auto, non-registered placed in pending
            policy == "GUESTS_PENDING" -> {
                if (matchedUser != null) {
                    if (matchedUser.status.equals(UserStatus.SUSPENDED.name, ignoreCase = true)) {
                        return@withContext buildResultPageHtml("Accès Suspendu", "Votre compte est suspendu.", false)
                    }
                    if (matchedUser.status.equals(UserStatus.AUTHORIZED.name, ignoreCase = true)) {
                        createSessionAndAuthorize(matchedUser.id, params, authFields, clientIp, settings)
                    } else {
                        logEvent("⏳ Utilisateur #${matchedUser.id} en attente pour $clientIp")
                        buildResultPageHtml("En Attente d'Approbation", "Votre compte est en cours de validation par l'administrateur.", true)
                    }
                } else {
                    val newUser = createNewUserFromParams(params, authFields, UserStatus.PENDING)
                    logEvent("⏳ Nouvel invité mis en attente pour $clientIp (Utilisateur #${newUser.id})")
                    buildResultPageHtml("En Attente d'Approbation", "Vos identifiants d'invité ont été soumis. Veuillez attendre l'approbation de l'administrateur.", true)
                }
            }

            // DEFAULT POLICY: PRE_REGISTERED_AUTO
            else -> {
                if (matchedUser == null) {
                    if (settings.requireAdminApproval) {
                        val newUser = createNewUserFromParams(params, authFields, UserStatus.PENDING)
                        logEvent("⏳ Utilisateur non pré-enregistré mis en attente d'approbation pour $clientIp")
                        return@withContext buildResultPageHtml("En Attente d'Approbation", "Vos identifiants ont été enregistrés et transmis à l'administrateur pour approbation.", true)
                    } else {
                        logEvent("❌ Connexion refusée pour $clientIp: Identifiants non reconnus")
                        return@withContext buildResultPageHtml(
                            title = "Identifiants Inconnus",
                            message = "Vos identifiants ne figurent pas dans la liste des accès autorisés par l'établissement.",
                            isSuccess = false
                        )
                    }
                }

                if (matchedUser.status.equals(UserStatus.SUSPENDED.name, ignoreCase = true)) {
                    logEvent("⛔ Accès suspendu pour $clientIp (Utilisateur #${matchedUser.id})")
                    return@withContext buildResultPageHtml(
                        title = "Accès Suspendu",
                        message = "Votre accès a été temporairement suspendu par l'administrateur.",
                        isSuccess = false
                    )
                }

                if (settings.requireAdminApproval && !matchedUser.status.equals(UserStatus.AUTHORIZED.name, ignoreCase = true)) {
                    database.userDao().updateUser(matchedUser.copy(status = UserStatus.PENDING.name))
                    logEvent("⏳ Demande d'approbation enregistrée pour $clientIp")
                    return@withContext buildResultPageHtml(
                        title = "En Attente d'Approbation",
                        message = "Vos identifiants sont validés ! Votre accès à Internet est en cours de validation par l'administrateur réseau.",
                        isSuccess = true
                    )
                }

                createSessionAndAuthorize(matchedUser.id, params, authFields, clientIp, settings)
            }
        }
    }

    private suspend fun createNewUserFromParams(
        params: Map<String, String>,
        authFields: List<FieldDefinitionEntity>,
        status: UserStatus
    ): UserEntity {
        val newUserId = database.userDao().insertUser(
            UserEntity(
                status = status.name,
                createdAt = System.currentTimeMillis(),
                notes = "Inscrit via le portail captif web"
            )
        )
        val fieldValues = authFields.mapNotNull { f ->
            val v = params[f.key].orEmpty().trim()
            if (v.isNotBlank()) UserFieldValueEntity(userId = newUserId, fieldId = f.id, value = v) else null
        }
        if (fieldValues.isNotEmpty()) {
            database.userDao().insertFieldValues(fieldValues)
        }
        return database.userDao().getUserById(newUserId) ?: UserEntity(id = newUserId, status = status.name)
    }

    private suspend fun createSessionAndAuthorize(
        userId: Long,
        params: Map<String, String>,
        authFields: List<FieldDefinitionEntity>,
        clientIp: String,
        settings: PortalSettingsEntity
    ): String {
        val durationMs = settings.sessionDurationHours * 3600000L
        val summary = authFields.mapNotNull { params[it.key] }.ifEmpty { listOf(clientIp) }.joinToString(" • ")
        val session = WifiSessionEntity(
            userId = userId,
            identifierSummary = summary,
            deviceName = "Appareil Navigateur ($clientIp)",
            deviceMac = "XX:XX:XX:XX:XX:XX",
            ipAddress = clientIp,
            startedAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + durationMs,
            isActive = true,
            isGuest = false,
            bytesUsedMb = 0.5
        )
        database.wifiSessionDao().insertSession(session)
        logEvent("✅ ACCÈS AUTORISÉ pour $clientIp ($summary)")

        return buildResultPageHtml(
            title = "Connexion Réussie !",
            message = "Bienvenue ! Votre appareil est désormais connecté au réseau Wi-Fi avec accès à Internet.",
            isSuccess = true
        )
    }

    private suspend fun executeEmergencyReset() {
        val settingsDao = database.portalSettingsDao()
        val sessionDao = database.wifiSessionDao()
        val current = settingsDao.getSettingsDirect() ?: PortalSettingsEntity()

        // Turn off portal restrictions & clear sessions
        settingsDao.insertOrUpdate(current.copy(isPortalActive = false, requireAdminApproval = false))
        sessionDao.terminateAllSessions()
    }

    private fun parseQuery(query: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (query.isBlank()) return map
        val pairs = query.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            if (idx > 0) {
                val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                val value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                map[key] = value
            }
        }
        return map
    }

    private fun sendHtmlResponse(writer: PrintWriter, status: Int, html: String) {
        val bytes = html.toByteArray(Charsets.UTF_8)
        writer.print("HTTP/1.1 $status OK\r\n")
        writer.print("Content-Type: text/html; charset=UTF-8\r\n")
        writer.print("Content-Length: ${bytes.size}\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.print(html)
        writer.flush()
    }

    private fun sendJsonResponse(writer: PrintWriter, status: Int, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        writer.print("HTTP/1.1 $status OK\r\n")
        writer.print("Content-Type: application/json; charset=UTF-8\r\n")
        writer.print("Content-Length: ${bytes.size}\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.print(json)
        writer.flush()
    }

    private fun sendEmpty204Response(writer: PrintWriter) {
        writer.print("HTTP/1.1 204 No Content\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.flush()
    }

    private fun sendTextResponse(writer: PrintWriter, status: Int, text: String) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        writer.print("HTTP/1.1 $status OK\r\n")
        writer.print("Content-Type: text/plain; charset=UTF-8\r\n")
        writer.print("Content-Length: ${bytes.size}\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.print(text)
        writer.flush()
    }

    private fun logEvent(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.FRENCH).format(Date())
        val entry = "[$time] $msg"
        val current = _serverLogs.value.toMutableList()
        current.add(0, entry)
        if (current.size > 50) current.removeAt(current.size - 1)
        _serverLogs.value = current
    }

    private fun buildCaptivePortalPageHtml(
        settings: PortalSettingsEntity,
        fields: List<FieldDefinitionEntity>,
        localIp: String,
        port: Int
    ): String {
        val fieldInputsHtml = fields.filter { it.isEnabled && it.isVisibleInPortal }.joinToString("\n") { field ->
            val inputType = when (field.type) {
                "PASSWORD" -> "password"
                "NUMBER" -> "number"
                "DATE" -> "date"
                else -> "text"
            }
            """
            <div class="field-group">
                <label>${field.label} ${if (field.isRequired) "<span style='color: #ef4444;'>*</span>" else ""}</label>
                <input type="$inputType" name="${field.key}" placeholder="Saisir ${field.label.lowercase()}" ${if (field.isRequired) "required" else ""} class="input-field" />
            </div>
            """.trimIndent()
        }

        return """
        <!DOCTYPE html>
        <html lang="fr">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>${settings.portalTitle}</title>
            <style>
                * { box-sizing: border-box; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; margin: 0; padding: 0; }
                body { background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%); min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 20px; color: #f8fafc; }
                .card { background: rgba(30, 41, 59, 0.95); border-radius: 24px; padding: 32px; width: 100%; max-width: 440px; box-shadow: 0 20px 40px rgba(0,0,0,0.5); border: 1px solid rgba(255,255,255,0.1); backdrop-filter: blur(10px); }
                .badge { display: inline-flex; align-items: center; gap: 8px; background: rgba(59, 130, 246, 0.2); border: 1px solid rgba(59, 130, 246, 0.4); padding: 6px 14px; border-radius: 20px; color: #60a5fa; font-size: 13px; font-weight: 600; margin-bottom: 20px; }
                h1 { font-size: 26px; font-weight: 800; color: #ffffff; margin-bottom: 8px; letter-spacing: -0.5px; }
                p.subtitle { font-size: 14px; color: #94a3b8; margin-bottom: 24px; line-height: 1.5; }
                .field-group { margin-bottom: 18px; text-align: left; }
                label { display: block; font-size: 13px; font-weight: 600; color: #cbd5e1; margin-bottom: 8px; }
                .input-field { width: 100%; padding: 14px 16px; background: #0f172a; border: 1px solid #334155; border-radius: 12px; color: #ffffff; font-size: 15px; outline: none; transition: border 0.2s; }
                .input-field:focus { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,0.25); }
                .btn-submit { width: 100%; padding: 16px; background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%); color: white; border: none; border-radius: 14px; font-size: 16px; font-weight: 700; cursor: pointer; margin-top: 10px; box-shadow: 0 4px 14px rgba(37,99,235,0.4); transition: transform 0.1s, background 0.2s; }
                .btn-submit:hover { background: #1d4ed8; transform: translateY(-1px); }
                .btn-submit:active { transform: translateY(1px); }
                .footer { margin-top: 24px; text-align: center; font-size: 12px; color: #64748b; }
            </style>
        </head>
        <body>
            <div class="card">
                <div class="badge">
                    <span>📶</span> Réseau : ${settings.networkSsid}
                </div>
                <h1>${settings.portalTitle}</h1>
                <p class="subtitle">${settings.welcomeMessage}</p>

                <form action="/api/authenticate" method="POST">
                    $fieldInputsHtml
                    <button type="submit" class="btn-submit">SE CONNECTER AU WI-FI</button>
                </form>

                <div class="footer">
                    Passerelle Autonome • Version Réseau Local
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    private fun buildResultPageHtml(title: String, message: String, isSuccess: Boolean): String {
        val isPending = title.contains("Attente", ignoreCase = true) || message.contains("approbation", ignoreCase = true)
        val icon = when {
            isPending -> "⏳"
            isSuccess -> "✅"
            else -> "❌"
        }
        val accentColor = when {
            isPending -> "#f59e0b"
            isSuccess -> "#22c55e"
            else -> "#ef4444"
        }
        val actionButton = when {
            isPending -> """
            <div style="background: rgba(245, 158, 11, 0.1); border: 1px solid rgba(245, 158, 11, 0.3); padding: 14px; border-radius: 12px; color: #fbbf24; font-size: 14px; margin-top: 10px;">
                🔄 Vérification automatique de l'autorisation en cours... <br/>
                <span style="font-size:12px; color:#cbd5e1;">Dès que l'administrateur valide votre demande, cette page se débloquera automatiquement.</span>
            </div>
            <script>
                setInterval(function() {
                    fetch('/api/session_status')
                        .then(function(r) { return r.json(); })
                        .then(function(data) {
                            if (data.status === 'AUTHORIZED') {
                                window.location.href = "https://www.google.com";
                            }
                        })
                        .catch(function(e) { console.log(e); });
                }, 3000);
            </script>
            """.trimIndent()
            isSuccess -> """
            <a href="https://www.google.com" style="background: linear-gradient(135deg, #22c55e 0%, #16a34a 100%); font-size: 16px; padding: 14px 28px;">
                🌐 ACCÉDER À INTERNET
            </a>
            <script>
                setTimeout(function() {
                    window.location.href = "https://www.google.com";
                }, 3000);
            </script>
            """.trimIndent()
            else -> """<a href="/portal">Réessayer sur le portail</a>"""
        }

        return """
        <!DOCTYPE html>
        <html lang="fr">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>$title</title>
            <style>
                body { background: #0f172a; color: white; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; display: flex; align-items: center; justify-content: center; min-height: 100vh; padding: 20px; margin: 0; }
                .card { background: #1e293b; padding: 36px; border-radius: 24px; text-align: center; max-width: 420px; width: 100%; border: 1px solid rgba(255,255,255,0.1); box-shadow: 0 20px 40px rgba(0,0,0,0.5); }
                .icon { font-size: 54px; margin-bottom: 16px; }
                h1 { color: $accentColor; font-size: 22px; margin-bottom: 12px; font-weight: 800; }
                p { color: #94a3b8; font-size: 15px; line-height: 1.5; margin-bottom: 24px; }
                a { display: inline-block; padding: 12px 24px; background: #334155; color: white; text-decoration: none; border-radius: 12px; font-weight: 700; transition: transform 0.1s; }
                a:hover { transform: translateY(-1px); }
            </style>
        </head>
        <body>
            <div class="card">
                <div class="icon">$icon</div>
                <h1>$title</h1>
                <p>$message</p>
                $actionButton
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    private fun buildEmergencySuccessHtml(): String {
        return """
        <!DOCTYPE html>
        <html lang="fr">
        <head><meta charset="UTF-8"><title>Reset Super-Admin Exécuté</title></head>
        <body style="background:#022c22; color:#4ade80; font-family:sans-serif; text-align:center; padding:50px;">
            <div style="background:#064e3b; padding:40px; border-radius:20px; display:inline-block; max-width:50px 100%;">
                <h1 style="color:#22c55e;">🚨 RÉINITIALISATION SUPER-ADMIN EFFECTUÉE</h1>
                <p style="color:#a7f3d0; font-size:18px;">Le portail captif a été désactivé en urgence. Toutes les restrictions sont levées.</p>
                <p>Le réseau est réouvert en connexion directe sans blocage.</p>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    private fun buildEmergencyForbiddenHtml(): String {
        return """
        <!DOCTYPE html>
        <html lang="fr">
        <head><meta charset="UTF-8"><title>Accès Refusé</title></head>
        <body style="background:#450a0a; color:#fca5a5; font-family:sans-serif; text-align:center; padding:50px;">
            <h1>⚠️ Clé de Récupération Invalide</h1>
            <p>La clé fournies pour le reset d'urgence super-admin est incorrecte.</p>
        </body>
        </html>
        """.trimIndent()
    }
}
