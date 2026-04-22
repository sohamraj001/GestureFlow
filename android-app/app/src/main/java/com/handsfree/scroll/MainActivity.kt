package com.handsfree.scroll

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.handsfree.scroll.databinding.ActivityMainBinding
import com.handsfree.scroll.services.CameraService
import com.handsfree.scroll.services.OverlayService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true) {
            checkOverlayPermission()
        } else {
            Toast.makeText(this, "Camera permission required for gesture detection", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showDisclaimer()
        setupUI()
    }

    private fun showDisclaimer() {
        AlertDialog.Builder(this)
            .setTitle("Accessibility Disclaimer")
            .setMessage("GestureFLOW uses Android Accessibility Services to perform swipe gestures on your behalf while you use Instagram or YouTube. We do not collect, store, or transmit any personal data.")
            .setPositiveButton("I Understand") { _, _ -> requestPermissions() }
            .setCancelable(false)
            .show()
    }

    private fun setupUI() {
        binding.btnStart.setOnClickListener { startSystem() }
        binding.btnStop.setOnClickListener { stopSystem() }

        binding.sensitivitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Map 0-100 to 0.30 - 0.05 (Inverse: high progress = low threshold = high sensitivity)
                val threshold = 0.30f - (progress / 100f * 0.25f)
                com.handsfree.scroll.utils.AppConfig.sensitivity = threshold
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun requestPermissions() {
        val permissions = arrayOf(Manifest.permission.CAMERA)
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            requestPermissionLauncher.launch(notGranted.toTypedArray())
        } else {
            checkOverlayPermission()
        }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this)
                .setTitle("Overlay Permission")
                .setMessage("To show the camera preview over other apps, please grant 'Display over other apps' permission.")
                .setPositiveButton("Settings") { _, _ ->
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    startActivity(intent)
                }
                .show()
        }
    }

    private fun startSystem() {
        if (!Settings.canDrawOverlays(this)) {
            checkOverlayPermission()
            return
        }

        binding.statusText.text = "AI ENGINE ACTIVE"
        binding.statusPulse.setBackgroundResource(R.drawable.status_dot_active)
        
        val pulse = AlphaAnimation(1.0f, 0.3f)
        pulse.duration = 1000
        pulse.repeatMode = Animation.REVERSE
        pulse.repeatCount = Animation.INFINITE
        binding.statusPulse.startAnimation(pulse)

        startService(Intent(this, CameraService::class.java))
        startService(Intent(this, OverlayService::class.java))
        
        Toast.makeText(this, "GestureFLOW Active", Toast.LENGTH_SHORT).show()
    }

    private fun stopSystem() {
        stopService(Intent(this, CameraService::class.java))
        stopService(Intent(this, OverlayService::class.java))
        
        binding.statusText.text = "SYSTEM STANDBY"
        binding.statusPulse.setBackgroundResource(R.drawable.status_dot_inactive)
        binding.statusPulse.clearAnimation()
        
        Toast.makeText(this, "System Disabled", Toast.LENGTH_SHORT).show()
    }
}
