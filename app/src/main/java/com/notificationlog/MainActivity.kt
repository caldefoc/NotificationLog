package com.notificationlog

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.notificationlog.databinding.ActivityMainBinding
import com.notificationlog.adapter.NotificationAdapter
import com.notificationlog.model.NotificationItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: NotificationAdapter
    private var currentSort: SortType = SortType.DATE
    
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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setupRecyclerView()
        setupUI()
        checkPermissions()
        observeViewModel()
        viewModel.loadNotifications(currentSort)
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.checkNotificationServiceStatus()
        viewModel.loadNotifications(currentSort)
        updatePermissionButtonText()
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter { item ->
            showNotificationDetails(item)
        }
        binding.notificationList.layoutManager = LinearLayoutManager(this)
        binding.notificationList.adapter = adapter
    }

    private fun setupUI() {
        binding.btnEnableService.setOnClickListener {
            openNotificationAccessSettings()
        }
        
        binding.btnRequestPermission.setOnClickListener {
            requestNotificationPermission()
        }
        
        binding.btnClear.setOnClickListener {
            viewModel.clearNotifications()
        }
        updatePermissionButtonText()
    }
    
    private fun checkPermissions() {
        // Check if notification listener is enabled
        if (!isNotificationServiceEnabled()) {
            showNotificationAccessDialog()
        }
        
        // Request POST_NOTIFICATIONS permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission()) {
                requestNotificationPermission()
            }
        }
    }
    
    private fun observeViewModel() {
        viewModel.serviceStatus.observe(this) { isRunning ->
            binding.tvServiceStatus.text = if (isRunning) {
                getString(R.string.service_running)
            } else {
                getString(R.string.service_stopped)
            }
            binding.btnEnableService.text = if (isRunning) {
                getString(R.string.notification_access_enabled)
            } else {
                getString(R.string.enable_notification_access)
            }
        }
        
        viewModel.notificationCount.observe(this) { count ->
            binding.tvNotificationCount.text = getString(R.string.notification_count, count)
        }

        viewModel.notifications.observe(this) { items ->
            adapter.submitList(items)
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

    private fun updatePermissionButtonText() {
        val hasPermission = hasNotificationPermission()
        binding.btnRequestPermission.text = if (hasPermission) {
            getString(R.string.notification_permission_granted)
        } else {
            getString(R.string.grant_permission)
        }
        binding.btnRequestPermission.isEnabled = !hasPermission
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

    private fun showNotificationDetails(item: NotificationItem) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(item.appName)
        val message = "${item.title}\n\n${item.content}"
        builder.setMessage(message)
        builder.setPositiveButton(android.R.string.ok, null)
        builder.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
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
            R.id.menu_save_screenshot -> {
                saveScreenshot()
                true
            }
            R.id.menu_save_text -> {
                saveTextLog()
                true
            }
            R.id.menu_about -> {
                showAboutDialog()
                true
            }
            R.id.menu_exit -> {
                finishAffinity()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveScreenshot() {
        val rootView = binding.root
        if (rootView.width == 0 || rootView.height == 0) return

        val bitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        rootView.draw(canvas)

        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (dir != null && (dir.exists() || dir.mkdirs())) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(dir, "notification_log_$timestamp.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Toast.makeText(this, "Screenshot saved to ${file.absolutePath}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveTextLog() {
        val items = viewModel.notifications.value.orEmpty()
        if (items.isEmpty()) {
            Toast.makeText(this, "No notifications to save", Toast.LENGTH_SHORT).show()
            return
        }

        val dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (dir != null && (dir.exists() || dir.mkdirs())) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(dir, "notification_log_$timestamp.txt")
            FileOutputStream(file).use { out ->
                items.forEach { n ->
                    val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(Date(n.timestamp))
                    val line = "[$time] ${n.appName}: ${n.title} - ${n.content}\n"
                    out.write(line.toByteArray())
                }
            }
            Toast.makeText(this, "Text log saved to ${file.absolutePath}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("About")
            .setMessage("Notification Log\nCaptures and displays your device notifications.")
            .setPositiveButton(android.R.string.ok, null)
            .show()
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
}
