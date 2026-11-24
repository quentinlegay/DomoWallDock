package app.walldock

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.os.Build
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import java.net.Inet4Address

/** Réglages : adresse HA, code, délai, autorisations et choix des applis du dock. */
class SettingsActivity : Activity() {

    override fun attachBaseContext(base: android.content.Context) = super.attachBaseContext(Language.wrap(base))

    private lateinit var prefs: Prefs
    private lateinit var platformGroup: RadioGroup
    private lateinit var languageGroup: RadioGroup
    private lateinit var haForm: ServerForm
    private lateinit var haUserField: EditText
    private lateinit var haPasswordField: EditText
    private lateinit var haDashboardField: EditText
    private lateinit var gladysForm: ServerForm
    private lateinit var gladysUserField: EditText
    private lateinit var gladysPasswordField: EditText
    private lateinit var gladysDashboardField: EditText
    private lateinit var haPanel: LinearLayout
    private lateinit var gladysPanel: LinearLayout
    private lateinit var bleCheck: CheckBox
    private lateinit var bleStatusText: TextView
    private lateinit var pinField: EditText
    private lateinit var delayField: EditText
    private lateinit var statusText: TextView
    private lateinit var handleGroup: RadioGroup
    private lateinit var sensitivityGroup: RadioGroup
    private lateinit var haStatusText: TextView
    private lateinit var saverCheck: CheckBox
    private lateinit var saverField: EditText
    private lateinit var saverUrlField: EditText
    private lateinit var immichUrlField: EditText
    private lateinit var immichKeyField: EditText
    private lateinit var immichAlbumField: EditText
    private lateinit var photoSecondsField: EditText
    private lateinit var photoFolderText: TextView
    private lateinit var zoomField: EditText
    private lateinit var systemBrightnessCheck: CheckBox
    private lateinit var bootCheck: CheckBox
    private lateinit var keepOnCheck: CheckBox
    private lateinit var listenOffCheck: CheckBox
    private lateinit var sendspinCheck: CheckBox
    private lateinit var calendarCheck: CheckBox
    private lateinit var scheduleCheck: CheckBox
    private lateinit var sleepField: TextView
    private lateinit var wakeField: TextView
    private lateinit var allowedField: EditText
    private lateinit var updateCheck: CheckBox
    private lateinit var jarvisCheck: CheckBox
    private lateinit var onDeviceCheck: CheckBox
    private lateinit var motionCheck: CheckBox
    private lateinit var motionOffField: EditText
    private lateinit var mqttCheck: CheckBox
    private lateinit var mqttHostField: EditText
    private lateinit var mqttPortField: EditText
    private lateinit var mqttUserField: EditText
    private lateinit var mqttPasswordField: EditText
    private lateinit var mqttStatusText: TextView
    private lateinit var chargeCheck: CheckBox
    private lateinit var chargeLowField: EditText
    private lateinit var chargeHighField: EditText
    private lateinit var jarvisText: TextView
    private lateinit var updateText: TextView
    private val updateListener: (String) -> Unit = { updateText.text = updateLine(it) }

