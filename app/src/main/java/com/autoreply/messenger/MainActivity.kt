package com.autoreply.messenger

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.autoreply.messenger.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: SettingsManager
    private lateinit var database: ReplyDatabase
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settings = SettingsManager(this)
        database = ReplyDatabase(this)
        historyAdapter = HistoryAdapter()

        setupNavigation()
        setupDashboard()
        setupSettings()
        setupHistory()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        updateStats()
    }

    // ===================== ĐIỀU HƯỚNG TAB =====================

    private fun setupNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> showSection(0)
                R.id.nav_settings -> showSection(1)
                R.id.nav_history -> showSection(2)
            }
            true
        }
    }

    private fun showSection(index: Int) {
        binding.dashboardSection.visibility = if (index == 0) View.VISIBLE else View.GONE
        binding.settingsSection.visibility = if (index == 1) View.VISIBLE else View.GONE
        binding.historySection.visibility = if (index == 2) View.VISIBLE else View.GONE

        if (index == 2) refreshHistory()
        if (index == 0) updateStats()
    }

    // ===================== TAB TỔNG QUAN =====================

    private fun setupDashboard() {
        // Toggle chính
        binding.masterToggle.isChecked = settings.isEnabled()
        updateToggleVisual()

        binding.masterToggle.setOnCheckedChangeListener { _, isChecked ->
            settings.setEnabled(isChecked)
            updateToggleVisual()
            updateStats()

            if (isChecked) {
                startKeepAlive()
                requestBatteryOptimization()
            } else {
                stopKeepAlive()
            }
        }

        // Nút cấp quyền thông báo
        binding.btnGrantPermission.setOnClickListener {
            openNotificationAccessSettings()
        }

        // Nút bỏ tối ưu pin
        binding.btnBatteryOpt.setOnClickListener {
            requestBatteryOptimization()
        }
    }

    private fun updateToggleVisual() {
        val enabled = settings.isEnabled()
        binding.masterToggle.isChecked = enabled
        binding.toggleCard.isActivated = enabled
        binding.tvToggleStatus.text = if (enabled) "Đang hoạt động" else "Đã tắt"
    }

    private fun updatePermissionStatus() {
        // Kiểm tra quyền nghe thông báo
        val granted = isNotificationAccessGranted()
        binding.btnGrantPermission.visibility = if (granted) View.GONE else View.VISIBLE
        binding.tvPermStatus.text = if (granted) "Quyền thông báo: Đã cấp" else "Quyền thông báo: Chưa cấp"
        binding.tvPermStatus.setTextColor(
            if (granted) getColor(R.color.accent) else getColor(R.color.danger)
        )

        // Kiểm tra tối ưu pin
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        val ignoring = pm.isIgnoringBatteryOptimizations(packageName)
        binding.btnBatteryOpt.visibility = if (ignoring) View.GONE else View.VISIBLE
        binding.tvBatteryStatus.text = if (ignoring) "Tối ưu pin: Đã bỏ qua" else "Tối ưu pin: Cần bỏ qua"
        binding.tvBatteryStatus.setTextColor(
            if (ignoring) getColor(R.color.accent) else getColor(R.color.warning)
        )
    }

    private fun updateStats() {
        binding.tvTodayCount.text = database.getTodayCount().toString()
        binding.tvTotalCount.text = database.getTotalCount().toString()
    }

    private fun isNotificationAccessGranted(): Boolean {
        val cn = ComponentName(this, AutoReplyService::class.java)
        val flat = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        return flat?.contains(cn.flattenToString()) == true
    }

    private fun openNotificationAccessSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }

    private fun requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    // Fallback: mở cài đặt pin
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    startActivity(intent)
                }
            }
        }
    }

    private fun startKeepAlive() {
        val intent = Intent(this, KeepAliveService::class.java).apply {
            action = KeepAliveService.ACTION_ENABLE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopKeepAlive() {
        val intent = Intent(this, KeepAliveService::class.java).apply {
            action = KeepAliveService.ACTION_DISABLE
        }
        startService(intent)
    }

    // ===================== TAB CÀI ĐẶT =====================

    private fun setupSettings() {
        // Tin nhắn phản hồi
        binding.etReplyMessage.setText(settings.getReplyMessage())
        binding.tvCharCount.text = "${settings.getReplyMessage().length} ký tự"
        binding.etReplyMessage.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val msg = binding.etReplyMessage.text.toString().trim()
                if (msg.isNotEmpty()) {
                    settings.setReplyMessage(msg)
                }
            }
        }

        // Độ trễ
        binding.sliderDelay.value = (settings.getDelayMs() / 1000f)
        updateDelayLabel(settings.getDelayMs())
        binding.sliderDelay.addOnChangeListener { _, value, _ ->
            val ms = (value * 1000).toLong()
            settings.setDelayMs(ms)
            updateDelayLabel(ms)
        }

        // Giới hạn tốc độ
        binding.sliderRateLimit.value = (settings.getRateLimitMs() / 1000f / 10f).coerceIn(0f, 30f)
        updateRateLimitLabel(settings.getRateLimitMs())
        binding.sliderRateLimit.addOnChangeListener { _, value, _ ->
            val ms = (value * 10000).toLong()
            settings.setRateLimitMs(ms)
            updateRateLimitLabel(ms)
        }

        // Phản hồi nhóm
        binding.switchGroupReply.isChecked = settings.isGroupReplyEnabled()
        binding.switchGroupReply.setOnCheckedChangeListener { _, isChecked ->
            settings.setGroupReplyEnabled(isChecked)
        }

        // Chế độ lọc liên hệ
        setupContactFilter()
    }

    private fun updateDelayLabel(ms: Long) {
        val seconds = ms / 1000.0
        binding.tvDelayValue.text = if (seconds < 1) "Ngay lập tức" else "${String.format("%.1f", seconds)} giây"
    }

    private fun updateRateLimitLabel(ms: Long) {
        val seconds = ms / 1000.0
        binding.tvRateLimitValue.text = if (seconds < 10) "Tối thiểu" else "${String.format("%.0f", seconds)} giây"
    }

    // ===================== LỌC LIÊN HỆ =====================

    private fun setupContactFilter() {
        // Cập nhật radio buttons
        when (settings.getFilterMode()) {
            "all" -> binding.radioAll.isChecked = true
            "whitelist" -> binding.radioWhitelist.isChecked = true
            "blacklist" -> binding.radioBlacklist.isChecked = true
        }

        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.radioWhitelist -> "whitelist"
                R.id.radioBlacklist -> "blacklist"
                else -> "all"
            }
            settings.setFilterMode(mode)
            updateContactListVisibility(mode)
        }

        updateContactListVisibility(settings.getFilterMode())
        refreshContactList()

        // Nút thêm liên hệ
        binding.btnAddContact.setOnClickListener {
            showAddContactDialog()
        }
    }

    private fun updateContactListVisibility(mode: String) {
        binding.contactListContainer.visibility =
            if (mode == "all") View.GONE else View.VISIBLE
        binding.tvFilterHint.text = when (mode) {
            "whitelist" -> "Chỉ phản hồi những người trong danh sách này"
            "blacklist" -> "Không phản hồi những người trong danh sách này"
            else -> "Phản hồi tất cả mọi người"
        }
    }

    private fun refreshContactList() {
        val contacts = settings.getContactList()
        binding.contactListLayout.removeAllViews()

        if (contacts.isEmpty()) {
            val tv = android.widget.TextView(this).apply {
                text = "Chưa có liên hệ nào"
                setTextColor(getColor(R.color.text_secondary))
                textSize = 14f
                setPadding(0, 16, 0, 16)
            }
            binding.contactListLayout.addView(tv)
        } else {
            contacts.forEach { name ->
                val itemView = layoutInflater.inflate(
                    R.layout.dialog_add_contact, binding.contactListLayout, false
                )
                itemView.findViewById<TextView>(R.id.tvContactName).text = name
                itemView.findViewById<View>(R.id.btnRemoveContact).setOnClickListener {
                    settings.removeContact(name)
                    refreshContactList()
                    Toast.makeText(this, "Đã xóa $name", Toast.LENGTH_SHORT).show()
                }
                binding.contactListLayout.addView(itemView)
            }
        }
    }

    private fun showAddContactDialog() {
        val input = android.widget.EditText(this).apply {
            hint = "Nhập tên liên hệ (giống trên Messenger)"
            setPadding(48, 32, 48, 32)
            textSize = 16f
        }

        AlertDialog.Builder(this)
            .setTitle("Thêm liên hệ")
            .setMessage("Nhập chính xác tên người dùng trên Messenger")
            .setView(input)
            .setPositiveButton("Thêm") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    settings.addContact(name)
                    refreshContactList()
                    Toast.makeText(this, "Đã thêm $name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    // ===================== TAB LỊCH SỬ =====================

    private fun setupHistory() {
        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = historyAdapter
        }

        binding.btnClearHistory.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Xóa lịch sử")
                .setMessage("Bạn có chắc muốn xóa toàn bộ lịch sử phản hồi?")
                .setPositiveButton("Xóa") { _, _ ->
                    database.clearHistory()
                    refreshHistory()
                    Toast.makeText(this, "Đã xóa lịch sử", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }

    private fun refreshHistory() {
        val history = database.getAllHistory()
        historyAdapter.setData(history)

        binding.emptyState.visibility = if (history.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerViewHistory.visibility = if (history.isEmpty()) View.GONE else View.VISIBLE
    }

    // ===================== LƯU CÀI ĐẶT =====================

    override fun onPause() {
        super.onPause()
        // Lưu tin nhắn phản hồi khi rời màn hình
        val msg = binding.etReplyMessage.text.toString().trim()
        if (msg.isNotEmpty()) {
            settings.setReplyMessage(msg)
        }
    }
}