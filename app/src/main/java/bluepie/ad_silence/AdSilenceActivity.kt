package bluepie.ad_silence

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Html.fromHtml
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat

class AdSilenceActivity : Activity() {

    private val TAG = "MainActivity"
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 6969

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ad_silence_activity)
        configurePermission()
        configureToggle()
        configureAdditionalViews()
    }

    override fun onResume() {
        super.onResume()
        Log.v(TAG, "onResume")
        // when resuming after permission is granted
        configurePermission()
        configureToggle()
        configureAdditionalViews()
    }

    private fun configurePermission() {
        notificationListenerPermission()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // android 13 and above
            notificationPostingPermission()
            decideAndActivePostingPermissionRequest()
        } else {
            // hide the notifications for android 13
            findViewById<Switch>(R.id.enable_notifications_switch)?.run {
                this.visibility = View.GONE
            }

            // hide for requesting permission for notification posting
            setNotificationPostingRequestPermission(false)

        }

    }


    private fun notificationListenerPermission() {
        // for notification listener
        findViewById<Button>(R.id.grant_permission)?.run {
            when (checkNotificationListenerPermission(applicationContext) ) {
                true -> {
                    this.text = getString(R.string.permission_granted)
                    this.isEnabled = false
                }
                false -> {
                    findViewById<Switch>(R.id.status_toggle)?.text =
                        getString(R.string.app_status_permission_not_granted)
                    this.isEnabled = true
                    this.setOnClickListener {
                        Log.v(TAG, "Opening Notification Settings")
                        startActivity(Intent(getString(R.string.notification_listener_settings_intent)))
                    }
                }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun decideAndActivePostingPermissionRequest() {
        val activity = this
        val preference = Preference(applicationContext)


        findViewById<Switch>(R.id.enable_notifications_switch)?.run {
            this.isChecked = preference.isNotificationsEnabled()

            this.setOnClickListener {
                val changedState = !preference.isNotificationsEnabled()
                this.isChecked = changedState
                preference.setNotificationEnabled(changedState)
                setNotificationPostingRequestPermission(changedState)

                // remove any existing notifications
                // forground service notifications should be cancelled from service directly
                Log.v(TAG, "[configNotifications] removing any existing notifications")
                (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
            }

            // setup views for notification settings
            setNotificationPostingRequestPermission(preference.isNotificationsEnabled())
        }

        findViewById<Button>(R.id.grant_notification_posting_perimisison)?.run {

            if (preference.isNotificationPostingPermissionGranted()) {
                this.text = getString(R.string.granted)
                this.isEnabled = false
            }

            this.setOnClickListener {
                // todo
                // show alert dialog for information
                when (preference.isNotificationPermissionRequested()) {
                    true -> {
                        // launch permission only if it is not triggered for the user already
                        navigateToSettingsPageToGrantNotificationPostingPermission()
                    }
                    false -> {
                        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
                    }
                }

            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun notificationPostingPermission() {
        //todo if greater than android 13, show button below grant permission showing grant notification posting permission.
        //     clicking on it will tirgger this code, if user cancels, launch settings directly and ask them to grant the permission.

        val preference = Preference(applicationContext)
        Log.v(TAG, "[permission][isAlreadyRequestedNotificationPosting] -> " + preference.isNotificationPermissionRequested())
        Log.v(TAG, "[permission][isGrantedPostingPermission] -> " + preference.isNotificationPostingPermissionGranted())

        if (checkNotificationPostingPermission(applicationContext)){
            Log.v(TAG, "[permission][notification][permissionGranted]")
            preference.setNotificationPostingPermission(true)
            return
        } else {
            Log.v(TAG, "[permission][notification][permissionDenied]")
            preference.setNotificationPostingPermission(false)
        }
        if (preference.isNotificationPermissionRequested()) {
            Log.v(TAG, "[permission] notification permission already requested,user denied")
            return
        }
        Log.v(TAG, "[permission] notification permission not granted")
    }

    private fun configureToggle() {
        val preference = Preference(applicationContext)
        val statusToggle = findViewById<Switch>(R.id.status_toggle)
        val appNotificationHelper = AppNotificationHelper(applicationContext)
        val utils = Utils()

        if (!checkNotificationListenerPermission(applicationContext)) {
            // even if appNotification is disabled, while granting permission
            //   force it to be enabled, otherwise it wont be listed in permission window
            appNotificationHelper.enable()
            utils.disableSwitch(statusToggle)
            statusToggle.text = getString(R.string.app_status_permission_not_granted)
            return
        } else {
            statusToggle.text = getString(R.string.app_status)
            utils.enableSwitch(statusToggle)
        }

        statusToggle.setOnClickListener {
            val toChange: Boolean = !preference.isEnabled()
            preference.setEnabled(toChange)
            if (toChange) appNotificationHelper.enable() else appNotificationHelper.disable()

        }

        if (preference.isEnabled()) {
            statusToggle.isChecked = true
            appNotificationHelper.start()
        } else {
            statusToggle.isChecked = false
        }

    }

    @SuppressLint("InflateParams")
    private fun configureAdditionalViews() {

        val utils = Utils()
        val dpi = resources.displayMetrics.density
        val isAccuradioInstalled = utils.isAccuradioInstalled(applicationContext)
        val isSpotifyInstalled = utils.isSpotifyInstalled(applicationContext)
        val isTidalInstalled = utils.isTidalInstalled(applicationContext)
        val isSpotifyLiteInstalled = utils.isSpotifyLiteInstalled(applicationContext)
        val isPandoraInstalled = utils.isPandoraInstalled(applicationContext)
        val isLiveOneInstalled = utils.isLiveOneInstalled(applicationContext)

        findViewById<Button>(R.id.about_btn)?.setOnClickListener {
            layoutInflater.inflate(R.layout.about, null)?.run {
                this.findViewById<Button>(R.id.github_button).setOnClickListener {
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.github_link))
                    ).run { startActivity(this) }
                }

                this.findViewById<Button>(R.id.report_issue_btn)?.setOnClickListener {
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(context.getString(R.string.github_issues_link))
                    ).run { startActivity(this) }
                }

                this.findViewById<TextView>(R.id.about_text_view).also {
                    it.movementMethod = LinkMovementMethod.getInstance()
                    it.text = fromHtml(getString(R.string.about_window_text))
                }

                with(AlertDialog.Builder(this@AdSilenceActivity).setView(this)) {
                    val linearLayout = LinearLayout(context).also {
                        it.setPadding(0, 16 * dpi.toInt(), 0, 0)
                        it.gravity = Gravity.CENTER
                        it.addView(
                            ImageView(context).also { imageView ->
                                imageView.layoutParams =
                                    ViewGroup.LayoutParams(56 * dpi.toInt(), 56 * dpi.toInt())
                                imageView.setBackgroundResource(R.mipmap.ic_launcher_round)
                            }
                        )
                        it.addView(
                            TextView(context).also { textView ->
                                textView.text = getString(R.string.app_name)
                                textView.setPadding(8 * dpi.toInt(), 0, 0, 0)
                                textView.textSize = 6 * dpi
                            }
                        )
                    }
                    this.setTitle(getString(R.string.app_name))
                        .setIcon(R.mipmap.ic_launcher_round)
                    this.setCustomTitle(linearLayout)
                    this.show()
                }
            }
        }


        findViewById<Button>(R.id.select_apps_btn)?.setOnClickListener {
            val appSelectionView = layoutInflater.inflate(R.layout.app_selection, null)
            val preference = Preference(applicationContext)

            appSelectionView.findViewById<Switch>(R.id.accuradio_selection_switch)?.run {
                this.isEnabled = isAccuradioInstalled
                this.isChecked = preference.isAppConfigured(SupportedApps.ACCURADIO)
                "${getString(R.string.accuradio)} ${
                    if (isAccuradioInstalled) "" else context.getString(
                        R.string.not_installed
                    )
                }".also { this.text = it }
                this.setOnClickListener {
                    preference.setAppConfigured(
                        SupportedApps.ACCURADIO,
                        !preference.isAppConfigured(SupportedApps.ACCURADIO)
                    )
                }
            }

            appSelectionView.findViewById<Switch>(R.id.spotify_selection_switch)?.run {
                this.isEnabled = isSpotifyInstalled
                this.isChecked = preference.isAppConfigured(SupportedApps.SPOTIFY)
                "${getString(R.string.spotify)} ${
                    if (isSpotifyInstalled) "" else context.getString(
                        R.string.not_installed
                    )
                }".also {
                    this.text = it
                }
                this.setOnClickListener {
                    preference.setAppConfigured(
                        SupportedApps.SPOTIFY,
                        !preference.isAppConfigured(SupportedApps.SPOTIFY)
                    )
                }
            }

            appSelectionView.findViewById<Switch>(R.id.tidal_selection_switch)?.run {
                this.isEnabled = isTidalInstalled
                this.isChecked = preference.isAppConfigured(SupportedApps.TIDAL)
                "${context.getString(R.string.tidal)} ${
                    if (isTidalInstalled) "" else context.getString(
                        R.string.not_installed
                    )
                }".also {
                    this.text = it
                }
                this.setOnClickListener {
                    preference.setAppConfigured(
                        SupportedApps.TIDAL,
                        !preference.isAppConfigured(SupportedApps.TIDAL)
                    )
                }
            }

            appSelectionView.findViewById<Switch>(R.id.spotify_lite_selection_switch)?.run {
                this.isEnabled = isSpotifyLiteInstalled
                this.isChecked = preference.isAppConfigured(SupportedApps.SPOTIFY_LITE)
                "${context.getString(R.string.spotify_lite)} ${
                    if (isSpotifyLiteInstalled) "" else context.getString(
                        R.string.not_installed
                    )
                }".also {
                    this.text = it
                }
                this.setOnClickListener {
                    preference.setAppConfigured(
                        SupportedApps.SPOTIFY_LITE,
                        !preference.isAppConfigured(SupportedApps.SPOTIFY_LITE)
                    )
                }
            }

            appSelectionView.findViewById<Switch>(R.id.pandora_selection_switch)?.run {
                this.isEnabled = isPandoraInstalled
                this.isChecked = preference.isAppConfigured(SupportedApps.PANDORA)
                "${context.getString(R.string.pandora)} ${
                    if (isPandoraInstalled) applicationContext.getString(R.string.beta) else context.getString(R.string.not_installed)
                }".also {
                    this.text = it
                }
                this.setOnClickListener {
                    preference.setAppConfigured(
                        SupportedApps.PANDORA,
                        !preference.isAppConfigured(SupportedApps.PANDORA)
                    )
                }
            }

            appSelectionView.findViewById<Switch>(R.id.liveone_selection_switch)?.run {
                this.isEnabled = isLiveOneInstalled
                this.isChecked = preference.isAppConfigured(SupportedApps.LiveOne)
                "${context.getString(R.string.liveone)} ${
                    if (isLiveOneInstalled) applicationContext.getString(R.string.beta) else context.getString(R.string.not_installed)
                }".also {
                    this.text = it
                }
                this.setOnClickListener{
                    preference.setAppConfigured(
                        SupportedApps.LiveOne,
                        !preference.isAppConfigured(SupportedApps.LiveOne)
                    )
                }
            }

            AlertDialog.Builder(this).setView(appSelectionView).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val preference = Preference(applicationContext)

        when(requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v(TAG, "[permission] permission granted in dialog")
                    preference.setNotificationPostingPermission(true)
                } else {
                    Log.v(TAG, "[permission] permission not granted in dialog")
                    preference.setNotificationPostingPermission(false)
                }
                preference.setNotificationPermissionRequested(true)
            }

        }
    }

    private fun setNotificationPostingRequestPermission(state: Boolean) {
        val status = if (state) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.notification_posting_permission_text_view)?.run {
            this.visibility = status
        }

        findViewById<Button>(R.id.grant_notification_posting_perimisison)?.run {
            this.visibility = status
        }
    }

    private fun navigateToSettingsPageToGrantNotificationPostingPermission() {
        val intent = Intent()
        val uri = Uri.fromParts("package", packageName, null)
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.data = uri
        startActivity(intent)
        finish()
    }

    private fun postANotificationA13() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // checking if notification is not present.. start it
            val mNotificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val notifications  = mNotificationManager.activeNotifications
            Log.v(TAG, "[notification-a13][count]" + notifications.size)
            if(notifications.isEmpty()) {
             AppNotificationHelper(applicationContext).updateNotification("AdSilence, listening for ads")
            }
        }
    }
}