    private val dockSelection = mutableListOf<String>()
    private val videoSelection = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        dockSelection.addAll(prefs.dockApps)
        // Android 13+ (obligatoire à partir d'Android 16) : retour géré par un rappel
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT) {
                if (!prefs.isConfigured) toast(tr(R.string.settings_save_first)) else finish()
            }
        }
        videoSelection.addAll(prefs.videoApps)

        val column = LinearLayout(this)
        column.orientation = LinearLayout.VERTICAL
        column.setPadding(dp(24), dp(8), dp(24), dp(24))

        // Langue de l'appli, tout en haut : celle de la tablette par défaut
        column.addView(heading(tr(R.string.settings_language), 18f))
        languageGroup = RadioGroup(this)
        languageGroup.orientation = RadioGroup.HORIZONTAL
        val languageNames = mapOf("" to tr(R.string.language_system), "fr" to "Français", "en" to "English", "de" to "Deutsch", "es" to "Español")
        for (tag in Language.CHOICES) {
            val choice = RadioButton(this)
            choice.id = View.generateViewId()
            choice.text = languageNames[tag]
            choice.tag = tag
            choice.setTextColor(Color.WHITE)
            val spacing = RadioGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            spacing.marginEnd = dp(16)
            languageGroup.addView(choice, spacing)
            if (tag == prefs.language) languageGroup.check(choice.id)
        }
        val languageScroll = android.widget.HorizontalScrollView(this)
        languageScroll.isHorizontalScrollBarEnabled = false
        languageScroll.addView(languageGroup)
        column.addView(languageScroll)

        // ===== 1. Connexion =====
        val connectionAnchor = section(tr(R.string.settings_section_connection))
        column.addView(connectionAnchor)
        // Plateforme lancée : Home Assistant ou Gladys Assistant
        column.addView(heading(tr(R.string.settings_platform), 18f))
        platformGroup = RadioGroup(this)
        platformGroup.orientation = RadioGroup.HORIZONTAL
        for ((value, text) in listOf(Prefs.PLATFORM_HA to tr(R.string.platform_ha), Prefs.PLATFORM_GLADYS to tr(R.string.platform_gladys))) {
            val choice = RadioButton(this)
            choice.id = View.generateViewId()
            choice.text = text
            choice.tag = value
            choice.textSize = 16f
            choice.setTextColor(Color.WHITE)
            val spacing = RadioGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            spacing.marginEnd = dp(24)
            platformGroup.addView(choice, spacing)
            if (value == prefs.platform) platformGroup.check(choice.id)
        }
        platformGroup.setOnCheckedChangeListener { _, _ -> onPlatformChanged() }
        column.addView(platformGroup)
        column.addView(label(tr(R.string.settings_platform_help)))

        // Deux colonnes sur un écran large (tablette en paysage), sinon l'une sous l'autre
        val wide = resources.configuration.screenWidthDp >= 720
        val platforms = LinearLayout(this)
        platforms.orientation = if (wide) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
        haPanel = panel()
        haPanel.addView(heading(tr(R.string.platform_ha), 18f))
        haForm = ServerForm(this, prefs.ha, ::label, ::field)
        haForm.build(haPanel)
        haPanel.addView(label(tr(R.string.settings_ha_user)))
        haUserField = field(prefs.haUser, "tablette", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
        haPanel.addView(haUserField)
        haPanel.addView(label(tr(R.string.settings_password)))
        haPasswordField = field(prefs.haPassword, "••••••", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        haPanel.addView(secret(haPasswordField))
        haPanel.addView(label(tr(R.string.settings_dashboard_default)))
        haDashboardField = field(prefs.haDashboard, tr(R.string.settings_ha_dashboard_hint), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)
        haPanel.addView(haDashboardField)
        haPanel.addView(testButton(false))
        // Intégration ESPHome (Jarvis, capteurs, proxy Bluetooth…) : état et chiffrement
        haStatusText = label("")
        haPanel.addView(haStatusText)
        haPanel.addView(button(tr(R.string.encryption_reset)) {
            AlertDialog.Builder(this)
                .setMessage(tr(R.string.encryption_reset_confirm))
                .setPositiveButton(tr(R.string.encryption_reset)) { _, _ ->
                    ApiService.instance?.resetEncryption()
                    updateHaStatus()
                }
                .setNegativeButton(tr(R.string.cancel), null)
                .show()
        })

        gladysPanel = panel()
        gladysPanel.addView(heading(tr(R.string.platform_gladys), 18f))
        gladysForm = ServerForm(this, prefs.gladys, ::label, ::field)
        gladysForm.build(gladysPanel)
        gladysPanel.addView(label(tr(R.string.settings_gladys_user)))
        gladysUserField = field(prefs.gladysUser, "tablette@maison.fr", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        gladysPanel.addView(gladysUserField)
        gladysPanel.addView(label(tr(R.string.settings_password)))
        gladysPasswordField = field(prefs.gladysPassword, "••••••", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        gladysPanel.addView(secret(gladysPasswordField))
        gladysPanel.addView(label(tr(R.string.settings_dashboard_default)))
        gladysDashboardField = field(prefs.gladysDashboard, tr(R.string.settings_gladys_dashboard_hint), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)
        gladysPanel.addView(gladysDashboardField)
        gladysPanel.addView(testButton(true))

        if (wide) {
            val left = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            left.marginEnd = dp(8)
            val right = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            right.marginStart = dp(8)
            platforms.addView(haPanel, left)
            platforms.addView(gladysPanel, right)
        } else {
            val spaced = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, WRAP_CONTENT)
            spaced.bottomMargin = dp(12)
            platforms.addView(haPanel, spaced)
            platforms.addView(gladysPanel, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, WRAP_CONTENT))
        }
        column.addView(platforms)
        column.addView(label(tr(R.string.settings_autologin_help)))
        highlightPlatform()

        // ===== 2. Réglages communs =====
        val commonAnchor = section(tr(R.string.settings_common))
        column.addView(commonAnchor)
        column.addView(label(tr(R.string.settings_pin)))
        pinField = field(prefs.pin, "1234", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD)
        column.addView(secret(pinField))

        column.addView(label(tr(R.string.settings_return_delay)))
        delayField = field(prefs.returnSeconds.toString(), "120", InputType.TYPE_CLASS_NUMBER)
        column.addView(delayField)
        column.addView(label(tr(R.string.settings_zoom)))
        zoomField = field(prefs.zoom.toString(), "100", InputType.TYPE_CLASS_NUMBER)
        column.addView(zoomField)

        column.addView(label(tr(R.string.settings_handle)))
        handleGroup = RadioGroup(this)
        val handleChoices = listOf(
            Prefs.HANDLE_BOTTOM_RIGHT to tr(R.string.handle_bottom_right),
            Prefs.HANDLE_BOTTOM_LEFT to tr(R.string.handle_bottom_left),
            Prefs.HANDLE_RIGHT_MIDDLE to tr(R.string.handle_right_middle),
            Prefs.HANDLE_HIDDEN to tr(R.string.handle_hidden)
        )
        for ((value, text) in handleChoices) {
            val choice = RadioButton(this)
            choice.id = View.generateViewId()
            choice.text = text
            choice.tag = value
            choice.setTextColor(Color.WHITE)
            handleGroup.addView(choice)
            if (value == prefs.handlePosition) handleGroup.check(choice.id)
        }
        column.addView(handleGroup)
        column.addView(label(tr(R.string.settings_allowed_urls)))
        allowedField = field(prefs.allowedUrls, tr(R.string.settings_allowed_urls_hint), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)
        column.addView(allowedField)
        systemBrightnessCheck = CheckBox(this)
        systemBrightnessCheck.text = tr(R.string.settings_system_brightness)
        systemBrightnessCheck.setTextColor(Color.WHITE)
        systemBrightnessCheck.isChecked = prefs.systemBrightness
        column.addView(systemBrightnessCheck)
        column.addView(label(tr(R.string.settings_system_brightness_help)))
        bootCheck = CheckBox(this)
        bootCheck.text = tr(R.string.settings_start_on_boot)
        bootCheck.setTextColor(Color.WHITE)
        bootCheck.isChecked = prefs.startOnBoot
        column.addView(bootCheck)
        keepOnCheck = CheckBox(this)
        keepOnCheck.text = tr(R.string.settings_keep_screen_on)
        keepOnCheck.setTextColor(Color.WHITE)
        keepOnCheck.isChecked = prefs.keepScreenOn
        column.addView(keepOnCheck)
        column.addView(label(tr(R.string.settings_keep_screen_on_help)))

        // Écran de veille : activation et délai (son contenu est dans les services)
        column.addView(heading(tr(R.string.settings_screensaver), 18f))
        saverCheck = CheckBox(this)
        saverCheck.text = tr(R.string.settings_screensaver_check)
        saverCheck.setTextColor(Color.WHITE)
        saverCheck.isChecked = prefs.screensaverEnabled
        column.addView(saverCheck)
        column.addView(label(tr(R.string.settings_screensaver_delay)))
        saverField = field(prefs.screensaverSeconds.toString(), "300", InputType.TYPE_CLASS_NUMBER)
        column.addView(saverField)

        // Écran éteint chaque nuit
        column.addView(heading(tr(R.string.settings_schedule_title), 18f))
        scheduleCheck = CheckBox(this)
        scheduleCheck.text = tr(R.string.settings_schedule)
        scheduleCheck.setTextColor(Color.WHITE)
        scheduleCheck.isChecked = prefs.scheduleEnabled
        column.addView(scheduleCheck)
        val times = LinearLayout(this)
        times.orientation = LinearLayout.HORIZONTAL
        sleepField = timeButton(prefs.sleepTime)
        wakeField = timeButton(prefs.wakeTime)
        val offLabel = label(tr(R.string.settings_schedule_off))
        offLabel.setPadding(0, offLabel.paddingTop, dp(4), offLabel.paddingBottom)
        times.addView(offLabel)
        times.gravity = Gravity.CENTER_VERTICAL
        times.addView(sleepField, LinearLayout.LayoutParams(dp(96), WRAP_CONTENT))
        val onLabel = label(tr(R.string.settings_schedule_on))
        onLabel.setPadding(dp(24), onLabel.paddingTop, dp(4), onLabel.paddingBottom)
        times.addView(onLabel)
        times.addView(wakeField, LinearLayout.LayoutParams(dp(96), WRAP_CONTENT))
        column.addView(times)

        // Détection de mouvement (caméra frontale)
        column.addView(heading(tr(R.string.settings_motion_title), 18f))
        motionCheck = CheckBox(this)
        motionCheck.text = tr(R.string.settings_motion)
        motionCheck.setTextColor(Color.WHITE)
        motionCheck.isChecked = prefs.motionEnabled
        column.addView(motionCheck)
        column.addView(label(tr(R.string.settings_motion_help)))
        column.addView(label(tr(R.string.settings_motion_off)))
        motionOffField = field(prefs.motionOffSeconds.toString(), "0", InputType.TYPE_CLASS_NUMBER)
        column.addView(motionOffField)

        column.addView(heading(tr(R.string.settings_permissions), 18f))
        statusText = TextView(this)
        statusText.setTextColor(0xFFB0C4E0.toInt())
        statusText.textSize = 14f
        statusText.setLineSpacing(0f, 1.3f)
        column.addView(statusText)
        column.addView(button(tr(R.string.settings_set_home)) {
            open(Intent(Settings.ACTION_HOME_SETTINGS))
        })
        column.addView(button(tr(R.string.settings_allow_overlay)) {
            open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })
        // Version Google Play : pas de service d'accessibilité
        if (!BuildConfig.PLAY_STORE) column.addView(button(tr(R.string.settings_enable_activity)) {
            open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        column.addView(button(tr(R.string.settings_exit_kiosk)) {
            toast(tr(R.string.settings_choose_home))
            open(Intent(Settings.ACTION_HOME_SETTINGS))
        })

        column.addView(heading(tr(R.string.backup_title), 18f))
        column.addView(label(tr(R.string.backup_help)))
        column.addView(button(tr(R.string.backup_export)) { SettingsBackup.startExport(this) })
        column.addView(button(tr(R.string.backup_import)) { SettingsBackup.startImport(this) })

        column.addView(heading(tr(R.string.settings_updates), 18f))
        if (BuildConfig.PLAY_STORE) column.addView(label(tr(R.string.updates_play)))
        else if (!BuildConfig.SELF_UPDATE) column.addView(label(tr(R.string.updates_manual)))
        updateText = TextView(this)
        updateText.setTextColor(0xFFB0C4E0.toInt())
        updateText.textSize = 14f
        updateText.setLineSpacing(0f, 1.3f)
        column.addView(updateText)
        updateCheck = CheckBox(this)
        updateCheck.text = tr(R.string.settings_auto_update)
        updateCheck.setTextColor(Color.WHITE)
        updateCheck.isChecked = prefs.autoUpdate
        // Version Google Play : les mises à jour passent par le Play Store
        if (!BuildConfig.PLAY_STORE && BuildConfig.SELF_UPDATE) {
            column.addView(updateCheck)
            column.addView(button(tr(R.string.settings_allow_install)) {
                open(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
            })
            column.addView(button(tr(R.string.settings_check_now)) { Updater.check(this, install = true) })
        }

        column.addView(heading(tr(R.string.settings_dock_apps), 18f))
        column.addView(label(tr(R.string.settings_dock_help)))
        // Liste repliée par défaut : elle peut être longue
        val appList = LinearLayout(this)
        appList.orientation = LinearLayout.VERTICAL
        appList.visibility = View.GONE
        val apps = Apps.listLaunchable(this)
        for (app in apps) appList.addView(appRow(app))
        lateinit var toggle: Button
        toggle = button(tr(R.string.dock_show, apps.size, dockSelection.size)) {
            val show = appList.visibility != View.VISIBLE
            appList.visibility = if (show) View.VISIBLE else View.GONE
            toggle.text = if (show) tr(R.string.dock_hide) else tr(R.string.dock_show, apps.size, dockSelection.size)
        }
        column.addView(toggle)
        column.addView(appList)

        // ===== 3. Services annexes =====
        val servicesAnchor = section(tr(R.string.settings_section_services))
        column.addView(servicesAnchor)
        // Réveils
        column.addView(heading(tr(R.string.alarm_section), 18f))
        column.addView(label(tr(R.string.alarm_help)))
        alarmList = LinearLayout(this)
        alarmList.orientation = LinearLayout.VERTICAL
        column.addView(alarmList)
        refreshAlarms()
        column.addView(button(tr(R.string.alarm_add)) { addAlarm() })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            getSystemService(android.app.AlarmManager::class.java)?.canScheduleExactAlarms() == false
        ) {
            column.addView(button(tr(R.string.alarm_exact)) {
                open(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
            })
        }

        // Calendrier
        column.addView(heading(tr(R.string.calendar_title), 18f))
        calendarCheck = CheckBox(this)
        calendarCheck.text = tr(R.string.calendar_in_dock)
        calendarCheck.setTextColor(Color.WHITE)
        calendarCheck.isChecked = prefs.calendarInDock
        column.addView(calendarCheck)

        column.addView(heading(tr(R.string.settings_charge_title), 18f))
        chargeCheck = CheckBox(this)
        chargeCheck.text = tr(R.string.settings_charge_enable)
        chargeCheck.setTextColor(Color.WHITE)
        chargeCheck.isChecked = prefs.chargeEnabled
        column.addView(chargeCheck)
        column.addView(label(tr(R.string.settings_charge_help)))
        column.addView(label(tr(R.string.settings_charge_low)))
        chargeLowField = field(prefs.chargeLow.toString(), "30", InputType.TYPE_CLASS_NUMBER)
        column.addView(chargeLowField)
        column.addView(label(tr(R.string.settings_charge_high)))
        chargeHighField = field(prefs.chargeHigh.toString(), "80", InputType.TYPE_CLASS_NUMBER)
        column.addView(chargeHighField)

        // MQTT (Gladys Assistant, ou tout broker) : capteurs, commandes et gestion de la charge
        column.addView(heading(tr(R.string.settings_mqtt_title), 18f))
        mqttStatusText = label("")
        column.addView(mqttStatusText)
        mqttCheck = CheckBox(this)
        mqttCheck.text = tr(R.string.settings_mqtt_enable)
        mqttCheck.setTextColor(Color.WHITE)
        mqttCheck.isChecked = prefs.mqttEnabled
        column.addView(mqttCheck)
        column.addView(label(tr(R.string.settings_mqtt_help)))
        mqttHostField = field(prefs.mqttHost, tr(R.string.settings_mqtt_host), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)
        column.addView(mqttHostField)
        mqttPortField = field(prefs.mqttPort.toString(), "1883", InputType.TYPE_CLASS_NUMBER)
        column.addView(mqttPortField)
        mqttUserField = field(prefs.mqttUser, tr(R.string.settings_mqtt_user), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
        column.addView(mqttUserField)
        mqttPasswordField = field(prefs.mqttPassword, tr(R.string.settings_mqtt_password), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        column.addView(secret(mqttPasswordField))
        column.addView(button(tr(R.string.test_mqtt)) {
            val host = mqttHostField.text.toString().trim()
            if (host.isEmpty()) {
                toast(tr(R.string.error_mqtt_host))
                return@button
            }
            toast(tr(R.string.autologin_test_running))
            ConnectionTests.mqtt(host, mqttPortField.text.toString().trim().toIntOrNull() ?: 1883,
                mqttUserField.text.toString().trim(), mqttPasswordField.text.toString()) { _, message ->
                showResult("MQTT", message)
            }
        })

        // Écran de veille photos
        column.addView(heading(tr(R.string.settings_photos_title), 18f))
        column.addView(label(tr(R.string.photos_help)))
        immichUrlField = field(prefs.immichUrl, tr(R.string.photos_immich_url), InputType.TYPE_TEXT_VARIATION_URI)
        column.addView(immichUrlField)
        immichKeyField = field(prefs.immichKey, tr(R.string.photos_immich_key), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        column.addView(secret(immichKeyField))
        immichAlbumField = field(prefs.immichAlbum, tr(R.string.photos_immich_album), InputType.TYPE_TEXT_VARIATION_URI)
        column.addView(immichAlbumField)
        column.addView(button(tr(R.string.test_immich)) {
            val url = immichUrlField.text.toString().trim()
            val key = immichKeyField.text.toString().trim()
            val album = immichAlbumField.text.toString().trim()
            if (url.isEmpty() || key.isEmpty() || album.isEmpty()) {
                toast(tr(R.string.test_immich_missing))
                return@button
            }
            toast(tr(R.string.autologin_test_running))
            ConnectionTests.immich(url, key, album) { _, message -> showResult("Immich", message) }
        })
        photoFolderText = label(if (prefs.photoFolder.isBlank()) tr(R.string.photos_folder_none) else tr(R.string.photos_folder_set, Uri.parse(prefs.photoFolder).lastPathSegment ?: ""))
        column.addView(photoFolderText)
        column.addView(button(tr(R.string.photos_folder_choose)) {
            try {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQUEST_PHOTO_FOLDER)
            } catch (e: Exception) {
                toast(tr(R.string.photos_folder_unavailable))
            }
        })
        column.addView(button(tr(R.string.photos_folder_clear)) {
            prefs.photoFolder = ""
            photoFolderText.text = tr(R.string.photos_folder_none)
        })
        column.addView(label(tr(R.string.photos_seconds)))
        photoSecondsField = field(prefs.photoSeconds.toString(), "15", InputType.TYPE_CLASS_NUMBER)
        column.addView(photoSecondsField)
        column.addView(label(tr(R.string.settings_saver_url)))
        saverUrlField = field(prefs.screensaverUrl, tr(R.string.settings_saver_url_hint), InputType.TYPE_TEXT_VARIATION_URI)
        column.addView(saverUrlField)

        column.addView(heading(tr(R.string.settings_sendspin_title), 18f))
        sendspinCheck = CheckBox(this)
        sendspinCheck.text = tr(R.string.settings_sendspin)
        sendspinCheck.setTextColor(Color.WHITE)
        sendspinCheck.isChecked = prefs.sendspinEnabled
        column.addView(sendspinCheck)
        column.addView(label(tr(R.string.settings_sendspin_help)))
        column.addView(label(tr(when (AppState.sendspinStatus) {
            "waiting" -> R.string.sendspin_waiting
            "connected" -> R.string.sendspin_connected
            "playing" -> R.string.sendspin_playing
            "error" -> R.string.sendspin_error
            else -> R.string.sendspin_off
        })))

        // Proxy Bluetooth ESPHome : annonces BLE des appareils proches transmises à HA
        column.addView(heading(tr(R.string.settings_ble_title), 18f))
        bleStatusText = label("")
        column.addView(bleStatusText)
        bleCheck = CheckBox(this)
        bleCheck.text = tr(R.string.settings_ble_enable)
        bleCheck.setTextColor(Color.WHITE)
        bleCheck.isChecked = prefs.bleProxyEnabled
        column.addView(bleCheck)
        column.addView(label(tr(R.string.settings_ble_help)))
        column.addView(button(tr(R.string.settings_allow_ble)) { requestBlePermissions() })

        column.addView(heading("Jarvis", 18f))
        jarvisText = TextView(this)
        jarvisText.setTextColor(0xFFB0C4E0.toInt())
        jarvisText.textSize = 14f
        jarvisText.setLineSpacing(0f, 1.3f)
        column.addView(jarvisText)
        jarvisCheck = CheckBox(this)
        jarvisCheck.text = tr(R.string.settings_jarvis_listen)
        jarvisCheck.setTextColor(Color.WHITE)
        jarvisCheck.isChecked = prefs.jarvisWakeWord
        column.addView(jarvisCheck)
        onDeviceCheck = CheckBox(this)
        onDeviceCheck.text = tr(R.string.settings_jarvis_on_device)
        onDeviceCheck.setTextColor(Color.WHITE)
        onDeviceCheck.isChecked = prefs.jarvisOnDevice
        column.addView(onDeviceCheck)
        column.addView(label(tr(R.string.settings_sensitivity)))
        sensitivityGroup = RadioGroup(this)
        val levels = listOf(
            tr(R.string.sensitivity_normal), tr(R.string.sensitivity_high), tr(R.string.sensitivity_very_high),
            tr(R.string.sensitivity_low)
        )
        Jarvis.SENSITIVITY_ORDER.forEach { index ->
            val text = levels[index]
            val choice = RadioButton(this)
            choice.id = View.generateViewId()
            choice.text = text
            choice.tag = index
            choice.setTextColor(Color.WHITE)
            sensitivityGroup.addView(choice)
            if (index == prefs.jarvisSensitivity) sensitivityGroup.check(choice.id)
        }
        column.addView(sensitivityGroup)
        column.addView(label(tr(R.string.voice_help)))
        column.addView(button(tr(R.string.voice_learn)) { VoiceTraining(this).start() })
        column.addView(button(tr(R.string.voice_forget)) {
            VoiceVerifier.forget(this)
            ApiService.instance?.jarvis?.reloadVoiceSettings()
            updateJarvisStatus()
            toast(tr(R.string.voice_forgotten))
        })
        column.addView(button(tr(R.string.settings_allow_mic)) {
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                toast(tr(R.string.settings_mic_already))
            } else {
                requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), MainActivity.REQUEST_MIC)
            }
        })
        listenOffCheck = CheckBox(this)
        listenOffCheck.text = tr(R.string.settings_listen_screen_off)
        listenOffCheck.setTextColor(Color.WHITE)
        listenOffCheck.isChecked = prefs.listenScreenOff
        column.addView(listenOffCheck)
        column.addView(button(tr(R.string.settings_test_jarvis)) { ApiService.instance?.jarvis?.talk() })

        column.addView(button(tr(R.string.settings_save_quit)) { save(close = true) })
        // Crédits : une ligne et les liens en icônes
        val credits = label(tr(R.string.about_credits, BuildConfig.VERSION_NAME))
        credits.gravity = Gravity.CENTER
        column.addView(credits, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, WRAP_CONTENT))
        val links = LinearLayout(this)
        links.orientation = LinearLayout.HORIZONTAL
        links.gravity = Gravity.CENTER
        for ((icon, name, url) in listOf(
            Triple(R.drawable.ic_link_web, "mcflypartages.fr", "https://mcflypartages.fr"),
            Triple(R.drawable.ic_link_mastodon, "Mastodon @McFlyPartages@mastodon.social", "https://mastodon.social/@McFlyPartages"),
            Triple(R.drawable.ic_link_facebook, "Facebook McFlyPartages", "https://www.facebook.com/McFlyPartages"),
            Triple(R.drawable.ic_link_x, "X @McFlyPartages", "https://x.com/McFlyPartages"),
            Triple(R.drawable.ic_link_tipeee, "Tipeee McFlyPartages", "https://fr.tipeee.com/mcflypartages"),
            Triple(R.drawable.ic_link_kofi, "Ko-fi McFlyPartages", "https://ko-fi.com/mcflypartages")
        )) {
            val view = ImageView(this)
            view.setImageResource(icon)
            view.contentDescription = name
            view.setPadding(dp(8), dp(8), dp(8), dp(8))
            val round = android.graphics.drawable.GradientDrawable()
            round.shape = android.graphics.drawable.GradientDrawable.OVAL
            round.setColor(0xFF0C1C3E.toInt())
            view.background = round
            view.setOnClickListener { open(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            val params = LinearLayout.LayoutParams(dp(43), dp(43))
            params.setMargins(dp(8), dp(6), dp(8), dp(16))
            links.addView(view, params)
        }
        column.addView(links, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, WRAP_CONTENT))

        val scroll = ScrollView(this)
        scroll.addView(column)

        // Barre fixe : accès rapide aux trois parties et bouton Enregistrer
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setPadding(dp(16), dp(8), dp(16), dp(8))
        bar.setBackgroundColor(0xFF0A1634.toInt())
        bar.elevation = dp(4).toFloat()
        if (resources.configuration.screenWidthDp >= 720) {
            val title = TextView(this)
            title.text = tr(R.string.settings_title)
            title.textSize = 18f
            title.setTextColor(0xFF3FD4FF.toInt())
            title.setPadding(dp(8), 0, dp(24), 0)
            bar.addView(title)
        }
        val tabRow = LinearLayout(this)
        tabRow.orientation = LinearLayout.HORIZONTAL
        val anchors = listOf(connectionAnchor, commonAnchor, servicesAnchor)
        val tabs = listOf(R.string.settings_section_connection, R.string.settings_common, R.string.settings_section_services)
            .mapIndexed { i, text ->
                tab(tr(text)) { scroll.smoothScrollTo(0, (anchors[i].top - dp(4)).coerceAtLeast(0)) }.also { tabRow.addView(it) }
            }
        val tabScroll = android.widget.HorizontalScrollView(this)
        tabScroll.isHorizontalScrollBarEnabled = false
        tabScroll.addView(tabRow)
        bar.addView(tabScroll, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        val saveTop = button(tr(R.string.settings_save)) { save(close = false) }
        bar.addView(saveTop)
        // L'onglet de la partie affichée est mis en avant pendant le défilement
        fun highlightTab() {
            val y = scroll.scrollY + dp(16)
            val current = anchors.indexOfLast { it.top <= y }.coerceAtLeast(0)
            tabs.forEachIndexed { i, t -> styleTab(t, i == current) }
        }
        scroll.setOnScrollChangeListener { _, _, _, _, _ -> highlightTab() }
        scroll.post { highlightTab() }

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(0xFF050C1E.toInt())
        root.addView(bar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, WRAP_CONTENT))
        root.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        updateHaStatus()
        updateText.text = updateLine(Updater.status)
        updateJarvisStatus()
        Updater.addListener(updateListener)
        mqttStatusText.post(mqttStatusRefresh)
    }

    /** L'état MQTT change tout seul (connexion, erreur) : rafraîchi tant que l'écran est ouvert. */
    private val mqttStatusRefresh = object : Runnable {
        override fun run() {
            val service = ApiService.instance
            mqttStatusText.text = when (service?.mqttStatus) {
                "connected" -> tr(R.string.mqtt_connected, "${service.friendlyName} (${service.deviceName})")
                "connecting" -> tr(R.string.mqtt_connecting)
                "error" -> tr(R.string.mqtt_error, service.mqttError)
                else -> tr(R.string.mqtt_off)
            }
            bleStatusText.text = BleProxy.statusText(this@SettingsActivity)
            mqttStatusText.postDelayed(this, 2_000)
        }
    }

    override fun onPause() {
        mqttStatusText.removeCallbacks(mqttStatusRefresh)
        Updater.removeListener(updateListener)
        super.onPause()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MainActivity.REQUEST_CAMERA) ApiService.instance?.applyMotionSettings()
        if (requestCode == REQUEST_BLE) ApiService.instance?.applyBleProxy()
        if (requestCode == MainActivity.REQUEST_MIC) {
            ApiService.instance?.onMicrophoneChanged()
            updateJarvisStatus()
        }
    }

    fun updateJarvisStatus() {
        val mic = checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val state = ApiService.instance?.jarvis?.state
        jarvisText.text = listOf(
            if (mic) tr(R.string.jarvis_mic_ok) else tr(R.string.jarvis_mic_missing),
            when (state) {
                Jarvis.State.WAITING -> tr(R.string.jarvis_status_waiting)
                Jarvis.State.OFF, null -> tr(R.string.jarvis_status_off)
                else -> tr(R.string.jarvis_status_busy)
            },
            if (ApiService.instance?.jarvis?.detectsOnDevice == true) {
                tr(R.string.jarvis_detect_tablet)
            } else {
                tr(R.string.jarvis_detect_ha)
            },
            (ApiService.instance?.jarvis?.learnedVoices ?: 0).let {
                if (it > 0) tr(R.string.voice_count, it) else tr(R.string.voice_none)
            }
        ).joinToString("\n")
    }

    private fun updateLine(status: String): String {
        val allowed = packageManager.canRequestPackageInstalls()
        return tr(R.string.update_installed_version, BuildConfig.VERSION_NAME) + "\n" +
            (if (allowed) tr(R.string.update_install_allowed) else tr(R.string.update_install_denied)) +
            "\n$status"
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_PHOTO_FOLDER) {
            val tree = data?.data
            if (resultCode == RESULT_OK && tree != null) {
                try {
                    contentResolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) {
                    // autorisation temporaire seulement
                }
                prefs.photoFolder = tree.toString()
                photoFolderText.text = tr(R.string.photos_folder_set, tree.lastPathSegment ?: "")
            }
            return
        }
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data
        if (resultCode != RESULT_OK || uri == null) return
        when (requestCode) {
            SettingsBackup.REQUEST_EXPORT ->
                toast(tr(if (SettingsBackup.write(this, uri)) R.string.backup_saved else R.string.backup_failed))
            SettingsBackup.REQUEST_IMPORT -> {
                if (SettingsBackup.read(this, uri)) {
                    toast(tr(R.string.backup_restored))
                    window.decorView.postDelayed({ SettingsBackup.restartApp(this) }, 1_500)
                } else {
                    toast(tr(R.string.backup_invalid))
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!prefs.isConfigured) {
            toast(tr(R.string.settings_save_first))
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    /** [close] : bouton du bas, on quitte les réglages ; bouton du haut, on reste. */
    private fun save(close: Boolean) {
        val platform = (platformGroup.findViewById<RadioButton>(platformGroup.checkedRadioButtonId)?.tag as? String) ?: Prefs.PLATFORM_HA
        val gladysActive = platform == Prefs.PLATFORM_GLADYS
        // La plateforme affichée doit être complète ; l'autre seulement si quelque chose est saisi
        val haProblem = if (!gladysActive || !haForm.isEmpty()) haForm.validate() else null
        val gladysProblem = if (gladysActive || !gladysForm.isEmpty()) gladysForm.validate() else null
        val pin = pinField.text.toString().trim()
        val delay = delayField.text.toString().trim().toIntOrNull() ?: 0
        val saverSeconds = saverField.text.toString().trim().toIntOrNull() ?: 0
        val chargeLow = chargeLowField.text.toString().trim().toIntOrNull() ?: -1
        val chargeHigh = chargeHighField.text.toString().trim().toIntOrNull() ?: -1
        val mqttHost = mqttHostField.text.toString().trim()
        when {
            mqttCheck.isChecked && mqttHost.isEmpty() -> toast(tr(R.string.error_mqtt_host))
            chargeCheck.isChecked && !mqttCheck.isChecked -> toast(tr(R.string.error_charge_mqtt))
            chargeCheck.isChecked && (chargeLow < 5 || chargeHigh > 100 || chargeHigh - chargeLow < 10) ->
                toast(tr(R.string.error_charge_levels))
            saverCheck.isChecked && saverSeconds < 10 -> toast(tr(R.string.error_saver_delay))
            haProblem != null -> toast(tr(R.string.platform_ha) + " : " + haProblem)
            gladysProblem != null -> toast(tr(R.string.platform_gladys) + " : " + gladysProblem)
            pin.length < 4 -> toast(tr(R.string.error_pin))
            delay < 10 -> toast(tr(R.string.error_delay))
            else -> {
                prefs.platform = platform
                val language = (languageGroup.findViewById<RadioButton>(languageGroup.checkedRadioButtonId)?.tag as? String) ?: ""
                val languageChanged = language != prefs.language
                haForm.store()
                prefs.haUser = haUserField.text.toString().trim()
                prefs.haPassword = haPasswordField.text.toString()
                prefs.haDashboard = haDashboardField.text.toString().trim()
                gladysForm.store()
                prefs.gladysUser = gladysUserField.text.toString().trim()
                prefs.gladysPassword = gladysPasswordField.text.toString()
                prefs.gladysDashboard = gladysDashboardField.text.toString().trim()
                val bleChanged = prefs.bleProxyEnabled != bleCheck.isChecked
                prefs.bleProxyEnabled = bleCheck.isChecked
                if (bleCheck.isChecked && !BleProxy.hasPermissions(this)) requestBlePermissions()
                if (bleChanged) ApiService.instance?.applyBleProxy(featuresChanged = true)
                prefs.pin = pin
                prefs.returnSeconds = delay
                prefs.screensaverEnabled = saverCheck.isChecked
                prefs.autoUpdate = updateCheck.isChecked
                prefs.screensaverUrl = saverUrlField.text.toString().trim()
                prefs.immichUrl = immichUrlField.text.toString().trim()
                prefs.immichKey = immichKeyField.text.toString().trim()
                prefs.immichAlbum = immichAlbumField.text.toString().trim()
                photoSecondsField.text.toString().toIntOrNull()?.let { prefs.photoSeconds = it }
                zoomField.text.toString().toIntOrNull()?.let { prefs.zoom = it }
                prefs.startOnBoot = bootCheck.isChecked
                prefs.keepScreenOn = keepOnCheck.isChecked
                prefs.listenScreenOff = listenOffCheck.isChecked
                prefs.sendspinEnabled = sendspinCheck.isChecked
                ApiService.instance?.applySendspin()
                prefs.calendarInDock = calendarCheck.isChecked
                prefs.scheduleEnabled = scheduleCheck.isChecked
                sleepField.text.toString().trim().let { if (Regex("^\\d{1,2}[:hH]\\d{0,2}$").matches(it)) prefs.sleepTime = it }
                wakeField.text.toString().trim().let { if (Regex("^\\d{1,2}[:hH]\\d{0,2}$").matches(it)) prefs.wakeTime = it }
                prefs.allowedUrls = allowedField.text.toString().trim()
                prefs.systemBrightness = systemBrightnessCheck.isChecked
                if (systemBrightnessCheck.isChecked && !Settings.System.canWrite(this)) {
                    try {
                        startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")))
                    } catch (e: Exception) {
                        // écran indisponible
                    }
                }
                AppState.controller?.applyScreen()
                val wakeChanged = prefs.jarvisWakeWord != jarvisCheck.isChecked
                prefs.jarvisWakeWord = jarvisCheck.isChecked
                val modeChanged = prefs.jarvisOnDevice != onDeviceCheck.isChecked
                if (modeChanged && onDeviceCheck.isChecked) WakeWordDetector.clearCrash(this)
                prefs.jarvisOnDevice = onDeviceCheck.isChecked
                prefs.motionEnabled = motionCheck.isChecked
                if (motionCheck.isChecked &&
                    checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions(arrayOf(android.Manifest.permission.CAMERA), MainActivity.REQUEST_CAMERA)
                }
                prefs.motionOffSeconds = motionOffField.text.toString().trim().toIntOrNull() ?: 0
                ApiService.instance?.applyMotionSettings()
                prefs.mqttEnabled = mqttCheck.isChecked
                prefs.mqttHost = mqttHost
                mqttPortField.text.toString().trim().toIntOrNull()?.let { prefs.mqttPort = it }
                prefs.mqttUser = mqttUserField.text.toString().trim()
                prefs.mqttPassword = mqttPasswordField.text.toString()
                prefs.chargeEnabled = chargeCheck.isChecked
                if (chargeCheck.isChecked) {
                    prefs.chargeLow = chargeLow
                    prefs.chargeHigh = chargeHigh
                }
                ApiService.instance?.applyMqtt()
                val level = sensitivityGroup.findViewById<RadioButton>(sensitivityGroup.checkedRadioButtonId)
                prefs.jarvisSensitivity = (level?.tag as? Int) ?: 0
                ApiService.instance?.jarvis?.reloadVoiceSettings()
                if (wakeChanged || modeChanged) ApiService.instance?.jarvis?.onSettingsChanged()
                if (saverSeconds >= 10) prefs.screensaverSeconds = saverSeconds
                prefs.dockApps = dockSelection.toList()
                prefs.videoApps = videoSelection.toSet()
                val checked = handleGroup.findViewById<RadioButton>(handleGroup.checkedRadioButtonId)
                prefs.handlePosition = (checked?.tag as? String) ?: Prefs.HANDLE_BOTTOM_RIGHT
                ApiService.instance?.onDockChanged()
                ApiService.instance?.reportSettings()
                toast(tr(R.string.settings_saved))
                if (languageChanged) {
                    // Nouvelle langue : on relance l'appli pour que tout (écrans, entités HA, MQTT) la prenne
                    prefs.language = language
                    toast(tr(R.string.language_restart))
                    window.decorView.postDelayed({ SettingsBackup.restartApp(this) }, 1_200)
                    return
                }
                if (close) finish()
            }
        }
    }

    private fun updateStatus() {
        val home = isDefaultHome()
        val overlay = Settings.canDrawOverlays(this)
        val activity = AppState.accessibilityActive
        statusText.text = listOf(
            (if (home) "✅ " else "⚠️ ") + tr(R.string.status_home) + (if (home) "" else "\n   " + tr(R.string.status_home_warning)),
            (if (overlay) tr(R.string.status_overlay_ok) else tr(R.string.status_overlay_missing)),
            if (BuildConfig.PLAY_STORE) null
            else if (activity) tr(R.string.status_activity_ok) else tr(R.string.status_activity_missing)
        ).filterNotNull().joinToString("\n")
    }

    private fun updateHaStatus() {
        val service = ApiService.instance
        val error = AppState.apiError
        val ip = localIpAddress() ?: tr(R.string.unknown_address)
        haStatusText.text = when {
            error != null -> "⚠️ $error"
            service == null -> tr(R.string.ha_service_stopped)
            service.connectedClients() > 0 -> tr(R.string.ha_connected, service.friendlyName, service.deviceName)
            else -> tr(R.string.ha_waiting, service.friendlyName, ip)
        } + if (service != null && error == null) {
            "\n" + tr(if (service.isEncrypted) R.string.encryption_on else R.string.encryption_off)
        } else {
            ""
        }
    }

    private fun localIpAddress(): String? {
        val connectivity = getSystemService(ConnectivityManager::class.java) ?: return null
        val properties = connectivity.getLinkProperties(connectivity.activeNetwork) ?: return null
        return properties.linkAddresses
            .map { it.address }
            .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
            ?.hostAddress
    }

    private fun isDefaultHome(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        @Suppress("DEPRECATION")
        val resolved = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName == packageName
    }

    private fun open(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: Exception) {
            toast(tr(R.string.setting_missing))
        }
    }

    private fun appRow(app: LaunchableApp): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(0, dp(6), 0, dp(6))

        val icon = ImageView(this)
        icon.setImageDrawable(app.icon)
        row.addView(icon, LinearLayout.LayoutParams(dp(36), dp(36)))

        val name = TextView(this)
        name.text = app.label
        name.setTextColor(Color.WHITE)
        name.textSize = 15f
        name.setPadding(dp(12), 0, 0, 0)
        row.addView(name, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))

        val inDock = CheckBox(this)
        inDock.text = "Dock"
        inDock.setTextColor(Color.WHITE)
        inDock.isChecked = app.packageName in dockSelection
        inDock.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                if (app.packageName !in dockSelection) dockSelection.add(app.packageName)
            } else {
                dockSelection.remove(app.packageName)
            }
        }
        row.addView(inDock)

        val video = CheckBox(this)
        video.text = tr(R.string.dock_video)
        video.setTextColor(Color.WHITE)
        video.isChecked = app.packageName in videoSelection
        video.setOnCheckedChangeListener { _, checked ->
            if (checked) videoSelection.add(app.packageName) else videoSelection.remove(app.packageName)
        }
        row.addView(video)
        return row
    }

    /** Heure « HH:mm » : un champ comme les autres ; un appui ouvre les roues heures / minutes. */
    private fun timeButton(value: String): TextView {
        val view = field(normalizeTime(value), "00:00", InputType.TYPE_NULL)
        view.isFocusable = false
        view.isCursorVisible = false
        view.gravity = Gravity.CENTER
        view.setOnClickListener {
            val (h, m) = normalizeTime(view.text.toString()).split(":").map { it.toInt() }
            // Thème Holo : roues des heures et des minutes, sur toutes les versions d'Android
            android.app.TimePickerDialog(this, android.R.style.Theme_Holo_Dialog, { _, hour, minute ->
                view.setText("%02d:%02d".format(hour, minute))
            }, h, m, true).show()
        }
        return view
    }

    /** « 7h », « 7:5 », « 07:05 » → « 07:05 ». */
    private fun normalizeTime(value: String): String {
        val parts = value.trim().split(':', 'h', 'H')
        val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        return "%02d:%02d".format(h, m)
    }

    /** Cadre arrondi d'une plateforme ; celui de la plateforme choisie est souligné en bleu. */
    private fun panel(): LinearLayout {
        val view = LinearLayout(this)
        view.orientation = LinearLayout.VERTICAL
        view.setPadding(dp(16), dp(4), dp(16), dp(16))
        return view
    }

    private val loginTester by lazy { AutoLogin(prefs) {} }

    /** Vérifie adresse, compte et mot de passe saisis (même s'ils ne sont pas encore enregistrés). */
    private fun testButton(gladys: Boolean): Button = button(tr(R.string.settings_test_login)) {
        val addresses = (if (gladys) gladysForm else haForm).addresses()
        val user = (if (gladys) gladysUserField else haUserField).text.toString().trim()
        val password = (if (gladys) gladysPasswordField else haPasswordField).text.toString()
        if (user.isEmpty() || password.isEmpty() || addresses.isEmpty()) {
            toast(tr(R.string.autologin_test_missing))
            return@button
        }
        toast(tr(R.string.autologin_test_running))
        // Adresse locale puis externe (accès « les deux ») : un résultat par adresse
        val lines = ArrayList<String>()
        fun next(i: Int) {
            if (i >= addresses.size) {
                showResult(tr(if (gladys) R.string.platform_gladys else R.string.platform_ha), lines.joinToString("\n\n"))
                return
            }
            val (name, address) = addresses[i]
            loginTester.test(gladys, address, user, password) { problem ->
                lines.add("$name — $address\n" + if (problem == null) tr(R.string.autologin_test_ok) else tr(R.string.autologin_error, problem))
                next(i + 1)
            }
        }
        next(0)
    }

    private fun showResult(title: String, message: String) {
        if (isFinishing) return
        AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK", null).show()
    }

    /** Champ secret avec un œil pour afficher ou masquer ce qui est saisi. */
    private fun secret(field: EditText): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.addView(field, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        val eye = TextView(this)
        eye.text = "👁"
        eye.textSize = 20f
        eye.setPadding(dp(12), dp(6), dp(8), dp(6))
        eye.alpha = 0.4f
        eye.contentDescription = tr(R.string.settings_show_password)
        var shown = false
        // Code administrateur : on reste sur le pavé numérique
        val numeric = field.inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_NUMBER
        eye.setOnClickListener {
            shown = !shown
            val cursor = field.selectionEnd
            field.inputType = when {
                numeric && shown -> InputType.TYPE_CLASS_NUMBER
                numeric -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                shown -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                else -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            // Le type « mot de passe » impose une police à chasse fixe : on garde la même partout
            field.typeface = android.graphics.Typeface.DEFAULT
            field.setSelection(cursor.coerceIn(0, field.text.length))
            eye.alpha = if (shown) 1f else 0.4f
            eye.contentDescription = tr(if (shown) R.string.settings_hide_password else R.string.settings_show_password)
        }
        row.addView(eye)
        // Même police dès l'ouverture, avant le premier appui sur l'œil
        field.typeface = android.graphics.Typeface.DEFAULT
        return row
    }

    /** Cases décochées au passage à Gladys, pour les remettre si l'on revient à Home Assistant. */
    private var haOnlySnapshot: List<Boolean>? = null

    /** Fonctions décochées en passant à Gladys : Jarvis (écoute, écran éteint), proxy Bluetooth, Music Assistant. */
    private fun haOnlyChecks() = listOf(jarvisCheck, listenOffCheck, bleCheck, sendspinCheck)

    /**
     * Passage à Gladys : les fonctions liées à Home Assistant sont décochées (on peut les
     * recocher, par exemple si HA est aussi utilisé). Retour à HA : elles reprennent leur état d'avant.
     */
    private fun onPlatformChanged() {
        highlightPlatform()
        val gladys = (platformGroup.findViewById<RadioButton>(platformGroup.checkedRadioButtonId)?.tag as? String) == Prefs.PLATFORM_GLADYS
        val boxes = haOnlyChecks()
        if (gladys) {
            if (boxes.none { it.isChecked }) return
            haOnlySnapshot = boxes.map { it.isChecked }
            boxes.forEach { it.isChecked = false }
            toast(tr(R.string.gladys_ha_features_off))
        } else if (!gladys) {
            haOnlySnapshot?.let { saved -> boxes.forEachIndexed { i, box -> box.isChecked = saved[i] } }
            haOnlySnapshot = null
        }
    }

    private fun highlightPlatform() {
        val chosen = (platformGroup.findViewById<RadioButton>(platformGroup.checkedRadioButtonId)?.tag as? String) ?: Prefs.PLATFORM_HA
        for ((view, value) in listOf(haPanel to Prefs.PLATFORM_HA, gladysPanel to Prefs.PLATFORM_GLADYS)) {
            val selected = value == chosen
            val shape = android.graphics.drawable.GradientDrawable()
            shape.cornerRadius = dp(16).toFloat()
            shape.setColor(if (selected) 0xFF0C1C3E.toInt() else 0xFF08122A.toInt())
            shape.setStroke(dp(2), if (selected) 0xFF3FD4FF.toInt() else 0xFF1C2C4E.toInt())
            view.background = shape
        }
    }

    private fun requestBlePermissions() {
        val missing = BleProxy.requiredPermissions().filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) {
            ApiService.instance?.applyBleProxy()
        } else {
            requestPermissions(missing.toTypedArray(), REQUEST_BLE)
        }
    }

    /** Titre d'une grande partie (cible des onglets de la barre du haut). */
    private fun section(s: String): TextView {
        val view = TextView(this)
        view.text = s
        view.textSize = 24f
        view.tag = HEADING_TAG
        view.setTextColor(Color.WHITE)
        view.setPadding(0, dp(28), 0, dp(4))
        val line = android.graphics.drawable.GradientDrawable()
        line.setColor(0x00000000)
        view.background = line
        return view
    }

    private fun tab(s: String, action: () -> Unit): TextView {
        val view = TextView(this)
        view.text = s
        view.textSize = 15f
        view.setPadding(dp(16), dp(8), dp(16), dp(8))
        view.setOnClickListener { action() }
        val params = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        params.marginEnd = dp(8)
        view.layoutParams = params
        styleTab(view, false)
        return view
    }

    private fun styleTab(view: TextView, active: Boolean) {
        val shape = android.graphics.drawable.GradientDrawable()
        shape.cornerRadius = dp(18).toFloat()
        shape.setColor(if (active) 0xFF3FD4FF.toInt() else 0x00000000)
        shape.setStroke(dp(1), 0xFF3FD4FF.toInt())
        view.background = shape
        view.setTextColor(if (active) 0xFF050C1E.toInt() else 0xFF3FD4FF.toInt())
    }

    private fun heading(s: String, size: Float): TextView {
        val view = TextView(this)
        view.text = s
        view.textSize = size
        view.setTextColor(0xFF3FD4FF.toInt())
        view.setPadding(0, dp(20), 0, dp(8))
        view.tag = HEADING_TAG
        return view
    }

    private fun label(s: String): TextView {
        val view = TextView(this)
        view.text = s
        view.textSize = 14f
        view.setTextColor(0xFFB0C4E0.toInt())
        view.setPadding(0, dp(12), 0, dp(4))
        return view
    }

    private fun field(value: String, hintText: String, type: Int): EditText {
        val view = EditText(this)
        view.setText(value)
        view.hint = hintText
        // Une ligne AVANT le type : isSingleLine remplace le masquage des mots de passe
        view.isSingleLine = true
        view.inputType = type
        view.setTextColor(Color.WHITE)
        view.setHintTextColor(0xFF7F9BC4.toInt())
        return view
    }

    private fun button(s: String, action: () -> Unit): Button {
        val view = Button(this)
        view.text = s
        view.isAllCaps = false
        view.setOnClickListener { action() }
        return view
    }

    // ---- Réveils ----

    private lateinit var alarmList: LinearLayout

    private fun refreshAlarms() {
        alarmList.removeAllViews()
        val alarms = Alarms.list(this)
        if (alarms.isEmpty()) alarmList.addView(label(tr(R.string.alarm_empty)))
        for (alarm in alarms) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = android.view.Gravity.CENTER_VERTICAL
            val check = CheckBox(this)
            check.isChecked = alarm.enabled
            check.setTextColor(Color.WHITE)
            check.textSize = 18f
            check.text = buildString {
                append(alarm.time).append("  ·  ").append(Alarms.daysLabel(alarm.days))
                if (alarm.label.isNotBlank()) append("  ·  ").append(alarm.label)
                if (alarm.sunrise) append("  ☀")
            }
            check.setOnCheckedChangeListener { _, on ->
                val all = Alarms.list(this)
                all.firstOrNull { it.id == alarm.id }?.enabled = on
                Alarms.save(this, all)
                ApiService.instance?.reportNextAlarm()
            }
            row.addView(check, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            val delete = Button(this)
            delete.text = "✕"
            delete.setOnClickListener {
                Alarms.save(this, Alarms.list(this).filterNot { it.id == alarm.id })
                ApiService.instance?.reportNextAlarm()
                refreshAlarms()
            }
            row.addView(delete)
            alarmList.addView(row)
        }
    }

    private fun addAlarm() {
        android.app.TimePickerDialog(this, { _, hour, minute ->
            val names = tr(R.string.alarm_day_names_long).split(",").toTypedArray()
            val chosen = BooleanArray(7) { it < 5 }
            AlertDialog.Builder(this)
                .setTitle(tr(R.string.alarm_days_title, "%02d:%02d".format(hour, minute)))
                .setMultiChoiceItems(names, chosen) { _, which, on -> chosen[which] = on }
                .setPositiveButton(tr(R.string.alarm_next_step)) { _, _ ->
                    var days = 0
                    chosen.forEachIndexed { i, on -> if (on) days = days or (1 shl i) }
                    val nameField = EditText(this)
                    nameField.hint = tr(R.string.alarm_name_hint)
                    val sunrise = CheckBox(this)
                    sunrise.text = tr(R.string.alarm_sunrise, Alarms.SUNRISE_MINUTES)
                    val box = LinearLayout(this)
                    box.orientation = LinearLayout.VERTICAL
                    box.setPadding(dp(20), dp(8), dp(20), 0)
                    box.addView(nameField)
                    box.addView(sunrise)
                    AlertDialog.Builder(this)
                        .setTitle(tr(R.string.alarm_add))
                        .setView(box)
                        .setPositiveButton("OK") { _, _ ->
                            Alarms.add(this, hour, minute, days, nameField.text.toString().trim(), sunrise.isChecked)
                            ApiService.instance?.reportNextAlarm()
                            refreshAlarms()
                        }
                        .setNegativeButton(tr(R.string.cancel), null)
                        .show()
                }
                .setNegativeButton(tr(R.string.cancel), null)
                .show()
        }, 7, 0, true).show()
    }

    companion object {
        private const val REQUEST_PHOTO_FOLDER = 54 // 51 et 52 : sauvegarde (SettingsBackup)
        private const val REQUEST_BLE = 53
        /** Repère des titres de section (captures du tutoriel). */
        const val HEADING_TAG = "settings_heading"
    }
}
