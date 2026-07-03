package com.quickcommand

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.quickcommand.databinding.ActivityAddEditCommandBinding
import com.quickcommand.model.ActionType
import com.quickcommand.model.Command
import com.quickcommand.model.CommandConverters
import com.quickcommand.model.GestureType
import com.quickcommand.viewmodel.CommandViewModel
import kotlinx.coroutines.launch

class AddEditCommandActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditCommandBinding
    private lateinit var viewModel: CommandViewModel
    private var editingCommand: Command? = null
    private var selectedGesture: GestureType = GestureType.CIRCLE
    private var selectedAction: ActionType = ActionType.OPEN_APP
    private var customGestureJson: String? = null

    companion object {
        const val REQUEST_GESTURE_CAPTURE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditCommandBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(CommandViewModel::class.java)

        setupSpinners()
        setupListeners()
        loadCommandIfEditing()
    }

    private fun setupSpinners() {
        // 手势类型下拉
        val gestureNames = GestureType.entries.map { it.displayName }
        binding.spGesture.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, gestureNames
        )

        binding.spGesture.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                selectedGesture = GestureType.entries[pos]
                binding.btnCaptureGesture.visibility =
                    if (selectedGesture == GestureType.CUSTOM) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 动作类型下拉
        val actionNames = ActionType.entries.map { it.displayName }
        binding.spAction.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, actionNames
        )

        binding.spAction.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                selectedAction = ActionType.entries[pos]
                updateActionParamHint()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateActionParamHint() {
        binding.tilActionParam.hint = when (selectedAction) {
            ActionType.OPEN_APP -> "应用包名 (如 com.tencent.mm)"
            ActionType.SET_REMINDER -> "延迟毫秒数 (如 5000 = 5秒)"
            ActionType.OPEN_WEBSITE -> "网址 (如 baidu.com)"
            else -> ""
        }
        binding.tilActionParam.visibility = when (selectedAction) {
            ActionType.OPEN_APP, ActionType.SET_REMINDER, ActionType.OPEN_WEBSITE -> View.VISIBLE
            else -> View.GONE
        }
    }

    private fun setupListeners() {
        binding.btnCaptureGesture.setOnClickListener {
            val intent = Intent(this, GestureCaptureActivity::class.java)
            intent.putExtra("capture_mode", true)
            startActivityForResult(intent, REQUEST_GESTURE_CAPTURE)
        }

        binding.btnPickApp.setOnClickListener {
            pickInstalledApp()
        }

        binding.btnSave.setOnClickListener { saveCommand() }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun pickInstalledApp() {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val apps = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)

        val appNames = apps.map { it.loadLabel(packageManager).toString() }.toTypedArray()
        val appPackages = apps.map { it.activityInfo.packageName }.toTypedArray()

        android.app.AlertDialog.Builder(this)
            .setTitle("选择应用")
            .setItems(appNames) { _, which ->
                binding.etActionParam.setText(appPackages[which])
            }
            .show()
    }

    private fun loadCommandIfEditing() {
        val commandId = intent.getLongExtra("command_id", -1)
        if (commandId == -1L) return

        binding.toolbar.title = "编辑命令"
        lifecycleScope.launch {
            viewModel.getCommand(commandId)?.let { cmd ->
                editingCommand = cmd
                binding.etName.setText(cmd.name)
                binding.etActionParam.setText(cmd.actionParam ?: "")

                val gestureIndex = GestureType.entries.indexOf(cmd.gestureType)
                if (gestureIndex >= 0) binding.spGesture.setSelection(gestureIndex)

                val actionIndex = ActionType.entries.indexOf(cmd.actionType)
                if (actionIndex >= 0) binding.spAction.setSelection(actionIndex)

                customGestureJson = cmd.customGesturePoints
            }
        }
    }

    private fun saveCommand() {
        val name = binding.etName.text.toString().trim()
        if (name.isEmpty()) {
            binding.etName.error = "请输入命令名称"
            return
        }
        if (selectedGesture == GestureType.CUSTOM && customGestureJson.isNullOrBlank()) {
            Snackbar.make(binding.root, "请先录制自定义手势", Snackbar.LENGTH_SHORT).show()
            return
        }

        val actionParam: String? = when (selectedAction) {
            ActionType.OPEN_APP, ActionType.SET_REMINDER, ActionType.OPEN_WEBSITE ->
                binding.etActionParam.text?.toString()?.trim()?.ifBlank { null }
            else -> null
        }

        val command = Command(
            id = editingCommand?.id ?: 0,
            name = name,
            gestureType = selectedGesture,
            customGesturePoints = if (selectedGesture == GestureType.CUSTOM) customGestureJson else null,
            actionType = selectedAction,
            actionParam = actionParam,
            isEnabled = editingCommand?.isEnabled ?: true
        )

        if (editingCommand != null) {
            viewModel.update(command)
            Toast.makeText(this, "命令已更新", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.insert(command)
            Toast.makeText(this, "命令已创建", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_GESTURE_CAPTURE && resultCode == RESULT_OK) {
            customGestureJson = data?.getStringExtra("gesture_points_json")
            binding.btnCaptureGesture.text = "✓ 手势已录制"
        }
    }
}
