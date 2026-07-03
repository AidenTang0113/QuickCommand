package com.quickcommand.data

import com.quickcommand.model.Command
import kotlinx.coroutines.flow.Flow

class CommandRepository(private val dao: CommandDao) {

    val allCommands: Flow<List<Command>> = dao.getAllCommands()

    val enabledCommands: Flow<List<Command>> = dao.getEnabledCommands()

    suspend fun getCommand(id: Long): Command? = dao.getCommandById(id)

    suspend fun insert(command: Command): Long = dao.insertCommand(command)

    suspend fun update(command: Command) = dao.updateCommand(command)

    suspend fun delete(command: Command) = dao.deleteCommand(command)

    suspend fun deleteById(id: Long) = dao.deleteCommandById(id)

    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setCommandEnabled(id, enabled)
}
