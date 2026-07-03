package com.quickcommand.data

import androidx.room.*
import com.quickcommand.model.Command
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandDao {

    @Query("SELECT * FROM commands ORDER BY createdAt DESC")
    fun getAllCommands(): Flow<List<Command>>

    @Query("SELECT * FROM commands WHERE isEnabled = 1 ORDER BY createdAt DESC")
    fun getEnabledCommands(): Flow<List<Command>>

    @Query("SELECT * FROM commands WHERE id = :id")
    suspend fun getCommandById(id: Long): Command?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: Command): Long

    @Update
    suspend fun updateCommand(command: Command)

    @Delete
    suspend fun deleteCommand(command: Command)

    @Query("DELETE FROM commands WHERE id = :id")
    suspend fun deleteCommandById(id: Long)

    @Query("UPDATE commands SET isEnabled = :enabled WHERE id = :id")
    suspend fun setCommandEnabled(id: Long, enabled: Boolean)
}
