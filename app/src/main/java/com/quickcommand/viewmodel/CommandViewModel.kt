package com.quickcommand.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickcommand.data.AppDatabase
import com.quickcommand.data.CommandRepository
import com.quickcommand.model.Command
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class CommandViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CommandRepository

    val allCommands: StateFlow<List<Command>>
    val enabledCommands: StateFlow<List<Command>>

    init {
        val dao = AppDatabase.getInstance(application).commandDao()
        repository = CommandRepository(dao)

        allCommands = repository.allCommands
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        enabledCommands = repository.enabledCommands
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun insert(command: Command, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val id = repository.insert(command)
                android.util.Log.d("CommandViewModel", "insert success, id=$id, name=${command.name}, gesture=${command.gestureType}, action=${command.actionType}")
                onComplete(id)
            } catch (e: Exception) {
                android.util.Log.e("CommandViewModel", "insert FAILED", e)
            }
        }
    }

    fun update(command: Command) {
        viewModelScope.launch {
            repository.update(command)
        }
    }

    fun delete(command: Command) {
        viewModelScope.launch {
            repository.delete(command)
        }
    }

    fun setEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(id, enabled)
        }
    }

    suspend fun getAllCommandsDirect(): List<Command> {
        return allCommands.first()
    }

    suspend fun getCommand(id: Long): Command? = repository.getCommand(id)

    suspend fun findMatchByGesture(gestureName: String): Command? {
        val all = allCommands.value
        return all.find { it.gestureType.name == gestureName && it.isEnabled }
    }
}
