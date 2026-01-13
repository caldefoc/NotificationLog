package com.notificationlog

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.notificationlog.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    
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
        
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        
        setupUI()
        checkPermissions()
        observeViewModel()
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
        }
        
        viewModel.notificationCount.observe(this) { count ->
            binding.tvNotificationCount.text = getString(R.string.notification_count, count)
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