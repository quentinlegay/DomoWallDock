package app.walldock

import android.content.Context

/** Réglages de WallDock, stockés localement sur la tablette. */
class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("walldock", Context.MODE_PRIVATE)

    /** Langue de l'appli ("" = celle de la tablette ; voir [Language]). */
    var language: String
        get() = sp.getString(Language.KEY, "") ?: ""
        set(value) = sp.edit().putString(Language.KEY, value).commit().let { }

    /** Plateforme affichée au lancement : [PLATFORM_HA] ou [PLATFORM_GLADYS]. */
    var platform: String
        get() = sp.getString("platform", PLATFORM_HA) ?: PLATFORM_HA
        set(value) = sp.edit().putString("platform", value).apply()

    val isGladys: Boolean
        get() = platform == PLATFORM_GLADYS

    // ---- Home Assistant ----
    /** Accès à Home Assistant : local, externe ou les deux. */
    val ha = Server("ha", 8123, KEY_URL)

    /** Adresse de Home Assistant utilisée maintenant (locale ou externe selon l'accès choisi). */
    val haUrl: String
        get() = ha.url
    /** Compte HA pour la connexion automatique (vide = connexion à la main dans la page). */
    var haUser: String
        get() = sp.getString("ha_user", "") ?: ""
        set(value) = sp.edit().putString("ha_user", value).apply()
    var haPassword: String
        get() = sp.getString("ha_password", "") ?: ""
        set(value) = sp.edit().putString("ha_password", value).apply()
    /** Tableau de bord par défaut (chemin comme /lovelace/0 ou adresse complète) ; vide = accueil. */
    var haDashboard: String
        get() = sp.getString("ha_dashboard", "") ?: ""
        set(value) = sp.edit().putString("ha_dashboard", value).apply()

    // ---- Gladys Assistant ----
    /** Accès à Gladys Assistant : local, externe ou les deux. */
    val gladys = Server("gladys", 80, "gladys_url")

    /** Adresse de Gladys utilisée maintenant (locale ou externe selon l'accès choisi). */
    val gladysUrl: String
        get() = gladys.url
    /** Adresse e-mail du compte Gladys (connexion automatique). */
    var gladysUser: String
        get() = sp.getString("gladys_user", "") ?: ""
        set(value) = sp.edit().putString("gladys_user", value).apply()
    var gladysPassword: String
        get() = sp.getString("gladys_password", "") ?: ""
        set(value) = sp.edit().putString("gladys_password", value).apply()
    /** Tableau de bord par défaut (chemin comme /dashboard/salon) ; vide = accueil. */
    var gladysDashboard: String
        get() = sp.getString("gladys_dashboard", "") ?: ""
        set(value) = sp.edit().putString("gladys_dashboard", value).apply()

    /** Adresse du serveur de la plateforme choisie. */
    val serverUrl: String
        get() = if (isGladys) gladysUrl else haUrl

    /** Page chargée au lancement : tableau de bord par défaut de la plateforme choisie. */
    val dashboardUrl: String
        get() {
            val page = (if (isGladys) gladysDashboard else haDashboard).trim()
            return if (page.isEmpty()) serverUrl else absoluteUrl(page, serverUrl)
        }

    /** Serveur Home Assistant pour ce qui vient de HA (sons /local/…, caméras, réponses de Jarvis). */
    val haBase: String
        get() = haUrl.ifBlank { serverUrl }

    /** Change le tableau de bord par défaut de la plateforme choisie (commande venue de HA). */
    fun setDashboard(value: String) {
        if (isGladys) gladysDashboard = value else haDashboard = value
    }

    /** Code administrateur demandé pour ouvrir les réglages. */
    var pin: String
        get() = sp.getString(KEY_PIN, "") ?: ""
        set(value) = sp.edit().putString(KEY_PIN, value).apply()

    /** Délai d'inactivité (secondes) avant le retour automatique au tableau de bord. */
    var returnSeconds: Int
        get() = sp.getInt(KEY_DELAY, 120)
        set(value) = sp.edit().putInt(KEY_DELAY, value).apply()

    /** Applis du dock, dans l'ordre d'affichage (noms de paquets). */
    var dockApps: List<String>
        get() = (sp.getString(KEY_DOCK, "") ?: "").split(",").filter { it.isNotBlank() }
        set(value) = sp.edit().putString(KEY_DOCK, value.joinToString(",")).apply()

    /** Applis « vidéo » : pas de retour automatique tant qu'elles sont ouvertes. */
    var videoApps: Set<String>
        get() = sp.getStringSet(KEY_VIDEO, emptySet())?.toSet() ?: emptySet()
        set(value) = sp.edit().putStringSet(KEY_VIDEO, value).apply()

    /** Position de la poignée qui ouvre le dock (voir HANDLE_*). */
    var handlePosition: String
        get() = sp.getString(KEY_HANDLE, HANDLE_BOTTOM_RIGHT) ?: HANDLE_BOTTOM_RIGHT
        set(value) = sp.edit().putString(KEY_HANDLE, value).apply()

    /** Écran de veille (horloge) sur le tableau de bord après une période sans toucher l'écran. */
    var screensaverEnabled: Boolean
        get() = sp.getBoolean(KEY_SAVER, false)
        set(value) = sp.edit().putBoolean(KEY_SAVER, value).apply()

    /** Garder l'écran allumé sur le tableau de bord (sinon la veille d'Android s'applique). */
    var keepScreenOn: Boolean
        get() = sp.getBoolean("keep_screen_on", true)
        set(value) = sp.edit().putBoolean("keep_screen_on", value).apply()

    /** Délai avant l'écran de veille, en secondes (ancien réglage en minutes repris). */
    var screensaverSeconds: Int
        get() = if (sp.contains("screensaver_seconds")) sp.getInt("screensaver_seconds", 300)
        else sp.getInt(KEY_SAVER_MINUTES, 5) * 60
        set(value) = sp.edit().putInt("screensaver_seconds", value.coerceIn(10, 86_400)).remove(KEY_SAVER_MINUTES).apply()

    /** Installer automatiquement les nouvelles versions publiées sur GitHub. */
    var autoUpdate: Boolean
        get() = sp.getBoolean(KEY_AUTO_UPDATE, true)
        set(value) = sp.edit().putBoolean(KEY_AUTO_UPDATE, value).apply()

    /** Jarvis écoute « Hey Jarvis » en permanence (sinon seulement via le bouton). */
    var jarvisWakeWord: Boolean
        get() = sp.getBoolean(KEY_JARVIS_WAKE, true)
        set(value) = sp.edit().putBoolean(KEY_JARVIS_WAKE, value).apply()

    /** « Hey Jarvis » reconnu par la tablette elle-même (sinon par openWakeWord dans HA). */
    /** Clé de chiffrement de l'API (32 octets), créée par Home Assistant ; null : pas de chiffrement. */
    var noiseKey: ByteArray?
        get() = sp.getString(KEY_NOISE, null)?.let {
            try {
                android.util.Base64.decode(it, android.util.Base64.NO_WRAP).takeIf { k -> k.size == 32 }
            } catch (e: IllegalArgumentException) {
                null
            }
        }
        set(value) {
            val edit = sp.edit()
            if (value == null) edit.remove(KEY_NOISE)
            else edit.putString(KEY_NOISE, android.util.Base64.encodeToString(value, android.util.Base64.NO_WRAP))
            edit.commit()
        }

    /** Détection de mouvement avec la caméra frontale. */
    var motionEnabled: Boolean
        get() = sp.getBoolean(KEY_MOTION, false)
        set(value) = sp.edit().putBoolean(KEY_MOTION, value).apply()

    /** Sensibilité du mouvement : 0 faible, 1 moyenne, 2 élevée. */
    var motionSensitivity: Int
        get() = sp.getInt(KEY_MOTION_SENSITIVITY, 1)
        set(value) = sp.edit().putInt(KEY_MOTION_SENSITIVITY, value).apply()

    /** Allumer l'écran (et quitter l'écran de veille) quand quelqu'un passe. */
    var motionWakesScreen: Boolean
        get() = sp.getBoolean(KEY_MOTION_WAKE, true)
        set(value) = sp.edit().putBoolean(KEY_MOTION_WAKE, value).apply()

    /** Éteindre l'écran après ce délai (secondes) sans mouvement ni toucher ; 0 = jamais. */
    var motionOffSeconds: Int
        get() = if (sp.contains("motion_off_seconds")) sp.getInt("motion_off_seconds", 0)
        else sp.getInt("motion_off_minutes", 0) * 60
        set(value) = sp.edit().putInt("motion_off_seconds", value.coerceIn(0, 43_200)).remove("motion_off_minutes").apply()

    // ---- MQTT : Gladys Assistant (ou tout broker) ----
    var mqttEnabled: Boolean
        get() = sp.getBoolean("mqtt_enabled", false)
        set(value) = sp.edit().putBoolean("mqtt_enabled", value).apply()
    var mqttHost: String
        get() = sp.getString("mqtt_host", "") ?: ""
        set(value) = sp.edit().putString("mqtt_host", value).apply()
    var mqttPort: Int
        get() = sp.getInt("mqtt_port", 1883)
        set(value) = sp.edit().putInt("mqtt_port", value.coerceIn(1, 65535)).apply()
    var mqttUser: String
        get() = sp.getString("mqtt_user", "") ?: ""
        set(value) = sp.edit().putString("mqtt_user", value).apply()
    var mqttPassword: String
        get() = sp.getString("mqtt_password", "") ?: ""
        set(value) = sp.edit().putString("mqtt_password", value).apply()

    // ---- Gestion de la charge (tablette toujours branchée, prise connectée pilotée par MQTT) ----
    var chargeEnabled: Boolean
        get() = sp.getBoolean("charge_enabled", false)
        set(value) = sp.edit().putBoolean("charge_enabled", value).apply()
    /** Sous ce niveau (%), la charge est demandée. */
    var chargeLow: Int
        get() = sp.getInt("charge_low", 30)
        set(value) = sp.edit().putInt("charge_low", value.coerceIn(5, 95)).apply()
    /** À partir de ce niveau (%), la charge est arrêtée. */
    var chargeHigh: Int
        get() = sp.getInt("charge_high", 80)
        set(value) = sp.edit().putInt("charge_high", value.coerceIn(10, 100)).apply()
    /** Dernière décision (charger ou non), gardée entre deux lancements ; null = pas encore décidé. */
    var chargeWanted: Boolean?
        get() = if (sp.contains("charge_wanted")) sp.getBoolean("charge_wanted", true) else null
        set(value) {
            if (value == null) sp.edit().remove("charge_wanted").apply()
            else sp.edit().putBoolean("charge_wanted", value).apply()
        }

    /** Sensibilité de « Hey Jarvis » : 0 normale, 1 élevée, 2 très élevée. */
    var jarvisSensitivity: Int
        get() = sp.getInt(KEY_JARVIS_SENSITIVITY, 0)
        set(value) = sp.edit().putInt(KEY_JARVIS_SENSITIVITY, value).apply()

    /** Relancer WallDock au démarrage de la tablette. */
    var startOnBoot: Boolean
        get() = sp.getBoolean(KEY_START_ON_BOOT, true)
        set(value) = sp.edit().putBoolean(KEY_START_ON_BOOT, value).apply()

    /** Page affichée comme écran de veille ("" = horloge de WallDock). */
    var screensaverUrl: String
        get() = sp.getString(KEY_SAVER_URL, "") ?: ""
        set(value) = sp.edit().putString(KEY_SAVER_URL, value).apply()

    /** Régler la luminosité du système (autorisation « modifier les paramètres système »). */
    var systemBrightness: Boolean
        get() = sp.getBoolean(KEY_SYSTEM_BRIGHTNESS, false)
        set(value) = sp.edit().putBoolean(KEY_SYSTEM_BRIGHTNESS, value).apply()

    /** Délai (s) sans toucher l'écran avant que le capteur « Activité » passe à « non ». */
    var activitySeconds: Int
        get() = sp.getInt(KEY_ACTIVITY_SECONDS, 60)
        set(value) = sp.edit().putInt(KEY_ACTIVITY_SECONDS, value).apply()

    /** Planification : écran éteint chaque nuit entre [sleepTime] et [wakeTime] (« HH:mm »). */
    var scheduleEnabled: Boolean
        get() = sp.getBoolean(KEY_SCHEDULE, false)
        set(value) = sp.edit().putBoolean(KEY_SCHEDULE, value).apply()

    var sleepTime: String
        get() = sp.getString(KEY_SLEEP_TIME, "23:00") ?: "23:00"
        set(value) = sp.edit().putString(KEY_SLEEP_TIME, value).apply()

    var wakeTime: String
        get() = sp.getString(KEY_WAKE_TIME, "07:00") ?: "07:00"
        set(value) = sp.edit().putString(KEY_WAKE_TIME, value).apply()

    /**
     * Adresses autorisées dans le tableau de bord (une par ligne ou séparées par des virgules :
     * nom de domaine ou début d'adresse). Vide = tout est autorisé. HA est toujours autorisé.
     */
    var allowedUrls: String
        get() = sp.getString(KEY_ALLOWED_URLS, "") ?: ""
        set(value) = sp.edit().putString(KEY_ALLOWED_URLS, value).apply()

    /** Vrai si l'adresse peut être ouverte (liste blanche). */
    fun isUrlAllowed(url: String): Boolean {
        val rules = allowedUrls.split(',', '\n', ';').map { it.trim() }.filter { it.isNotEmpty() }
        if (rules.isEmpty()) return true
        if (url.startsWith("about:") || url.startsWith("data:") || url.startsWith("blob:") || url.startsWith("javascript:")) return true
        val uri = android.net.Uri.parse(url)
        val host = uri.host?.lowercase() ?: return false
        val servers = (ha.hosts + gladys.hosts + listOfNotNull(android.net.Uri.parse(dashboardUrl).host)).map { it.lowercase() }
        if (host in servers) return true
        return rules.any { rule ->
            val r = rule.lowercase()
            if (r.contains("://")) url.lowercase().startsWith(r)
            else host == r.removePrefix("*.") || host.endsWith("." + r.removePrefix("*."))
        }
    }

    /** Zoom du tableau de bord en % (moins de 100 = plus de choses à l'écran). */
    var zoom: Int
        get() = sp.getInt("dashboard_zoom", 100)
        set(value) = sp.edit().putInt("dashboard_zoom", value.coerceIn(50, 200)).apply()

    /** Jarvis continue d'écouter quand l'écran de la tablette est éteint (verrou de veille partiel). */
    var listenScreenOff: Boolean
        get() = sp.getBoolean("listen_screen_off", true)
        set(value) = sp.edit().putBoolean("listen_screen_off", value).apply()

    /** Proxy Bluetooth ESPHome : annonces BLE des appareils proches transmises à HA. */
    var bleProxyEnabled: Boolean
        get() = sp.getBoolean("ble_proxy_enabled", false)
        set(value) = sp.edit().putBoolean("ble_proxy_enabled", value).apply()

    // ---- Lecteur Music Assistant (Sendspin) ----
    var sendspinEnabled: Boolean
        get() = sp.getBoolean("sendspin_enabled", false)
        set(value) = sp.edit().putBoolean("sendspin_enabled", value).apply()
    var sendspinVolume: Int
        get() = sp.getInt("sendspin_volume", 60)
        set(value) = sp.edit().putInt("sendspin_volume", value.coerceIn(0, 100)).apply()
    var sendspinMuted: Boolean
        get() = sp.getBoolean("sendspin_muted", false)
        set(value) = sp.edit().putBoolean("sendspin_muted", value).apply()

    /** Clé privée X25519 de la tablette pour Sendspin (créée une fois, garde le même lecteur dans MA). */
    val sendspinKey: ByteArray
        get() {
            sp.getString("sendspin_key", null)?.let {
                val k = android.util.Base64.decode(it, android.util.Base64.NO_WRAP)
                if (k.size == 32) return k
            }
            val k = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
            sp.edit().putString("sendspin_key", android.util.Base64.encodeToString(k, android.util.Base64.NO_WRAP)).apply()
            return k
        }

    // ---- Écran de veille photos : album Immich ou dossier de la tablette ----
    var immichUrl: String
        get() = sp.getString("immich_url", "") ?: ""
        set(value) = sp.edit().putString("immich_url", value).apply()
    var immichKey: String
        get() = sp.getString("immich_key", "") ?: ""
        set(value) = sp.edit().putString("immich_key", value).apply()
    /** Lien de l'album Immich (ou son identifiant). */
    var immichAlbum: String
        get() = sp.getString("immich_album", "") ?: ""
        set(value) = sp.edit().putString("immich_album", value).apply()
    /** Dossier de photos choisi (adresse SAF) ; vide = aucun. */
    var photoFolder: String
        get() = sp.getString("photo_folder", "") ?: ""
        set(value) = sp.edit().putString("photo_folder", value).apply()
    /** Durée d'affichage de chaque photo (secondes). */
    var photoSeconds: Int
        get() = sp.getInt("photo_seconds", 15)
        set(value) = sp.edit().putInt("photo_seconds", value.coerceIn(3, 600)).apply()

    /** Bouton « Calendrier » (calendriers de HA) dans le dock. */
    var calendarInDock: Boolean
        get() = sp.getBoolean("calendar_in_dock", true)
        set(value) = sp.edit().putBoolean("calendar_in_dock", value).apply()

    var jarvisOnDevice: Boolean
        get() = sp.getBoolean(KEY_JARVIS_ON_DEVICE, true)
        set(value) = sp.edit().putBoolean(KEY_JARVIS_ON_DEVICE, value).apply()

    /**
     * Adresse complète d'un son ou d'une page : les chemins (/local/…) sont rattachés au
     * serveur [base] (par défaut celui de la plateforme choisie).
     */
    fun absoluteUrl(address: String, base: String = serverUrl): String {
        val trimmed = address.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        val origin = base.substringBefore("://") + "://" + base.substringAfter("://").substringBefore("/")
        return origin + "/" + trimmed.trimStart('/')
    }

    /**
     * Accès à un serveur (HA ou Gladys) : adresse locale (http/https, IP, port) et/ou adresse
     * externe (https). En mode « les deux », [Endpoints] dit laquelle répond.
     */
    inner class Server(val key: String, val defaultPort: Int, private val legacyKey: String) {
        var access: String
            get() = migrated().getString("${key}_access", ACCESS_LOCAL) ?: ACCESS_LOCAL
            set(value) = sp.edit().putString("${key}_access", value).apply()
        var scheme: String
            get() = migrated().getString("${key}_scheme", "http") ?: "http"
            set(value) = sp.edit().putString("${key}_scheme", value).apply()
        /** Adresse IPv4 locale, ex. 192.168.80.2 (vide = pas d'adresse locale). */
        var ip: String
            get() = migrated().getString("${key}_ip", "") ?: ""
            set(value) = sp.edit().putString("${key}_ip", value).apply()
        var port: Int
            get() = migrated().getInt("${key}_port", defaultPort)
            set(value) = sp.edit().putInt("${key}_port", value.coerceIn(1, 65535)).apply()
        /** Adresse externe, toujours en https (ex. https://ha.exemple.fr). */
        var external: String
            get() = migrated().getString("${key}_external", "") ?: ""
            set(value) = sp.edit().putString("${key}_external", value).apply()

        val localUrl: String
            get() {
                if (ip.isBlank()) return ""
                val implicit = (scheme == "http" && port == 80) || (scheme == "https" && port == 443)
                return "$scheme://$ip" + if (implicit) "" else ":$port"
            }

        /** Adresse à utiliser maintenant. */
        val url: String
            get() = when (access) {
                ACCESS_EXTERNAL -> external
                ACCESS_BOTH -> if (Endpoints.isExternal(key)) external.ifBlank { localUrl } else localUrl.ifBlank { external }
                else -> localUrl
            }

        /** Noms d'hôte des deux adresses (liste blanche, connexion automatique). */
        val hosts: List<String>
            get() = listOf(localUrl, external).filter { it.isNotBlank() }.mapNotNull { android.net.Uri.parse(it).host }

        /** Reprise de l'ancienne adresse unique (ex. http://192.168.80.2:8123) au premier usage. */
        private fun migrated(): android.content.SharedPreferences {
            if (sp.contains("${key}_access")) return sp
            val old = sp.getString(legacyKey, "") ?: ""
            val uri = android.net.Uri.parse(old)
            val host = uri.host ?: ""
            val edit = sp.edit()
            if (Regex("^\\d{1,3}(\\.\\d{1,3}){3}$").matches(host)) {
                val scheme = uri.scheme ?: "http"
                edit.putString("${key}_access", ACCESS_LOCAL).putString("${key}_scheme", scheme).putString("${key}_ip", host)
                    .putInt("${key}_port", if (uri.port > 0) uri.port else if (scheme == "https") 443 else defaultPort)
            } else if (old.startsWith("https://")) {
                edit.putString("${key}_access", ACCESS_EXTERNAL).putString("${key}_external", old)
            } else {
                edit.putString("${key}_access", ACCESS_LOCAL)
            }
            edit.commit()
            return sp
        }
    }

    val isConfigured: Boolean
        get() = serverUrl.isNotBlank() && pin.isNotBlank()

    companion object {
        const val ACCESS_LOCAL = "local"
        const val ACCESS_EXTERNAL = "external"
        const val ACCESS_BOTH = "both"
        const val PLATFORM_HA = "ha"
        const val PLATFORM_GLADYS = "gladys"

        const val HANDLE_BOTTOM_RIGHT = "bottom_right"
        const val HANDLE_BOTTOM_LEFT = "bottom_left"
        const val HANDLE_RIGHT_MIDDLE = "right_middle"
        const val HANDLE_HIDDEN = "hidden"

        private const val KEY_JARVIS_WAKE = "jarvis_wake_word"
        private const val KEY_JARVIS_ON_DEVICE = "jarvis_on_device"
        private const val KEY_JARVIS_SENSITIVITY = "jarvis_sensitivity"
        private const val KEY_START_ON_BOOT = "start_on_boot"
        private const val KEY_SCHEDULE = "schedule_enabled"
        private const val KEY_SLEEP_TIME = "schedule_sleep"
        private const val KEY_WAKE_TIME = "schedule_wake"
        private const val KEY_ALLOWED_URLS = "allowed_urls"
        private const val KEY_SAVER_URL = "screensaver_url"
        private const val KEY_SYSTEM_BRIGHTNESS = "system_brightness"
        private const val KEY_ACTIVITY_SECONDS = "activity_seconds"
        private const val KEY_NOISE = "api_noise_key"
        private const val KEY_MOTION = "motion_enabled"
        private const val KEY_MOTION_SENSITIVITY = "motion_sensitivity"
        private const val KEY_MOTION_WAKE = "motion_wake_screen"
        private const val KEY_AUTO_UPDATE = "auto_update"
        private const val KEY_HANDLE = "handle_position"
        private const val KEY_SAVER = "screensaver_enabled"
        private const val KEY_SAVER_MINUTES = "screensaver_minutes"
        private const val KEY_URL = "ha_url"
        private const val KEY_PIN = "pin"
        private const val KEY_DELAY = "return_seconds"
        private const val KEY_DOCK = "dock_apps"
        private const val KEY_VIDEO = "video_apps"
    }
}

