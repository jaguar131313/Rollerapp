package com.example.rollerapp.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rollerapp.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(private val repository: ConveyorRepository) : ViewModel() {

    val conveyors = repository.allConveyors.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    private val _selectedConveyor = MutableStateFlow<String?>(null)
    val selectedConveyor = _selectedConveyor.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn = _isSignedIn.asStateFlow()

    val inspections = selectedConveyor.flatMapLatest { conveyor ->
        if (conveyor != null) repository.getInspections(conveyor) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val replacements = selectedConveyor.flatMapLatest { conveyor ->
        if (conveyor != null) repository.getReplacements(conveyor) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val journal = selectedConveyor.flatMapLatest { conveyor ->
        if (conveyor != null) repository.getJournal(conveyor) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        _isSignedIn.value = isUserSignedIn()
    }

    fun selectConveyor(name: String) {
        _selectedConveyor.value = name
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedDamages = MutableStateFlow<Set<String>>(emptySet())
    val selectedDamages = _selectedDamages.asStateFlow()

    private val _selectedRollers = MutableStateFlow<Set<String>>(emptySet())
    val selectedRollers = _selectedRollers.asStateFlow()

    val filteredInspections = combine(inspections, _searchQuery, _selectedDamages, _selectedRollers) { list, query, damages, rollers ->
        list.filter { item ->
            val matchesQuery = query.isBlank() || 
                item.opora.toString().contains(query) || 
                item.roller.contains(query, ignoreCase = true) || 
                item.damage.contains(query, ignoreCase = true)
            
            val matchesDamage = damages.isEmpty() || damages.contains(item.damage)
            val matchesRoller = rollers.isEmpty() || rollers.contains(item.roller)
            
            matchesQuery && matchesDamage && matchesRoller
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    
    fun toggleDamageFilter(damage: String) {
        val current = _selectedDamages.value
        _selectedDamages.value = if (current.contains(damage)) current - damage else current + damage
    }

    fun toggleRollerFilter(roller: String) {
        val current = _selectedRollers.value
        _selectedRollers.value = if (current.contains(roller)) current - roller else current + roller
    }

    fun resetFilters() {
        _searchQuery.value = ""
        _selectedDamages.value = emptySet()
        _selectedRollers.value = emptySet()
    }

    fun addConveyor(name: String, number: String = "") {
        viewModelScope.launch {
            repository.addConveyor(name, number)
            if (_selectedConveyor.value == null) {
                _selectedConveyor.value = name
            }
        }
    }

    fun deleteConveyor(name: String) {
        viewModelScope.launch {
            repository.deleteConveyor(name)
            if (_selectedConveyor.value == name) {
                _selectedConveyor.value = null
            }
        }
    }

    // Actions for Inspections
    fun addInspection(opora: Int, roller: String, damage: String, spare: Boolean, nearest: String?) {
        val conveyor = _selectedConveyor.value ?: return
        viewModelScope.launch {
            repository.addInspection(
                Inspection(
                    conveyorId = conveyor,
                    opora = opora,
                    roller = roller,
                    damage = damage,
                    spare = spare,
                    nearestRoller = nearest
                )
            )
        }
    }

    fun deleteInspection(inspection: Inspection, moveToHistory: Boolean) {
        viewModelScope.launch {
            repository.deleteInspection(inspection, moveToHistory)
        }
    }

    fun updateInspection(inspection: Inspection) {
        viewModelScope.launch {
            repository.updateInspection(inspection)
        }
    }

    // Actions for Replacements
    fun addReplacement(opora: Int, roller: String, reason: String) {
        val conveyor = _selectedConveyor.value ?: return
        viewModelScope.launch {
            repository.addReplacement(
                Replacement(
                    conveyorId = conveyor,
                    opora = opora,
                    roller = roller,
                    reason = reason
                )
            )
        }
    }

    fun updateReplacement(replacement: Replacement) {
        viewModelScope.launch {
            repository.updateReplacement(replacement)
        }
    }

    fun deleteReplacement(replacement: Replacement) {
        viewModelScope.launch {
            repository.deleteReplacement(replacement)
        }
    }

    // Actions for Journal
    fun addJournalEntry(text: String) {
        val conveyor = _selectedConveyor.value ?: return
        viewModelScope.launch {
            repository.addJournalEntry(BeltJournal(conveyorId = conveyor, text = text))
        }
    }

    fun updateJournalEntry(entry: BeltJournal) {
        viewModelScope.launch {
            repository.updateJournalEntry(entry)
        }
    }

    fun deleteJournalEntry(entry: BeltJournal) {
        viewModelScope.launch {
            repository.deleteJournalEntry(entry)
        }
    }

    fun exportToExcel(context: android.content.Context, type: String, onComplete: (String) -> Unit, onError: (String) -> Unit) {
        val conveyor = _selectedConveyor.value ?: return
        viewModelScope.launch {
            try {
                val timestamp = java.text.SimpleDateFormat("dd_MM_yyyy_HH_mm", java.util.Locale.getDefault()).format(java.util.Date())
                val fileName = when (type) {
                    "inspections" -> {
                        val data = inspections.value
                        val name = "inspections_${conveyor}_$timestamp.xlsx"
                        com.example.rollerapp.utils.ExcelHelper.exportInspectionsToExcel(context, data, name)
                        name
                    }
                    "replacements" -> {
                        val data = replacements.value
                        val name = "replacements_${conveyor}_$timestamp.xlsx"
                        com.example.rollerapp.utils.ExcelHelper.exportReplacementsToExcel(context, data, name)
                        name
                    }
                    else -> throw IllegalArgumentException("Unknown export type")
                }
                onComplete("Файл сохранен: $fileName")
            } catch (e: Exception) {
                onError("Ошибка экспорта: ${e.message}")
            }
        }
    }

    fun importFromText(text: String) {
        val conveyor = _selectedConveyor.value ?: return
        viewModelScope.launch {
            val lines = text.lines()
            for (line in lines) {
                if (line.isBlank()) continue
                val parts = line.trim().split(" ")
                if (parts.size >= 3) {
                    val opora = parts[0].toIntOrNull() ?: continue
                    val roller = parts[1] + " " + parts[2]
                    val damage = if (parts.size > 3) parts[3] else "Не указано"
                    repository.addInspection(
                        Inspection(
                            conveyorId = conveyor,
                            opora = opora,
                            roller = roller,
                            damage = damage,
                            spare = false,
                            timestamp = System.currentTimeMillis(),
                            lastModified = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    fun syncData(onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.syncAll()
            _isSyncing.value = false
            onResult(result)
        }
    }

    // --- Authentication actions (email/password) ---
    fun register(email: String, password: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = repository.registerWithEmail(email, password)
            if (result.isSuccess) {
                _isSignedIn.value = true
                val prefs = com.example.rollerapp.App.instance.getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
                prefs.edit().putBoolean("signed_in", true).apply()
                syncData { /* ignore */ }
            }
            onResult(result)
        }
    }

    fun signIn(email: String, password: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = repository.signInWithEmail(email, password)
            if (result.isSuccess) {
                _isSignedIn.value = true
                val prefs = com.example.rollerapp.App.instance.getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
                prefs.edit().putBoolean("signed_in", true).apply()
                syncData { /* ignore */ }
            }
            onResult(result)
        }
    }

    fun isUserSignedIn(): Boolean {
        val prefs = com.example.rollerapp.App.instance.getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
        return prefs.getBoolean("signed_in", false) || repository.isUserSignedIn()
    }

    fun signOut() {
        repository.signOut()
        _isSignedIn.value = false
        val prefs = com.example.rollerapp.App.instance.getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("signed_in", false).apply()
    }

    // Admin operations
    suspend fun clearRemoteData(): Result<Unit> {
        return repository.clearRemoteData()
    }

    suspend fun removeRemoteDuplicates(): Result<Unit> {
        return repository.removeRemoteDuplicates()
    }
}
