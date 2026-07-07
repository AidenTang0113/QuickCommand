package com.quickcommand.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.quickcommand.databinding.ItemCommandBinding
import com.quickcommand.model.ActionType
import com.quickcommand.model.Command
import com.quickcommand.model.GestureType

class CommandAdapter(
    private val onToggle: (Command, Boolean) -> Unit,
    private val onEdit: (Command) -> Unit,
    private val onDelete: (Command) -> Unit
) : ListAdapter<Command, CommandAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCommandBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemCommandBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(command: Command) {
            binding.tvName.text = command.name
            binding.tvGesture.text = "手势: ${command.gestureType.displayName}"
            binding.tvAction.text = "动作: ${getActionDisplay(command)}"
            binding.swEnabled.isChecked = command.isEnabled

            binding.swEnabled.setOnCheckedChangeListener { _, isChecked ->
                onToggle(command, isChecked)
            }
            binding.root.setOnClickListener { onEdit(command) }
            binding.btnDelete.setOnClickListener { onDelete(command) }
        }
    }

    private fun getActionDisplay(command: Command): String {
        return when (command.actionType) {
            ActionType.OPEN_APP -> "打开 ${command.actionParam ?: "应用"}"
            ActionType.SET_REMINDER -> {
                val sec = (command.actionParam?.toLongOrNull() ?: 3000L) / 1000
                "提醒 (${sec}秒后)"
            }
            ActionType.OPEN_WEBSITE -> "打开 ${command.actionParam ?: "网页"}"
            else -> command.actionType.displayName
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<Command>() {
        override fun areItemsTheSame(old: Command, new: Command) = old.id == new.id
        override fun areContentsTheSame(old: Command, new: Command) = old == new
    }
}