/** État partagé entre l'écran principal et les services (même processus). */
object AppState {
    /** État du lecteur Music Assistant : off, waiting, connected, playing, error. */
    @Volatile
    var sendspinStatus = "off"

    /** Vrai quand le tableau de bord est au premier plan. */
    @Volatile
    var dashboardVisible = false

    /** Dernière interaction détectée (horloge SystemClock.elapsedRealtime). */
    @Volatile
    var lastInteraction = 0L

    /** Dernier toucher sur l'écran de WallDock (elapsedRealtime). */
    @Volatile
    var lastTouch = android.os.SystemClock.elapsedRealtime()

    /** Secondes sans toucher l'écran (WallDock ou, avec la détection d'activité, une autre appli). */
    val idleSeconds: Long
        get() = (android.os.SystemClock.elapsedRealtime() - maxOf(lastTouch, lastInteraction)) / 1000

    /** Vrai quand le service d'accessibilité (détection d'activité) est actif. */
    @Volatile
    var accessibilityActive = false

    /** Écran « allumé » (faux = écran noir, la tablette peut se mettre en veille). Piloté par HA. */
    @Volatile
    var screenOn = true

    /** Luminosité demandée par HA, en % (1–100), ou -1 pour la luminosité système. */
    @Volatile
    var brightness = -1f

