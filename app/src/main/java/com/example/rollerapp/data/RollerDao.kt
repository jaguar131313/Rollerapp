package com.example.rollerapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RollerDao {
    // Conveyors
    @Query("SELECT * FROM conveyors")
    fun getAllConveyors(): Flow<List<Conveyor>>

    @Query("SELECT * FROM conveyors WHERE name = :name LIMIT 1")
    suspend fun getConveyorByName(name: String): Conveyor?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConveyor(conveyor: Conveyor)

    @Delete
    suspend fun deleteConveyor(conveyor: Conveyor)

    // Inspections
    @Query("SELECT * FROM inspections WHERE conveyor = :conveyorName ORDER BY timestamp DESC")
    fun getInspectionsByConveyor(conveyorName: String): Flow<List<Inspection>>

    @Query("SELECT * FROM inspections WHERE conveyor = :conveyorName")
    suspend fun getInspectionsByConveyorSync(conveyorName: String): List<Inspection>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInspection(inspection: Inspection)

    @Update
    suspend fun updateInspection(inspection: Inspection)

    @Delete
    suspend fun deleteInspection(inspection: Inspection)

    @Query("SELECT * FROM inspections WHERE conveyor = :conveyorName AND opora = :opora AND roller = :roller LIMIT 1")
    suspend fun findInspection(conveyorName: String, opora: Int, roller: String): Inspection?

    // Replacements
    @Query("SELECT * FROM replacements WHERE conveyor = :conveyorName ORDER BY timestamp DESC")
    fun getReplacementsByConveyor(conveyorName: String): Flow<List<Replacement>>

    @Query("SELECT * FROM replacements WHERE conveyor = :conveyorName")
    suspend fun getReplacementsByConveyorSync(conveyorName: String): List<Replacement>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplacement(replacement: Replacement)

    @Update
    suspend fun updateReplacement(replacement: Replacement)

    @Delete
    suspend fun deleteReplacement(replacement: Replacement)

    // Belt Journal
    @Query("SELECT * FROM belt_journal WHERE conveyor = :conveyorName ORDER BY timestamp DESC")
    fun getJournalByConveyor(conveyorName: String): Flow<List<BeltJournal>>

    @Query("SELECT * FROM belt_journal WHERE conveyor = :conveyorName")
    suspend fun getJournalByConveyorSync(conveyorName: String): List<BeltJournal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: BeltJournal)

    @Update
    suspend fun updateJournalEntry(entry: BeltJournal)

    @Delete
    suspend fun deleteJournalEntry(entry: BeltJournal)
}
