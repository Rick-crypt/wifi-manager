package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.WifiManagerDatabase
import com.example.data.entity.FieldDefinitionEntity
import com.example.data.gateway.AutonomousGatewayEngine
import com.example.data.model.AuthResult
import com.example.data.model.FieldType
import com.example.data.model.PortalStatus
import com.example.data.repository.WifiManagerRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: WifiManagerDatabase
    private lateinit var repository: WifiManagerRepository

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, WifiManagerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        WifiManagerDatabase.seedDefaultData(database)
        repository = WifiManagerRepository(database, context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `test app name is configured correctly`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("WiFi Manager", appName)
    }

    @Test
    fun `test seed data populates default fields and portal settings`() = runBlocking {
        val fields = repository.allFields.first()
        assertTrue("Fields should be seeded", fields.isNotEmpty())
        assertTrue("Should contain passport_number", fields.any { it.key == "passport_number" })

        val settings = repository.portalSettings.first()
        assertEquals("WiFiPass2026!", settings.directBoxPassword)
    }

    @Test
    fun `test direct box password authentication when portal disabled`() = runBlocking {
        repository.togglePortalActivation(false)

        val successResult = repository.authenticateDirectBox("WiFiPass2026!")
        assertTrue("Correct box password should succeed", successResult is AuthResult.DirectBoxSuccess)

        val failResult = repository.authenticateDirectBox("WrongPassword123")
        assertTrue("Wrong box password should fail", failResult is AuthResult.Error)
    }

    /**
     * TEST 1 (Section 13) : Portail activé → téléphone administrateur éteint
     * Résultat : Le portail reste fonctionnel sur le gateway en toute autonomie.
     */
    @Test
    fun `test 1 - portal remains functional on gateway when admin phone is powered off`() = runBlocking {
        // Step 1: Deploy to gateway
        repository.deployToGateway().toList()
        val gwConfig = repository.gatewayConfig.first()
        assertEquals(PortalStatus.ACTIVE, gwConfig.status)

        // Step 2: Simulate admin phone powered off (no admin connection, engine runs autonomously)
        val engine = repository.autonomousEngine
        assertNotNull("Autonomous gateway engine should exist", engine)

        // Step 3: Gateway evaluates connection autonomously without admin phone
        val payload = repository.buildConfigPayload()
        val eval = engine!!.evaluateConnection(
            enteredValues = mapOf("passport_number" to "P123456", "nationality" to "Gabonaise", "promotion" to "2026"),
            payload = payload
        )
        assertTrue("Gateway should authenticate user autonomously", eval is AuthResult.Success)
    }

    /**
     * TEST 2 (Section 13) : Portail activé → application mobile fermée
     * Résultat : Portail fonctionnel depuis le stockage persistant.
     */
    @Test
    fun `test 2 - portal remains functional when mobile app is closed`() = runBlocking {
        repository.deployToGateway().toList()

        // Create new independent engine reading from persistent file
        val newEngine = AutonomousGatewayEngine(context)
        val status = newEngine.status.value
        assertTrue("Persistent gateway state should remember active portal", status.portalActive)
        assertTrue("Persistent gateway version should be > 0", status.deployedVersion > 0)
    }

    /**
     * TEST 3 (Section 13) : Portail activé → backend temporairement indisponible
     * Résultat : Portail fonctionnel selon les règles locales déjà déployées.
     */
    @Test
    fun `test 3 - portal functions autonomously when backend is offline`() = runBlocking {
        repository.deployToGateway().toList()

        // Disconnect backend
        repository.setBackendOnline(false)
        val health = repository.systemHealth.first()
        assertFalse("Backend should be offline", health.backendOnline)
        assertTrue("Portal should still be active on gateway", health.portalActive)
        assertTrue("Autonomous operation should be declared active", health.isAutonomousOperationActive)

        // Local gateway evaluation continues
        val payload = repository.buildConfigPayload()
        val eval = repository.autonomousEngine!!.evaluateConnection(
            enteredValues = mapOf("passport_number" to "P123456", "nationality" to "Gabonaise", "promotion" to "2026"),
            payload = payload
        )
        assertTrue("Evaluation succeeds locally despite backend offline", eval is AuthResult.Success)
    }

    /**
     * TEST 4 (Section 13) : Routeur redémarré
     * Résultat : Configuration restaurée depuis le stockage persistant du gateway.
     */
    @Test
    fun `test 4 - router reboot restores persistent configuration`() = runBlocking {
        repository.deployToGateway().toList()
        val deployedVersion = repository.gatewayConfig.first().deployedConfigVersion

        // Simulate reboot
        repository.simulateRouterReboot()
        val engineStatus = repository.autonomousEngine!!.status.value
        assertTrue("Gateway should restore active status after reboot", engineStatus.portalActive)
        assertEquals(deployedVersion, engineStatus.deployedVersion)
    }

    /**
     * TEST 5 (Section 13) : Nouvel appareil se connecte après extinction du téléphone admin
     * Résultat : Captive portal présenté (champs requis demandés).
     */
    @Test
    fun `test 5 - new device gets captive portal challenge without admin phone`() = runBlocking {
        repository.deployToGateway().toList()
        val payload = repository.buildConfigPayload()

        // New client arrives with empty input
        val eval = repository.autonomousEngine!!.evaluateConnection(
            enteredValues = emptyMap(),
            payload = payload,
            clientMac = "AA:BB:CC:DD:EE:FF"
        )
        assertTrue("Captive portal should challenge missing required fields", eval is AuthResult.ValidationError)
    }

    /**
     * TEST 6 (Section 13) : Utilisateur autorisé se connecte
     * Résultat : Accès Internet accordé par la passerelle.
     */
    @Test
    fun `test 6 - authorized user is granted internet access by gateway`() = runBlocking {
        repository.deployToGateway().toList()
        val payload = repository.buildConfigPayload()

        val eval = repository.autonomousEngine!!.evaluateConnection(
            enteredValues = mapOf("passport_number" to "P123456", "nationality" to "Gabonaise", "promotion" to "2026"),
            payload = payload,
            clientMac = "11:22:33:44:55:66"
        )
        assertTrue("Access granted", eval is AuthResult.Success)
        val leases = repository.autonomousEngine!!.activeClientLeases.value
        assertTrue("Active lease should be stored for client", leases.containsKey("11:22:33:44:55:66"))
    }

    /**
     * TEST 7 (Section 13) : Utilisateur non autorisé se connecte
     * Résultat : Internet bloqué (identifiants inconnus ou suspendu).
     */
    @Test
    fun `test 7 - unauthorized or suspended user is blocked by gateway`() = runBlocking {
        repository.deployToGateway().toList()
        val payload = repository.buildConfigPayload()

        // 1. Unknown user
        val unknownEval = repository.autonomousEngine!!.evaluateConnection(
            enteredValues = mapOf("passport_number" to "UNKNOWN_999", "nationality" to "Autre", "promotion" to "2026"),
            payload = payload
        )
        assertTrue("Unknown user should be rejected", unknownEval is AuthResult.NotFound)

        // 2. Suspended user (User 3 in seed: K456789, Congolaise, 2026)
        val suspendedEval = repository.autonomousEngine!!.evaluateConnection(
            enteredValues = mapOf("passport_number" to "K456789", "nationality" to "Congolaise", "promotion" to "2026"),
            payload = payload
        )
        assertTrue("Suspended user should be blocked", suspendedEval is AuthResult.Suspended)
    }

    /**
     * TEST 8 (Section 13) : Session expirée
     * Résultat : Accès révoqué localement par le gestionnaire de sessions.
     */
    @Test
    fun `test 8 - expired session is revoked locally`() = runBlocking {
        val engine = repository.autonomousEngine!!
        // Grant a lease with expiration in the past
        engine.grantAccess("99:88:77:66:55:44", "192.168.1.200", "Expiring Client", -1000L, false)
        assertTrue("Client initially has lease", engine.activeClientLeases.value.containsKey("99:88:77:66:55:44"))

        val expiredCount = engine.checkAndExpireSessions()
        assertTrue("Expired count should be >= 1", expiredCount >= 1)
        assertFalse("Expired client lease should be removed", engine.activeClientLeases.value.containsKey("99:88:77:66:55:44"))
    }

    /**
     * TEST 9 (Section 13) : Configuration backend différente de celle du routeur
     * Résultat : État 'désynchronisé' (SYNC_REQUIRED) détecté.
     */
    @Test
    fun `test 9 - config change detects desynchronization and sync restores it`() = runBlocking {
        repository.deployToGateway().toList()
        val syncedGw = repository.gatewayConfig.first()
        assertEquals(syncedGw.configVersion, syncedGw.deployedConfigVersion)
        assertEquals(PortalStatus.ACTIVE, syncedGw.status)

        // Admin modifies a field in database
        val newField = FieldDefinitionEntity(
            id = 0,
            key = "department_code",
            label = "Code Département",
            type = FieldType.TEXT.name
        )
        repository.saveField(newField)

        val desyncedGw = repository.gatewayConfig.first()
        assertTrue("Config version should have incremented", desyncedGw.configVersion > desyncedGw.deployedConfigVersion)
        assertTrue("Sync should be required", desyncedGw.isSyncRequired)
        assertEquals(PortalStatus.SYNC_REQUIRED, desyncedGw.status)

        // Perform synchronization
        val syncResult = repository.syncWithGateway()
        assertTrue("Sync should succeed", syncResult)

        val resyncedGw = repository.gatewayConfig.first()
        assertEquals(resyncedGw.configVersion, resyncedGw.deployedConfigVersion)
        assertEquals(PortalStatus.ACTIVE, resyncedGw.status)
    }
}
