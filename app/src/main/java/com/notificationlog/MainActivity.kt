package com.notificationlog

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.widget.TooltipCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.notificationlog.adapter.NotificationAdapter
import com.notificationlog.databinding.ActivityMainBinding
import com.notificationlog.model.NotificationItem
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: NotificationAdapter
    private lateinit var prefs: SharedPreferences
    private var currentSort: SortType = SortType.DATE
    private var captureEnabled: Boolean = true
    private val notificationsUpdatedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            viewModel.loadNotifications(currentSort)
            val lastCaptured = intent.getLongExtra("last_captured", 0)
            if (lastCaptured > 0) {
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                binding.tvLastCaptured.text = getString(R.string.last_captured_label, sdf.format(Date(lastCaptured)))
            }
        }
    }
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            showPermissionDeniedDialog()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        prefs = getSharedPreferences("com.notificationlog.prefs", Context.MODE_PRIVATE)
        captureEnabled = prefs.getBoolean("capture_enabled", true)
        if (!captureEnabled) {
            captureEnabled = true
            prefs.edit().putBoolean("capture_enabled", true).apply()
        }
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setupRecyclerView()
        setupMenuBar()
        setupBottomButtons()
        checkPermissions()
        observeViewModel()
        
        createNotificationChannel()
        
        viewModel.loadNotifications(currentSort)
    }
    
    override fun onResume() {
        super.onResume()
        registerReceiver(
            notificationsUpdatedReceiver,
            IntentFilter("com.notificationlog.NOTIFICATIONS_UPDATED")
        )
        viewModel.checkNotificationServiceStatus()
        viewModel.loadNotifications(currentSort)
        updateServiceStatusText()
        updateButtonStates()
        checkRunningNotification()
    }
    
    override fun onPause() {
        super.onPause()
        unregisterReceiver(notificationsUpdatedReceiver)
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(
            onItemClick = { item -> showNotificationDetails(item) },
            onDeleteClick = { item -> showDeleteConfirmation(item) }
        )
        binding.notificationList.layoutManager = LinearLayoutManager(this)
        binding.notificationList.adapter = adapter
    }

    private fun setupMenuBar() {
        binding.menuFile.setOnClickListener { showFileMenu(it) }
        binding.menuSort.setOnClickListener { showSortMenu(it) }
        binding.menuSave.setOnClickListener { showSaveMenu(it) }
        binding.menuAbout.setOnClickListener { showAboutDialog() }
        binding.menuExit.setOnClickListener { showExitConfirmation() }
    }

    private fun showFileMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menu.add(0, R.id.menu_file_exit, 0, "Exit")
        popup.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.menu_file_exit) {
                showExitConfirmation()
                true
            } else false
        }
        popup.show()
    }

    private fun showSortMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menu.add(0, R.id.menu_sort_date, 0, "By Date")
        popup.menu.add(0, R.id.menu_sort_app, 0, "By App")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_sort_date -> {
                    currentSort = SortType.DATE
                    viewModel.loadNotifications(currentSort)
                    true
                }
                R.id.menu_sort_app -> {
                    currentSort = SortType.APP
                    viewModel.loadNotifications(currentSort)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showSaveMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menu.add(0, R.id.menu_save_screenshot, 0, "Save Screenshot")
        popup.menu.add(0, R.id.menu_save_text, 0, "Save Text Log")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_save_screenshot -> {
                    saveScreenshot()
                    true
                }
                R.id.menu_save_text -> {
                    saveTextLog()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun setupBottomButtons() {
        TooltipCompat.setTooltipText(binding.btnEnableService, "Enable Notification Access")
        TooltipCompat.setTooltipText(binding.btnRequestPermission, "Grant Permission")
        TooltipCompat.setTooltipText(binding.btnClear, "Clear All Notifications")
        TooltipCompat.setTooltipText(binding.btnCapture, "Start Capture")

        binding.btnEnableService.setOnClickListener {
            openNotificationAccessSettings()
        }
        
        binding.btnRequestPermission.setOnClickListener {
            showNotificationPermissionDialog()
        }
        
        binding.btnClear.setOnClickListener {
            showClearAllConfirmation()
        }

        binding.btnCapture.setOnClickListener {
            toggleCapture()
        }

        updateCaptureButton()
    }
    
    private fun updateServiceStatusText() {
        val isRunning = isNotificationServiceEnabled()
        binding.tvServiceStatus.text = if (isRunning) {
            if (captureEnabled) "Capture Running" else "Capture Stopped"
        } else {
            getString(R.string.service_stopped)
        }
    }

    private fun updateButtonStates() {
        val isServiceRunning = isNotificationServiceEnabled()
        val hasPermission = hasNotificationPermission()
        val hasNotifications = (viewModel.notifications.value?.size ?: 0) > 0

        // Service Button
        if (isServiceRunning) {
            binding.btnEnableService.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            TooltipCompat.setTooltipText(binding.btnEnableService, "Notification Access Enabled")
        } else {
            binding.btnEnableService.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            TooltipCompat.setTooltipText(binding.btnEnableService, "Enable Notification Access")
        }

        // Permission Button
        if (hasPermission) {
            binding.btnRequestPermission.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            TooltipCompat.setTooltipText(binding.btnRequestPermission, "Permission Granted")
        } else {
            binding.btnRequestPermission.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            TooltipCompat.setTooltipText(binding.btnRequestPermission, "Grant Permission")
        }

        // Clear Button
        if (hasNotifications) {
            binding.btnClear.isEnabled = true
            binding.btnClear.setColorFilter(ContextCompat.getColor(this, android.R.color.black))
        } else {
            binding.btnClear.isEnabled = false
            binding.btnClear.setColorFilter(ContextCompat.getColor(this, android.R.color.darker_gray))
        }

        updateCaptureButton()
    }

    private fun updateCaptureButton() {
        if (captureEnabled) {
            binding.btnCapture.setImageResource(android.R.drawable.ic_media_pause)
            binding.btnCapture.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            binding.tvCaptureLabel.text = "Stop"
            TooltipCompat.setTooltipText(binding.btnCapture, "Stop Capture")
        } else {
            binding.btnCapture.setImageResource(android.R.drawable.ic_media_play)
            binding.btnCapture.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            binding.tvCaptureLabel.text = "Start"
            TooltipCompat.setTooltipText(binding.btnCapture, "Start Capture")
        }
    }

    private fun toggleCapture() {
        captureEnabled = !captureEnabled
        prefs.edit().putBoolean("capture_enabled", captureEnabled).apply()
        updateCaptureButton()
        updateServiceStatusText()
        checkRunningNotification()
        Toast.makeText(
            this,
            if (captureEnabled) "Capture started" else "Capture stopped",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun checkPermissions() {
        if (!isNotificationServiceEnabled()) {
            showNotificationAccessDialog()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission()) {
                requestNotificationPermission()
            }
        }
    }
    
    private fun observeViewModel() {
        viewModel.serviceStatus.observe(this) { isRunning ->
            updateServiceStatusText()
            updateButtonStates()
            checkRunningNotification()
            if (!isRunning) {
                showNotificationAccessDialog()
            }
        }
        
        viewModel.notificationCount.observe(this) { count ->
            binding.tvNotificationCount.text = getString(R.string.notification_count, count)
            updateButtonStates()
        }

        viewModel.notifications.observe(this) { items ->
            adapter.submitList(items)
            updateButtonStates()
        }
    }
    
    private fun isNotificationServiceEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners?.contains(packageName) == true
    }
    
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun openNotificationAccessSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    
    private fun showNotificationAccessDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.enable_notification_access))
            .setMessage(getString(R.string.notification_access_required))
            .setPositiveButton(getString(R.string.open_settings)) { _, _ ->
                openNotificationAccessSettings()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showNotificationPermissionDialog() {
        if (hasNotificationPermission()) {
            AlertDialog.Builder(this)
                .setTitle("Notification Permission")
                .setMessage("Notification permission is already granted.")
                .setPositiveButton(android.R.string.ok, null)
                .show()
        } else {
            AlertDialog.Builder(this)
                .setTitle("Grant Notification Permission")
                .setMessage("This app needs notification permission to show its own notifications.")
                .setPositiveButton("Proceed") { _, _ ->
                    requestNotificationPermission()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
    
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_required))
            .setMessage(getString(R.string.post_notification_permission))
            .setPositiveButton(getString(R.string.grant_permission)) { _, _ ->
                requestNotificationPermission()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showNotificationDetails(item: NotificationItem) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val dateStr = dateFormat.format(Date(item.timestamp))
        
        val message = """
            App: ${item.appName}
            Package: ${item.packageName}
            Date: $dateStr
            
            Title: ${item.title}
            
            Content:
            ${item.content}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Notification Details")
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
    
    private fun showDeleteConfirmation(item: NotificationItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete Notification")
            .setMessage("Are you sure you want to delete this notification?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteNotification(item)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showClearAllConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Clear All")
            .setMessage("Are you sure you want to delete all notifications?")
            .setPositiveButton("Clear All") { _, _ ->
                viewModel.clearNotifications()
                Toast.makeText(this, "Notifications cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        
        // Set checked state for running notification
        val showRunning = prefs.getBoolean("show_running_notif", false)
        menu.findItem(R.id.menu_toggle_running_notif).isChecked = showRunning
        
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_toggle_theme -> {
                val isDarkMode = prefs.getBoolean("dark_mode", false)
                val newMode = !isDarkMode
                prefs.edit().putBoolean("dark_mode", newMode).apply()
                AppCompatDelegate.setDefaultNightMode(
                    if (newMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                )
                Toast.makeText(
                    this,
                    if (newMode) "Dark mode enabled" else "Light mode enabled",
                    Toast.LENGTH_SHORT
                ).show()
                recreate()
                true
            }
            R.id.menu_toggle_running_notif -> {
                val newState = !item.isChecked
                item.isChecked = newState
                prefs.edit().putBoolean("show_running_notif", newState).apply()
                checkRunningNotification()
                true
            }
            // Keep existing handling for completeness
            R.id.menu_sort_date -> {
                currentSort = SortType.DATE
                viewModel.loadNotifications(currentSort)
                true
            }
            R.id.menu_sort_app -> {
                currentSort = SortType.APP
                viewModel.loadNotifications(currentSort)
                true
            }
            R.id.menu_refresh -> {
                viewModel.loadNotifications(currentSort)
                true
            }
            R.id.menu_save_screenshot -> { saveScreenshot(); true }
            R.id.menu_save_text -> { saveTextLog(); true }
            R.id.menu_about -> { showAboutDialog(); true }
            R.id.menu_file_exit -> {
                showExitConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showExitConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Exit")
            .setMessage("Stop notification logging and exit?")
            .setPositiveButton("Exit") { _, _ ->
                captureEnabled = false
                prefs.edit().putBoolean("capture_enabled", false).apply()
                checkRunningNotification()
                finishAffinity()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveScreenshot() {
        val rootView = binding.root
        if (rootView.width == 0 || rootView.height == 0) return

        val bitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        rootView.draw(canvas)

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "notification_log_$timestamp.png"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/NotificationLog")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            try {
                contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                }
                Toast.makeText(this, "Screenshot saved to Pictures/NotificationLog", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error saving screenshot: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveTextLog() {
        val items = viewModel.notifications.value.orEmpty()
        if (items.isEmpty()) {
            Toast.makeText(this, "No notifications to save", Toast.LENGTH_SHORT).show()
            return
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "notification_log_$timestamp.txt"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/NotificationLog")
            }
            val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            if (uri != null) {
                writeTextToUri(uri, items)
                Toast.makeText(this, "Log saved to Documents/NotificationLog", Toast.LENGTH_LONG).show()
            }
        } else {
            // Legacy storage
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val subDir = File(dir, "NotificationLog")
            if (!subDir.exists()) subDir.mkdirs()
            val file = File(subDir, filename)
            try {
                java.io.FileOutputStream(file).use { out ->
                    writeTextToStream(out, items)
                }
                Toast.makeText(this, "Log saved to ${file.absolutePath}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error saving log: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun writeTextToUri(uri: Uri, items: List<NotificationItem>) {
        try {
            contentResolver.openOutputStream(uri)?.use { out ->
                writeTextToStream(out, items)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error writing log: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun writeTextToStream(out: OutputStream, items: List<NotificationItem>) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        items.forEach { n ->
            val time = dateFormat.format(Date(n.timestamp))
            val line = "[$time] ${n.appName} (${n.packageName}): ${n.title} - ${n.content}\n"
            out.write(line.toByteArray())
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("About")
            .setMessage("Notification Log\nVersion 1.0\n\nCaptures and displays your device notifications.")
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
    
    // Persistent Notification Logic
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Service Status"
            val descriptionText = "Shows when notification logging is active"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel("service_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun checkRunningNotification() {
        val show = prefs.getBoolean("show_running_notif", false)
        val isRunning = isNotificationServiceEnabled()
        val capture = prefs.getBoolean("capture_enabled", true)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (show && isRunning && capture) {
            val notification = NotificationCompat.Builder(this, "service_channel")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle("Notification Log capture is running...")
                .setContentText("Capturing notifications in background")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build()
            notificationManager.notify(1001, notification)
        } else {
            notificationManager.cancel(1001)
        }
    }
}