    /** Nom de l'appli au premier plan, remonté à HA. */
    @Volatile
    var currentApp = tr(R.string.dashboard)

    /** Message d'erreur du serveur Home Assistant, ou null si tout va bien. */
    @Volatile
    var apiError: String? = null

    /** Écran principal, quand il existe : appliquer l'écran et la luminosité, recharger la page. */
    @Volatile
    var controller: DashboardController? = null

    // Demandes de HA en attente, traitées dès que le tableau de bord revient au premier plan
    @Volatile
    var pendingHome = false

    @Volatile
    var pendingApp: String? = null

    @Volatile
    var pendingUrl: String? = null
}

interface DashboardController {
    /** Calendriers de HA en plein écran (« jour », « semaine », « mois », « agenda »). */
    fun showCalendar(view: String) {}
    fun applyScreen()
    fun reloadDashboard()
    /** Revenir à la page d'accueil configurée. */
    fun showHome()
    fun openApp(packageName: String)
    fun openUrl(url: String)
    /** Capture de l'écran de WallDock (null si WallDock n'est pas affiché). */
    fun captureScreen(done: (android.graphics.Bitmap?) -> Unit) = done(null)
    /** Réglages modifiés depuis Home Assistant : poignée, adresse du tableau de bord… */
    fun applySettings() {}
    /** Quelqu'un passe devant la tablette : quitter l'écran de veille. */
    fun userPresent() {}
    /** Demander l'autorisation de la caméra (détection de mouvement activée depuis HA). */
    fun requestCameraPermission() {}
}

/**
 * Accès « les deux » : on essaie l'adresse locale, et on passe sur l'adresse externe
 * quand elle ne répond pas (tablette hors de la maison). Réseau : à appeler hors du fil d'affichage.
 */
object Endpoints {
    private val external = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    fun isExternal(key: String): Boolean = external[key] == true

    /** Teste les adresses locales des serveurs en mode « les deux » ; vrai si un choix a changé. */
    fun probe(prefs: Prefs): Boolean {
        var changed = false
        for (server in listOf(prefs.ha, prefs.gladys)) {
            if (server.access != Prefs.ACCESS_BOTH || server.external.isBlank() || server.localUrl.isBlank()) continue
            val useExternal = !reachable(server.localUrl)
            val previous = external.put(server.key, useExternal) ?: false
            if (previous != useExternal) changed = true
        }
        return changed
    }

    private fun reachable(address: String): Boolean = try {
        val connection = java.net.URL(address).openConnection() as java.net.HttpURLConnection
        connection.connectTimeout = 2_000
        connection.readTimeout = 2_000
        connection.instanceFollowRedirects = false
        try {
            connection.responseCode in 200..499
        } finally {
            connection.disconnect()
        }
    } catch (e: Exception) {
        false
    }
}
